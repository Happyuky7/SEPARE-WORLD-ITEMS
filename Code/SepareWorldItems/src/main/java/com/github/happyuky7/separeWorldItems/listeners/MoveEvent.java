package com.github.happyuky7.separeWorldItems.listeners;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.api.events.GroupContextChangeEvent;
import com.github.happyuky7.separeWorldItems.managers.PlayerContextState;
import com.github.happyuky7.separeWorldItems.managers.PlayerContextState.Context;
import com.github.happyuky7.separeWorldItems.managers.PlayerDataSwitching;
import com.github.happyuky7.separeWorldItems.managers.PlayerSwitchQueue;
import com.github.happyuky7.separeWorldItems.managers.integrations.WorldGuardManager;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.utils.PluginSchedulers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class MoveEvent implements Listener {

    // Prevent rapid back-and-forth switches from overwriting saved data.
    private static final long MIN_SWITCH_INTERVAL_MS = 250L;
    private static final ConcurrentMap<UUID, Long> LAST_SWITCH_AT_BY_PLAYER = new ConcurrentHashMap<>();

    // Confirm switch after a short delay to avoid WG border flapping.
    private static final long MOVE_CONFIRM_DELAY_TICKS = 2L;
    private static final ConcurrentMap<UUID, PendingSwitch> PENDING_SWITCH_BY_PLAYER = new ConcurrentHashMap<>();
    private static final AtomicLong PENDING_SEQ = new AtomicLong(0L);

    private static final class PendingSwitch {
        private final long seq;
        private final Context from;
        private final Context to;

        private PendingSwitch(long seq, Context from, Context to) {
            this.seq = seq;
            this.from = from;
            this.to = to;
        }
    }

    /**
     * Used by {@code WorldChangeEvent} to avoid double-switching on the same portal/teleport.
     */
    public static boolean wasSwitchedRecently(UUID uuid, long withinMs) {
        if (uuid == null) {
            return false;
        }
        Long at = LAST_SWITCH_AT_BY_PLAYER.get(uuid);
        return at != null && (System.currentTimeMillis() - at) <= withinMs;
    }

    public static void markSwitched(UUID uuid) {
        if (uuid == null) {
            return;
        }
        LAST_SWITCH_AT_BY_PLAYER.put(uuid, System.currentTimeMillis());
    }

    private static String getRegionGroupAt(Location location) {
        return WorldGuardManager.getConfiguredRegionGroupAt(location);
    }

    private static String getWorldGroupAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return "default";
        }
        return WorldGuardManager.getWorldGroup(location.getWorld().getName());
    }

    private static Context computeContext(Location location, boolean isRegion, String group) {
        if (group == null || group.isEmpty()) {
            return new Context(false, "default");
        }
        return new Context(isRegion, group);
    }

    private static PlayerDataScope toScope(Context ctx) {
        return ctx.worldGuardRegion ? PlayerDataScope.WORLDGUARD_GROUP : PlayerDataScope.WORLD_GROUP;
    }

    private static void fireContextChangeEvent(Player player, Context from, Context to, Location regionLookupLocation) {
        String regionNow = to.worldGuardRegion ? WorldGuardManager.getConfiguredRegionAt(regionLookupLocation) : null;
        try {
            GroupContextChangeEvent.ContextType fromType = from.worldGuardRegion
                ? GroupContextChangeEvent.ContextType.WORLDGUARD_REGION
                : GroupContextChangeEvent.ContextType.WORLD;
            GroupContextChangeEvent.ContextType toType = to.worldGuardRegion
                ? GroupContextChangeEvent.ContextType.WORLDGUARD_REGION
                : GroupContextChangeEvent.ContextType.WORLD;
            SepareWorldItems.getInstance().getServer().getPluginManager().callEvent(
                new GroupContextChangeEvent(
                    player,
                    fromType,
                    from.groupName,
                    toType,
                    to.groupName,
                    regionNow
                )
            );
        } catch (Throwable ignored) {
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.debug")) {
            SepareWorldItems.getInstance().getLogger().info(
                    "[WG] " + player.getName() + " context switched " + from + " -> " + to
                            + (regionNow != null ? (" (region=" + regionNow + ")") : "")
            );
        }
    }

    private static CompletableFuture<Void> doSwitch(Player player, Context fromContext, Context toContext, Location regionLookupLocation, boolean force) {
        if (player == null || fromContext == null || toContext == null || fromContext.equals(toContext)) {
            return CompletableFuture.completedFuture(null);
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long lastAt = LAST_SWITCH_AT_BY_PLAYER.get(uuid);
        if (!force && lastAt != null && (now - lastAt) < MIN_SWITCH_INTERVAL_MS) {
            return CompletableFuture.completedFuture(null);
        }

        SepareWorldItems plugin = SepareWorldItems.getInstance();

        return PlayerSwitchQueue.enqueue(uuid, () -> {
            if (!player.isOnline()) {
                return CompletableFuture.completedFuture(null);
            }

            Context tracked = PlayerContextState.get(uuid);
            Context actualFrom = tracked != null ? tracked : fromContext;

            // Drop stale requests that would switch away from what we already applied.
            if (actualFrom.equals(toContext)) {
                return CompletableFuture.completedFuture(null);
            }

            PlayerDataScope fromScope = toScope(actualFrom);
            PlayerDataScope toScope = toScope(toContext);

            return PlayerDataSwitching.switchContext(plugin, player, fromScope, actualFrom.groupName, toScope, toContext.groupName)
                .handle((v, t) -> t)
                .thenCompose(t -> PluginSchedulers.runSyncFuture(plugin, () -> {
                    if (t != null) {
                        plugin.getLogger().warning("[Switch] Failed for " + player.getName() + ": " + t.getMessage());
                        return;
                    }

                    PlayerContextState.set(uuid, toContext);
                    markSwitched(uuid);
                    fireContextChangeEvent(player, actualFrom, toContext, regionLookupLocation != null ? regionLookupLocation : player.getLocation());
                }));
        });
    }

    private static Context computeCurrentContext(Location location) {
        String regionGroup = getRegionGroupAt(location);
        if (regionGroup != null) {
            return computeContext(location, true, regionGroup);
        }
        return computeContext(location, false, getWorldGroupAt(location));
    }

    private static void scheduleConfirmedSwitch(Player player, Context fromContext, Context toContext) {
        if (player == null || fromContext == null || toContext == null || fromContext.equals(toContext)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        PendingSwitch existing = PENDING_SWITCH_BY_PLAYER.get(uuid);
        if (existing != null && existing.from.equals(fromContext) && existing.to.equals(toContext)) {
            return;
        }

        long seq = PENDING_SEQ.incrementAndGet();
        PENDING_SWITCH_BY_PLAYER.put(uuid, new PendingSwitch(seq, fromContext, toContext));

        Runnable job = () -> {
            PendingSwitch pending = PENDING_SWITCH_BY_PLAYER.get(uuid);
            if (pending == null || pending.seq != seq) {
                return;
            }

            // Only switch if player is still in the expected destination context.
            Context current = computeCurrentContext(player.getLocation());
            if (!pending.to.equals(current)) {
                // Context changed again; drop this pending switch.
                PENDING_SWITCH_BY_PLAYER.remove(uuid, pending);
                return;
            }

            // Ensure our tracked context matches the expected "from".
            Context tracked = PlayerContextState.get(uuid);
            if (tracked != null && !tracked.equals(pending.from)) {
                // Tracking drifted; let the next move event decide.
                PENDING_SWITCH_BY_PLAYER.remove(uuid, pending);
                return;
            }

            doSwitch(player, pending.from, pending.to, player.getLocation(), false);
            PENDING_SWITCH_BY_PLAYER.remove(uuid, pending);
        };

        SepareWorldItems plugin = SepareWorldItems.getInstance();
        if (plugin.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> job.run(), MOVE_CONFIRM_DELAY_TICKS);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, job, MOVE_CONFIRM_DELAY_TICKS);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();

        String fromRegionGroup = getRegionGroupAt(from);
        String toRegionGroup = getRegionGroupAt(to);

        boolean fromInRegion = fromRegionGroup != null;
        boolean toInRegion = toRegionGroup != null;

        // If neither side is a configured WG region, do nothing here.
        // World changes are handled by WorldChangeEvent.
        if (!fromInRegion && !toInRegion) {
            // Keep tracking aligned with the world-group so a later region-entry can switch correctly.
            Context worldCtx = computeContext(to, false, getWorldGroupAt(to));
            PlayerContextState.set(uuid, worldCtx);
            return;
        }

        Context fromContext = fromInRegion
            ? computeContext(from, true, fromRegionGroup)
            : computeContext(from, false, getWorldGroupAt(from));

        Context toContext = toInRegion
            ? computeContext(to, true, toRegionGroup)
            : computeContext(to, false, getWorldGroupAt(to));

        // Keep tracking aligned with actual from-context.
        PlayerContextState.set(uuid, fromContext);

        // Teleport is a strong signal; drop any pending move-based switch.
        PENDING_SWITCH_BY_PLAYER.remove(uuid);

        doSwitch(player, fromContext, toContext, to, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        // No-op by design: world-only switching is handled by WorldChangeEvent.
        // Region enter/leave is handled by MoveEvent via move/teleport.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {


        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Ignore micro-moves inside the same block in the same world.
        // IMPORTANT: do NOT ignore cross-world teleports even if block coords match.
        if (from.getWorld() != null && to.getWorld() != null
            && from.getWorld().equals(to.getWorld())
            && from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();

        String fromRegionGroup = getRegionGroupAt(from);
        String toRegionGroup = getRegionGroupAt(to);

        boolean fromInRegion = fromRegionGroup != null;
        boolean toInRegion = toRegionGroup != null;

        // If neither side is a configured WG region, do nothing.
        // World/group switching is handled by WorldChangeEvent.
        if (!fromInRegion && !toInRegion) {
            // Keep tracking aligned with the current world-group so region entry works later.
            Context worldCtx = computeContext(to, false, getWorldGroupAt(to));
            PlayerContextState.set(uuid, worldCtx);
            return;
        }

        final Context fromContext = fromInRegion
            ? computeContext(from, true, fromRegionGroup)
            : computeContext(from, false, getWorldGroupAt(from));

        final Context toContext = toInRegion
            ? computeContext(to, true, toRegionGroup)
            : computeContext(to, false, getWorldGroupAt(to));

        // Align tracking to the actual from-context (important after world changes).
        Context tracked = PlayerContextState.putIfAbsent(uuid, fromContext);
        if (tracked != null && !tracked.equals(fromContext)) {
            PlayerContextState.set(uuid, fromContext);
        }

        if (fromContext.equals(toContext)) {
            return;
        }

        // Confirm after a short delay to avoid WG border flapping.
        scheduleConfirmedSwitch(player, fromContext, toContext);
    }
}

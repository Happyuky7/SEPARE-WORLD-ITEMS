package com.github.happyuky7.swi.common.switching;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.event.EventBus;
import com.github.happyuky7.swi.api.event.GroupSwitchPostEvent;
import com.github.happyuky7.swi.api.event.GroupSwitchPreEvent;
import com.github.happyuky7.swi.api.event.PlayerDataLoadEvent;
import com.github.happyuky7.swi.api.event.PlayerDataSaveEvent;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.api.section.DataSectionRegistry;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.platform.SwiPlatform;
import com.github.happyuky7.swi.common.storage.PlayerDataStore;
import com.github.happyuky7.swi.common.yaml.YamlCodec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Platform-independent core: save the live state into the old context, clear, load the new context.
 *
 * <p>Order of operations (same guarantees as the Bukkit plugin):</p>
 * <ol>
 *     <li>Read both records (async for remote backends).</li>
 *     <li>On the server thread: write live state into the old record, clear, apply the new record.</li>
 *     <li>Persist the old record. Local backends persist before clearing, so a failed write keeps the
 *     player's items; remote backends persist afterwards and dump a recovery file if that fails.</li>
 * </ol>
 */
public final class SwitchEngine<P> {

    private final SwiPlatform platform;
    private final PlayerAdapter<P> players;
    private final Supplier<SwiSettings> settings;
    private final Supplier<PlayerDataStore> store;
    private final EventBus events;
    private final DataSectionRegistry customSections;
    private final List<SectionHandler<P>> builtins;
    private final Consumer<DataNode> formatStamp;
    private final ContextTracker tracker = new ContextTracker();
    private final SwitchQueue queue = new SwitchQueue();
    private volatile Consumer<DataNode> recordValidator = record -> { };

    /**
     * @param formatStamp writes {@code swi.*} metadata (format, data version) into every saved record
     */
    public SwitchEngine(SwiPlatform platform, PlayerAdapter<P> players, Supplier<SwiSettings> settings,
                        Supplier<PlayerDataStore> store, EventBus events, DataSectionRegistry customSections,
                        List<SectionHandler<P>> builtins, Consumer<DataNode> formatStamp) {
        this.platform = platform;
        this.players = players;
        this.settings = settings;
        this.store = store;
        this.events = events;
        this.customSections = customSections;
        this.builtins = new ArrayList<>(builtins);
        this.formatStamp = formatStamp;
    }

    /**
     * Called with every record read before anything is changed on the player; throw to abort the switch
     * (e.g. a record written by another platform whose items cannot be read here).
     */
    public void setRecordValidator(Consumer<DataNode> validator) {
        this.recordValidator = validator;
    }

    public ContextTracker tracker() {
        return tracker;
    }

    /**
     * Starts tracking a player that just joined; their live state belongs to {@code current}.
     */
    public void onJoin(P player, GroupContext current) {
        tracker.set(players.uuid(player), current);
    }

    public void onQuit(P player) {
        UUID uuid = players.uuid(player);
        // Let pending switches finish before forgetting the context.
        queue.enqueue(uuid, () -> {
            tracker.remove(uuid);
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Moves the player's data to {@code to}. Safe to call from the server thread; runs queued per player.
     *
     * @return completes when the switch (including persistence) finished, or immediately if nothing to do
     */
    public CompletableFuture<Void> switchTo(P player, GroupContext to, String regionId) {
        UUID uuid = players.uuid(player);
        return queue.enqueue(uuid, () -> runSync(() -> prepare(player, to, regionId))
                .thenCompose(plan -> plan == null ? CompletableFuture.<Void>completedFuture(null) : execute(player, plan)))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        platform.logger().error("[Switch] Failed for " + players.name(player) + " -> " + to, unwrap(t));
                    }
                });
    }

    private static final class Plan {
        final GroupContext from;
        final GroupContext to;
        final String regionId;

        Plan(GroupContext from, GroupContext to, String regionId) {
            this.from = from;
            this.to = to;
            this.regionId = regionId;
        }
    }

    private Plan prepare(P player, GroupContext to, String regionId) {
        UUID uuid = players.uuid(player);
        if (!players.isOnline(player)) {
            return null;
        }
        GroupContext from = tracker.get(uuid);
        if (from == null) {
            tracker.set(uuid, to);
            return null;
        }
        if (from.equals(to)) {
            return null;
        }
        GroupSwitchPreEvent pre = events.post(new GroupSwitchPreEvent(player, uuid, players.name(player), from, to, regionId));
        if (pre.isCancelled()) {
            platform.logger().debug("[Switch] " + players.name(player) + " " + from + " -> " + to + " cancelled by an addon");
            return null;
        }
        return new Plan(from, to, regionId);
    }

    private CompletableFuture<Void> execute(P player, Plan plan) {
        UUID uuid = players.uuid(player);
        String name = players.name(player);
        PlayerDataStore st = store.get();

        if (!st.type().isRemote()) {
            return runSync(() -> {
                DataNode fromRecord = st.load(plan.from, uuid);
                DataNode toRecord = st.load(plan.to, uuid);
                recordValidator.accept(fromRecord);
                recordValidator.accept(toRecord);
                writeLiveState(player, plan.from, fromRecord);
                st.save(plan.from, uuid, name, fromRecord);
                swapIn(player, plan, toRecord);
                return null;
            });
        }

        CompletableFuture<DataNode> fromF = runAsync(() -> st.load(plan.from, uuid));
        CompletableFuture<DataNode> toF = runAsync(() -> st.load(plan.to, uuid));
        return fromF.thenCombine(toF, (fromRecord, toRecord) -> new DataNode[]{fromRecord, toRecord})
                .thenCompose(records -> runSync(() -> {
                    if (!players.isOnline(player)) {
                        return null;
                    }
                    recordValidator.accept(records[0]);
                    recordValidator.accept(records[1]);
                    writeLiveState(player, plan.from, records[0]);
                    swapIn(player, plan, records[1]);
                    return records[0];
                }))
                .thenCompose(fromRecord -> fromRecord == null ? CompletableFuture.<Void>completedFuture(null)
                        : runAsync(() -> {
                    try {
                        st.save(plan.from, uuid, name, fromRecord);
                    } catch (RuntimeException e) {
                        writeRecovery(uuid, plan.from, fromRecord);
                        throw e;
                    }
                    return null;
                }));
    }

    private void swapIn(P player, Plan plan, DataNode toRecord) {
        UUID uuid = players.uuid(player);
        clearAll(player);
        apply(player, plan.to, toRecord);
        tracker.set(uuid, plan.to);
        events.post(new GroupSwitchPostEvent(player, uuid, players.name(player), plan.from, plan.to, plan.regionId));
        platform.logger().debug("[Switch] " + players.name(player) + " " + plan.from + " -> " + plan.to);
    }

    /**
     * Writes every enabled section of the player's live state into {@code record} (keeps keys of disabled sections).
     */
    public void writeLiveState(P player, GroupContext context, DataNode record) {
        SwiSettings s = settings.get();
        for (SectionHandler<P> h : handlers()) {
            if (s.isSaveEnabled(context.group(), h.toggleKey(), h.enabledByDefault())) {
                try {
                    h.save(player, record);
                } catch (RuntimeException e) {
                    platform.logger().error("[Switch] Section '" + h.toggleKey() + "' failed to save for " + players.name(player), e);
                }
            }
        }
        formatStamp.accept(record);
        events.post(new PlayerDataSaveEvent(player, players.uuid(player), players.name(player), context, record));
    }

    public void apply(P player, GroupContext context, DataNode record) {
        events.post(new PlayerDataLoadEvent(player, players.uuid(player), players.name(player), context, record));
        SwiSettings s = settings.get();
        for (SectionHandler<P> h : handlers()) {
            if (s.isSaveEnabled(context.group(), h.toggleKey(), h.enabledByDefault())) {
                try {
                    h.load(player, record);
                } catch (RuntimeException e) {
                    platform.logger().error("[Switch] Section '" + h.toggleKey() + "' failed to load for " + players.name(player), e);
                }
            }
        }
    }

    /**
     * Resets every section, enabled or not (a disabled section is not carried between groups).
     */
    public void clearAll(P player) {
        for (SectionHandler<P> h : handlers()) {
            try {
                h.clear(player);
            } catch (RuntimeException e) {
                platform.logger().error("[Switch] Section '" + h.toggleKey() + "' failed to clear for " + players.name(player), e);
            }
        }
    }

    private List<SectionHandler<P>> handlers() {
        List<SectionHandler<P>> all = new ArrayList<>(builtins);
        for (DataSection<?> section : customSections.all()) {
            CustomSectionHandler<P> h = CustomSectionHandler.adapt(section, players.playerType());
            if (h != null) {
                all.add(h);
            }
        }
        return all;
    }

    private void writeRecovery(UUID uuid, GroupContext context, DataNode record) {
        File file = new File(platform.dataFolder(), "recovery" + File.separator
                + uuid + "-" + context.type() + "-" + context.group() + "-" + System.currentTimeMillis() + ".yml");
        try {
            YamlCodec.write(file, record);
            platform.logger().warn("[Switch] Could not persist data of " + uuid + "; a copy was written to " + file);
        } catch (Exception e) {
            platform.logger().error("[Switch] Could not write recovery file " + file, e);
        }
    }

    private <T> CompletableFuture<T> runSync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, platform.syncExecutor());
    }

    private <T> CompletableFuture<T> runAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, platform.asyncExecutor());
    }

    private static Throwable unwrap(Throwable t) {
        while ((t instanceof java.util.concurrent.CompletionException || t instanceof java.util.concurrent.ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}

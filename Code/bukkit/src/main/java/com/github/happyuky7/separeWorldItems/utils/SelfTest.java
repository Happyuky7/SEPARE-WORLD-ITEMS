package com.github.happyuky7.separeWorldItems.utils;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.integrations.WorldGuardManager;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.api.SepareWorldItemsApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side integration test, enabled with {@code -Dsepareworlditems.selftest=true}.
 *
 * <p>Checks the API registration, a storage round trip of a Bukkit item and, when WorldGuard is installed
 * with a region named {@code swi_selftest} around 0,0 in the first world mapped to a configured region group,
 * the region lookup. Logs {@code [SelfTest] PASSED/FAILED} and shuts the server down.</p>
 */
public final class SelfTest {

    public static final String PROPERTY = "separeworlditems.selftest";

    private SelfTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void schedule(SepareWorldItems plugin) {
        PluginSchedulers.runSyncLater(plugin, () -> run(plugin), 40L);
    }

    private static void run(SepareWorldItems plugin) {
        List<String> failures = new ArrayList<>();
        try {
            SepareWorldItemsApi api = com.github.happyuky7.swi.api.SepareWorldItems.get();
            check(failures, api.platform() == Platform.BUKKIT, "API platform is BUKKIT");
            plugin.getLogger().info("[SelfTest] API " + api.version() + " on Minecraft " + api.minecraftVersion());

            // Storage round trip with a real Bukkit ItemStack.
            PlayerDataStore store = plugin.getPlayerDataStore();
            UUID uuid = UUID.randomUUID();
            YamlConfiguration cfg = new YamlConfiguration();
            ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, 1);
            cfg.set("inventory.0", stack);
            cfg.set("exp-level", 7);
            store.save(PlayerDataScope.WORLD_GROUP, "swi_selftest", uuid, "SelfTest", cfg);
            YamlConfiguration loaded = store.load(PlayerDataScope.WORLD_GROUP, "swi_selftest", uuid);
            check(failures, stack.equals(loaded.getItemStack("inventory.0")), "item survives a storage round trip");
            check(failures, loaded.getInt("exp-level") == 7, "scalar survives a storage round trip");

            // WorldGuard region lookup (6.x or 7.x).
            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                World world = Bukkit.getWorlds().get(0);
                check(failures, WorldGuardManager.isWorldGuardEnabled(), "WorldGuard integration active (supported version detected)");
                String inside = WorldGuardManager.getConfiguredRegionAt(new Location(world, 0.5, 64, 0.5));
                String outside = WorldGuardManager.getConfiguredRegionAt(new Location(world, 500.5, 64, 500.5));
                check(failures, "swi_selftest".equals(inside), "region swi_selftest found at 0,64,0 (got " + inside + ")");
                check(failures, outside == null, "no region at 500,64,500 (got " + outside + ")");
                check(failures, "selftestgroup".equals(WorldGuardManager.getConfiguredRegionGroupAt(new Location(world, 0.5, 64, 0.5))),
                        "region group resolved");
            } else {
                java.io.File[] wgJars = plugin.getDataFolder().getParentFile().listFiles(
                        (dir, name) -> name.toLowerCase(java.util.Locale.ROOT).startsWith("worldguard") && name.endsWith(".jar"));
                check(failures, wgJars == null || wgJars.length == 0, "WorldGuard jar is present but the plugin did not load");
                plugin.getLogger().info("[SelfTest] WorldGuard not enabled, region checks skipped");
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("[SelfTest] crashed: " + t);
            t.printStackTrace();
            failures.add("crash: " + t);
        }

        if (failures.isEmpty()) {
            plugin.getLogger().info("[SelfTest] PASSED on " + Bukkit.getVersion());
        } else {
            for (String f : failures) {
                plugin.getLogger().warning("[SelfTest] FAIL " + f);
            }
            plugin.getLogger().warning("[SelfTest] FAILED (" + failures.size() + ") on " + Bukkit.getVersion());
        }
        Bukkit.shutdown();
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) {
            failures.add(what);
        }
    }
}

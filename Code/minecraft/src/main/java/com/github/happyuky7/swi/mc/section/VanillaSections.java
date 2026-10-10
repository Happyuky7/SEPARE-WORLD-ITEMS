package com.github.happyuky7.swi.mc.section;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.data.PlayerSnapshot;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.switching.SectionHandler;
import com.github.happyuky7.swi.mc.compat.Effects;
import com.github.happyuky7.swi.mc.compat.ItemCodec;
import com.github.happyuky7.swi.mc.compat.Mc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Built-in sections shared by the Fabric, NeoForge and Forge mods (vanilla Mojang-named classes only). Keys and toggles match the Bukkit plugin
 * ({@code settings.options.saves.<toggle>}); items are stored as SNBT strings.
 */
public final class VanillaSections {

    /** Vanilla slot layout of {@link Inventory#getItem(int)}: 0-35 main, 36-39 armor, 40 off-hand. */
    private static final int MAIN_SLOTS = 36;
    private static final String[] ARMOR_KEYS = {"boots", "leggings", "chestplate", "helmet"};
    private static final int OFF_HAND_SLOT = 40;

    public static final List<String> TOGGLES = Collections.unmodifiableList(Arrays.asList(
            "gamemode", "flying", "fly-speed", "exp", "enderchest", "inventory", "potion-effects", "food-level", "off-hand", "health"));

    private VanillaSections() {
    }

    public static List<SectionHandler<ServerPlayer>> create(Supplier<SwiSettings> settings) {
        List<SectionHandler<ServerPlayer>> list = new ArrayList<>();
        list.add(new Gamemode());
        list.add(new Flying());
        list.add(new FlySpeed());
        list.add(new Exp());
        list.add(new EnderChest());
        list.add(new MainInventory());
        list.add(new PotionEffects());
        list.add(new Food());
        list.add(new OffHand());
        list.add(new Health(settings));
        return list;
    }

    /**
     * Marks records written by a mod so readers know items are SNBT.
     *
     * @param platformId "fabric", "neoforge" or "forge"
     */
    public static java.util.function.Consumer<DataNode> stamper(String platformId) {
        return record -> {
            record.set("swi.format", PlayerSnapshot.FORMAT_SNBT);
            record.set("swi.platform", platformId);
            record.set("swi.data-version", Mc.dataVersion());
        };
    }

    static final class Gamemode implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "gamemode"; }

        public void save(ServerPlayer p, DataNode r) {
            r.set("gamemode", Mc.gameMode(p).name());
        }

        public void load(ServerPlayer p, DataNode r) {
            String mode = r.getString("gamemode");
            if (mode != null) {
                try {
                    p.setGameMode(GameType.valueOf(mode.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // unknown value: keep current mode
                }
            }
        }

        public void clear(ServerPlayer p) {
            p.setGameMode(GameType.SURVIVAL);
        }
    }

    static final class Flying implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "flying"; }

        public void save(ServerPlayer p, DataNode r) {
            Abilities a = Mc.abilities(p);
            r.set("allow_flight", a.mayfly);
            r.set("flying", a.flying);
        }

        public void load(ServerPlayer p, DataNode r) {
            Abilities a = Mc.abilities(p);
            if (r.contains("allow_flight")) a.mayfly = r.getBoolean("allow_flight", false);
            if (r.contains("flying")) a.flying = a.mayfly && r.getBoolean("flying", false);
            p.onUpdateAbilities();
        }

        public void clear(ServerPlayer p) {
            GameType mode = Mc.gameMode(p);
            Abilities a = Mc.abilities(p);
            // Creative/spectator always may fly; setGameMode already restored their abilities.
            if (mode != GameType.CREATIVE && mode != GameType.SPECTATOR) {
                a.mayfly = false;
                a.flying = false;
                p.onUpdateAbilities();
            }
        }
    }

    static final class FlySpeed implements SectionHandler<ServerPlayer> {
        /** Bukkit exposes fly speed doubled (default 0.1); keep that scale for shared records. */
        private static final float BUKKIT_SCALE = 2f;

        public String toggleKey() { return "fly-speed"; }

        public void save(ServerPlayer p, DataNode r) {
            r.set("fly_speed", Math.round(Mc.abilities(p).getFlyingSpeed() * BUKKIT_SCALE * 10000d) / 10000d);
        }

        public void load(ServerPlayer p, DataNode r) {
            if (r.contains("fly_speed")) {
                float speed = r.getFloat("fly_speed", 0.1f) / BUKKIT_SCALE;
                Mc.setFlyingSpeed(p, Math.max(0f, Math.min(0.5f, speed)));
                p.onUpdateAbilities();
            }
        }

        public void clear(ServerPlayer p) {
            Mc.setFlyingSpeed(p, 0.05f);
            p.onUpdateAbilities();
        }
    }

    static final class Exp implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "exp"; }

        public void save(ServerPlayer p, DataNode r) {
            r.set("exp", (double) p.experienceProgress);
            r.set("exp-level", p.experienceLevel);
            r.set("exp-total", p.totalExperience);
        }

        public void load(ServerPlayer p, DataNode r) {
            if (r.contains("exp-level")) {
                p.experienceProgress = Math.max(0f, Math.min(1f, r.getFloat("exp", 0f)));
                p.totalExperience = r.getInt("exp-total", p.totalExperience);
                // setExperienceLevels also forces the client to resync the XP bar.
                p.setExperienceLevels(Math.max(0, r.getInt("exp-level", 0)));
            }
        }

        public void clear(ServerPlayer p) {
            p.experienceProgress = 0f;
            p.totalExperience = 0;
            p.setExperienceLevels(0);
        }
    }

    static final class EnderChest implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "enderchest"; }

        public void save(ServerPlayer p, DataNode r) {
            writeContainer(p, p.getEnderChestInventory(), 0, p.getEnderChestInventory().getContainerSize(), r, "ender-chest");
        }

        public void load(ServerPlayer p, DataNode r) {
            readContainer(p, p.getEnderChestInventory(), 0, p.getEnderChestInventory().getContainerSize(), r, "ender-chest");
        }

        public void clear(ServerPlayer p) {
            p.getEnderChestInventory().clearContent();
        }
    }

    /** Main inventory + armor, like the plugin's "inventory" toggle. */
    static final class MainInventory implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "inventory"; }

        public void save(ServerPlayer p, DataNode r) {
            Inventory inv = Mc.inventory(p);
            writeContainer(p, inv, 0, MAIN_SLOTS, r, "inventory");
            r.remove("armor_contents");
            for (int i = 0; i < ARMOR_KEYS.length; i++) {
                String snbt = ItemCodec.encode(p, inv.getItem(MAIN_SLOTS + i));
                if (snbt != null) r.set("armor_contents." + ARMOR_KEYS[i], snbt);
            }
        }

        public void load(ServerPlayer p, DataNode r) {
            Inventory inv = Mc.inventory(p);
            readContainer(p, inv, 0, MAIN_SLOTS, r, "inventory");
            for (int i = 0; i < ARMOR_KEYS.length; i++) {
                inv.setItem(MAIN_SLOTS + i, ItemCodec.decode(p, r.get("armor_contents." + ARMOR_KEYS[i])));
            }
            inv.setChanged();
        }

        public void clear(ServerPlayer p) {
            Inventory inv = Mc.inventory(p);
            for (int i = 0; i < OFF_HAND_SLOT; i++) {
                inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
            inv.setChanged();
        }
    }

    static final class OffHand implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "off-hand"; }

        public void save(ServerPlayer p, DataNode r) {
            r.set("off_hand_item", ItemCodec.encode(p, Mc.inventory(p).getItem(OFF_HAND_SLOT)));
        }

        public void load(ServerPlayer p, DataNode r) {
            Mc.inventory(p).setItem(OFF_HAND_SLOT, ItemCodec.decode(p, r.get("off_hand_item")));
        }

        public void clear(ServerPlayer p) {
            Mc.inventory(p).setItem(OFF_HAND_SLOT, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    static final class PotionEffects implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "potion-effects"; }

        public void save(ServerPlayer p, DataNode r) {
            r.remove("potion_effect");
            int i = 0;
            for (MobEffectInstance e : p.getActiveEffects()) {
                String id = Effects.id(e);
                if (id == null) continue;
                String base = "potion_effect." + i++;
                r.set(base + ".type", id);
                r.set(base + ".duration", e.getDuration());
                r.set(base + ".amplifier", e.getAmplifier());
                r.set(base + ".ambient", e.isAmbient());
                r.set(base + ".particles", e.isVisible());
                r.set(base + ".icon", e.showIcon());
            }
        }

        public void load(ServerPlayer p, DataNode r) {
            DataNode effects = r.getNode("potion_effect");
            if (effects == null) return;
            for (String key : effects.keys()) {
                DataNode e = effects.getNode(key);
                if (e == null) continue;
                MobEffectInstance inst = Effects.create(normalizeId(e.getString("type")), e.getInt("duration", 0),
                        e.getInt("amplifier", 0), e.getBoolean("ambient", false), e.getBoolean("particles", true), e.getBoolean("icon", true));
                if (inst != null) p.addEffect(inst);
            }
        }

        public void clear(ServerPlayer p) {
            p.removeAllEffects();
        }

        /** Bukkit records store names like "SPEED"; turn them into ids. */
        private static String normalizeId(String type) {
            if (type == null || type.indexOf(':') >= 0) return type;
            return "minecraft:" + type.toLowerCase(Locale.ROOT);
        }
    }

    static final class Food implements SectionHandler<ServerPlayer> {
        public String toggleKey() { return "food-level"; }

        public void save(ServerPlayer p, DataNode r) {
            r.set("hunger", p.getFoodData().getFoodLevel());
            r.set("saturation", (double) p.getFoodData().getSaturationLevel());
        }

        public void load(ServerPlayer p, DataNode r) {
            if (r.contains("hunger")) p.getFoodData().setFoodLevel(r.getInt("hunger", 20));
            if (r.contains("saturation")) Mc.setSaturation(p, r.getFloat("saturation", 5f));
        }

        public void clear(ServerPlayer p) {
            p.getFoodData().setFoodLevel(20);
            Mc.setSaturation(p, 5f);
        }
    }

    /** settings.health-options.type: VANILLA (alias BUKKIT) handles health; CUSTOM leaves it to other mods. */
    static final class Health implements SectionHandler<ServerPlayer> {
        private final Supplier<SwiSettings> settings;

        Health(Supplier<SwiSettings> settings) {
            this.settings = settings;
        }

        public String toggleKey() { return "health"; }

        private boolean vanilla() {
            return "VANILLA".equals(settings.get().healthType());
        }

        public void save(ServerPlayer p, DataNode r) {
            if (!vanilla()) return;
            r.set("health", (double) p.getHealth());
            AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
            if (max != null) r.set("max-health", max.getBaseValue());
        }

        public void load(ServerPlayer p, DataNode r) {
            if (!vanilla()) return;
            AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
            if (max != null && r.contains("max-health")) max.setBaseValue(r.getDouble("max-health", 20d));
            if (r.contains("health")) p.setHealth((float) Math.max(0.5d, Math.min(r.getDouble("health", 20d), p.getMaxHealth())));
        }

        public void clear(ServerPlayer p) {
            if (!vanilla()) return;
            AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
            if (max != null) max.setBaseValue(20d);
            p.setHealth(p.getMaxHealth());
        }
    }

    static void writeContainer(ServerPlayer p, Container c, int from, int to, DataNode r, String key) {
        r.remove(key);
        for (int i = from; i < to; i++) {
            String snbt = ItemCodec.encode(p, c.getItem(i));
            if (snbt != null) r.set(key + "." + i, snbt);
        }
    }

    static void readContainer(ServerPlayer p, Container c, int from, int to, DataNode r, String key) {
        for (int i = from; i < to; i++) {
            c.setItem(i, ItemCodec.decode(p, r.get(key + "." + i)));
        }
    }
}

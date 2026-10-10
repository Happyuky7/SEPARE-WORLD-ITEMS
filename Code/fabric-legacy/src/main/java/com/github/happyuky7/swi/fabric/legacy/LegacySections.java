package com.github.happyuky7.swi.fabric.legacy;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.data.PlayerSnapshot;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.switching.SectionHandler;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Built-in sections for Minecraft 1.12.2 (legacy Yarn names). Same keys and toggles as the modern mod
 * and the Bukkit plugin; items are SNBT strings.
 */
final class LegacySections {

    /** 1.12.2 world data version. */
    static final int DATA_VERSION = 1343;

    private static final int MAIN_SLOTS = 36;
    private static final String[] ARMOR_KEYS = {"boots", "leggings", "chestplate", "helmet"};
    private static final int OFF_HAND_SLOT = 40;

    static final List<String> TOGGLES = Collections.unmodifiableList(Arrays.asList(
            "gamemode", "flying", "fly-speed", "exp", "enderchest", "inventory", "potion-effects", "food-level", "off-hand", "health"));

    private LegacySections() {
    }

    static List<SectionHandler<ServerPlayerEntity>> create(Supplier<SwiSettings> settings) {
        List<SectionHandler<ServerPlayerEntity>> list = new ArrayList<>();
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

    static void stamp(DataNode record) {
        record.set("swi.format", PlayerSnapshot.FORMAT_SNBT);
        record.set("swi.platform", "fabric");
        record.set("swi.data-version", DATA_VERSION);
    }

    // ---- items ----

    static String encode(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.toNbt(new NbtCompound()).toString();
    }

    static ItemStack decode(Object value) {
        if (!(value instanceof String) || ((String) value).isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(StringNbtReader.parse((String) value));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    static void writeContainer(Inventory inv, int from, int to, DataNode r, String key) {
        r.remove(key);
        for (int i = from; i < to; i++) {
            String snbt = encode(inv.getInvStack(i));
            if (snbt != null) r.set(key + "." + i, snbt);
        }
    }

    static void readContainer(Inventory inv, int from, int to, DataNode r, String key) {
        for (int i = from; i < to; i++) {
            inv.setInvStack(i, decode(r.get(key + "." + i)));
        }
    }

    // ---- sections ----

    static final class Gamemode implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "gamemode"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.set("gamemode", p.interactionManager.getGameMode().name());
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            String mode = r.getString("gamemode");
            if (mode == null) return;
            try {
                GameMode gm = GameMode.valueOf(mode.toUpperCase(Locale.ROOT));
                if (gm != GameMode.NOT_SET) p.method_3170(gm); // setGameMode
            } catch (IllegalArgumentException ignored) {
                // unknown value: keep current mode
            }
        }

        public void clear(ServerPlayerEntity p) {
            p.method_3170(GameMode.SURVIVAL);
        }
    }

    static final class Flying implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "flying"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.set("allow_flight", p.abilities.allowFlying);
            r.set("flying", p.abilities.flying);
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            PlayerAbilities a = p.abilities;
            if (r.contains("allow_flight")) a.allowFlying = r.getBoolean("allow_flight", false);
            if (r.contains("flying")) a.flying = a.allowFlying && r.getBoolean("flying", false);
            p.sendAbilitiesUpdate();
        }

        public void clear(ServerPlayerEntity p) {
            GameMode mode = p.interactionManager.getGameMode();
            if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
                p.abilities.allowFlying = false;
                p.abilities.flying = false;
                p.sendAbilitiesUpdate();
            }
        }
    }

    static final class FlySpeed implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "fly-speed"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.set("fly_speed", Math.round(p.abilities.getFlySpeed() * 2f * 10000d) / 10000d);
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            if (r.contains("fly_speed")) {
                setFlySpeed(p, Math.max(0f, Math.min(0.5f, r.getFloat("fly_speed", 0.1f) / 2f)));
            }
        }

        public void clear(ServerPlayerEntity p) {
            setFlySpeed(p, 0.05f);
        }

        /** PlayerAbilities.setFlySpeed is client-only in 1.12.2: go through NBT. */
        private static void setFlySpeed(ServerPlayerEntity p, float speed) {
            NbtCompound tag = new NbtCompound();
            p.abilities.serialize(tag);
            tag.getCompound("abilities").putFloat("flySpeed", speed);
            p.abilities.deserialize(tag);
            p.sendAbilitiesUpdate();
        }
    }

    static final class Exp implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "exp"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.set("exp", (double) p.experienceProgress);
            r.set("exp-level", p.experienceLevel);
            r.set("exp-total", p.totalExperience);
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            if (r.contains("exp-level")) {
                p.experienceProgress = Math.max(0f, Math.min(1f, r.getFloat("exp", 0f)));
                p.totalExperience = r.getInt("exp-total", p.totalExperience);
                p.experienceLevel = Math.max(0, r.getInt("exp-level", 0));
                p.incrementXp(0); // forces the XP bar resync
            }
        }

        public void clear(ServerPlayerEntity p) {
            p.experienceProgress = 0f;
            p.totalExperience = 0;
            p.experienceLevel = 0;
            p.incrementXp(0);
        }
    }

    static final class EnderChest implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "enderchest"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            writeContainer(p.getEnderChestInventory(), 0, p.getEnderChestInventory().getInvSize(), r, "ender-chest");
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            readContainer(p.getEnderChestInventory(), 0, p.getEnderChestInventory().getInvSize(), r, "ender-chest");
        }

        public void clear(ServerPlayerEntity p) {
            p.getEnderChestInventory().clear();
        }
    }

    static final class MainInventory implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "inventory"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            PlayerInventory inv = p.inventory;
            writeContainer(inv, 0, MAIN_SLOTS, r, "inventory");
            r.remove("armor_contents");
            for (int i = 0; i < ARMOR_KEYS.length; i++) {
                String snbt = encode(inv.getInvStack(MAIN_SLOTS + i));
                if (snbt != null) r.set("armor_contents." + ARMOR_KEYS[i], snbt);
            }
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            PlayerInventory inv = p.inventory;
            readContainer(inv, 0, MAIN_SLOTS, r, "inventory");
            for (int i = 0; i < ARMOR_KEYS.length; i++) {
                inv.setInvStack(MAIN_SLOTS + i, decode(r.get("armor_contents." + ARMOR_KEYS[i])));
            }
            inv.markDirty();
        }

        public void clear(ServerPlayerEntity p) {
            for (int i = 0; i < OFF_HAND_SLOT; i++) {
                p.inventory.setInvStack(i, ItemStack.EMPTY);
            }
            p.inventory.markDirty();
        }
    }

    static final class OffHand implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "off-hand"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.set("off_hand_item", encode(p.inventory.getInvStack(OFF_HAND_SLOT)));
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            p.inventory.setInvStack(OFF_HAND_SLOT, decode(r.get("off_hand_item")));
        }

        public void clear(ServerPlayerEntity p) {
            p.inventory.setInvStack(OFF_HAND_SLOT, ItemStack.EMPTY);
        }
    }

    static final class PotionEffects implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "potion-effects"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.remove("potion_effect");
            int i = 0;
            for (StatusEffectInstance e : p.getStatusEffectInstances()) {
                Identifier id = StatusEffect.REGISTRY.getIdentifier(e.getStatusEffect());
                if (id == null) continue;
                String base = "potion_effect." + i++;
                r.set(base + ".type", id.toString());
                r.set(base + ".duration", e.getDuration());
                r.set(base + ".amplifier", e.getAmplifier());
                r.set(base + ".ambient", e.isAmbient());
                r.set(base + ".particles", e.shouldShowParticles());
            }
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            DataNode effects = r.getNode("potion_effect");
            if (effects == null) return;
            for (String key : effects.keys()) {
                DataNode e = effects.getNode(key);
                if (e == null) continue;
                String type = e.getString("type");
                if (type == null) continue;
                if (type.indexOf(':') < 0) type = "minecraft:" + type.toLowerCase(Locale.ROOT);
                StatusEffect effect = StatusEffect.REGISTRY.get(new Identifier(type));
                if (effect != null) {
                    p.addStatusEffect(new StatusEffectInstance(effect, e.getInt("duration", 0), e.getInt("amplifier", 0),
                            e.getBoolean("ambient", false), e.getBoolean("particles", true)));
                }
            }
        }

        public void clear(ServerPlayerEntity p) {
            p.clearStatusEffects();
        }
    }

    static final class Food implements SectionHandler<ServerPlayerEntity> {
        public String toggleKey() { return "food-level"; }

        public void save(ServerPlayerEntity p, DataNode r) {
            r.set("hunger", p.getHungerManager().getFoodLevel());
            r.set("saturation", (double) p.getHungerManager().getSaturationLevel());
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            if (r.contains("hunger")) p.getHungerManager().setFoodLevel(r.getInt("hunger", 20));
            if (r.contains("saturation")) setSaturation(p, r.getFloat("saturation", 5f));
        }

        public void clear(ServerPlayerEntity p) {
            p.getHungerManager().setFoodLevel(20);
            setSaturation(p, 5f);
        }

        /** The saturation setter is client-only in 1.12.2: go through NBT. */
        private static void setSaturation(ServerPlayerEntity p, float value) {
            NbtCompound tag = new NbtCompound();
            p.getHungerManager().serialize(tag);
            tag.putFloat("foodSaturationLevel", value);
            p.getHungerManager().deserialize(tag);
        }
    }

    static final class Health implements SectionHandler<ServerPlayerEntity> {
        private final Supplier<SwiSettings> settings;

        Health(Supplier<SwiSettings> settings) {
            this.settings = settings;
        }

        public String toggleKey() { return "health"; }

        private boolean vanilla() {
            return "VANILLA".equals(settings.get().healthType());
        }

        private static EntityAttributeInstance max(ServerPlayerEntity p) {
            return p.getAttributeContainer().get(EntityAttributes.GENERIC_MAX_HEALTH);
        }

        public void save(ServerPlayerEntity p, DataNode r) {
            if (!vanilla()) return;
            r.set("health", (double) p.getHealth());
            if (max(p) != null) r.set("max-health", max(p).getBaseValue());
        }

        public void load(ServerPlayerEntity p, DataNode r) {
            if (!vanilla()) return;
            if (max(p) != null && r.contains("max-health")) max(p).setBaseValue(r.getDouble("max-health", 20d));
            if (r.contains("health")) p.setHealth((float) Math.max(0.5d, Math.min(r.getDouble("health", 20d), p.getMaxHealth())));
        }

        public void clear(ServerPlayerEntity p) {
            if (!vanilla()) return;
            if (max(p) != null) max(p).setBaseValue(20d);
            p.setHealth(p.getMaxHealth());
        }
    }
}

package com.github.happyuky7.swi.mc.compat;

import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;

/**
 * Every Minecraft call that changed name/shape between the supported versions goes through here,
 * so the rest of the mod stays free of version branches.
 */
public final class Mc {

    private Mc() {
    }

    public static ServerLevel level(ServerPlayer player) {
        //#if MC >= 1.21.11
        return player.level();
        //#elseif MC >= 1.20
        //$$ return player.serverLevel();
        //#else
        //$$ return player.getLevel();
        //#endif
    }

    /**
     * @return dimension id, e.g. {@code minecraft:the_nether}
     */
    public static String dimensionId(ServerLevel level) {
        //#if MC >= 1.21.11
        return level.dimension().identifier().toString();
        //#else
        //$$ return level.dimension().location().toString();
        //#endif
    }

    public static Inventory inventory(ServerPlayer player) {
        //#if MC >= 1.17
        return player.getInventory();
        //#else
        //$$ return player.inventory;
        //#endif
    }

    public static Abilities abilities(ServerPlayer player) {
        //#if MC >= 1.17
        return player.getAbilities();
        //#else
        //$$ return player.abilities;
        //#endif
    }

    /**
     * Puts a stack in the player's inventory, dropping it at their feet when full, so it is never lost.
     */
    public static void giveOrDrop(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!inventory(player).add(stack) && !stack.isEmpty()) {
            // Spawned by hand: Player#drop changed its signature several times.
            ServerLevel level = level(player);
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                    level, player.getX(), player.getY(), player.getZ(), stack);
            item.setPickUpDelay(0);
            level.addFreshEntity(item);
        }
    }

    /**
     * @return a stack of the item with that id ({@code create:wrench}), or null when no such item is registered
     */
    public static net.minecraft.world.item.ItemStack stackOf(String id, int count) {
        //#if MC >= 1.21.11
        net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
        java.util.Optional<net.minecraft.world.item.Item> item = key == null ? java.util.Optional.empty()
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key);
        //#elseif MC >= 1.19.3
        //$$ net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse(id);
        //$$ java.util.Optional<net.minecraft.world.item.Item> item = key == null ? java.util.Optional.empty()
        //$$         : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key);
        //#else
        //$$ net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse(id);
        //$$ java.util.Optional<net.minecraft.world.item.Item> item = key == null ? java.util.Optional.empty()
        //$$         : net.minecraft.core.Registry.ITEM.getOptional(key);
        //#endif
        return item.map(i -> new net.minecraft.world.item.ItemStack(i, count)).orElse(null);
    }

    public static GameType gameMode(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer();
    }

    /**
     * 1.16.5's {@code Abilities.setFlyingSpeed} is client-only (missing on dedicated servers): go through NBT there.
     */
    public static void setFlyingSpeed(ServerPlayer player, float speed) {
        //#if MC >= 1.17
        abilities(player).setFlyingSpeed(speed);
        //#else
        //$$ net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        //$$ abilities(player).addSaveData(tag);
        //$$ tag.getCompound("abilities").putFloat("flySpeed", speed);
        //$$ abilities(player).loadSaveData(tag);
        //#endif
    }

    /**
     * 1.16.5's {@code FoodData.setSaturation} is client-only: go through NBT there.
     */
    public static void setSaturation(ServerPlayer player, float saturation) {
        //#if MC >= 1.17
        player.getFoodData().setSaturation(saturation);
        //#else
        //$$ net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        //$$ player.getFoodData().addAdditionalSaveData(tag);
        //$$ tag.putFloat("foodSaturationLevel", saturation);
        //$$ player.getFoodData().readAdditionalSaveData(tag);
        //#endif
    }

    public static int dataVersion() {
        //#if MC >= 1.21.6
        return SharedConstants.getCurrentVersion().dataVersion().version();
        //#elseif MC >= 1.17
        //$$ return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        //#else
        //$$ return SharedConstants.getCurrentVersion().getWorldVersion();
        //#endif
    }

    public static String minecraftVersion() {
        //#if MC >= 1.21.6
        return SharedConstants.getCurrentVersion().name();
        //#else
        //$$ return SharedConstants.getCurrentVersion().getName();
        //#endif
    }
}

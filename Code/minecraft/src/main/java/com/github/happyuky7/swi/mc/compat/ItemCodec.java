package com.github.happyuky7.swi.mc.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
//#if MC >= 1.20.5
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
//#endif

/**
 * Item stacks <-> SNBT strings (human readable inside the YAML records).
 */
public final class ItemCodec {

    private ItemCodec() {
    }

    /**
     * @return SNBT, or null for an empty stack
     */
    public static String encode(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        //#if MC >= 1.20.5
        RegistryOps<Tag> ops = Mc.level(player).registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.encodeStart(ops, stack).result().map(Tag::toString).orElse(null);
        //#else
        //$$ return stack.save(new CompoundTag()).toString();
        //#endif
    }

    /**
     * @return the stack, or {@link ItemStack#EMPTY} when the value is missing or unreadable
     */
    public static ItemStack decode(ServerPlayer player, Object value) {
        if (!(value instanceof String) || ((String) value).isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag;
        try {
            //#if MC >= 1.21.5
            tag = TagParser.parseCompoundFully((String) value);
            //#else
            //$$ tag = TagParser.parseTag((String) value);
            //#endif
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
        //#if MC >= 1.20.5
        RegistryOps<Tag> ops = Mc.level(player).registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
        //#else
        //$$ return ItemStack.of(tag);
        //#endif
    }
}

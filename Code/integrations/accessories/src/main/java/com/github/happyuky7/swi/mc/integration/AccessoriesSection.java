//#if MC >= 1.21 && MC < 26.1
package com.github.happyuky7.swi.mc.integration;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.mc.section.SlotContainerCodec;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Accessories slots (and their cosmetic slots) saved per group.
 * Record layout: {@code custom.accessories.<slot>.items|cosmetic.<i>}. Toggle: {@code settings.options.saves.accessories}.
 */
public final class AccessoriesSection implements DataSection<ServerPlayer> {

    @Override
    public @NotNull String id() {
        return "accessories";
    }

    @Override
    public @NotNull Class<ServerPlayer> playerType() {
        return ServerPlayer.class;
    }

    @Override
    public void save(@NotNull ServerPlayer player, @NotNull DataNode out) {
        AccessoriesCapability.getOptionally(player).ifPresent(cap -> {
            for (Map.Entry<String, AccessoriesContainer> e : cap.getContainers().entrySet()) {
                SlotContainerCodec.save(player, SlotContainerCodec.of(e.getValue().getAccessories()), out.getOrCreateNode(e.getKey() + ".items"));
                SlotContainerCodec.save(player, SlotContainerCodec.of(e.getValue().getCosmeticAccessories()), out.getOrCreateNode(e.getKey() + ".cosmetic"));
            }
        });
    }

    @Override
    public void load(@NotNull ServerPlayer player, @NotNull DataNode in) {
        AccessoriesCapability.getOptionally(player).ifPresent(cap -> {
            for (Map.Entry<String, AccessoriesContainer> e : cap.getContainers().entrySet()) {
                SlotContainerCodec.load(player, SlotContainerCodec.of(e.getValue().getAccessories()), in.getNode(e.getKey() + ".items"));
                SlotContainerCodec.load(player, SlotContainerCodec.of(e.getValue().getCosmeticAccessories()), in.getNode(e.getKey() + ".cosmetic"));
            }
            for (String type : in.keys()) {
                if (!cap.getContainers().containsKey(type)) {
                    SlotContainerCodec.load(player, SlotContainerCodec.NONE, in.getNode(type + ".items"));
                    SlotContainerCodec.load(player, SlotContainerCodec.NONE, in.getNode(type + ".cosmetic"));
                }
            }
        });
    }

    /** In-game self-test hook: an item in the first Accessories slot must follow the group. */
    public static com.github.happyuky7.swi.mc.VanillaSelfTest.Hook selfTestHook() {
        return new com.github.happyuky7.swi.mc.SlotSelfTest("Accessories", p -> AccessoriesCapability.getOptionally(p)
                .flatMap(cap -> cap.getContainers().values().stream().filter(c -> c.getSize() > 0).findFirst())
                .map(c -> SlotContainerCodec.of(c.getAccessories())).orElse(null));
    }

    @Override
    public void clear(@NotNull ServerPlayer player) {
        AccessoriesCapability.getOptionally(player).ifPresent(cap -> {
            for (AccessoriesContainer c : cap.getContainers().values()) {
                SlotContainerCodec.clear(SlotContainerCodec.of(c.getAccessories()));
                SlotContainerCodec.clear(SlotContainerCodec.of(c.getCosmeticAccessories()));
            }
        });
    }
}
//#endif

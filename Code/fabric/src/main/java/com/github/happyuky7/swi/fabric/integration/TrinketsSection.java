//#if MC >= 1.18 && MC < 26.1
package com.github.happyuky7.swi.fabric.integration;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.mc.section.SlotContainerCodec;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Trinkets slots saved per group. Record layout: {@code custom.trinkets.<group>/<slot>.<i>}.
 * Toggle: {@code settings.options.saves.trinkets}.
 */
public final class TrinketsSection implements DataSection<ServerPlayer> {

    @Override
    public @NotNull String id() {
        return "trinkets";
    }

    @Override
    public @NotNull Class<ServerPlayer> playerType() {
        return ServerPlayer.class;
    }

    @Override
    public void save(@NotNull ServerPlayer player, @NotNull DataNode out) {
        TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
            for (Map.Entry<String, Map<String, TrinketInventory>> group : c.getInventory().entrySet()) {
                for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                    SlotContainerCodec.save(player, slots(slot.getValue()), out.getOrCreateNode(group.getKey() + "/" + slot.getKey()));
                }
            }
        });
    }

    @Override
    public void load(@NotNull ServerPlayer player, @NotNull DataNode in) {
        TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
            java.util.Set<String> known = new java.util.HashSet<>();
            for (Map.Entry<String, Map<String, TrinketInventory>> group : c.getInventory().entrySet()) {
                for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                    String key = group.getKey() + "/" + slot.getKey();
                    known.add(key);
                    SlotContainerCodec.load(player, slots(slot.getValue()), in.getNode(key));
                }
            }
            // Slots that no longer exist on this server: give their items back.
            for (String key : in.keys()) {
                if (!known.contains(key)) {
                    SlotContainerCodec.load(player, SlotContainerCodec.NONE, in.getNode(key));
                }
            }
        });
    }

    @Override
    public void clear(@NotNull ServerPlayer player) {
        TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
            for (Map<String, TrinketInventory> group : c.getInventory().values()) {
                for (TrinketInventory inv : group.values()) {
                    SlotContainerCodec.clear(slots(inv));
                }
            }
        });
    }

    private static SlotContainerCodec.Slots slots(TrinketInventory inv) {
        return SlotContainerCodec.of(inv);
    }
}
//#endif

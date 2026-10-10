package com.github.happyuky7.swi.api.section;

import com.github.happyuky7.swi.api.data.DataNode;
import org.jetbrains.annotations.NotNull;

/**
 * Extra player data that SepareWorldItems saves and restores per group, next to the built-in
 * inventory/XP/health... Use it to keep your plugin's or mod's own state separated per world group
 * (mana, skills, backpacks, currencies...).
 *
 * <pre>{@code
 * public final class ManaSection implements DataSection<ServerPlayer> {
 *     public String id() { return "mymod:mana"; }
 *     public Class<ServerPlayer> playerType() { return ServerPlayer.class; }
 *     public void save(ServerPlayer p, DataNode out) { out.set("value", MyMod.getMana(p)); }
 *     public void load(ServerPlayer p, DataNode in) { MyMod.setMana(p, in.getDouble("value", 0)); }
 *     public void clear(ServerPlayer p) { MyMod.setMana(p, 0); }
 * }
 * SepareWorldItems.get().sections().register(new ManaSection());
 * }</pre>
 *
 * <p>Methods run on the server thread. Each section gets its own node, stored under
 * {@code custom.<id>} in the player's record, so ids never clash with built-in keys.
 * Server owners can toggle a section per group with {@code settings.options.saves.<id>} in the config.</p>
 *
 * @param <P> platform player type ({@code org.bukkit.entity.Player} or {@code ServerPlayer})
 */
public interface DataSection<P> {

    /**
     * @return unique id, namespaced with your plugin/mod id, e.g. {@code "auraskills:mana"}.
     * Allowed characters: {@code a-z 0-9 _ - :}
     */
    @NotNull String id();

    /**
     * @return the platform player class this section handles; it is ignored on other platforms
     */
    @NotNull Class<P> playerType();

    /**
     * @return whether the section is saved when the config has no toggle for it
     */
    default boolean enabledByDefault() {
        return true;
    }

    /**
     * Writes the player's current state into {@code out} (empty node owned by this section).
     */
    void save(@NotNull P player, @NotNull DataNode out);

    /**
     * Applies saved state. {@code in} is empty when the player has no data for this section yet;
     * in that case apply your defaults.
     */
    void load(@NotNull P player, @NotNull DataNode in);

    /**
     * Resets the player's state before the destination data is loaded.
     */
    default void clear(@NotNull P player) {
    }
}

package com.github.happyuky7.swi.api.data;

import com.github.happyuky7.swi.api.context.GroupContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Saved data of one player in one {@link GroupContext}. Immutable: {@link #data()} returns a copy.
 *
 * <p>Well-known keys shared by every platform (absent when that section is disabled):</p>
 * <ul>
 *     <li>{@code gamemode} (SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR)</li>
 *     <li>{@code allow_flight}, {@code flying}, {@code fly_speed}</li>
 *     <li>{@code exp} (progress 0..1), {@code exp-level}</li>
 *     <li>{@code hunger}, {@code health}, {@code max-health}</li>
 *     <li>{@code inventory.<slot>}, {@code armor_contents.<helmet|chestplate|leggings|boots>},
 *     {@code off_hand_item}, {@code ender-chest.<slot>} (item format depends on {@link #format()})</li>
 *     <li>{@code potion_effect.<n>.type|duration|amplifier}</li>
 *     <li>{@code custom.<sectionId>...} for sections registered through the API</li>
 * </ul>
 */
public final class PlayerSnapshot {

    /** Items serialized by Bukkit's ConfigurationSerializable (plugin, historical format). */
    public static final String FORMAT_BUKKIT = "bukkit";
    /** Items serialized as SNBT strings by the Fabric mod. */
    public static final String FORMAT_SNBT = "snbt";

    private final UUID uuid;
    private final String playerName;
    private final GroupContext context;
    private final DataNode data;

    public PlayerSnapshot(@NotNull UUID uuid, @Nullable String playerName, @NotNull GroupContext context, @NotNull DataNode data) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.playerName = playerName;
        this.context = Objects.requireNonNull(context, "context");
        this.data = Objects.requireNonNull(data, "data").copy();
    }

    public @NotNull UUID uuid() {
        return uuid;
    }

    public @Nullable String playerName() {
        return playerName;
    }

    public @NotNull GroupContext context() {
        return context;
    }

    /**
     * @return a mutable copy of all saved values
     */
    public @NotNull DataNode data() {
        return data.copy();
    }

    /**
     * @return {@link #FORMAT_BUKKIT} or {@link #FORMAT_SNBT}
     */
    public @NotNull String format() {
        return data.getString("swi.format", FORMAT_BUKKIT);
    }

    /**
     * @return Minecraft data version the items were saved with, or -1 when unknown
     */
    public int dataVersion() {
        return data.getInt("swi.data-version", -1);
    }

    public @Nullable String gamemode() {
        return data.getString("gamemode");
    }

    public @Nullable Integer expLevel() {
        return data.contains("exp-level") ? data.getInt("exp-level", 0) : null;
    }

    public @Nullable Float exp() {
        return data.contains("exp") ? data.getFloat("exp", 0f) : null;
    }

    public @Nullable Double health() {
        return data.contains("health") ? data.getDouble("health", 20d) : null;
    }

    public @Nullable Integer hunger() {
        return data.contains("hunger") ? data.getInt("hunger", 20) : null;
    }

    /**
     * @return the data a custom {@link com.github.happyuky7.swi.api.section.DataSection} stored, or null
     */
    public @Nullable DataNode customSection(@NotNull String sectionId) {
        DataNode node = data.getNode("custom." + sectionId);
        return node != null ? node.copy() : null;
    }
}

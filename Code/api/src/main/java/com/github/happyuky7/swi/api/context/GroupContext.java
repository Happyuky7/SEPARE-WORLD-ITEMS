package com.github.happyuky7.swi.api.context;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Identifies one storage context: a group name plus the kind of group.
 * Every player has one independent set of data (inventory, ender chest, XP...) per context.
 */
public final class GroupContext {

    private final ContextType type;
    private final String group;

    private GroupContext(ContextType type, String group) {
        this.type = Objects.requireNonNull(type, "type");
        this.group = Objects.requireNonNull(group, "group");
    }

    public static @NotNull GroupContext of(@NotNull ContextType type, @NotNull String group) {
        return new GroupContext(type, group);
    }

    public static @NotNull GroupContext world(@NotNull String group) {
        return new GroupContext(ContextType.WORLD, group);
    }

    public static @NotNull GroupContext region(@NotNull String group) {
        return new GroupContext(ContextType.REGION, group);
    }

    public @NotNull ContextType type() {
        return type;
    }

    public @NotNull String group() {
        return group;
    }

    public boolean isRegion() {
        return type == ContextType.REGION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupContext)) return false;
        GroupContext that = (GroupContext) o;
        return type == that.type && group.equals(that.group);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + group.hashCode();
    }

    @Override
    public String toString() {
        return type + ":" + group;
    }
}

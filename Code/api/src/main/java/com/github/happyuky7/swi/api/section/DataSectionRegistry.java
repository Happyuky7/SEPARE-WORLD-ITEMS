package com.github.happyuky7.swi.api.section;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Registry of custom {@link DataSection}s. Thread-safe.
 */
public interface DataSectionRegistry {

    /**
     * @throws IllegalArgumentException if the id is invalid or already registered
     */
    void register(@NotNull DataSection<?> section);

    /**
     * @return true when a section with that id was removed
     */
    boolean unregister(@NotNull String id);

    @Nullable DataSection<?> get(@NotNull String id);

    /**
     * @return snapshot of the registered sections, in registration order
     */
    @NotNull Collection<DataSection<?>> all();
}

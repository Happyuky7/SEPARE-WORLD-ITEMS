package com.github.happyuky7.swi.common.section;

import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.api.section.DataSectionRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class SimpleSectionRegistry implements DataSectionRegistry {

    private static final Pattern ID = Pattern.compile("[a-z0-9_\\-:]{1,64}");

    private final Map<String, DataSection<?>> sections = new LinkedHashMap<>();
    private final Collection<String> reservedIds;

    /**
     * @param reservedIds built-in section ids (inventory, exp...) that addons cannot take
     */
    public SimpleSectionRegistry(Collection<String> reservedIds) {
        this.reservedIds = new ArrayList<>(reservedIds);
    }

    @Override
    public synchronized void register(@NotNull DataSection<?> section) {
        String id = section.id();
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid section id '" + id + "': use 1-64 chars of a-z 0-9 _ - :");
        }
        if (reservedIds.contains(id) || sections.containsKey(id)) {
            throw new IllegalArgumentException("Section id '" + id + "' is already registered");
        }
        sections.put(id, section);
    }

    @Override
    public synchronized boolean unregister(@NotNull String id) {
        return sections.remove(id) != null;
    }

    @Override
    public synchronized @Nullable DataSection<?> get(@NotNull String id) {
        return sections.get(id);
    }

    @Override
    public synchronized @NotNull Collection<DataSection<?>> all() {
        return Collections.unmodifiableList(new ArrayList<>(sections.values()));
    }
}

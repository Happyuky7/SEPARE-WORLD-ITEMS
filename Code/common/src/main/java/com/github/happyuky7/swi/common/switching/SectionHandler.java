package com.github.happyuky7.swi.common.switching;

import com.github.happyuky7.swi.api.data.DataNode;

/**
 * One piece of player state (inventory, XP...). Built-in handlers write the historical flat keys
 * into the record root; custom API sections are wrapped by {@link CustomSectionHandler}.
 *
 * @param <P> platform player type
 */
public interface SectionHandler<P> {

    /**
     * Key under {@code settings.options.saves} that toggles this handler.
     */
    String toggleKey();

    default boolean enabledByDefault() {
        return true;
    }

    void save(P player, DataNode record);

    void load(P player, DataNode record);

    void clear(P player);
}

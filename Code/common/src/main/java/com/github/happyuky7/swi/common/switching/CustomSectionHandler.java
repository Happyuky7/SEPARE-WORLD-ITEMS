package com.github.happyuky7.swi.common.switching;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.section.DataSection;

/**
 * Adapts an API {@link DataSection} to a {@link SectionHandler}, isolating it under {@code custom.<id>}.
 */
public final class CustomSectionHandler<P> implements SectionHandler<P> {

    private final DataSection<P> section;

    public CustomSectionHandler(DataSection<P> section) {
        this.section = section;
    }

    /**
     * @return a handler when the section targets this platform's player type, otherwise null
     */
    @SuppressWarnings("unchecked")
    public static <P> CustomSectionHandler<P> adapt(DataSection<?> section, Class<P> platformPlayer) {
        return section.playerType().isAssignableFrom(platformPlayer)
                ? new CustomSectionHandler<>((DataSection<P>) section)
                : null;
    }

    @Override
    public String toggleKey() {
        return section.id();
    }

    @Override
    public boolean enabledByDefault() {
        return section.enabledByDefault();
    }

    @Override
    public void save(P player, DataNode record) {
        DataNode out = DataNode.create();
        section.save(player, out);
        record.set("custom." + section.id(), out);
    }

    @Override
    public void load(P player, DataNode record) {
        DataNode in = record.getNode("custom." + section.id());
        section.load(player, in != null ? in : DataNode.create());
    }

    @Override
    public void clear(P player) {
        section.clear(player);
    }
}

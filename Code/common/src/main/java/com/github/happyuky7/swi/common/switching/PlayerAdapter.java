package com.github.happyuky7.swi.common.switching;

import java.util.UUID;

/**
 * Minimal view of a platform player needed by {@link SwitchEngine}.
 */
public interface PlayerAdapter<P> {

    Class<P> playerType();

    UUID uuid(P player);

    String name(P player);

    boolean isOnline(P player);
}

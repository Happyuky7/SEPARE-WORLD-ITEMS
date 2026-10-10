package com.github.happyuky7.swi.api.event;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancelled);
}

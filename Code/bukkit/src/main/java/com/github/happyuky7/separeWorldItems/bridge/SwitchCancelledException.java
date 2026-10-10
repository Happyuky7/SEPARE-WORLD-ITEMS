package com.github.happyuky7.separeWorldItems.bridge;

/**
 * Completes a switch future when an API listener cancelled {@link com.github.happyuky7.swi.api.event.GroupSwitchPreEvent}.
 */
public final class SwitchCancelledException extends RuntimeException {

    public SwitchCancelledException() {
        super("Group switch cancelled by an API listener", null, false, false);
    }

    public static boolean isCause(Throwable t) {
        while (t != null) {
            if (t instanceof SwitchCancelledException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}

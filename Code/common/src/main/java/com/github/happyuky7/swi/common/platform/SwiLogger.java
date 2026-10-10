package com.github.happyuky7.swi.common.platform;

/**
 * Minimal logger so common code does not depend on java.util.logging, SLF4J or Log4j.
 */
public interface SwiLogger {

    void info(String message);

    void warn(String message);

    void error(String message, Throwable error);

    /**
     * Logged only when {@code settings.debug} is enabled.
     */
    void debug(String message);
}

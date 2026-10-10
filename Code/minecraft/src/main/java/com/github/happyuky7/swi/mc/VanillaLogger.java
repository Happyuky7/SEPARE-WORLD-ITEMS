package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.common.platform.SwiLogger;

import java.util.function.BooleanSupplier;

//#if MC >= 1.18
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//#else
//$$ import org.apache.logging.log4j.LogManager;
//$$ import org.apache.logging.log4j.Logger;
//#endif

public final class VanillaLogger implements SwiLogger {

    //#if MC >= 1.18
    private static final Logger LOG = LoggerFactory.getLogger("SepareWorldItems");
    //#else
    //$$ private static final Logger LOG = LogManager.getLogger("SepareWorldItems");
    //#endif

    private final BooleanSupplier debug;

    public VanillaLogger(BooleanSupplier debug) {
        this.debug = debug;
    }

    @Override
    public void info(String message) {
        LOG.info(message);
    }

    @Override
    public void warn(String message) {
        LOG.warn(message);
    }

    @Override
    public void error(String message, Throwable error) {
        LOG.error(message, error);
    }

    @Override
    public void debug(String message) {
        if (debug.getAsBoolean()) {
            LOG.info("[debug] " + message);
        }
    }
}

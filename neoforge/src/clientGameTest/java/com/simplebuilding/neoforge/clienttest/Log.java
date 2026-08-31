package com.simplebuilding.neoforge.clienttest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every line this test writes goes both through the logger (so it lands in the run's latest.log)
 * and straight to stdout with a fixed prefix. The stdout copy is what survives the hard
 * {@code Runtime.halt} at the end of the run - log4j's appenders are never flushed on halt.
 */
final class Log {

    static final String PREFIX = "[simplebuilding-clienttest]";

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding-clienttest");

    private Log() {
    }

    static void info(String message) {
        LOGGER.info("{} {}", PREFIX, message);
        System.out.println(PREFIX + " " + message);
        System.out.flush();
    }

    static void error(String message, Throwable throwable) {
        LOGGER.error("{} {}", PREFIX, message, throwable);
        System.out.println(PREFIX + " " + message);

        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }

        System.out.flush();
    }
}

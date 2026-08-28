package me.shedaniel.autoconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shim for Cloth Config's {@code ConfigEntry} annotation holder. Only the GUI
 * markers used by SimplebuildingConfig are provided; on Forge they are inert
 * metadata (no config screen exists without cloth-config).
 */
public interface ConfigEntry {
    interface Gui {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface Tooltip {
            int count() default 1;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface CollapsibleObject {
            boolean startExpanded() default false;
        }
    }
}

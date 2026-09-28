package me.shedaniel.autoconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shim for Cloth Config's {@code ConfigEntry} annotation holder. Only the GUI
 * markers used by SimplebuildingConfig are provided; on Forge they are inert
 * metadata (no config screen exists without cloth-config). They keep Cloth's
 * names and RUNTIME retention, so ConfigOptionTests reads the same layout
 * (categories, tooltips) on all three loaders.
 */
public interface ConfigEntry {

    /** Tab of a top-level field in the config screen. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Category {
        String value();
    }

    /** Integer slider bounds. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface BoundedDiscrete {
        long min() default 0;

        long max();
    }

    /** RGB colour entry for an int field. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface ColorPicker {
        boolean allowAlpha() default false;
    }

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

        /** The object's fields are shown flat in the field's category. */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface TransitiveObject {
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface Excluded {
        }
    }
}

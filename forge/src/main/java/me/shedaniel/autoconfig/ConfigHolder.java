package me.shedaniel.autoconfig;

/** Shim for Cloth Config's {@code ConfigHolder} (Forge has no cloth-config for MC 26.x). */
@FunctionalInterface
public interface ConfigHolder<T extends ConfigData> {
    T getConfig();

    /** Cloth writes the file here; the Forge shim has no disk persistence, so nothing happens. */
    default void save() {
    }
}

package me.shedaniel.autoconfig;

/** Shim for Cloth Config's {@code ConfigHolder} (Forge has no cloth-config for MC 26.x). */
@FunctionalInterface
public interface ConfigHolder<T extends ConfigData> {
    T getConfig();
}

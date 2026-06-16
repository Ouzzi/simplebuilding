package me.shedaniel.autoconfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal stand-in for Cloth Config's {@code AutoConfig}. Cloth Config ships no
 * MinecraftForge build for the MC 26.x line, so the Forge module provides this
 * shim: it returns a single default instance per config class. There is no
 * disk persistence and no config GUI on Forge (those require cloth-config).
 */
public final class AutoConfig {
    private static final Map<Class<?>, ConfigData> INSTANCES = new ConcurrentHashMap<>();

    private AutoConfig() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends ConfigData> ConfigHolder<T> getConfigHolder(Class<T> configClass) {
        T config = (T) INSTANCES.computeIfAbsent(configClass, AutoConfig::instantiate);
        return () -> config;
    }

    private static ConfigData instantiate(Class<?> configClass) {
        try {
            return (ConfigData) configClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate config " + configClass.getName(), e);
        }
    }
}

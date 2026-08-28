package me.shedaniel.autoconfig;

/**
 * Minimal stand-in for Cloth Config's {@code ConfigData} marker interface.
 * Cloth Config ships no MinecraftForge build for the MC 26.x line, so the
 * Forge module bundles this tiny shim to keep the shared config code compiling
 * and running (with default values) on Forge.
 */
public interface ConfigData {
}

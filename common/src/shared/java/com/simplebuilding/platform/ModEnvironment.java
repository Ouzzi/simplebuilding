package com.simplebuilding.platform;

public final class ModEnvironment {
    @FunctionalInterface
    public interface ModLoadedCheck {
        boolean isModLoaded(String modId);
    }

    private static ModLoadedCheck modLoadedCheck = modId -> false;

    // Fabric: FabricLoader#isDevelopmentEnvironment, NeoForge/Forge: !FMLEnvironment.isProduction().
    // Jeder Loader setzt es beim Start; ohne Loader (Datagen) bleibt es aus.
    private static boolean developmentEnvironment = false;

    private ModEnvironment() {
    }

    public static void setModLoadedCheck(ModLoadedCheck check) {
        modLoadedCheck = check != null ? check : modId -> false;
    }

    public static boolean isModLoaded(String modId) {
        return modLoadedCheck.isModLoaded(modId);
    }

    public static void setDevelopmentEnvironment(boolean development) {
        developmentEnvironment = development;
    }

    /** Laeuft das Spiel aus einer Entwicklungsumgebung (IDE/Gradle-Lauf) statt aus einem Release? */
    public static boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }
}

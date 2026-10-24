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

    // Das Release-Jar der 26.3-Fabric-Mod laesst com.simplebuilding.gametest und .dev weg (REV24-F-14); wer die
    // Testzentrale anfasst, fragt vorher hier. Laden ohne Initialisieren, damit die Abfrage nichts ausloest.
    private static final boolean TEST_CENTRE = classPresent("com.simplebuilding.dev.testcentre.TestCentreCommand");

    /** Liegen die Klassen der Testzentrale im Spiel (Entwicklungslauf, Gametests), nicht im Release-Jar? */
    public static boolean hasTestCentre() {
        return TEST_CENTRE;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, ModEnvironment.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}

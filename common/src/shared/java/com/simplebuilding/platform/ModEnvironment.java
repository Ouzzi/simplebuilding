package com.simplebuilding.platform;

public final class ModEnvironment {
    @FunctionalInterface
    public interface ModLoadedCheck {
        boolean isModLoaded(String modId);
    }

    private static ModLoadedCheck modLoadedCheck = modId -> false;

    private ModEnvironment() {
    }

    public static void setModLoadedCheck(ModLoadedCheck check) {
        modLoadedCheck = check != null ? check : modId -> false;
    }

    public static boolean isModLoaded(String modId) {
        return modLoadedCheck.isModLoaded(modId);
    }
}

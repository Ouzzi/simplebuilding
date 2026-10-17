package com.simplebuilding.modules.simpletweaks.play;

import java.util.function.Predicate;

/** Which partner mods are loaded; the loader entry point sets the check (Fabric/NeoForge/Forge mod lists). */
public final class Mods {
    public static volatile Predicate<String> check = id -> false;
    public static boolean loaded(String id) { return check.test(id); }
    private Mods() {}
}

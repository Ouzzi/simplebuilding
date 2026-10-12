package com.simplebuilding.datafix;

/**
 * 26.2: SimpleLib (and its {@code IdMoves}) does not exist on this line; the mod's own registry alias mixins
 * ({@code MappedRegistryAliasMixin}, Forge {@code NamespacedWrapperAliasMixin} plus {@code ForgeRegistryBootstrap}
 * aliases) resolve {@link LegacyItemIds}. The 26.3 twin hands the tables to SimpleLib instead.
 */
public final class IdMovesHook {
    /** Constant (inlined by javac, read by {@link AliasMixinPlugin} before any game class loads). */
    public static final boolean LIB_ALIASES = false;

    private IdMovesHook() {}

    /** Nothing to hand over on 26.2. */
    public static void register() {
    }
}

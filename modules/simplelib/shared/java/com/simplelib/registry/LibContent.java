package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.api.IdMoves;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * VANILLA36 (owner 12.11., VANILLA27 = A; docs/ai/PLAN-A-VANILLA36-2026-11-12.md): SimpleLib registers its content - the
 * entries in registries Fabric syncs (blocks, items, data components, potion, sound) - only when an installed mod asks for
 * it, so a dedicated server with only logic mods lets Vanilla clients in.
 *
 * <p><b>Who asks.</b> A mod declares in its {@code fabric.mod.json}: {@code "custom": {"simplelib:content": [...]}} - a
 * non-empty list asks, an empty list needs nothing. A mod without the declaration asks when it names SimpleLib as a
 * dependency or its id starts with {@code simple} (every content mod keeps working unchanged). Stage 1 is all or
 * nothing: one request registers everything as before.
 *
 * <p><b>Always.</b> A physical client registers everything (singleplayer and LAN worlds stay as they were, and Fabric
 * accepts extra client entries when it joins a server without them); the server switch {@code alwaysRegisterContent}
 * keeps old worlds of a server that only runs logic mods. Without content, a few old ids move to Vanilla
 * ({@link #vanillaFallbacks}) instead of vanishing. Forge and NeoForge never decide: there everything stays registered.
 */
public final class LibContent {
    /** The custom key in fabric.mod.json. */
    public static final String KEY = "simplelib:content";
    private static volatile @Nullable Boolean enabled;

    private LibContent() {}

    /** Whether SimpleLib's content is (or will be) registered; true until a loader decided otherwise. */
    public static boolean enabled() {
        Boolean on = enabled;
        return on == null || on;
    }

    /** Whether a loader made the decision (Fabric). */
    public static boolean decided() {
        return enabled != null;
    }

    /**
     * Whether one loaded mod asks for SimpleLib's content. {@code declared} = its {@value #KEY} list, null without one.
     */
    public static boolean requests(String modId, boolean dependsOnLib, @Nullable List<String> declared) {
        if (SimpleLib.MOD_ID.equals(modId)) return false;
        if (declared != null) return !declared.isEmpty();
        return dependsOnLib || modId.startsWith("simple");
    }

    /** The rule: the server switch, a physical client or any request switches the content on. */
    public static boolean needed(boolean always, boolean physicalClient, List<String> requesters) {
        return always || physicalClient || !requesters.isEmpty();
    }

    /** The loader's decision, once, before SimpleLib registers anything. */
    public static void decide(boolean always, boolean physicalClient, List<String> requesters) {
        boolean on = needed(always, physicalClient, requesters);
        enabled = on;
        if (on) {
            SimpleLib.LOG.info("SimpleLib content registered ({})", always ? "alwaysRegisterContent" : physicalClient ? "client"
                    : "asked by " + String.join(", ", requesters.subList(0, Math.min(8, requesters.size()))) + (requesters.size() > 8 ? ", ..." : ""));
        } else {
            SimpleLib.LOG.info("SimpleLib content not registered: no installed mod asks for it, Vanilla clients may join."
                    + " Old worlds with SimpleLib blocks or items: set alwaysRegisterContent=true in simplelib-server.json.");
            vanillaFallbacks();
        }
    }

    /**
     * Without content: saved ids of the content most likely in an old logic-mod world (bastion loot, the crafted guide)
     * load as their Vanilla counterparts instead of vanishing (IdMoves answer only lookups that miss).
     */
    static void vanillaFallbacks() {
        Identifier wart = SimpleLib.id("warped_wart");
        IdMoves.move(Registries.BLOCK, wart, Identifier.withDefaultNamespace("nether_wart"));
        IdMoves.move(Registries.ITEM, wart, Identifier.withDefaultNamespace("nether_wart"));
        IdMoves.move(Registries.POTION, SimpleLib.id("warped"), Identifier.withDefaultNamespace("awkward"));
        IdMoves.move(Registries.ITEM, com.simplelib.api.ModpackGuide.ITEM_ID, Identifier.withDefaultNamespace("book"));
    }
}

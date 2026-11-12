package com.simplelib.test;

import com.simplelib.SimpleLib;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibContent;
import com.simplelib.registry.LibItems;
import com.simplelib.registry.LibPotions;
import com.simplelib.registry.LibSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** VANILLA36 (docs/ai/PLAN-A-VANILLA36-2026-11-12.md): SimpleLib's content follows the installed mods' requests ({@link LibContent}). */
public final class ContentRequestTests {
    private ContentRequestTests() {}

    /** Who asks (declaration, dependency, "simple" id) and when the content is on (switch, client, any request). */
    public static void rule(GameTestHelper h) {
        h.assertTrue(!LibContent.requests(SimpleLib.MOD_ID, true, null), "SimpleLib asks for its own content");
        h.assertTrue(LibContent.requests("simplebeacons", true, null), "an undeclared mod that depends on SimpleLib does not ask");
        h.assertTrue(LibContent.requests("simplefoo", false, null), "an undeclared Simple mod (Jar-in-Jar only) does not ask");
        h.assertTrue(!LibContent.requests("othermod", false, null), "a foreign mod without SimpleLib dependency asks");
        h.assertTrue(!LibContent.requests("simpleweather", true, List.of()), "an empty declaration asks");
        h.assertTrue(LibContent.requests("othermod", false, List.of("materials")), "a declared list does not ask");
        h.assertTrue(!LibContent.needed(false, false, List.of()), "nobody asks, still on");
        h.assertTrue(LibContent.needed(true, false, List.of()), "alwaysRegisterContent ignored");
        h.assertTrue(LibContent.needed(false, true, List.of()), "a physical client must always register");
        h.assertTrue(LibContent.needed(false, false, List.of("simplebeacons")), "a request ignored");
        h.succeed();
    }

    /**
     * This run's registries match the decision: on = every SimpleLib content entry exists; off = no simplelib entry in
     * the synced registries Fabric checks for Vanilla clients.
     */
    public static void matchesRegistries(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        if (LibContent.enabled()) {
            if (LibComponents.WARM == null || BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(LibComponents.RARITY) == null) problems.add("components");
            if (LibItems.WARPED_WART == null || LibItems.MODPACK_GUIDE == null) problems.add("items");
            if (LibPotions.WARPED == null || LibSounds.LEGENDARY == null) problems.add("potion/sound");
        } else {
            for (Registry<?> registry : List.<Registry<?>>of(BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM, BuiltInRegistries.DATA_COMPONENT_TYPE,
                    BuiltInRegistries.POTION, BuiltInRegistries.SOUND_EVENT)) {
                for (Identifier id : registry.keySet()) if (SimpleLib.MOD_ID.equals(id.getNamespace())) problems.add(id.toString());
            }
        }
        h.assertTrue(problems.isEmpty(), "content " + (LibContent.enabled() ? "on" : "off") + ", but: " + problems);
        h.succeed();
    }
}

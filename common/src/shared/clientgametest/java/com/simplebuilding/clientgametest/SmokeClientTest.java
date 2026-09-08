package com.simplebuilding.clientgametest;

import java.util.ArrayList;
import java.util.List;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import net.minecraft.client.resources.language.I18n;

/**
 * Proves the client harness works - a real client boots, a world exists, the scene builds, a
 * screenshot lands - and then checks what the mod contributes to a joined client.
 *
 * <p>This is the first test written in the shared step form. It runs on all four client targets
 * from one body: Fabric and NeoForge, MC 26.2 and 1.21.11.
 */
public final class SmokeClientTest {

    private SmokeClientTest() {
    }

    /** What can be seen before any world exists: a client that booted with the mod on it. */
    public static void beforeWorld(Script script) {
        script.shot("smoke-main-menu");
    }

    /** Everything that needs a joined world. */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");
        script.shot("smoke-in-world");

        modLanguageFileReachesTheClient(script);
    }

    /**
     * The mod's language file is on the client and its entries resolve.
     *
     * <p>The control comes first and is the reason this test can say anything at all:
     * {@code I18n.get} echoes a key back when it has no entry for it, so "translated" and
     * "missing" are told apart only by that echo. If a future version returned an empty string or
     * a placeholder instead, every assertion below would pass for a client with no language file
     * at all - so the echo is asserted on a key that deliberately does not exist.
     *
     * <p><b>Not covered:</b> that other languages fall back to English. That is vanilla -
     * {@code LanguageManager} always stacks the selected language on top of {@code en_us} - and
     * the mod contributes nothing to it.
     */
    private static void modLanguageFileReachesTheClient(Script script) {
        List<String> keys = List.of(
                ModBlocks.NIHILITH_ORE.getDescriptionId(),
                ModBlocks.ASTRALIT_ORE.getDescriptionId(),
                ModItems.ASTRALIT_ORE_ITEM.getDescriptionId(),
                ModItems.NIHILITH_SHARD.getDescriptionId(),
                ModItems.ASTRALIT_DUST.getDescriptionId());

        script.act("the client language resolves the mod's keys", client -> {
            String missingKey = "block.simplebuilding.a_block_that_does_not_exist";

            if (!missingKey.equals(I18n.get(missingKey))) {
                throw new AssertionError("control failed: I18n does not echo unknown keys any more "
                        + "(it answered \"" + I18n.get(missingKey) + "\"), so \"translated\" cannot "
                        + "be told from \"missing\" the way this test does it");
            }

            List<String> untranslated = new ArrayList<>();
            for (String key : keys) {
                String text = I18n.get(key);
                if (key.equals(text) || text.isBlank()) {
                    untranslated.add(key);
                }
            }

            if (!untranslated.isEmpty()) {
                throw new AssertionError("the client language has no entry for " + untranslated);
            }
        });
    }
}

package com.simplebuilding.modules.simpletweaks.deed;

import com.simplelib.api.RecipeViewer;
import net.minecraft.gametest.framework.GameTestHelper;

/** JEI24: the deed copy (a special crafting recipe JEI cannot show itself) in "Special Ways". */
public final class DeedJeiTests {
    private DeedJeiTests() {
    }

    public static void deedCopy(GameTestHelper h) {
        h.assertTrue(RecipeViewer.special("simpletweaks").stream().anyMatch(e -> e.id().equals("simpletweaks/claim_deed_copy")
                && e.output().is(ClaimDeedItem.item())), "deed copy in Special Ways");
        h.succeed();
    }
}

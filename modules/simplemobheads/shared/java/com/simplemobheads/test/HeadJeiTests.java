package com.simplemobheads.test;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.MobDrop;
import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.drop.HeadDropCondition;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** JEI24: every head that drops by a charged creeper is in the "Mob Drops" tab, with its own item. */
public final class HeadJeiTests {
    private HeadJeiTests() {
    }

    public static void chargedCreeperHeads(GameTestHelper h) {
        List<MobDrop> drops = RecipeViewer.mobDrops(SimpleMobHeads.MOD_ID);
        int expected = 0;
        for (HeadDef head : MobHeads.heads()) {
            Identifier mob = Identifier.tryParse(head.mob());
            if (!"default".equals(head.drop()) || mob == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(mob)
                    || MobHeads.item(head.id()) == null || !HeadDropCondition.allowed(head) || !SimpleMobHeads.config().creeperDrops) continue;
            expected++;
            h.assertTrue(drops.stream().anyMatch(d -> d.id().endsWith("/" + head.id()) && d.results().get(0).is(MobHeads.item(head.id()))
                    && d.victim() == BuiltInRegistries.ENTITY_TYPE.getValue(mob)), "head " + head.id() + " missing in Mob Drops");
        }
        h.assertTrue(drops.size() == expected && expected > 0, "heads in the tab: " + drops.size() + " of " + expected);
        h.succeed();
    }
}

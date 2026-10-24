package com.simplebuilding.modules.simplemobs;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.MobDrop;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

/** JEI24: Simple Mobs' drops in the recipe viewer against the server's loot tables, the Withers' code drops, the Shellker. */
public final class MobsJeiTests {
    private MobsJeiTests() {
    }

    public static void mobDrops(GameTestHelper h) {
        List<String> problems = RecipeViewer.lootDropProblems(h.getLevel().getServer(), MobsViewer.OWNER, MobsViewer.OWNER);
        h.assertTrue(problems.isEmpty(), "loot tables without JEI entry: " + problems);
        List<MobDrop> drops = RecipeViewer.mobDrops(MobsViewer.OWNER);
        h.assertTrue(drops.stream().anyMatch(d -> d.victim() == MobsRegistry.HOLLOW_HOWLER
                && d.results().stream().anyMatch(s -> s.is(MobsRegistry.ECHO_CORE))), "howler drops the echo core");
        for (var wither : List.of(WitherMobs.BLIGHTED_WITHER, WitherMobs.ABYSSAL_WITHER)) {
            h.assertTrue(drops.stream().anyMatch(d -> d.victim() == wither && d.results().stream().anyMatch(s -> s.is(WitherMobs.HEART))),
                    "Nether Heart from " + wither);
        }
        h.assertTrue(RecipeViewer.special(MobsViewer.OWNER).stream().anyMatch(e -> e.output().is(EndNetherMobs.SHELLKER_EGG)), "Shellker dressing");
        h.succeed();
    }
}

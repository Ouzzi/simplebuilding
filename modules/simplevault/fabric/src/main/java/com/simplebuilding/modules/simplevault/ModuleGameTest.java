package com.simplebuilding.modules.simplevault;

import com.simplebuilding.modules.simplevault.test.VaultTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link VaultTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplevault:empty")
    public void registered(GameTestHelper h) {
        VaultTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void subModSwitch(GameTestHelper h) {
        VaultTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void vaultRegistered(GameTestHelper h) {
        VaultTests.ALL.get("vault_registered").accept(h);
    }

    @GameTest(structure = "simplevault:empty", maxTicks = 400)
    public void vaultBedrockHull(GameTestHelper h) {
        VaultTests.ALL.get("vault_bedrock_hull").accept(h);
    }

    @GameTest(structure = "simplevault:empty", maxTicks = 400)
    public void vaultLayoutConnected(GameTestHelper h) {
        VaultTests.ALL.get("vault_layout_connected").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void vaultLayeredBiome(GameTestHelper h) {
        VaultTests.ALL.get("vault_layered_biome").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void vaultGateCloses(GameTestHelper h) {
        VaultTests.ALL.get("vault_gate_closes").accept(h);
    }

    @GameTest(structure = "simplevault:empty", maxTicks = 400)
    public void vaultPuzzleSolvable(GameTestHelper h) {
        VaultTests.ALL.get("vault_puzzle_solvable").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void vaultBedrockHeld(GameTestHelper h) {
        VaultTests.ALL.get("vault_bedrock_held").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void vaultGapGenerated(GameTestHelper h) {
        VaultTests.ALL.get("vault_gap_generated").accept(h);
    }

    @GameTest(structure = "simplevault:empty")
    public void guideBook(GameTestHelper h) {
        VaultTests.ALL.get("guide_book").accept(h);
    }
}

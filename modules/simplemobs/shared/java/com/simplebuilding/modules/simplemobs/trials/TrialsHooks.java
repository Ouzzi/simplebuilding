package com.simplebuilding.modules.simplemobs.trials;

import com.simplelib.api.InWorldUpgrades;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-independent hooks of the trial chain. The Evil Vault (diamond block in the off hand, 5 strikes) and the Void Vault (netherite block
 * on an Evil Vault) are plain {@code simplelib:in_world_upgrades} entries ({@code data/simplemobs/in_world_upgrades/trials.json}): a hammer
 * mod's hammer or, without one, any axe strikes the vault - the same way and the same recipe-viewer page as every other in-world upgrade.
 * This handler only moves the vault's config to the new tier, because the block entity keeps the old key and loot table.
 */
public final class TrialsHooks {
    private TrialsHooks() {}

    public static void register() {
        InWorldUpgrades.registerHandler(block -> block instanceof TrialVaultBlock vault && vault.kind() != TrialKind.WEATHERED, (level, pos, to) -> {
            TrialKind kind = ((TrialVaultBlock) to).kind();
            if (!Trials.chainOn(kind)) return 0;                 // Void off: the netherite block stays where it is
            BlockState old = level.getBlockState(pos);
            level.setBlock(pos, to.withPropertiesOf(old), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof VaultBlockEntity vault) vault.setConfig(Trials.vaultConfig(kind, false));
            return 1;
        });
    }
}

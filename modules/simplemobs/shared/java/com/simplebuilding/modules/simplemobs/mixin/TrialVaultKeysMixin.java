package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.MobsConfig;
import com.simplebuilding.modules.simplemobs.trials.TrialKind;
import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import com.simplebuilding.modules.simplemobs.trials.Trials;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Evil / Void Vaults (BS27-5/6): the key of any stage opens the vault (Vanilla compares the exact stack, so the stage would have to match
 * the config's), and the loot table is the one of the key's stage ({@code simplemobs:vault/evil_3}, ...). Everything else in the
 * Vanilla vault stays: once per player and key stage is not kept - once per player and vault, like Vanilla. TR31-R: every item at most once per opening (config).
 */
@Mixin(VaultBlockEntity.Server.class)
public abstract class TrialVaultKeysMixin {
    @Inject(method = "isValidToInsert", at = @At("HEAD"), cancellable = true)
    private static void simplemobs$anyStage(VaultConfig config, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        TrialKind wanted = Trials.keyKind(config.keyItem());
        if (wanted != null) cir.setReturnValue(Trials.keyKind(stack) == wanted && Trials.chainOn(wanted) && stack.getCount() >= config.keyItem().getCount());
    }

    @Inject(method = "resolveItemsToEject", at = @At("HEAD"), cancellable = true)
    private static void simplemobs$stageLoot(ServerLevel level, VaultConfig config, BlockPos pos, Player player, ItemInstance key, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(key instanceof ItemStack stack)) return;
        TrialKind kind = Trials.keyKind(stack);
        if (kind == null) return;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(Trials.vaultLoot(kind, Trials.keyStage(stack), false));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withLuck(player.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.TOOL, key).create(LootContextParamSets.VAULT);
        cir.setReturnValue(MobsConfig.trialLootNoRepeat ? TrialsExtras.drawDistinct(() -> table.getRandomItems(params)) : table.getRandomItems(params));
    }
}

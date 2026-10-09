package com.simplebuilding.mixin;

import com.simplebuilding.util.ShulkerLids;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Die Vanilla-Shulkerkisten (alle 17 Farben) sind die erste Stufe der Shulkerkisten-Aufwertung:
 * Vorschlaghammer plus zwei Rissige Diamanten in der Nebenhand ({@code TieredShulkerBoxes}). Wie bei
 * den Kupfertruhen ({@link ChestBlockMixin}) reicht die Kiste den Rechtsklick im Schmiedestand an den
 * Hammer weiter, statt ihr Menue zu oeffnen, wenn die Aufwertung beginnen kann. Die gestuften Kisten
 * erben die Methode nicht (sie ueberschreiben sie und fragen selbst).
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$passToTheHammer(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, InteractionHand.MAIN_HAND)) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        // Shulker state (owner N17): an open-standing box closes on a right-click instead of opening its menu.
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity box && ShulkerLids.keptOpen(box)) {
            if (!level.isClientSide()) ShulkerLids.setKeptOpen(level, pos, box, false, player);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /** Shulker state: a shulker shell opens a closed placed box for good (its lid stays up). */
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.SHULKER_SHELL) && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity box
                && !ShulkerLids.keptOpen(box) && box.getAnimationStatus() == net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.AnimationStatus.CLOSED
                && com.simplebuilding.util.TransformTargets.mayTransform(level, player, pos, hit.getDirection(), stack)) {
            if (!level.isClientSide()) ShulkerLids.setKeptOpen(level, pos, box, true, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Shulker state: an open-standing box drops as an open box (Vanilla's loot copies only its own components). */
    @Inject(method = "getDrops", at = @At("RETURN"))
    private void simplebuilding$dropOpen(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (ShulkerLids.keptOpen(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY))) {
            for (ItemStack drop : cir.getReturnValue()) {
                if (drop.is(state.getBlock().asItem())) drop.set(com.simplebuilding.component.ModDataComponentTypes.SHULKER_OPEN, true);
            }
        }
    }
}

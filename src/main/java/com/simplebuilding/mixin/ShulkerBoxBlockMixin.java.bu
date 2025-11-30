package com.simplebuilding.mixin;

import com.simplebuilding.util.IEnchantableShulkerBox;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext; // WICHTIG: Dein Log sagt LootWorldContext
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;


// TODO
// Entchantments:
// - Color Palette: changes pickmode to random blocks from inventory
// - Master Builder: allows to place blocks by rightclicking (also link to wand functionality?)
// - Range: increases the range of block placement

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin extends Block {

    public ShulkerBoxBlockMixin(Settings settings) {
        super(settings);
    }

    // Wir nutzen nur getDroppedStacks, da onPlaced durch readComponents in der BlockEntity abgedeckt ist.
    @Inject(method = "getDroppedStacks", at = @At("RETURN"))
    private void restoreEnchantsToItem(BlockState state, LootWorldContext.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        // Versuche BlockEntity zu holen
        BlockEntity be = builder.getOptional(LootContextParameters.BLOCK_ENTITY);

        if (be instanceof IEnchantableShulkerBox enchantableBe) {
            ItemEnchantmentsComponent storedEnchants = enchantableBe.simplebuilding$getEnchantments();

            if (storedEnchants != null && !storedEnchants.isEmpty()) {
                List<ItemStack> drops = cir.getReturnValue();

                for (ItemStack stack : drops) {
                    // Prüfen ob es die Shulker Box ist
                    if (Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                        stack.set(DataComponentTypes.ENCHANTMENTS, storedEnchants);
                    }
                }
            }
        }
    }
}
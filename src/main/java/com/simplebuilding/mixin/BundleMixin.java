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
import net.minecraft.loot.context.LootWorldContext;
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
// - Deep Pockets: increases inventory size

@Mixin(ShulkerBoxBlock.class)
public abstract class BundleMixin extends Block {

    public BundleMixin(Settings settings) {
        super(settings);
    }

}
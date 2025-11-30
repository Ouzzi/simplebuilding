package com.simplebuilding.block.custom;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ReinforcedShulkerBoxBlock extends ShulkerBoxBlock {

    public ReinforcedShulkerBoxBlock(DyeColor color, AbstractBlock.Settings settings) {
        super(color, settings);
    }

    // Optional: Hier kannst du Interaktionen überschreiben.
    // Da dein ReinforcedShulkerBoxItem aber eine Bundle-Logik hat,
    // willst du vielleicht verhindern, dass das GUI beim Platzieren
    // mit Rechtsklick geöffnet wird, oder du lässt es wie eine normale Shulker Box.

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Ruft die Standard Shulker Box GUI auf
        return super.onUse(state, world, pos, player, hit);
    }

    // WICHTIG: Damit beim Abbauen dein Custom Item droppt und nicht die Vanilla Shulker Box
    // muss ggf. die getPickStack oder onBreak Logik angepasst werden,
    // oder du verlässt dich auf Loot Tables (empfohlen).
}
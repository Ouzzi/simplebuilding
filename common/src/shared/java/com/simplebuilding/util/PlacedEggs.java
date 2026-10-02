package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedEggBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Eier ablegen (Besitzer 2026-10-02): Schleichen + Rechtsklick mit einem Vanilla-, blauen oder braunen Ei stellt es
 * aufrecht auf einen tragenden Boden ({@link PlacedEggBlock}). Es gelten dieselben Server-Optionen wie fuer die
 * Kleinteile ({@code server.features.placeVanillaItems}, {@code placeDisabledItems}).
 */
public final class PlacedEggs {
    private PlacedEggs() {
    }

    public static boolean isPlaceableEgg(ItemStack stack) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES || PlacedEggBlock.Egg.of(stack) == null) {
            return false;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        return features.placeVanillaItems && !PlacedTemplates.itemListed(features.placeDisabledItems, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Aus {@code Item#useOn}: null, wenn nichts abgelegt wird (dann wirft das Ei wie gewohnt beim naechsten Klick). */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isSecondaryUseActive() || !isPlaceableEgg(stack) || !player.mayBuild()) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState state = ModBlocks.PLACED_EGG.getStateForPlacement(place);
        if (state == null) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, 11)) {
                return null;
            }
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.5F, 1.6F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}

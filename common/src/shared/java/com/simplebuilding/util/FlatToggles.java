package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Queue N55 (Besitzer 2026-10-15): ein Rechtsklick mit dem Vorschlaghammer auf ein platziertes Haeufchen schaltet dessen
 * unterstuetzte 3D-Teile (Barren, Ziegel, Diamanten, Smaragde; {@link PlacedSmallParts.Kind#switchable}) auf die flache
 * Item-Darstellung um, der naechste Schlag wieder auf 3D. Der Zustand ist {@link PlacedSmallPartsBlock#FLAT} im Blockzustand;
 * er gilt fuer alle umschaltbaren Teile des Flecks, Eier, Kerzen und Seegurken bleiben, wie sie sind. Ein Wechsel kostet den
 * Hammer einen Punkt Haltbarkeit (wie eine Umformung, {@link SledgehammerItem#RESHAPE_DAMAGE}) und braucht dieselben Rechte
 * wie jede Umwandlung ({@link TransformTargets#mayTransform}). Schallplatten haben Vorrang ({@link DiscFlips}); mit Schleichen schaltet der Hammer auch dort auf flach.
 *
 * <p>Auch die einzeln abgelegten Vorlagen, Scherben und Scheiben (Wand, Decke, Vorlage am Boden; {@link PlacedTemplateBlock}) schalten
 * so um ({@link PlacedTemplateBlock#FLAT}, Besitzer D7); hat der Spieler Hammer und Aufwertungs-Material in den Haenden, gilt dort
 * weiter der Schlag der Besatz-Aufwertung ({@link PlacedTemplates#isHammerStance}).
 */
public final class FlatToggles {
    private FlatToggles() {
    }

    /** Ob ein Rechtsklick mit {@code tool} auf das Haeufchen bei {@code pos} 3D/flach umschaltet. */
    public static boolean canToggle(Level level, BlockPos pos, ItemStack tool) {
        return com.simplebuilding.items.custom.SledgehammerItem.isHammer(tool.getItem()) && level.getBlockState(pos).getBlock() instanceof PlacedSmallPartsBlock
                && level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile && PlacedSmallParts.anySwitchable(pile.parts());
    }

    /** Ob ein Rechtsklick mit {@code tool} auf eine einzeln abgelegte Vorlage/Scherbe/Scheibe bei {@code pos} umschaltet. */
    public static boolean canToggleTemplate(Level level, BlockPos pos, ItemStack tool, Player player) {
        return com.simplebuilding.items.custom.SledgehammerItem.isHammer(tool.getItem()) && level.getBlockState(pos).getBlock() instanceof PlacedTemplateBlock
                && PlacedSolids.shape(PlacedTemplates.templateAt(level, pos)) != null && !PlacedTemplates.isHammerStance(player);
    }

    /** Schaltet eine einzeln abgelegte Vorlage/Scherbe/Scheibe um (Server; der Client sagt nur voraus). */
    public static boolean toggleTemplate(Level level, BlockPos pos, Player player, ItemStack tool, InteractionHand hand) {
        if (!canToggleTemplate(level, pos, tool, player) || !TransformTargets.mayTransform(level, player, pos, Direction.UP, tool)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        boolean flat = !state.getValue(PlacedTemplateBlock.FLAT);
        level.setBlock(pos, state.setValue(PlacedTemplateBlock.FLAT, flat), Block.UPDATE_ALL);
        tool.hurtAndBreak(SledgehammerItem.RESHAPE_DAMAGE, player, hand);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, flat ? 1.2F : 1.8F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
        return true;
    }

    /** Schaltet um (Server; der Client sagt nur voraus). False, wenn nichts passt oder der Spieler hier nichts aendern darf. */
    public static boolean toggle(Level level, BlockPos pos, Player player, ItemStack tool, InteractionHand hand) {
        if (!canToggle(level, pos, tool) || !TransformTargets.mayTransform(level, player, pos, Direction.UP, tool)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        boolean flat = !state.getValue(PlacedSmallPartsBlock.FLAT);
        level.setBlock(pos, state.setValue(PlacedSmallPartsBlock.FLAT, flat), Block.UPDATE_ALL);
        tool.hurtAndBreak(SledgehammerItem.RESHAPE_DAMAGE, player, hand);
        // Flach: ein dumpfer Schlag; zurueck in 3D: heller, als wuerde das Teil aufgerichtet.
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, flat ? 1.2F : 1.8F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
        return true;
    }
}

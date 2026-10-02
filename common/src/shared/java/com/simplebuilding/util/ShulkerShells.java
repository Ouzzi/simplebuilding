package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Stufen-Shulkerschalen (Besitzer 2026-10-02): Verstaerkt, Netherit, Enderit.
 *
 * <p><b>Aufwertung in der Welt.</b> Eine Schale liegt abgelegt auf dem Boden (Schleichen + Rechtsklick, sie steht im
 * Tag {@code simplebuilding:placeable_small}, siehe {@link PlacedSmallParts}). Ein Rechtsklick mit dem passenden Klumpen
 * auf das Haeufchen wertet genau eine Schale um genau eine Stufe auf und verbraucht genau einen Klumpen:
 * Shulkerschale + Eisenklumpen -&gt; Verstaerkte, Verstaerkte + Netheritklumpen -&gt; Netherit, Netherit +
 * Enderitklumpen -&gt; Enderit. Liegen mehrere passende Schalen, trifft es die zuletzt gelegte.
 * Der Handhinweis ({@link TransformTargets}) fragt {@link #canUpgrade}, die Aktion fragt dasselbe.
 */
public final class ShulkerShells {
    private ShulkerShells() {
    }

    /** Eine Stufe der Kette: Schale + Klumpen -&gt; neue Schale. */
    public record Step(Item shell, Item nugget, Item result) {
    }

    /** Die Kette, von unten nach oben (leer ohne das Feature). */
    public static List<Step> steps() {
        if (!com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS || ModItems.REINFORCED_SHULKER_SHELL == null) {
            return List.of();
        }
        return List.of(
                new Step(Items.SHULKER_SHELL, Items.IRON_NUGGET, ModItems.REINFORCED_SHULKER_SHELL),
                new Step(ModItems.REINFORCED_SHULKER_SHELL, ModItems.NETHERITE_NUGGET, ModItems.NETHERITE_SHULKER_SHELL),
                new Step(ModItems.NETHERITE_SHULKER_SHELL, ModItems.ENDERITE_NUGGET, ModItems.ENDERITE_SHULKER_SHELL));
    }

    /** Die Stufen-Schale einer Truhen-/Shulkerstufe (null ohne das Feature). */
    public static @Nullable Item shellOf(ChestTier tier) {
        if (!com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
            return null;
        }
        return switch (tier) {
            case REINFORCED -> ModItems.REINFORCED_SHULKER_SHELL;
            case NETHERITE -> ModItems.NETHERITE_SHULKER_SHELL;
            case ENDERITE -> ModItems.ENDERITE_SHULKER_SHELL;
        };
    }

    /** Die drei Stufen-Schalen (leer ohne das Feature). */
    public static List<Item> tierShells() {
        List<Item> out = new ArrayList<>();
        for (Step step : steps()) {
            out.add(step.result());
        }
        return out;
    }

    /** Was {@code nugget} aus {@code shell} macht, oder null. */
    public static @Nullable Item upgradeOf(ItemStack shell, ItemStack nugget) {
        for (Step step : steps()) {
            if (shell.is(step.shell()) && nugget.is(step.nugget())) {
                return step.result();
            }
        }
        return null;
    }

    /** Index der zuletzt gelegten Schale im Haeufchen, die {@code nugget} aufwertet, oder -1. */
    public static int targetIndex(List<ItemStack> parts, ItemStack nugget) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            if (upgradeOf(parts.get(i), nugget) != null) {
                return i;
            }
        }
        return -1;
    }

    /** Ob ein Rechtsklick mit {@code nugget} auf das Haeufchen bei {@code pos} eine Schale aufwertet. */
    public static boolean canUpgrade(Level level, BlockPos pos, ItemStack nugget) {
        if (nugget.isEmpty() || !(level.getBlockState(pos).getBlock() instanceof PlacedSmallPartsBlock)
                || !(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile)) {
            return false;
        }
        return targetIndex(pile.parts(), nugget) >= 0;
    }

    /**
     * Wertet eine Schale im Haeufchen auf (Server; der Client sagt nur voraus): ersetzt sie an Ort und Stelle, verbraucht
     * einen Klumpen (nicht im Kreativmodus). False, wenn nichts passt oder der Spieler hier nichts aendern darf.
     */
    public static boolean upgrade(Level level, BlockPos pos, Player player, ItemStack nugget) {
        if (!canUpgrade(level, pos, nugget)
                || !TransformTargets.mayTransform(level, player, pos, net.minecraft.core.Direction.UP, nugget)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) level.getBlockEntity(pos);
        List<ItemStack> parts = new ArrayList<>(pile.parts());
        int index = targetIndex(parts, nugget);
        Item result = upgradeOf(parts.get(index), nugget);
        ItemStack upgraded = new ItemStack(result);
        parts.set(index, upgraded);
        pile.setParts(parts);
        nugget.consume(1, player);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, result), pos.getX() + 0.5, pos.getY() + 0.1,
                    pos.getZ() + 0.5, 8, 0.2, 0.05, 0.2, 0.05);
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
        return true;
    }
}

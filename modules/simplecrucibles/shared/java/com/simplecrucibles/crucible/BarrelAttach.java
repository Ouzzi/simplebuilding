package com.simplecrucibles.crucible;

import com.simplelib.api.BarrelDock;
import com.simplelib.api.InWorldStrikes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * WH26-6: attaching a barrel (Simple Storage's block, found through SimpleLib's {@link BarrelDock} contract) to a crucible.
 * Next to a crucible, six strikes (axe without a hammer mod, sledgehammer with one) attach it; every strike shows breaking
 * cracks on the barrel. Without Simple Storage there is no such block and nothing here ever runs.
 */
public final class BarrelAttach {
    public static final int ATTACH_STRIKES = BarrelDock.ATTACH_STRIKES;

    private BarrelAttach() {}

    /** The crucible next to an unattached barrel, preferring the direction the barrel faces. */
    public static @Nullable Direction crucibleSide(Level level, BlockPos pos, BlockState state) {
        return BarrelDock.isBarrel(state) ? BarrelDock.hostSide(level, pos, state) : null;
    }

    /** Whether an unattached barrel stands next to a crucible without a barrel. */
    public static boolean canAttach(Level level, BlockPos pos, BlockState state) {
        if (!BarrelDock.isBarrel(state) || state.getValue(BarrelDock.ATTACHED)) return false;
        return crucibleSide(level, pos, state) != null;
    }

    /** One attach strike (axe 1 durability, sledgehammer 2); true when it counted. */
    public static boolean attachStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        BlockState state = level.getBlockState(pos);
        if (!BarrelDock.isBarrel(state) || state.getValue(BarrelDock.ATTACHED)) return false;
        Direction side = crucibleSide(level, pos, state);
        if (side == null || player.getCooldowns().isOnCooldown(tool)) return false;
        BlockPos cruciblePos = pos.relative(side);
        if (CrucibleBlockEntity.attachedBarrelPos(level, cruciblePos) != null) return false; // one barrel per crucible (owner 60)
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof BarrelDock.Barrel be)) return true;
        int done = be.addAttachStrike();
        BlockState attached = state.setValue(BarrelDock.FACING, side).setValue(BarrelDock.ATTACHED, true);
        // The shared feedback of every in-world conversion (cracks, particles, rising hit sound) plus the growing target preview.
        InWorldStrikes.feedback(server, pos, state, SoundEvents.COPPER_HIT, 0.8F, done, ATTACH_STRIKES);
        InWorldStrikes.preview(server, pos, attached, done, ATTACH_STRIKES);
        if (done >= ATTACH_STRIKES) {
            server.setBlock(pos, attached, Block.UPDATE_ALL);
            be.onAttached();
            server.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            if (server.getBlockEntity(cruciblePos) instanceof CrucibleBlockEntity crucible) crucible.markHeatDirty();
        }
        if (!player.getAbilities().instabuild) tool.hurtAndBreak(toolDamage, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(tool, CrucibleBlankBlock.STRIKE_COOLDOWN);
        return true;
    }

    /** Breaking-crack stage (0..9) shown after {@code done} of the six strikes. */
    public static int crackStage(int done) {
        return InWorldStrikes.crackStage(done, ATTACH_STRIKES);
    }

    /** Whether the axe way may attach (principle 5a). */
    public static boolean axeAllowed() {
        return com.simplelib.api.SimpleLibApi.axeWaysEnabled();
    }
}

package com.simplemachines.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import org.jetbrains.annotations.Nullable;

/**
 * The piston head of one piston tier: {@code reinforced_piston_head} (Reinforced and Reinforced Sticky, sticky through
 * {@code type=sticky} as in Vanilla), {@code netherite_piston_head} and {@code enderite_piston_head}.
 *
 * <p>All behaviour is Vanilla's {@link PistonHeadBlock}; new is only <em>which</em> base fits ({@link #fitsBase}, put into
 * Vanilla's private {@code isFittingBase} by {@code PistonHeadBlockMixin}) and the pick-block item. {@code PistonBlockMixin} sets
 * it in {@code PistonBaseBlock#moveBlocks}, the client draws it while retracting through {@code PistonHeadRendererMixin}.
 */
public class ModPistonHeadBlock extends PistonHeadBlock {
    /** The piston tier a head belongs to. */
    public enum Tier {
        REINFORCED, NETHERITE, ENDERITE
    }

    private final Tier tier;

    public ModPistonHeadBlock(Tier tier, Properties settings) {
        super(settings);
        this.tier = tier;
    }

    public Tier tier() {
        return this.tier;
    }

    /** The head a piston upgrade sets when it extends, or {@code null} for every other block (Enderite first: it extends Netherite). */
    public static @Nullable Block headFor(Block base) {
        if (base instanceof EnderitePistonBlock) {
            return PistonUpgrades.ENDERITE_PISTON_HEAD;
        }
        if (base instanceof NetheriteBreakerPistonBlock) {
            return PistonUpgrades.NETHERITE_PISTON_HEAD;
        }
        if (base instanceof ReinforcedPistonBlock) {
            return PistonUpgrades.REINFORCED_PISTON_HEAD;
        }
        return null;
    }

    /** The head type of a base: sticky only for the Reinforced Sticky Piston. */
    public static PistonType typeFor(Block base) {
        return base instanceof ReinforcedPistonBlock reinforced && reinforced.isStickyPiston()
                ? PistonType.STICKY : PistonType.DEFAULT;
    }

    /** Vanilla's {@code isFittingBase} rule for this tier: the base sets exactly this head, the type fits, extended, same facing. */
    public boolean fitsBase(BlockState arm, BlockState base) {
        return headFor(base.getBlock()) == this
                && arm.getValue(TYPE) == typeFor(base.getBlock())
                && base.getValue(PistonBaseBlock.EXTENDED)
                && base.getValue(FACING) == arm.getValue(FACING);
    }

    /** The piston the head belongs to (Vanilla gives piston or sticky piston here). */
    public Block baseBlock(PistonType type) {
        return switch (this.tier) {
            case REINFORCED -> type == PistonType.STICKY ? PistonUpgrades.REINFORCED_STICKY_PISTON : PistonUpgrades.REINFORCED_PISTON;
            case NETHERITE -> PistonUpgrades.NETHERITE_PISTON;
            case ENDERITE -> PistonUpgrades.ENDERITE_PISTON;
        };
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(baseBlock(state.getValue(TYPE)));
    }
}

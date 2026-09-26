package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
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
 * Der Kolbenkopf einer Kolbenstufe der Mod: {@code reinforced_piston_head} (verstaerkter und
 * verstaerkter klebriger Kolben, klebrig ueber {@code type=sticky} wie bei Vanilla),
 * {@code netherite_piston_head} und {@code enderite_piston_head}.
 *
 * <p>Alles Verhalten ist Vanillas {@link PistonHeadBlock}: Form (auch die kurze Form waehrend der
 * Bewegung), kein Drop, Abbauen des Kopfes bricht die Basis mit ihrem Drop (im Kreativmodus ohne),
 * ohne passende Basis verschwindet er. Neu ist nur, <em>welche</em> Basis passt
 * ({@link #fitsBase}, von {@code PistonHeadBlockMixin} in Vanillas privates
 * {@code isFittingBase} eingesetzt) und welcher Gegenstand beim Auswaehlen mit der mittleren
 * Maustaste herauskommt.
 *
 * <p>Gesetzt wird der Kopf von Vanillas {@code PistonBaseBlock#moveBlocks}; {@code PistonBlockMixin}
 * tauscht dort fuer die Kolben der Mod {@code Blocks.PISTON_HEAD} gegen {@link #headFor}. Beim
 * Einfahren zeichnet der Client den Kopf ueber {@code PistonHeadRendererMixin} ebenfalls in der
 * Stufe des Kolbens.
 */
public class ModPistonHeadBlock extends PistonHeadBlock {

    /** Die Kolbenstufe, zu der ein Kopf gehoert. */
    public enum Tier {
        REINFORCED, NETHERITE, ENDERITE
    }

    private final Tier tier;
    private final MapCodec<ModPistonHeadBlock> codec;

    public ModPistonHeadBlock(Tier tier, Properties settings) {
        super(settings);
        this.tier = tier;
        this.codec = simpleCodec(properties -> new ModPistonHeadBlock(tier, properties));
    }

    public Tier tier() {
        return this.tier;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected MapCodec<PistonHeadBlock> codec() {
        return (MapCodec<PistonHeadBlock>) (Object) this.codec;
    }

    /**
     * Der Kopf, den ein Kolben der Mod beim Ausfahren setzt, oder {@code null} fuer jeden anderen
     * Block. Der Enderitkolben erbt vom Netheritkolben und wird deshalb zuerst gefragt.
     */
    public static @Nullable Block headFor(Block base) {
        if (base instanceof EnderitePistonBlock) {
            return ModBlocks.ENDERITE_PISTON_HEAD;
        }
        if (base instanceof NetheriteBreakerPistonBlock) {
            return ModBlocks.NETHERITE_PISTON_HEAD;
        }
        if (base instanceof ReinforcedPistonBlock) {
            return ModBlocks.REINFORCED_PISTON_HEAD;
        }
        return null;
    }

    /** Der Kopftyp einer Basis: klebrig nur fuer den verstaerkten klebrigen Kolben. */
    public static PistonType typeFor(Block base) {
        return base instanceof ReinforcedPistonBlock reinforced && reinforced.isStickyPiston()
                ? PistonType.STICKY : PistonType.DEFAULT;
    }

    /**
     * Vanillas Regel aus {@code isFittingBase} fuer diese Stufe: die Basis setzt genau diesen Kopf,
     * der Kopftyp passt zu ihrer Klebrigkeit, sie ist ausgefahren und schaut in dieselbe Richtung.
     */
    public boolean fitsBase(BlockState arm, BlockState base) {
        return headFor(base.getBlock()) == this
                && arm.getValue(TYPE) == typeFor(base.getBlock())
                && base.getValue(PistonBaseBlock.EXTENDED)
                && base.getValue(FACING) == arm.getValue(FACING);
    }

    /** Der Kolben, zu dem der Kopf gehoert (Vanilla gibt hier Kolben oder klebrigen Kolben). */
    public Block baseBlock(PistonType type) {
        return switch (this.tier) {
            case REINFORCED -> type == PistonType.STICKY ? ModBlocks.REINFORCED_STICKY_PISTON : ModBlocks.REINFORCED_PISTON;
            case NETHERITE -> ModBlocks.NETHERITE_PISTON;
            case ENDERITE -> ModBlocks.ENDERITE_PISTON;
        };
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(baseBlock(state.getValue(TYPE)));
    }
}

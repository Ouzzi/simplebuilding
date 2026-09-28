package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.BlockCodecs;

import com.simplebuilding.version.McVersion;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.PistonBoreEffects;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;

/**
 * Enderitkolben: bricht gewoehnliche Bloecke genau wie der Netheritkolben (alles aus
 * {@link NetheriteBreakerPistonBlock} gilt unveraendert), durchbricht aber tiefer.
 *
 * <p>Steht ein durchbrechbarer Block ({@link PistonBreach}) direkt vorn und bezahlt ein
 * Redstoneblock daneben, geht der Durchbruch bis zu {@link #BREACH_DEPTH} Zellen in Blickrichtung:
 * <ul>
 *   <li>Luft wird uebersprungen;</li>
 *   <li>an einem Block im Tag {@code simplebuilding:piston_breach_immune} endet der Durchbruch, auch
 *       wenn der Brecher ihn sonst brechen duerfte;</li>
 *   <li>ein durchbrechbarer Block verschwindet ohne Drop;</li>
 *   <li>ein gewoehnlicher Block, den der Brecher bei der anliegenden Signalstaerke brechen darf
 *       (Haerte 0 bis {@code (Signal / 15) * 50}, Push-Reaktion nicht BLOCK), wird wie vom Brecher
 *       zerstoert, mit seinem normalen Drop;</li>
 *   <li>an allem anderen (ein unzerstoerbarer Block mit Block-Entity, ein zu harter Block,
 *       Push-Reaktion BLOCK, Fluessigkeiten) endet der Durchbruch ebenfalls.</li>
 * </ul>
 * Jeder zerstoerte Block zeigt seine Bruchpartikel und spielt seinen Abbauklang und den Bohrklang
 * der Mod ({@link PistonBoreEffects}). Danach verschwinden der Redstoneblock und der Kolben selbst,
 * wie beim Netheritkolben.
 *
 * <p>Haltbarkeit (2026-09-28): wie beim Netheritkolben 1 je beim Ausfahren zerstoertem Block, hier
 * {@link #ENDERITE_MAX_DURABILITY} (281) = ein Neuntel der Enderitspitzhacke (2530 / 9). Reparatur mit einem
 * Enderitklumpen (volle Haltbarkeit); wer nicht repariert, faellt auf den Netheritkolben mit voller
 * Netherit-Haltbarkeit zurueck.
 */
public class EnderitePistonBlock extends NetheriteBreakerPistonBlock {
    public static final MapCodec<EnderitePistonBlock> CODEC = BlockCodecs.simple(EnderitePistonBlock::new);

    /** Wie viele Zellen vor der Front der Durchbruch hoechstens reicht. */
    public static final int BREACH_DEPTH = 3;

    public EnderitePistonBlock(Properties settings) {
        super(settings);
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    /** Haltbarkeit des Enderitkolbens: ein Neuntel der Enderitspitzhacke (2530 -> 281). */
    public static final int ENDERITE_MAX_DURABILITY = ninthOf(com.simplebuilding.items.ModToolMaterials.ENDERITE.durability());

    /** Schaden innerhalb der Rissstufe beim Enderitkolben (0 bis 35). */
    public static final IntegerProperty ENDERITE_WEAR_STEP =
            IntegerProperty.create("wear_step", 0, stepsPerStage(ENDERITE_MAX_DURABILITY) - 1);

    @Override
    protected IntegerProperty wearStepProperty() {
        return ENDERITE_WEAR_STEP;
    }

    @Override
    public int maxDurability() {
        return ENDERITE_MAX_DURABILITY;
    }

    @Override
    protected net.minecraft.world.item.Item repairNugget() {
        return com.simplebuilding.items.ModItems.ENDERITE_NUGGET;
    }

    /**
     * Aufgebraucht zerfaellt er eine Stufe tiefer: zum Netheritkolben derselben Richtung mit voller
     * Netherit-Haltbarkeit - verbraucht ist nur die Enderit-Schicht, die Netherit-Aufwertung darunter
     * ist unversehrt.
     */
    @Override
    protected BlockState wornOutState(BlockState state) {
        return withDamage(com.simplebuilding.blocks.ModBlocks.NETHERITE_PISTON.defaultBlockState()
                .setValue(FACING, state.getValue(FACING)), 0);
    }

    /**
     * Das Kolben-Ereignis ({@code PlatformServices#mayPistonMove}) geht einmal fuer den ganzen
     * Durchbruch raus, das Abbau-Ereignis je Block (Audit N16: vorher je Block beides).
     *
     * @return ob der vorderste Block zerstoert wurde. Jeder Block geht vorher durch den
     *         Plattform-Wächter ({@link #mayBreak}); lehnt er einen tieferen ab, endet der Durchbruch
     *         dort, lehnt er schon den vordersten ab, passiert gar nichts.
     */
    @Override
    protected boolean breach(ServerLevel world, BlockPos pos, Direction facing) {
        // Vor dem Durchbruch gemessen: der bezahlende Redstoneblock liegt noch daneben.
        int power = world.getBestNeighborSignal(pos);
        float breakThreshold = (power / 15.0f) * 50.0f;
        if (!com.simplebuilding.platform.PlatformServices.mayPistonMove(world, pos, facing)) {
            return false;
        }
        for (int depth = 1; depth <= BREACH_DEPTH; depth++) {
            BlockPos target = pos.relative(facing, depth);
            BlockState targetState = world.getBlockState(target);
            if (targetState.isAir()) {
                continue;
            }
            if (targetState.is(ModTags.Blocks.PISTON_BREACH_IMMUNE)) {
                return true;
            }
            boolean breachable = PistonBreach.isBreachable(targetState, world, target);
            if (!breachable && !breakerCanBreak(targetState, world, target, breakThreshold)) {
                return true;
            }
            if (!mayBreak(world, pos, facing, target)) {
                return depth > 1;
            }
            PistonBoreEffects.destroy(world, target, !breachable);
        }
        return true;
    }

    /** Die Regel des normalen Brechers aus {@link NetheriteBreakerPistonBlock#triggerEvent}. */
    private static boolean breakerCanBreak(BlockState state, Level world, BlockPos pos, float breakThreshold) {
        float hardness = state.getDestroySpeed(world, pos);
        return hardness >= 0 && hardness <= breakThreshold && McVersion.pushReaction(state) != McVersion.PUSH_BLOCKED;
    }
}

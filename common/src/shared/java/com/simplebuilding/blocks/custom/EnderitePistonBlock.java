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
 * <p>Verschleiss (Kolben-Balance 2026-09-27): jeder normal gebrochene Block kostet wie beim
 * Netheritkolben {@code max(1, aufgerundete Haerte)} Punkte, hier auf {@code enderitePistonWearBudget}
 * (Standard {@value #DEFAULT_WEAR_BUDGET}) verteilt: 256 Punkte je Stufe, also rund 1000 Steine oder
 * 680 Tiefenschiefer bis zur Reparatur mit einem Enderitklumpen - doppelt so lang wie der
 * Netheritkolben (1024), weil Stufe und Reparatur teurer sind. Ein langer Tunnel (einige tausend
 * Bloecke) kostet damit ein paar Klumpen; wer nicht repariert, faellt auf den Netheritkolben zurueck.
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

    /**
     * Der Enderitkolben verschleisst wie der Netheritkolben (Kolben-Balance 2026-09-27, Teil 5 der
     * Bauwerkzeug-Notizen: das Tunnelbohren war zu leicht), mit eigenem Budget
     * ({@code enderitePistonWearBudget}), Enderitklumpen als Reparatur und dem Netheritkolben als
     * Zerfallsziel. Bestehende Enderitkolben in alten Welten laden mit dem Standardwert 0: ein
     * Blockzustand ohne gespeicherte Eigenschaft bekommt ihren Standardwert, ein Datenfixer ist nicht
     * noetig.
     */
    @Override
    protected boolean wears() {
        return true;
    }

    /** Standardbudget des Enderitkolbens, siehe {@code SimplebuildingConfig#enderitePistonWearBudget}. */
    public static final int DEFAULT_WEAR_BUDGET = 2048;

    /** Das Verschleissbudget des Enderitkolbens aus der Konfiguration; 0 = kein Verschleiss. */
    public static int enderiteWearBudget() {
        com.simplebuilding.config.SimplebuildingConfig config = com.simplebuilding.Simplebuilding.getConfig();
        return config == null ? DEFAULT_WEAR_BUDGET : Math.max(0, config.enderitePistonWearBudget);
    }

    @Override
    protected int configuredWearBudget() {
        return enderiteWearBudget();
    }

    @Override
    protected net.minecraft.world.item.Item repairNugget() {
        return com.simplebuilding.items.ModItems.ENDERITE_NUGGET;
    }

    /** Verbraucht zerfaellt er eine Stufe tiefer: zum Netheritkolben derselben Richtung, unversehrt. */
    @Override
    protected BlockState wornOutState(BlockState state) {
        return com.simplebuilding.blocks.ModBlocks.NETHERITE_PISTON.defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(WEAR, 0);
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

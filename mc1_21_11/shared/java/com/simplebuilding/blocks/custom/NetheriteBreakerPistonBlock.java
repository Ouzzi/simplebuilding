package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.PistonBoreEffects;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * Der Netheritkolben: bricht beim Ausfahren den Block davor (Haerte bis {@code Signal / 15 * 50}) und
 * durchbricht bezahlt "Unzerstoerbares" ({@link PistonBreach}).
 *
 * <h2>Verschleiss (Audit 2026-09-26 #23, Besitzer-Entscheidung 2026-09-27)</h2>
 * Bis dahin brach der Brecher ewig und bis Haerte 50 (Antiker Schutt, Obsidian, Spawner) - ein
 * billiger Dauer-Tunnelbohrer. Jetzt nutzt er sich ab:
 * <ul>
 *   <li>Die Eigenschaft {@link #WEAR} (0 bis {@link #WEAR_STAGES}{@code - 1}) zeigt die Stufe. Sie
 *       bleibt beim Aus- und Einfahren (Vanilla legt beim Einfahren den Standardzustand in den
 *       bewegten Block, {@code PistonBlockMixin#simplebuilding$keepWear} traegt sie hinueber), beim
 *       Verschieben durch andere Kolben und am Item: {@link #getDrops} schreibt sie in die
 *       Block-Zustands-Komponente, und {@code BlockItem} setzt sie beim Platzieren wieder.
 *       Aufheben setzt also nichts zurueck.</li>
 *   <li>Jeder normal gebrochene Block kostet {@link #wearCost} Punkte ({@code max(1, aufgerundete
 *       Haerte)}: Erde 1, Stein 2, Tiefenschiefer 3, Antiker Schutt 30). Eine Stufe sind
 *       {@code netheriteBreakerWearBudget / 8} Punkte (Standard 1024 -> 128); Reste zaehlen als
 *       Wahrscheinlichkeit, im Mittel also genau das Budget. Budget 0 schaltet den Verschleiss ab.</li>
 *   <li>Steigt eine Stufe, raucht der Kolben kurz und knirscht (Amboss); die Seiten zeigen ab Stufe
 *       2, 4 und 6 immer tiefere Risse. Ist die letzte Stufe voll, zerfaellt er mit Bruchklang und
 *       Partikeln zum verstaerkten Kolben (die Netherit-Aufwertung ist weg) und faehrt als solcher
 *       aus.</li>
 *   <li>Reparatur: Rechtsklick mit einem Netheritklumpen setzt den Verschleiss auf 0 und kostet den
 *       Klumpen (ausser im Kreativmodus) - so viel wie die Aufwertung selbst.</li>
 *   <li>Tooltip des Items: {@code Verschleiss n/8}, sobald er nicht 0 ist.</li>
 * </ul>
 * Bezahlte Durchbrueche verbrauchen den Kolben ohnehin und kosten keinen Verschleiss. Der
 * Enderitkolben ({@link EnderitePistonBlock}) nutzt sich (noch) nicht ab und hat die Eigenschaft gar
 * nicht ({@link #wears}); derselbe Mechanismus liesse sich dort mit {@code wears() = true}, eigenem
 * Budget und dem Netheritkolben als Zerfallsziel einschalten (spaeter: Kolben-Balance).
 */
public class NetheriteBreakerPistonBlock extends PistonBaseBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = simpleCodec(NetheriteBreakerPistonBlock::new);

    /** Sichtbare Verschleissstufen; bei der achten ist der Brecher verbraucht. */
    public static final int WEAR_STAGES = 8;

    /** Verschleissstufe 0 (neu) bis 7 (kurz vor dem Zerfall). */
    public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, WEAR_STAGES - 1);

    public NetheriteBreakerPistonBlock(Properties settings) {
        super(false, settings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    /**
     * Ob dieser Kolben verschleisst und die Eigenschaft {@link #WEAR} traegt. Wird schon im
     * Konstruktor von {@code Block} gefragt (Zustandsdefinition), darf also nur eine Konstante
     * liefern.
     */
    protected boolean wears() {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (wears()) {
            builder.add(WEAR);
        }
    }

    /** Die Verschleissstufe eines Zustands; 0 fuer Kolben ohne Verschleiss. */
    public static int wearOf(BlockState state) {
        return state.hasProperty(WEAR) ? state.getValue(WEAR) : 0;
    }

    /** Was ein normal gebrochener Block kostet: {@code max(1, aufgerundete Haerte)}. */
    public static int wearCost(float hardness) {
        return Math.max(1, Mth.ceil(hardness));
    }

    /** Das Verschleissbudget aus der Konfiguration; 0 = kein Verschleiss. */
    public static int wearBudget() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null ? 1024 : Math.max(0, config.netheriteBreakerWearBudget);
    }

    /** Budgets einzelner Kolbenstellen, nur fuer Spieltests ({@link #overrideWearBudgetAt}). */
    private static final java.util.Map<BlockPos, Integer> BUDGET_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Spieltests: ein festes Budget fuer den Kolben an {@code pos}, bis das Runnable laeuft. Die
     * Tests laufen parallel; die Konfiguration umzustellen, wuerde jeden anderen Brecher mittreffen.
     */
    public static Runnable overrideWearBudgetAt(BlockPos pos, int budget) {
        BlockPos key = pos.immutable();
        BUDGET_OVERRIDES.put(key, budget);
        return () -> BUDGET_OVERRIDES.remove(key);
    }

    /** Das Budget fuer den Kolben an {@code pos}: Test-Ueberschreibung oder Konfiguration. */
    public static int wearBudgetAt(BlockPos pos) {
        Integer override = BUDGET_OVERRIDES.isEmpty() ? null : BUDGET_OVERRIDES.get(pos);
        return override != null ? override : wearBudget();
    }

    /**
     * Wie viele Stufen ein Block der Haerte {@code hardness} kostet: {@code Kosten / (Budget / 8)},
     * der Rest als Wahrscheinlichkeit ({@code roll} gleichverteilt in [0, 1)). 0, wenn der
     * Verschleiss abgeschaltet ist.
     */
    public static int wearSteps(float hardness, int budget, double roll) {
        if (budget <= 0) {
            return 0;
        }
        double exact = wearCost(hardness) / (budget / (double) WEAR_STAGES);
        int whole = (int) Math.floor(exact);
        return whole + (roll < exact - whole ? 1 : 0);
    }

    /**
     * Rechnet den Verschleiss fuer einen normal gebrochenen Block an.
     *
     * @return der neue Zustand (eine hoehere Stufe ist schon gesetzt), oder {@code null}, wenn der
     *         Brecher damit verbraucht ist
     */
    private @Nullable BlockState addWear(ServerLevel world, BlockPos pos, BlockState state, float hardness) {
        if (!state.hasProperty(WEAR)) {
            return state;
        }
        int steps = wearSteps(hardness, wearBudgetAt(pos), world.getRandom().nextDouble());
        if (steps <= 0) {
            return state;
        }
        int wear = state.getValue(WEAR) + steps;
        if (wear >= WEAR_STAGES) {
            return null;
        }
        BlockState worn = state.setValue(WEAR, wear);
        world.setBlock(pos, worn, Block.UPDATE_CLIENTS);
        world.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6 + wear * 2, 0.35, 0.35, 0.35, 0.01);
        world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25F, 1.4F - wear * 0.05F);
        return worn;
    }

    /**
     * Der verbrauchte Brecher zerfaellt zum verstaerkten Kolben derselben Blickrichtung und faehrt
     * sofort als solcher aus. Der neue Block geht wie beim Brechen sofort an die Clients: das
     * Block-Ereignis-Paket, das der Server gleich schickt, spielt der Client an dem Block nach, der
     * dann bei ihm steht - sonst fuehre er als Netheritkolben mit Netheritkopf aus.
     */
    private boolean wearOut(ServerLevel world, BlockPos pos, BlockState state, int type, int data) {
        BlockState reinforced = ModBlocks.REINFORCED_PISTON.defaultBlockState().setValue(FACING, state.getValue(FACING));
        world.setBlock(pos, reinforced, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
        world.getServer().getPlayerList().broadcast(null, pos.getX(), pos.getY(), pos.getZ(),
                64.0, world.dimension(), new ClientboundBlockUpdatePacket(world, pos));
        world.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
        world.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.4, 0.4, 0.4, 0.02);
        return reinforced.triggerEvent(world, pos, type, data);
    }

    /**
     * Der Verschleiss reist mit dem Item: die Beutetabelle droppt den Kolben schlicht, hier bekommt
     * er die Stufe in die Block-Zustands-Komponente, aus der {@code BlockItem} sie beim Platzieren
     * wieder setzt. Nur bei Verschleiss: ein unversehrter Kolben stapelt weiter mit frisch
     * aufgewerteten. (Im Code statt als {@code copy_state} in der Beutetabelle, weil deren Bedingungen
     * zwischen 1.21.11, 26.2 und 26.3 ihr Format wechseln.)
     */
    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        java.util.List<ItemStack> drops = super.getDrops(state, params);
        int wear = wearOf(state);
        if (wear > 0) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    drop.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                            net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(WEAR, wear));
                }
            }
        }
        return drops;
    }

    /**
     * Reparatur: ein Netheritklumpen setzt den Verschleiss auf 0 (Kreativ: kostenlos). Ohne
     * Verschleiss oder mit etwas anderem in der Hand verhaelt sich der Kolben wie immer.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.NETHERITE_NUGGET) || wearOf(state) == 0) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
        if (world instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(WEAR, 0), Block.UPDATE_CLIENTS);
            server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            server.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.4, 0.4, 0.4, 0.05);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Vanilla reiht das Ausfahr-Ereignis nur ein, wenn {@code PistonStructureResolver#resolve}
     * gelingt, und der verweigert jeden durchbrechbaren Block ({@link PistonBreach}). Ein bezahlter
     * Durchbruch braucht deshalb sein eigenes Ereignis; es haengt bewusst nicht davon ab, was hinter
     * dem Ziel liegt (Schublimit, Obsidian, Bauhoehe). Doppelte Ereignisse fasst der Server zusammen,
     * und sobald der Kolben weg ist, verwirft er die uebrigen.
     */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, world, pos, block, orientation, movedByPiston);
        if (!world.isClientSide()) {
            queueBreachIfPaid(world, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, world, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()) && !world.isClientSide()) {
            queueBreachIfPaid(world, pos, state);
        }
    }

    private void queueBreachIfPaid(Level world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        if (PistonBreach.isBreachable(world, pos.relative(facing)) && PistonBreach.findFuel(world, pos, facing) != null) {
            world.blockEvent(pos, this, TRIGGER_EXTEND, facing.get3DDataValue());
        }
    }

    /**
     * Der Durchbruch: steht ein durchbrechbarer Block direkt vorn und bezahlt ein Redstoneblock
     * daneben ({@link PistonBreach#findFuel}), wird {@link #breach} ausgefuehrt, dann verschwinden
     * der Redstoneblock und der Kolben selbst, alles ohne Drop. {@code moveBlocks} laeuft nie, es
     * bleibt also weder Kopf noch bewegter Block zurueck. Kein EXTENDED-Waechter: ein ausgefahrener
     * Kolben ohne Kopf bezahlt genauso mit Redstoneblock und sich selbst.
     *
     * <p>Jeder Block, den der Durchbruch zerstoert, geht vorher durch
     * {@link PlatformServices#mayPistonBreak} (Schutz-Mods, {@code PistonEvent.Pre}, Bruch-Ereignis
     * mit Fake-Spieler). Verweigert der Wächter schon den vordersten Block, passiert nichts: weder
     * der Redstoneblock noch der Kolben werden verbraucht.
     *
     * @return ob durchbrochen wurde; dann gibt es kein Ausfahren mehr
     */
    private boolean breachIfPaid(BlockState state, ServerLevel world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (!PistonBreach.isBreachable(world, pos.relative(facing))) {
            return false;
        }
        BlockPos fuel = PistonBreach.findFuel(world, pos, facing);
        if (fuel == null) {
            return false;
        }
        if (!breach(world, pos, facing)) {
            return false;
        }
        world.destroyBlock(fuel, false);
        world.destroyBlock(pos, false);
        return true;
    }

    /**
     * Netheritkolben: nur der durchbrechbare Block direkt vorn, ohne Drop, mit Partikeln und Klang
     * ({@link PistonBoreEffects}).
     *
     * @return ob der vorderste Block zerstoert wurde; {@code false}, wenn der Wächter ablehnte
     */
    protected boolean breach(ServerLevel world, BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        if (!PlatformServices.mayPistonMove(world, pos, facing) || !mayBreak(world, pos, facing, front)) {
            return false;
        }
        PistonBoreEffects.destroy(world, front, false);
        return true;
    }

    /**
     * Fragt den Plattform-Wächter ({@link PlatformServices#mayPistonBreak}) für einen Block. Das
     * Kolben-Ereignis ({@link PlatformServices#mayPistonMove}) fragt der Aufrufer einmal je Aktion.
     */
    protected static boolean mayBreak(ServerLevel world, BlockPos piston, Direction facing, BlockPos target) {
        return PlatformServices.mayPistonBreak(world, piston, facing, target, world.getBlockState(target));
    }

    /**
     * Ausfahren mit Brechen, nur auf dem Server:
     * <ol>
     *   <li>ein bezahlter Durchbruch geht vor (dann kein Ausfahren);</li>
     *   <li>sonst wird nur gebrochen, wenn Vanilla gleich danach wirklich ausfaehrt
     *       ({@link #hasVanillaExtendSignal}), der Block hart genug fuer das Signal ist und der
     *       Plattform-Wächter zustimmt - erst dann, direkt vor {@code super.triggerEvent}. Nach dem
     *       Brechen liegt vorn Luft, {@code moveBlocks} kann also nicht mehr scheitern; ein
     *       NeoForge/Forge-{@code PistonEvent.Pre}, das den Zug absagt, hat der Wächter schon vorher
     *       gefragt.</li>
     * </ol>
     * Jeder so gebrochene Block kostet Verschleiss ({@link #addWear}); ist der Brecher damit
     * verbraucht, faehrt statt seiner der verstaerkte Kolben aus ({@link #wearOut}).
     * Der Client bricht nie selbst (audit 2026-09-26 #47: Geisterbloecke, wenn Client und Server
     * verschieden entschieden). Damit er beim Nachspielen des Ausfahr-Ereignisses den Block nicht
     * mitschiebt, schickt {@link PistonBoreEffects#destroy} die Entfernung sofort, also vor dem
     * Block-Ereignis-Paket.
     */
    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        if (world instanceof ServerLevel server) {
            if (type == TRIGGER_EXTEND && breachIfPaid(state, server, pos)) {
                return false;
            }
            if (type == TRIGGER_EXTEND && !state.getValue(EXTENDED)
                    && hasVanillaExtendSignal(server, pos, state.getValue(FACING))) {
                Direction facing = state.getValue(FACING);
                BlockPos targetPos = pos.relative(facing);
                BlockState targetState = server.getBlockState(targetPos);
                if (!targetState.isAir()) {
                    float blockHardness = targetState.getDestroySpeed(server, targetPos);
                    float breakThreshold = (server.getBestNeighborSignal(pos) / 15.0f) * 50.0f;
                    if (blockHardness >= 0 && blockHardness <= breakThreshold
                            && targetState.getPistonPushReaction() != PushReaction.BLOCK
                            && PlatformServices.mayPistonMove(server, pos, facing)
                            && mayBreak(server, pos, facing, targetPos)) {
                        // Mit Beute, Bruchpartikeln, Abbauklang und dem Bohrklang der Mod.
                        PistonBoreEffects.destroy(server, targetPos, true);
                        BlockState worn = addWear(server, pos, state, blockHardness);
                        if (worn == null) {
                            return wearOut(server, pos, state, type, data);
                        }
                        state = worn;
                    }
                }
            }
        }
        return super.triggerEvent(state, world, pos, type, data);
    }

    /**
     * Vanillas eigene Nachpruefung aus {@code PistonBaseBlock#triggerEvent} (dort private
     * {@code getNeighborSignal}): Signal von jeder Seite ausser der Schubrichtung, von unten, oder
     * ueber die Quasi-Konnektivitaet von oben. {@code super.triggerEvent} verwirft ein
     * Ausfahr-Ereignis ohne dieses Signal - der Brecher darf dann auch nichts zerstoert haben. Bis
     * 2026-09 brach er vorher, mit {@code getBestNeighborSignal}, das die Schubrichtung mitzaehlt:
     * ein Signal, das zwischen Einreihen und Ausfuehren des Block-Ereignisses verschwand, oder ein
     * Block davor, der selbst die einzige Signalquelle war, kostete den Block, ohne dass der Kolben
     * je ausfuhr.
     */
    private static boolean hasVanillaExtendSignal(Level level, BlockPos pos, Direction push) {
        for (Direction direction : Direction.values()) {
            if (direction != push && level.hasSignal(pos.relative(direction), direction)) {
                return true;
            }
        }
        if (level.hasSignal(pos, Direction.DOWN)) {
            return true;
        }
        BlockPos above = pos.above();
        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && level.hasSignal(above.relative(direction), direction)) {
                return true;
            }
        }
        return false;
    }
}
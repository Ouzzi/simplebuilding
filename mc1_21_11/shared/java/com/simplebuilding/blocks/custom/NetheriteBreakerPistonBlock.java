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
 * <h2>Haltbarkeit (Besitzer-Entscheidung 2026-09-28, ersetzt den Verschleiss vom 2026-09-27)</h2>
 * Der Brecher hat eine echte Haltbarkeit wie ein Werkzeug:
 * <ul>
 *   <li><b>Hoechstwert:</b> ein Neuntel der Spitzhacke seiner Stufe, weil die Aufwertung einen
 *       Klumpen (1/9 Barren) kostet: Netherit {@code 2031 / 9 = 226}
 *       ({@link #NETHERITE_MAX_DURABILITY}), Enderit {@code 2530 / 9 = 281}
 *       ({@code EnderitePistonBlock#ENDERITE_MAX_DURABILITY}).</li>
 *   <li><b>Kosten:</b> genau 1 je Block, den der Kolben beim Ausfahren zerstoert - wie eine
 *       Spitzhacke, unabhaengig von der Haerte (vorhersehbar, der Balken ist ehrlich). Aus- und
 *       Einfahren ohne Brechen, Schieben und Ziehen kosten nichts; bezahlte Durchbrueche verbrauchen
 *       den Kolben ohnehin.</li>
 *   <li><b>Im Block:</b> der Schaden steckt in zwei Eigenschaften. {@link #WEAR} (0 bis 7) ist die
 *       sichtbare Rissstufe = {@code Schaden * 8 / Hoechstwert}, die Modelle haengen nur an ihr;
 *       {@code wear_step} ({@link #wearStepProperty}) zaehlt den Schaden innerhalb der Stufe. Beide
 *       ueberleben Aus-/Einfahren ({@code PistonBlockMixin#simplebuilding$keepWear}) und das
 *       Verschieben durch andere Kolben. Eine Block-Entity kaeme nicht in Frage: Vanilla schiebt
 *       keine Bloecke mit Block-Entity.</li>
 *   <li><b>Am Item:</b> {@link #getDrops} gibt einem beschaedigten Kolben {@code max_damage},
 *       {@code damage} und Stapelgroesse 1 - der normale Haltbarkeitsbalken in Hand und Inventar; ein
 *       unversehrter bleibt stapelbar und ohne Balken. {@link #getStateForPlacement} liest den Schaden
 *       beim Setzen zurueck. Aufheben repariert also nichts.</li>
 *   <li><b>Aufgebraucht:</b> der Kolben zerfaellt eine Stufe tiefer ({@link #wornOutState}):
 *       Enderit zum Netheritkolben mit voller Netherit-Haltbarkeit (die Netherit-Aufwertung darunter
 *       ist unversehrt, verbraucht ist nur die Enderit-Schicht), Netherit zum verstaerkten Kolben, und
 *       faehrt sofort als solcher aus.</li>
 *   <li><b>Reparatur:</b> Rechtsklick mit dem Klumpen der Stufe ({@link #repairNugget}) stellt die
 *       volle Haltbarkeit her und kostet den Klumpen (Kreativ: kostenlos) - so viel wie die
 *       Aufwertung. Weniger waere sinnlos: zerfallen lassen und neu aufwerten gaebe fuer denselben
 *       Klumpen volle Haltbarkeit.</li>
 *   <li><b>Alte Welten:</b> ein Kolben mit altem {@code wear=n} laedt mit {@code wear_step=0}, also
 *       mit Schaden {@link #firstDamageOfStage}{@code (n)} = derselbe Bruchteil n/8. Alte Items mit
 *       {@code block_state {wear:n}} setzen beim Platzieren dieselbe Stufe; einzeln im Inventar
 *       stellt {@code NetheritePistonItem#inventoryTick} sie auf den Haltbarkeitsbalken um.</li>
 *   <li><b>Konfiguration:</b> {@code breakerPistonsLoseDurability} (Standard an) schaltet die
 *       Abnutzung ab. Die alten Budgets (Haertepunkte) sind entfallen: der Hoechstwert steckt im
 *       Wertebereich der Blockeigenschaft und muss auf Server und Client gleich sein.</li>
 * </ul>
 */
public class NetheriteBreakerPistonBlock extends PistonBaseBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = simpleCodec(NetheriteBreakerPistonBlock::new);

    /** Sichtbare Rissstufen; die Modelle zeigen je zwei Stufen dieselbe Textur. */
    public static final int WEAR_STAGES = 8;

    /** Rissstufe 0 (neu) bis 7 (fast aufgebraucht) = {@code Schaden * 8 / Hoechstwert}. */
    public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, WEAR_STAGES - 1);

    /** Ein Neuntel einer Werkzeughaltbarkeit, gerundet: die Aufwertung kostet 1 Klumpen = 1/9 Barren. */
    public static int ninthOf(int toolDurability) {
        return Math.round(toolDurability / 9.0F);
    }

    /** Haltbarkeit des Netheritkolbens: ein Neuntel der Netheritspitzhacke (2031 -> 226). */
    public static final int NETHERITE_MAX_DURABILITY = ninthOf(net.minecraft.world.item.ToolMaterial.NETHERITE.durability());

    /** Die groesste Stufe bei {@code max} Haltbarkeit hat {@code ceil(max / 8)} Schadenspunkte. */
    public static int stepsPerStage(int maxDurability) {
        return (maxDurability + WEAR_STAGES - 1) / WEAR_STAGES;
    }

    /** Schaden innerhalb der Rissstufe beim Netheritkolben (0 bis 28). */
    public static final IntegerProperty NETHERITE_WEAR_STEP =
            IntegerProperty.create("wear_step", 0, stepsPerStage(NETHERITE_MAX_DURABILITY) - 1);

    public NetheriteBreakerPistonBlock(Properties settings) {
        super(false, settings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    /**
     * Die Eigenschaft, die den Schaden innerhalb der Rissstufe zaehlt. Wird schon im Konstruktor von
     * {@code Block} gefragt (Zustandsdefinition), darf also nur eine Konstante liefern.
     */
    protected IntegerProperty wearStepProperty() {
        return NETHERITE_WEAR_STEP;
    }

    /** Die volle Haltbarkeit dieser Kolbenstufe; eine Konstante (sie bestimmt den Wertebereich oben). */
    public int maxDurability() {
        return NETHERITE_MAX_DURABILITY;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WEAR, wearStepProperty());
    }

    /** Die Rissstufe eines Zustands; 0 fuer Kolben ohne Haltbarkeit. */
    public static int wearOf(BlockState state) {
        return state.hasProperty(WEAR) ? state.getValue(WEAR) : 0;
    }

    /** Die Rissstufe bei {@code damage} von {@code max}: {@code damage * 8 / max}, hoechstens 7. */
    public static int stageOf(int damage, int maxDurability) {
        return Mth.clamp(damage * WEAR_STAGES / maxDurability, 0, WEAR_STAGES - 1);
    }

    /** Der kleinste Schaden der Rissstufe {@code stage}: {@code ceil(stage * max / 8)}. */
    public static int firstDamageOfStage(int stage, int maxDurability) {
        return (stage * maxDurability + WEAR_STAGES - 1) / WEAR_STAGES;
    }

    /** Die volle Haltbarkeit des Kolbens in diesem Zustand; 0, wenn er keine hat. */
    public static int maxDurabilityOf(BlockState state) {
        return state.getBlock() instanceof NetheriteBreakerPistonBlock breaker ? breaker.maxDurability() : 0;
    }

    /** Der Schaden eines Zustands (0 = neu); 0 fuer Kolben ohne Haltbarkeit. */
    public static int damageOf(BlockState state) {
        if (!(state.getBlock() instanceof NetheriteBreakerPistonBlock breaker)) {
            return 0;
        }
        int max = breaker.maxDurability();
        int stage = state.getValue(WEAR);
        int nextStage = stage + 1 < WEAR_STAGES ? firstDamageOfStage(stage + 1, max) : max;
        return Math.min(firstDamageOfStage(stage, max) + state.getValue(breaker.wearStepProperty()), nextStage - 1);
    }

    /** Die verbleibende Haltbarkeit eines Zustands. */
    public static int durabilityOf(BlockState state) {
        return maxDurabilityOf(state) - damageOf(state);
    }

    /**
     * Setzt den Schaden (auf 0 bis Hoechstwert - 1 begrenzt); Rissstufe und Schritt folgen daraus.
     * Zustaende anderer Bloecke kommen unveraendert zurueck.
     */
    public static BlockState withDamage(BlockState state, int damage) {
        if (!(state.getBlock() instanceof NetheriteBreakerPistonBlock breaker)) {
            return state;
        }
        int max = breaker.maxDurability();
        int clamped = Mth.clamp(damage, 0, max - 1);
        int stage = stageOf(clamped, max);
        return state.setValue(WEAR, stage).setValue(breaker.wearStepProperty(), clamped - firstDamageOfStage(stage, max));
    }

    /** Ob die Brecher Haltbarkeit verlieren ({@code breakerPistonsLoseDurability}). */
    public static boolean losesDurability() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.breakerPistonsLoseDurability;
    }

    /** Kolbenstellen, an denen die Abnutzung wie bei ausgeschalteter Option ruht; nur fuer Spieltests. */
    private static final java.util.Set<BlockPos> DURABILITY_FROZEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Spieltests: der Kolben an {@code pos} verliert keine Haltbarkeit, bis das Runnable laeuft - wie
     * mit {@code breakerPistonsLoseDurability = false}, ohne die Konfiguration umzustellen (die Tests
     * laufen parallel, jeder andere Brecher wuerde es mitbekommen).
     */
    public static Runnable freezeDurabilityAt(BlockPos pos) {
        BlockPos key = pos.immutable();
        DURABILITY_FROZEN.add(key);
        return () -> DURABILITY_FROZEN.remove(key);
    }

    /** Der Klumpen, der diesen Kolben repariert (so viel wie seine Aufwertung kostet). */
    protected net.minecraft.world.item.Item repairNugget() {
        return ModItems.NETHERITE_NUGGET;
    }

    /**
     * Der Zustand, zu dem der aufgebrauchte Kolben zerfaellt: eine Stufe tiefer, hier der verstaerkte
     * Kolben, mit derselben Blickrichtung.
     */
    protected BlockState wornOutState(BlockState state) {
        return ModBlocks.REINFORCED_PISTON.defaultBlockState().setValue(FACING, state.getValue(FACING));
    }

    /**
     * Zieht fuer einen beim Ausfahren zerstoerten Block 1 Haltbarkeit ab.
     *
     * @return der neue Zustand (schon gesetzt), oder {@code null}, wenn der Brecher damit aufgebraucht ist
     */
    private @Nullable BlockState addWear(ServerLevel world, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof NetheriteBreakerPistonBlock) || !losesDurability()
                || (!DURABILITY_FROZEN.isEmpty() && DURABILITY_FROZEN.contains(pos))) {
            return state;
        }
        int damage = damageOf(state) + 1;
        if (damage >= maxDurability()) {
            return null;
        }
        BlockState worn = withDamage(state, damage);
        world.setBlock(pos, worn, Block.UPDATE_CLIENTS);
        int wear = worn.getValue(WEAR);
        if (wear > state.getValue(WEAR)) {
            world.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    6 + wear * 2, 0.35, 0.35, 0.35, 0.01);
            world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25F, 1.4F - wear * 0.05F);
        }
        return worn;
    }

    /**
     * Der aufgebrauchte Brecher zerfaellt eine Stufe tiefer ({@link #wornOutState}: Netherit zum
     * verstaerkten Kolben, Enderit zum Netheritkolben mit voller Haltbarkeit) und faehrt sofort als
     * solcher aus. Der neue Block geht wie beim Brechen sofort an die Clients: das
     * Block-Ereignis-Paket, das der Server gleich schickt, spielt der Client an dem Block nach, der
     * dann bei ihm steht - sonst fuehre er mit dem alten Kopf aus.
     */
    private boolean wearOut(ServerLevel world, BlockPos pos, BlockState state, int type, int data) {
        BlockState lower = wornOutState(state);
        world.setBlock(pos, lower, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
        world.getServer().getPlayerList().broadcast(null, pos.getX(), pos.getY(), pos.getZ(),
                64.0, world.dimension(), new ClientboundBlockUpdatePacket(world, pos));
        world.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
        world.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.4, 0.4, 0.4, 0.02);
        return lower.triggerEvent(world, pos, type, data);
    }

    /**
     * Die Haltbarkeit reist mit dem Item: die Beutetabelle droppt den Kolben schlicht, hier bekommt
     * ein beschaedigter {@code max_damage}, {@code damage} und Stapelgroesse 1 (ein Item mit
     * Haltbarkeit darf nicht stapeln) - Vanilla zeichnet dann den Balken. Ein unversehrter Kolben
     * bleibt ohne Komponenten und stapelt weiter mit frisch aufgewerteten. (Im Code statt in der
     * Beutetabelle, weil deren Format zwischen 1.21.11, 26.2 und 26.3 wechselt.)
     */
    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        java.util.List<ItemStack> drops = super.getDrops(state, params);
        int damage = damageOf(state);
        if (damage > 0) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem()) && drop.getCount() == 1) {
                    com.simplebuilding.items.custom.NetheritePistonItem.setDamage(drop, damage, maxDurability());
                }
            }
        }
        return drops;
    }

    /**
     * Setzt den Schaden aus dem Item ({@code damage} bei {@code max_damage}) auf den Block. Ein altes
     * Item mit {@code block_state {wear:n}} bringt keinen Schaden mit; dessen Stufe setzt
     * {@code BlockItem} danach selbst (Schritt 0 = derselbe Bruchteil n/8).
     */
    @Override
    public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        ItemStack stack = context.getItemInHand();
        if (stack.has(net.minecraft.core.component.DataComponents.MAX_DAMAGE)) {
            state = withDamage(state, stack.getDamageValue());
        }
        return state;
    }

    /**
     * Reparatur: der Klumpen der Stufe ({@link #repairNugget}: Netherit bzw. Enderit) stellt die
     * volle Haltbarkeit her (Kreativ: kostenlos). Unversehrt oder mit etwas anderem in der Hand
     * verhaelt sich der Kolben wie immer.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(repairNugget()) || damageOf(state) == 0) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
        if (world instanceof ServerLevel server) {
            server.setBlock(pos, withDamage(state, 0), Block.UPDATE_CLIENTS);
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
     * Jeder so gebrochene Block kostet 1 Haltbarkeit ({@link #addWear}); ist der Brecher damit
     * aufgebraucht, faehrt statt seiner die Stufe darunter aus ({@link #wearOut}).
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
                        BlockState worn = addWear(server, pos, state);
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
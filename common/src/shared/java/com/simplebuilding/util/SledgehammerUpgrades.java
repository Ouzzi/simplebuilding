package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Maschinen in der Welt aufwerten: Vorschlaghammer in der Haupthand, Nugget in der Nebenhand,
 * Rechtsklick fuenf Sekunden auf der Maschine halten.
 *
 * <ul>
 *   <li>Verstaerkt -&gt; Netherit mit einem Netherit-Nugget, ab einem Diamant-Vorschlaghammer,
 *       {@value #NETHERITE_DAMAGE_PER_HIT} Haltbarkeit je Schlag;</li>
 *   <li>Netherit -&gt; Enderit mit einem Enderit-Nugget, ab einem Netherit-Vorschlaghammer,
 *       {@value #ENDERITE_DAMAGE_PER_HIT} Haltbarkeit je Schlag.</li>
 * </ul>
 * Aufwertbar sind Trichter, Ofen, Raeucherofen, Schmelzofen und der (nicht klebrige) Kolben.
 *
 * <p><b>Ablauf.</b> {@link #tryBegin} legt je Spieler und Seite einen Auftrag an und startet eine
 * Item-Benutzung von genau {@value #UPGRADE_TICKS} Ticks. {@link #tick} prueft jeden Tick, ob der
 * Auftrag noch gilt, und schlaegt in den Ticks 20, 40, 60 und 80 zu; der fuenfte Schlag in Tick 100
 * ist {@link #finish}: Der Block wird unter Beibehaltung seiner Eigenschaften (Blickrichtung,
 * LIT, ENABLED ...) durch die naechste Stufe ersetzt, seine Block-Entity bleibt dieselbe (siehe
 * {@code shouldChangedStateKeepBlockEntity} in den vier Mod-Blockklassen), und ein Nugget wird
 * verbraucht - im Kreativmodus weder Nugget noch Haltbarkeit.
 *
 * <p><b>Abbruch</b>, ohne Nugget und ohne Umbau (schon geschlagene Schlaege bleiben bezahlt):
 * Rechtsklick losgelassen, Blick fuer mehr als {@value #AIM_GRACE_TICKS} Ticks nicht mehr auf dem
 * Block, ausser Reichweite, der Block ist nicht mehr die Ausgangsstufe, das Nugget ist nicht mehr in
 * der Nebenhand, der Hammer zerbricht.
 *
 * <p><b>Nur wenn aufwertbar.</b> Mit einem Nugget in der Nebenhand faengt der Hammer nur dann an zu
 * schmieden, wenn der Block mit genau diesem Nugget und diesem Hammer aufgewertet werden kann; sonst
 * bleibt alles beim Alten (Menue oeffnen, Umformen, Diamantblock zerschlagen). Bei einer
 * Maschinen-Stufe, die nur am falschen Nugget, am zu schwachen Hammer oder an einem ausgefahrenen
 * bzw. mit Strom versorgten Kolben scheitert, erscheint ein Hinweis in der Aktionsleiste.
 *
 * <p><b>Seiten.</b> Server und Client fuehren je eine eigene Auftragsliste (im Einzelspieler laufen
 * beide im selben Prozess). Der Client prueft genauso wie der Server und bricht bei sich selbst ab;
 * Schlaege, Klaenge, Partikel, Haltbarkeit und der Umbau passieren nur auf dem Server, der
 * Armschwung geht an alle Beobachter und an den Spieler selbst.
 */
public final class SledgehammerUpgrades {

    /** Dauer einer Aufwertung: fuenf Sekunden. */
    public static final int UPGRADE_TICKS = 100;
    /** Ein Hammerschlag pro Sekunde. */
    public static final int HIT_INTERVAL = 20;
    public static final int NETHERITE_DAMAGE_PER_HIT = 4;
    public static final int ENDERITE_DAMAGE_PER_HIT = 10;
    /**
     * Abklingzeit des Hammers nach einer fertigen Aufwertung. Solange sie laeuft, oeffnet ein
     * weiter gehaltener Rechtsklick auf der gerade aufgewerteten Maschine nicht deren Menue.
     */
    public static final int FINISH_COOLDOWN_TICKS = 20;
    /**
     * So viele Ticks hintereinander darf der Blick den Block verfehlen, bevor der Server abbricht:
     * er kennt die Blickrichtung erst einen Tick nach dem Client. Der Client selbst bricht sofort ab.
     */
    public static final int AIM_GRACE_TICKS = 2;
    /** Zusaetzliche Reichweite auf dem Server, wie Vanillas eigene Pruefung sie gewaehrt. */
    private static final double SERVER_REACH_BUFFER = 1.0;
    /** Mindestabstand zwischen zwei Hinweisen in der Aktionsleiste. */
    private static final int HINT_INTERVAL_TICKS = 60;

    /** Rang der Hammerstufen fuer die Mindestanforderung; alles unter Diamant ist 0. */
    public static final int RANK_DIAMOND = 1;
    public static final int RANK_NETHERITE = 2;
    public static final int RANK_ENDERITE = 3;

    /** Eine Aufwertungsstufe: von welchem Block zu welchem, mit welchem Nugget und welchem Hammer. */
    public record Upgrade(Block from, Block to, Item nugget, int minHammerRank, int damagePerHit, boolean toEnderite) {
    }

    /** Ein laufender Auftrag eines Spielers. */
    private static final class Job {
        final BlockPos pos;
        final Upgrade upgrade;
        final Vec3 hitPoint;
        final Direction face;
        int missedAimTicks;

        Job(BlockPos pos, Upgrade upgrade, Vec3 hitPoint, Direction face) {
            this.pos = pos;
            this.upgrade = upgrade;
            this.hitPoint = hitPoint;
            this.face = face;
        }
    }

    private static final Map<UUID, Job> SERVER_JOBS = new HashMap<>();
    private static final Map<UUID, Job> CLIENT_JOBS = new HashMap<>();
    private static final Map<UUID, Long> LAST_HINT = new HashMap<>();
    private static @Nullable Map<Block, Upgrade> table;

    private SledgehammerUpgrades() {
    }

    // =====================================================================================
    // TABELLE UND BEDINGUNGEN
    // =====================================================================================

    private static Map<Block, Upgrade> table() {
        if (table == null) {
            Map<Block, Upgrade> map = new HashMap<>();
            toNetherite(map, ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER);
            toNetherite(map, ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE);
            toNetherite(map, ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER);
            toNetherite(map, ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE);
            toNetherite(map, ModBlocks.REINFORCED_PISTON, ModBlocks.NETHERITE_PISTON);
            toEnderite(map, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER);
            toEnderite(map, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE);
            toEnderite(map, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER);
            toEnderite(map, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE);
            toEnderite(map, ModBlocks.NETHERITE_PISTON, ModBlocks.ENDERITE_PISTON);
            table = map;
        }
        return table;
    }

    private static void toNetherite(Map<Block, Upgrade> map, Block from, Block to) {
        map.put(from, new Upgrade(from, to, ModItems.NETHERITE_NUGGET, RANK_DIAMOND, NETHERITE_DAMAGE_PER_HIT, false));
    }

    private static void toEnderite(Map<Block, Upgrade> map, Block from, Block to) {
        map.put(from, new Upgrade(from, to, ModItems.ENDERITE_NUGGET, RANK_NETHERITE, ENDERITE_DAMAGE_PER_HIT, true));
    }

    /** Die Aufwertung, die von {@code block} ausgeht, oder null (keine Maschine, oder schon Enderit). */
    public static @Nullable Upgrade upgradeOf(Block block) {
        return table().get(block);
    }

    public static boolean isUpgradeNugget(ItemStack stack) {
        return stack.is(ModItems.NETHERITE_NUGGET) || stack.is(ModItems.ENDERITE_NUGGET);
    }

    /** Hammer in der Haupthand, Netherit- oder Enderit-Nugget in der Nebenhand. */
    public static boolean isSmithingStance(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof SledgehammerItem && isUpgradeNugget(entity.getOffhandItem());
    }

    /** Diamant 1, Netherit 2, Enderit 3, jede andere Stufe (und jedes andere Item) 0. */
    public static int hammerRank(ItemStack stack) {
        if (stack.is(ModItems.ENDERITE_SLEDGEHAMMER)) {
            return RANK_ENDERITE;
        }
        if (stack.is(ModItems.NETHERITE_SLEDGEHAMMER)) {
            return RANK_NETHERITE;
        }
        if (stack.is(ModItems.DIAMOND_SLEDGEHAMMER)) {
            return RANK_DIAMOND;
        }
        return 0;
    }

    /**
     * Ein Kolben, der gerade ausgefahren ist oder Strom bekommt, wird nicht aufgewertet: als
     * Brecher wuerde er sofort nach dem Umbau ausfahren und den Block vor sich zerstoeren oder,
     * mit einem Redstoneblock daneben, einen Durchbruch ausloesen und sich selbst verbrauchen.
     */
    public static boolean isBusyPiston(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PistonBaseBlock)) {
            return false;
        }
        return state.getValue(PistonBaseBlock.EXTENDED) || level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
    }

    /** Warum die Aufwertung nicht beginnen kann (Sprachschluessel-Endung), oder null, wenn sie kann. */
    private static @Nullable String refusal(Level level, BlockPos pos, BlockState state, Player player, Upgrade upgrade) {
        if (!player.getOffhandItem().is(upgrade.nugget())) {
            return "wrong_nugget";
        }
        if (hammerRank(player.getMainHandItem()) < upgrade.minHammerRank()) {
            return "hammer_too_weak";
        }
        if (isBusyPiston(level, pos, state)) {
            return "piston_busy";
        }
        return null;
    }

    /** Ob ein Rechtsklick mit Hammer und Nugget auf diesen Block jetzt eine Aufwertung beginnt. */
    public static boolean canBegin(BlockState state, Level level, BlockPos pos, Player player) {
        if (!isSmithingStance(player)) {
            return false;
        }
        Upgrade upgrade = upgradeOf(state.getBlock());
        return upgrade != null && refusal(level, pos, state, player, upgrade) == null;
    }

    /**
     * Fuer die vier Mod-Blockklassen mit Menue: im Schmiedestand laeuft der Rechtsklick nicht ins
     * Menue, sondern an den Hammer weiter - wenn die Aufwertung beginnen kann, oder solange der
     * Hammer nach einer fertigen Aufwertung abkuehlt. Sonst oeffnet das Menue wie immer; scheitert es
     * nur am Nugget, am Hammer oder am Kolben, erscheint zusaetzlich der Hinweis.
     */
    public static boolean shouldSkipBlockUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isSmithingStance(player)) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(player.getMainHandItem())) {
            return true;
        }
        Upgrade upgrade = upgradeOf(state.getBlock());
        if (upgrade == null) {
            return false;
        }
        String refusal = refusal(level, pos, state, player, upgrade);
        if (refusal != null) {
            hint(level, player, refusal, upgrade);
            return false;
        }
        return true;
    }

    // =====================================================================================
    // ABLAUF
    // =====================================================================================

    private static Map<UUID, Job> jobs(Level level) {
        return level.isClientSide() ? CLIENT_JOBS : SERVER_JOBS;
    }

    /** Ob fuer diesen Spieler auf seiner Seite gerade eine Aufwertung laeuft. */
    public static boolean hasJob(Player player) {
        return jobs(player.level()).containsKey(player.getUUID());
    }

    /**
     * Fuer die Render-Mixins: haut dieses Wesen gerade mit dem Hammer auf eine Maschine? Beim
     * eigenen Spieler entscheidet der Auftrag, bei allen anderen, was sie in den Haenden halten.
     */
    public static boolean isHammering(LivingEntity entity) {
        if (!entity.isUsingItem() || entity.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (entity instanceof Player player && player.isLocalPlayer()) {
            return CLIENT_JOBS.containsKey(player.getUUID());
        }
        return isSmithingStance(entity);
    }

    /**
     * Rechtsklick des Hammers auf einen Block. Null, wenn der Block mit diesem Nugget und diesem
     * Hammer nicht aufwertbar ist - dann laeuft das gewohnte Verhalten des Hammers weiter.
     */
    public static @Nullable InteractionResult tryBegin(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return null;
        }
        Level level = context.getLevel();
        // Jede Benutzung des Hammers beginnt hier, also faengt hier auch jede frisch an: ein
        // liegengebliebener Auftrag (Benutzung durch Slotwechsel beendet, ohne releaseUsing) darf
        // weder die Dauer noch die Ticks einer Umform-Ladung uebernehmen.
        jobs(level).remove(player.getUUID());
        if (!level.isClientSide() && player.isUsingItem()) {
            // Der Client schickt keinen Klick, solange er selbst benutzt; kommt trotzdem einer, hat
            // er eine Benutzung schon bei sich beendet. Neu anfangen, damit beide Seiten wieder
            // dieselben Ticks zaehlen.
            player.stopUsingItem();
        }
        if (context.getHand() != InteractionHand.MAIN_HAND || !isSmithingStance(player)) {
            return null;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Upgrade upgrade = upgradeOf(state.getBlock());
        if (upgrade == null) {
            return null;
        }
        String refusal = refusal(level, pos, state, player, upgrade);
        if (refusal != null) {
            hint(level, player, refusal, upgrade);
            return null;
        }
        if (!player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        jobs(level).put(player.getUUID(), new Job(pos.immutable(), upgrade, context.getClickLocation(), context.getClickedFace()));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        return InteractionResult.CONSUME;
    }

    /** Jeder Tick der Benutzung, auf beiden Seiten; {@code remainingTicks} laeuft von 100 bis 1. */
    public static void tick(Level level, Player player, ItemStack hammer, int remainingTicks) {
        Job job = jobs(level).get(player.getUUID());
        if (job == null) {
            return;
        }
        if (!stillValid(level, player, job)) {
            player.releaseUsingItem();
            return;
        }
        int elapsed = UPGRADE_TICKS - remainingTicks + 1;
        if (elapsed % HIT_INTERVAL == 0 && elapsed < UPGRADE_TICKS && level instanceof ServerLevel serverLevel) {
            strike(serverLevel, player, hammer, job, elapsed / HIT_INTERVAL);
        }
    }

    /**
     * Der fuenfte Schlag. Gibt zurueck, ob eine Aufwertung lief - dann darf die Umform- und
     * Zerschlag-Logik des Hammers danach nicht mehr laufen, auch wenn der Auftrag abgebrochen ist.
     */
    public static boolean finish(Level level, Player player, ItemStack hammer) {
        Job job = jobs(level).remove(player.getUUID());
        if (job == null) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel) || !stillValid(level, player, job)) {
            return true;
        }
        BlockState old = level.getBlockState(job.pos);
        BlockState upgraded = job.upgrade.to().withPropertiesOf(old);
        level.setBlock(job.pos, upgraded, Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_CHANGE, job.pos, GameEvent.Context.of(player, upgraded));
        // Im Kreativmodus verbraucht consume nichts, hurtAndBreak kostet nichts.
        player.getOffhandItem().consume(1, player);
        finishEffects(serverLevel, job, old);
        player.swing(InteractionHand.MAIN_HAND, true);
        hammer.hurtAndBreak(job.upgrade.damagePerHit(), player, EquipmentSlot.MAINHAND);
        if (!hammer.isEmpty() && hasConnection(player)) {
            player.getCooldowns().addCooldown(hammer, FINISH_COOLDOWN_TICKS);
        }
        return true;
    }

    /** Rechtsklick losgelassen oder Benutzung sonst beendet: Auftrag verwerfen. */
    public static void clear(LivingEntity entity) {
        if (entity instanceof Player player) {
            jobs(player.level()).remove(player.getUUID());
        }
    }

    private static boolean stillValid(Level level, Player player, Job job) {
        BlockState state = level.getBlockState(job.pos);
        Upgrade upgrade = job.upgrade;
        if (!state.is(upgrade.from())
                || !(player.getMainHandItem().getItem() instanceof SledgehammerItem)
                || !player.getOffhandItem().is(upgrade.nugget())
                || hammerRank(player.getMainHandItem()) < upgrade.minHammerRank()
                || isBusyPiston(level, job.pos, state)) {
            return false;
        }
        boolean server = !level.isClientSide();
        double reach = player.blockInteractionRange() + (server ? SERVER_REACH_BUFFER : 0.0);
        HitResult hit = player.pick(reach, 1.0F, false);
        boolean aimed = hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(job.pos);
        if (aimed) {
            job.missedAimTicks = 0;
            return true;
        }
        return server && ++job.missedAimTicks <= AIM_GRACE_TICKS;
    }

    // =====================================================================================
    // EFFEKTE (nur Server)
    // =====================================================================================

    private static void strike(ServerLevel level, Player player, ItemStack hammer, Job job, int hitNumber) {
        BlockState state = level.getBlockState(job.pos);
        float pitch = 0.75F + 0.08F * hitNumber + (level.getRandom().nextFloat() - 0.5F) * 0.1F;
        level.playSound(null, job.pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35F, pitch);
        level.playSound(null, job.pos, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.BLOCKS, 1.0F, job.upgrade.toEnderite() ? 0.5F : 0.6F);
        if (job.upgrade.toEnderite()) {
            level.playSound(null, job.pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.4F, 1.2F + 0.1F * hitNumber);
        }

        Vec3 at = impactPoint(job);
        burst(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at, 10, 0.2, 0.15);
        burst(level, ParticleTypes.CRIT, at, 8, 0.1, 0.35);
        if (job.upgrade.toEnderite()) {
            burst(level, ParticleTypes.REVERSE_PORTAL, at, 10, 0.2, 0.05);
        } else {
            burst(level, ParticleTypes.LAVA, at, 2, 0.1, 0.0);
        }

        player.swing(InteractionHand.MAIN_HAND, true);
        hammer.hurtAndBreak(job.upgrade.damagePerHit(), player, EquipmentSlot.MAINHAND);
        if (hammer.isEmpty()) {
            // Zerbrochen: die Benutzung endet im naechsten Tick von selbst (anderes Item in der Hand).
            SERVER_JOBS.remove(player.getUUID());
        }
    }

    private static void finishEffects(ServerLevel level, Job job, BlockState oldState) {
        level.playSound(null, job.pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, job.pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.25F);
        Vec3 at = impactPoint(job);
        Vec3 center = Vec3.atCenterOf(job.pos);
        burst(level, new BlockParticleOption(ParticleTypes.BLOCK, oldState), at, 20, 0.25, 0.2);
        burst(level, ParticleTypes.CRIT, at, 14, 0.15, 0.45);
        if (job.upgrade.toEnderite()) {
            level.playSound(null, job.pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.9F);
            level.playSound(null, job.pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 1.2F);
            burst(level, ParticleTypes.REVERSE_PORTAL, center, 40, 0.5, 0.1);
            burst(level, ParticleTypes.END_ROD, center, 12, 0.4, 0.05);
        } else {
            level.playSound(null, job.pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.4F);
            burst(level, ParticleTypes.FLAME, center, 12, 0.4, 0.02);
            burst(level, ParticleTypes.LARGE_SMOKE, center, 10, 0.4, 0.02);
            burst(level, ParticleTypes.LAVA, center, 6, 0.3, 0.0);
        }
    }

    /** Der Trefferpunkt des Klicks, eine Spur vor die Flaeche gezogen. */
    private static Vec3 impactPoint(Job job) {
        return job.hitPoint.add(job.face.getStepX() * 0.05, job.face.getStepY() * 0.05, job.face.getStepZ() * 0.05);
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    // =====================================================================================
    // HINWEISE
    // =====================================================================================

    private static boolean hasConnection(Player player) {
        return !(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection != null;
    }

    private static void hint(Level level, Player player, String reason, Upgrade upgrade) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_HINT.get(player.getUUID());
        if (last != null && now >= last && now - last < HINT_INTERVAL_TICKS) {
            return;
        }
        LAST_HINT.put(player.getUUID(), now);
        MutableComponent message = switch (reason) {
            case "wrong_nugget" -> Component.translatable("message.simplebuilding.smithing.wrong_nugget",
                    Component.translatable(upgrade.nugget().getDescriptionId()));
            case "hammer_too_weak" -> Component.translatable("message.simplebuilding.smithing.hammer_too_weak",
                    Component.translatable((upgrade.minHammerRank() >= RANK_NETHERITE
                            ? ModItems.NETHERITE_SLEDGEHAMMER : ModItems.DIAMOND_SLEDGEHAMMER).getDescriptionId()));
            default -> Component.translatable("message.simplebuilding.smithing.piston_busy");
        };
        serverPlayer.sendOverlayMessage(message.withStyle(ChatFormatting.RED));
    }

    /**
     * Die Hinweiszeile fuer die Tooltips der aufwertbaren Maschinen, oder null. Verstaerkte
     * Maschinen nennen das Netherit-Nugget, Netherit-Maschinen das Enderit-Nugget.
     */
    public static @Nullable Component tooltipHint(ItemStack stack) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return null;
        }
        Upgrade upgrade = upgradeOf(blockItem.getBlock());
        if (upgrade == null) {
            return null;
        }
        return Component.translatable(upgrade.toEnderite()
                ? "tooltip.simplebuilding.hammer_upgrade.enderite"
                : "tooltip.simplebuilding.hammer_upgrade.netherite").withStyle(ChatFormatting.GRAY);
    }
}

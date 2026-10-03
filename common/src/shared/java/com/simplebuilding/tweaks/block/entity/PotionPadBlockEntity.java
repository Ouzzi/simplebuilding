package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.util.PlayerScan;
import com.simplebuilding.tweaks.PotionPadRules;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.PotionPadBlock;
import com.simplebuilding.tweaks.component.TweaksComponents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Trank-Pad: haelt den zuletzt darauf geworfenen Trank ({@link PotionContents}, Schluessel
 * {@code Potion}) und gibt Spielern, die darauf stehen, dessen Wirkungen (siehe {@link PotionPadBlock}).
 *
 * <p><b>Aufladen in drei Schritten</b> (Besitzer 2026-09-28): wer auf dem Pad steht, bekommt nach
 * 1 s 25 %, nach 2 s 50 % und nach 3 s 100 % der Stufendauer ({@link #RAMP_PERCENT}); jeder Schritt
 * zeigt Partikel in der Trankfarbe und einen leisen Klang. Wer vorher absteigt, behaelt, was er schon
 * hat, und faengt beim naechsten Betreten wieder bei 0 an. Sofortwirkungen (Heilung, Schaden) wirken
 * einmal, beim 3-s-Schritt.
 *
 * <p><b>Abklingzeit</b>: erst der 100-%-Schritt setzt das ganze Pad fuer die doppelte Wirkdauer in die
 * Abklingzeit ({@link PotionPadBlock#cooldownAt}); ein abgebrochenes Aufladen startet sie nicht (sonst
 * koennte ein kurzer Schritt das Pad fuer alle sperren). Waehrend der Abklingzeit gibt das Pad nichts,
 * und sie laeuft nur, solange das Pad gesetzt ist (der Block-Entity-Ticker). Abgebaut reist die
 * Restzeit als {@link TweaksComponents#POTION_PAD_COOLDOWN} mit dem Item und laeuft beim Setzen weiter;
 * der Blockzustand {@link PotionPadBlock#COOLING} zeigt die animierte Textur.
 *
 * <p>Der Trank reist als {@code potion_contents} mit dem Item (Abbau, Strg+Mittelklick) und kommt beim
 * Setzen zurueck, neben der Easter-Stufe der Basisklasse. Alle halbe Sekunde steigen Partikel in der
 * Trankfarbe auf, solange das Pad bereit ist.
 *
 * <p><b>Automatisierung</b> (Besitzer 2026-09-28): Traenke kommen nur ueber einen Wurftrank aufs Pad,
 * der darauf zerschellt - ein Werfer kann ihn werfen; ein Trichter kann nichts einlegen (das Pad ist
 * kein Behaelter), und weitergegeben wird genau die Wirkung dieses Tranks. Redstone schaltet das Pad
 * ab (die Abklingzeit laeuft weiter); der Komparator liest 0 ohne Trank, 15 bereit und waehrend der
 * Abklingzeit 1..14, je nachdem, wie weit sie ist ({@link #comparatorSignal}).
 *
 * <p><b>Balance je Wirkung</b> (Besitzer 2026-09-29, docs/TRANK-PADS.md, {@link PotionPadRules}): der Trank
 * wird nie verbraucht. Wer eine Wirkung bekommt (alle, nur der Besitzer, niemand), wie stark (hoechstens die
 * Vanilla-Stufe), wie lange (Stufendauer, aber nie laenger als der Trank selbst) und wie oft (Abklingzeit aus
 * der laengsten gegebenen Dauer mal Wirkungsfaktor, dazu eine Sperre je Spieler fuer Heilung/Schaden/
 * Regeneration ueber alle Pads) steht in den Regeln. Ein Spieler, fuer den nichts erlaubt ist, laedt gar
 * nicht auf und kann das Pad so auch nicht fuer andere in die Abklingzeit schicken. Mobs bekommen nie etwas.
 */
public class PotionPadBlockEntity extends OwnedBlockEntity implements PadSignalSource {
    /** Ein Aufladeschritt dauert eine Sekunde. */
    public static final int RAMP_STEP_TICKS = 20;
    /** Anteil der Stufendauer nach Schritt 1, 2, 3. */
    public static final int[] RAMP_PERCENT = {25, 50, 100};
    /** Zahl der Schritte; der letzte gibt die volle Dauer und startet die Abklingzeit. */
    public static final int RAMP_STEPS = RAMP_PERCENT.length;

    private @Nullable PotionContents stored;
    /** Verbleibende Abklingzeit in Ticks (0 = bereit). */
    private int cooldown;
    /** Volle Laenge der laufenden Abklingzeit (fuer den Komparator; 0 = unbekannt, dann die Stufen-Abklingzeit). */
    private int cooldownTotal;
    /** Wie lange jeder Spieler schon ununterbrochen auf dem Pad steht, in Ticks (nicht gespeichert). */
    private final Map<UUID, Integer> standing = new HashMap<>();

    public PotionPadBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.POTION_PAD, pos, state);
    }

    public static boolean isThrownPotion(Projectile projectile) {
        return projectile instanceof AbstractThrownPotion;
    }

    public @Nullable PotionContents getStored() {
        return stored;
    }

    public void setStored(@Nullable PotionContents contents) {
        this.stored = contents == null || !contents.hasEffects() ? null : contents;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Verbleibende Abklingzeit in Ticks (0 = bereit). */
    public int getCooldown() {
        return cooldown;
    }

    public boolean isCoolingDown() {
        return cooldown > 0;
    }

    /** Setzt die Abklingzeit und zieht den Blockzustand {@link PotionPadBlock#COOLING} nach. */
    public void setCooldown(int ticks) {
        this.cooldown = Math.max(0, ticks);
        this.cooldownTotal = cooldown == 0 ? 0 : Math.max(cooldownTotal, cooldown);
        this.standing.clear();
        setChanged();
        syncCoolingState();
    }

    /** Farbe der gespeicherten Wirkungen, fuer die Partikel. */
    public int color() {
        return stored == null ? 0 : stored.getColor();
    }

    /** Anteil der Stufendauer nach Schritt {@code step} (1..{@link #RAMP_STEPS}). */
    public static int rampPercent(int step) {
        return RAMP_PERCENT[Math.max(1, Math.min(RAMP_STEPS, step)) - 1];
    }

    /**
     * 0 ohne Trank, 15 bereit; in der Abklingzeit steigt das Signal von 1 bis 14, je weiter sie
     * abgelaufen ist (gemessen an der vollen Abklingzeit des gesetzten Pads).
     */
    @Override
    public int comparatorSignal() {
        if (stored == null) {
            return 0;
        }
        if (cooldown <= 0) {
            return 15;
        }
        int total = cooldownTotal > 0 ? cooldownTotal
                : level != null && getBlockState().getBlock() instanceof PotionPadBlock pad ? pad.cooldownAt(level, worldPosition) : 0;
        if (total <= 0) {
            return 1;
        }
        int elapsed = Math.max(0, total - cooldown);
        return Math.max(1, Math.min(14, 1 + (int) (13L * elapsed / total)));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PotionPadBlockEntity be) {
        tickPad(level, pos, state, be);
        if (!be.isRemoved() && level.getBlockState(pos).getBlock() instanceof PotionPadBlock) {
            // Sichtbarer Zustand (Besitzer 2026-09-29): hell gluehend, solange es bereit ist.
            com.simplebuilding.tweaks.block.PadBlock.setActive(level, pos, PotionPadBlock.ACTIVE, be.isReady(level, pos));
        }
    }

    /** Bereit: Trank gespeichert, keine Abklingzeit, weder per Config noch per Redstone abgeschaltet. */
    public boolean isReady(Level level, BlockPos pos) {
        return stored != null && cooldown <= 0 && SimpleTweaks.config().pads.enablePotionPads
                && !com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, pos);
    }

    private static void tickPad(Level level, BlockPos pos, BlockState state, PotionPadBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (be.cooldown > 0) {
            int signalBefore = be.comparatorSignal();
            be.cooldown--;
            if (be.comparatorSignal() != signalBefore) {
                // Nur bei einem Stufenwechsel (hoechstens 14-mal je Abklingzeit) die Komparatoren wecken.
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            // Nur den Chunk als ungespeichert markieren: setChanged() fragte zusaetzlich jeden Tick die
            // vier Nachbarn nach Komparatoren ab (docs/PERFORMANCE.md); das Komparator-Signal meldet
            // sich oben nur, wenn es wirklich die Stufe wechselt. Der Wechsel des COOLING-Zustands meldet sich selbst per setBlock.
            level.blockEntityChanged(pos);
            if (be.cooldown == 0) {
                be.standing.clear();
                if (be.stored != null) {
                    serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | be.color()),
                            pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.0);
                }
            }
            be.syncCoolingState();
            return;
        }
        be.syncCoolingState();
        if (be.stored == null || !SimpleTweaks.config().pads.enablePotionPads
                || com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, pos)) {
            // Ohne Trank oder abgeschaltet (Config tweaks.pads.enablePotionPads, Redstone-Signal): nichts geben.
            be.standing.clear();
            return;
        }
        long time = level.getGameTime();
        if (time % 10 == 0) {
            serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | be.color()),
                    pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.0);
        }
        List<Player> players = PlayerScan.playersIn(level, area(pos), Player.class, p -> p.isAlive() && !p.isSpectator());
        Set<UUID> now = new HashSet<>();
        for (Player player : players) {
            if (!be.hasAnythingFor(player, time)) {
                // Nichts erlaubt (fremdes Pad mit schaedlicher Wirkung, gesperrt): kein Aufladen, keine Abklingzeit.
                continue;
            }
            UUID id = player.getUUID();
            now.add(id);
            int ticks = be.standing.merge(id, 1, Integer::sum);
            int step = stepTicks();
            if (ticks % step == 0 && ticks / step <= RAMP_STEPS) {
                be.grant(serverLevel, player, ticks / step);
                if (be.cooldown > 0) {
                    // Der 100-%-Schritt hat das Pad in die Abklingzeit gesetzt: niemand sonst bekommt mehr etwas.
                    return;
                }
            }
        }
        be.standing.keySet().retainAll(now);
    }

    /**
     * Dauer eines Aufladeschritts: Config {@code tweaks.padTuning.potionPadChargeStepTicks}
     * (Standard {@link #RAMP_STEP_TICKS}).
     */
    public static int stepTicks() {
        return SimpleTweaks.config().padTuning.potionPadStepTicks();
    }

    /** Bereich, in dem ein Spieler als "auf dem Pad" gilt: die Blockspalte des Pads, halber Block hoch. */
    public static AABB area(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1);
    }

    /**
     * Ob der Spieler von diesem Pad gerade irgendetwas bekaeme: mindestens eine gespeicherte Wirkung, die
     * {@link PotionPadRules} ihm erlaubt (Besitzer bzw. herrenloses Pad fuer schaedliche Wirkungen) und fuer
     * die er nicht gesperrt ist.
     */
    public boolean hasAnythingFor(Player player, long gameTime) {
        if (stored == null) {
            return false;
        }
        boolean owner = isOwnerOrUnowned(player);
        for (MobEffectInstance effect : stored.getAllEffects()) {
            if (PotionPadRules.allows(effect.getEffect(), owner)
                    && !PotionPadRules.isLockedOut(player.getUUID(), effect.getEffect(), gameTime)) {
                return true;
            }
        }
        return false;
    }

    /** Besitzer des Pads - oder niemandem gehoert es (per Befehl gesetzt), dann zaehlt jeder als Besitzer. */
    public boolean isOwnerOrUnowned(Player player) {
        return getOwner() == null || isOwner(player);
    }

    /**
     * Aufladeschritt {@code step} (1..{@link #RAMP_STEPS}) nach {@link PotionPadRules}: jede fuer diesen
     * Spieler erlaubte, nicht gesperrte Dauerwirkung mit {@link #rampPercent} ihrer vollen Dauer (Stufendauer,
     * gedeckelt durch die Dauer im Trank und die Regel) und hoechstens der Regel-Stufe (eine laengere Wirkung,
     * die der Spieler schon hat, bleibt, siehe {@code MobEffectInstance#update}); beim letzten Schritt
     * zusaetzlich die Sofortwirkungen, die Sperren je Spieler und die Abklingzeit aus der teuersten gegebenen
     * Wirkung. Ohne Trank, in der Abklingzeit oder wenn fuer den Spieler nichts erlaubt ist: nichts. Der Trank
     * bleibt immer im Pad.
     */
    public void grant(ServerLevel level, Player player, int step) {
        if (stored == null || cooldown > 0 || step < 1 || step > RAMP_STEPS) {
            return;
        }
        BlockState state = getBlockState();
        int tierTicks = state.getBlock() instanceof PotionPadBlock pad ? pad.effectDurationAt(level, worldPosition) : PotionPadBlock.effectDuration(1);
        boolean last = step == RAMP_STEPS;
        boolean owner = isOwnerOrUnowned(player);
        long now = level.getGameTime();
        double factor = SimpleTweaks.config().padTuning.potionPadCooldown();
        int padCooldown = 0;
        boolean gave = false;
        for (MobEffectInstance effect : stored.getAllEffects()) {
            Holder<MobEffect> type = effect.getEffect();
            if (!PotionPadRules.allows(type, owner) || PotionPadRules.isLockedOut(player.getUUID(), type, now)) {
                continue;
            }
            gave = true;
            int amplifier = PotionPadRules.amplifier(type, effect.getAmplifier());
            boolean instant = type.value().isInstantaneous();
            int full = instant ? 0 : PotionPadRules.fullDuration(effect, tierTicks);
            if (instant) {
                if (last) {
                    type.value().applyInstantaneousEffect(level, null, null, player, amplifier, 1.0);
                }
            } else {
                player.addEffect(new MobEffectInstance(type, Math.max(1, full * rampPercent(step) / 100), amplifier,
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
            if (last) {
                PotionPadRules.lockOut(player.getUUID(), type, now);
                padCooldown = Math.max(padCooldown, PotionPadRules.cooldownFor(type, instant, full, factor));
            }
        }
        if (!gave) {
            return;
        }
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.2;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | color()),
                x, y + 0.3 * step, z, 6 * step, 0.25, 0.15 * step, 0.25, 0.0);
        if (last) {
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.2F);
            com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.POTION_PAD);
            this.cooldownTotal = 0;
            setCooldown(padCooldown);
        } else {
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5F, 0.8F + 0.3F * step);
        }
    }

    /** Blockzustand {@link PotionPadBlock#COOLING} = Abklingzeit laeuft (animierte Textur). */
    private void syncCoolingState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        boolean cooling = cooldown > 0;
        if (state.getBlock() instanceof PotionPadBlock && state.hasProperty(PotionPadBlock.COOLING)
                && state.getValue(PotionPadBlock.COOLING) != cooling) {
            com.simplebuilding.tweaks.block.PadBlock.setActive(level, worldPosition, PotionPadBlock.COOLING, cooling);
            if (!cooling && stored != null && level instanceof ServerLevel server) {
                // Wieder bereit (Immersion 2026-09-28): ein Glockenspiel-Ton und ein Wirbel in der Trankfarbe.
                server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.2F);
                server.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | color()),
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.0);
            }
        }
    }

    /** Beim Setzen: Trank und Restabklingzeit vom Item uebernehmen (ein abgebautes Pad traegt beides). */
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        PotionContents contents = components.get(DataComponents.POTION_CONTENTS);
        this.stored = contents == null || !contents.hasEffects() ? null : contents;
        Integer rest = components.get(TweaksComponents.POTION_PAD_COOLDOWN);
        this.cooldown = rest == null ? 0 : Math.max(0, rest);
    }

    /** Fuer Strg+Mittelklick: Trank und Restabklingzeit zurueck aufs Item. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (stored != null) {
            components.set(DataComponents.POTION_CONTENTS, stored);
        }
        if (cooldown > 0) {
            components.set(TweaksComponents.POTION_PAD_COOLDOWN, cooldown);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Potion");
        output.discard("Cooldown");
        output.discard("CooldownTotal");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (stored != null) {
            output.store("Potion", PotionContents.CODEC, stored);
        }
        if (cooldown > 0) {
            output.putInt("Cooldown", cooldown);
            output.putInt("CooldownTotal", cooldownTotal);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored = input.read("Potion", PotionContents.CODEC).filter(PotionContents::hasEffects).orElse(null);
        this.cooldown = Math.max(0, input.getIntOr("Cooldown", 0));
        this.cooldownTotal = cooldown == 0 ? 0 : Math.max(cooldown, input.getIntOr("CooldownTotal", 0));
    }
}

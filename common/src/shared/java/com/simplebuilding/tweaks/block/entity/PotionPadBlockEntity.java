package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.util.PlayerScan;
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
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        int total = level != null && getBlockState().getBlock() instanceof PotionPadBlock pad ? pad.cooldownAt(level, worldPosition) : 0;
        if (total <= 0) {
            return 1;
        }
        int elapsed = Math.max(0, total - cooldown);
        return Math.max(1, Math.min(14, 1 + (int) (13L * elapsed / total)));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PotionPadBlockEntity be) {
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
     * Aufladeschritt {@code step} (1..{@link #RAMP_STEPS}): die Dauerwirkungen mit {@link #rampPercent}
     * der Stufendauer und der Verstaerkung des Tranks (eine laengere Wirkung, die der Spieler schon hat,
     * bleibt, siehe {@code MobEffectInstance#update}); beim letzten Schritt zusaetzlich die
     * Sofortwirkungen, danach beginnt die Abklingzeit. Ohne Trank oder in der Abklingzeit: nichts.
     */
    public void grant(ServerLevel level, Player player, int step) {
        if (stored == null || cooldown > 0 || step < 1 || step > RAMP_STEPS) {
            return;
        }
        BlockState state = getBlockState();
        int full = state.getBlock() instanceof PotionPadBlock pad ? pad.effectDurationAt(level, worldPosition) : PotionPadBlock.effectDuration(1);
        int duration = full * rampPercent(step) / 100;
        boolean last = step == RAMP_STEPS;
        for (MobEffectInstance effect : stored.getAllEffects()) {
            if (effect.getEffect().value().isInstantaneous()) {
                if (last) {
                    effect.getEffect().value().applyInstantaneousEffect(level, null, null, player, effect.getAmplifier(), 1.0);
                }
            } else {
                player.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.2;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | color()),
                x, y + 0.3 * step, z, 6 * step, 0.25, 0.15 * step, 0.25, 0.0);
        if (last) {
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.2F);
            com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.POTION_PAD);
            setCooldown(state.getBlock() instanceof PotionPadBlock pad ? pad.cooldownAt(level, worldPosition) : 2 * full);
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
            level.setBlock(worldPosition, state.setValue(PotionPadBlock.COOLING, cooling), Block.UPDATE_ALL);
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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (stored != null) {
            output.store("Potion", PotionContents.CODEC, stored);
        }
        if (cooldown > 0) {
            output.putInt("Cooldown", cooldown);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored = input.read("Potion", PotionContents.CODEC).filter(PotionContents::hasEffects).orElse(null);
        this.cooldown = Math.max(0, input.getIntOr("Cooldown", 0));
    }
}

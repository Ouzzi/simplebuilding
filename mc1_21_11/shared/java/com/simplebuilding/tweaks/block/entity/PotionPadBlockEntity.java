package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.tweaks.block.PotionPadBlock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * {@code Potion}) und gibt Spielern, die es betreten, dessen Wirkungen (siehe {@link PotionPadBlock}).
 *
 * <p>Ablauf je Tick: wer neu auf dem Pad steht, bekommt alle Wirkungen; wer stehen bleibt, bekommt
 * die Dauerwirkungen jede Sekunde wieder auf die volle Stufendauer aufgefrischt (Vanillas
 * {@code MobEffectInstance#update} verlaengert nur bis zu dieser Dauer, nie darueber). Sofortwirkungen
 * nur beim Betreten und hoechstens alle {@link #INSTANT_COOLDOWN_TICKS} Ticks je Spieler, damit Hin-
 * und Herhuepfen kein Dauerheilen wird. Alle halbe Sekunde steigen Partikel in der Trankfarbe auf.
 */
public class PotionPadBlockEntity extends OwnedBlockEntity {
    /** Sofortwirkungen (Heilung/Schaden): hoechstens einmal je 2 s je Spieler. */
    public static final int INSTANT_COOLDOWN_TICKS = 40;
    /** Dauerwirkungen stehender Spieler werden so oft aufgefrischt. */
    public static final int REFRESH_TICKS = 20;

    private @Nullable PotionContents stored;
    /** Spieler, die beim letzten Tick auf dem Pad standen (nicht gespeichert). */
    private final Set<UUID> standing = new HashSet<>();
    /** Spielzeit der letzten Sofortwirkung je Spieler (nicht gespeichert). */
    private final Map<UUID, Long> lastInstant = new HashMap<>();

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

    /** Farbe der gespeicherten Wirkungen, fuer die Partikel. */
    public int color() {
        return stored == null ? 0 : stored.getColor();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PotionPadBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long time = level.getGameTime();
        if (be.stored != null && time % 10 == 0) {
            serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | be.color()),
                    pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.0);
        }
        List<Player> players = level.getEntitiesOfClass(Player.class, area(pos), p -> p.isAlive() && !p.isSpectator());
        Set<UUID> now = new HashSet<>();
        for (Player player : players) {
            UUID id = player.getUUID();
            now.add(id);
            boolean entered = !be.standing.contains(id);
            if (be.stored != null && (entered || time % REFRESH_TICKS == 0)) {
                be.apply(serverLevel, player, entered, time, state);
            }
        }
        be.standing.clear();
        be.standing.addAll(now);
        be.lastInstant.values().removeIf(t -> time - t > INSTANT_COOLDOWN_TICKS);
    }

    /** Bereich, in dem ein Spieler als "auf dem Pad" gilt: die Blockspalte des Pads, halber Block hoch. */
    public static AABB area(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1);
    }

    /**
     * Gibt dem Spieler die gespeicherten Wirkungen: Dauerwirkungen mit der Stufendauer und der
     * Verstaerkung des Tranks, Sofortwirkungen nur beim Betreten ({@code entered}) und ausserhalb der
     * Abklingzeit.
     */
    public void apply(ServerLevel level, Player player, boolean entered, long time, BlockState state) {
        if (stored == null) {
            return;
        }
        int duration = state.getBlock() instanceof PotionPadBlock pad ? pad.effectDuration() : PotionPadBlock.effectDuration(1);
        boolean instantAllowed = entered && time - lastInstant.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) >= INSTANT_COOLDOWN_TICKS;
        boolean instantApplied = false;
        for (MobEffectInstance effect : stored.getAllEffects()) {
            if (effect.getEffect().value().isInstantenous()) {
                if (instantAllowed) {
                    effect.getEffect().value().applyInstantenousEffect(level, null, null, player, effect.getAmplifier(), 1.0);
                    instantApplied = true;
                }
            } else {
                player.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.POTION_PAD);
        if (instantApplied) {
            lastInstant.put(player.getUUID(), time);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (stored != null) {
            output.store("Potion", PotionContents.CODEC, stored);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored = input.read("Potion", PotionContents.CODEC).filter(PotionContents::hasEffects).orElse(null);
    }
}

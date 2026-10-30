package com.simpledinos.block;

import com.simpledinos.DinoKind;
import com.simpledinos.DinoVariants;
import com.simpledinos.DinosRegistry;
import com.simpledinos.Progress;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.Dino;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Brooding state of an egg. A sitter rides an invisible armor stand (the seat; Vanilla armor stands that are invisible cannot be hurt); while somebody sits, the egg's scheduled tick
 * credits the elapsed ticks to the {@link Progress} (5 min = 100 %); after the sitter gets up, the progress runs back at twice
 * the speed. At 100 % the egg is brooded: it cracks now and then and hatches after {@code hatchSeconds}, for the sitter with
 * the most sitting time.
 */
public class DinoEggBlockEntity extends BlockEntity {
    /** Tag of the invisible seat entities. */
    public static final String SEAT_TAG = "simpledinos_seat";
    /** The seat sits this far below the egg's base: the rider's feet end up on top of the egg (armor stand: 1.975 high). */
    public static final double SEAT_DROP = 0.8;
    /** Ticks without a sitter before the progress starts to run back. */
    public static final int GRACE = 10;
    /** A single step never credits more than this many ticks (a long sleep of the chunk gives no free progress). */
    public static final int MAX_STEP = 40;
    public static final int CRACK_THIRD = 25, CRACK_SECOND = 55, CRACK_FIRST = 85;

    private final Progress progress = new Progress(GRACE);
    private long lastStep;
    private long incubatedAt;
    private long hatchAt;
    private @Nullable UUID seat;
    private @Nullable UUID owner;
    private final Map<UUID, Integer> sitting = new HashMap<>();
    private long nextCrack;

    public DinoEggBlockEntity(BlockPos pos, BlockState state) {
        super(DinosRegistry.EGG_BE, pos, state);
    }

    public DinoKind kind() {
        return getBlockState().getBlock() instanceof DinoEggBlock egg ? egg.kind() : DinoKind.TREX;
    }

    public Progress progressState() {
        return progress;
    }

    public boolean brooded() {
        return hatchAt > 0;
    }

    public long hatchAt() {
        return hatchAt;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public int sitTicks(UUID player) {
        return sitting.getOrDefault(player, 0);
    }

    // ------------------------------------------------------------------------------------------------ sitting

    /** Seats the player on the egg; false when somebody else sits on it. */
    public boolean sit(ServerLevel level, Player player) {
        Entity existing = seat == null ? null : level.getEntity(seat);
        if (existing instanceof ArmorStand taken && !taken.getPassengers().isEmpty() && !taken.hasPassenger(player)) return false;
        ArmorStand stand;
        if (existing instanceof ArmorStand s) {
            stand = s;
        } else {
            BlockPos pos = this.worldPosition;
            stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY() - SEAT_DROP, pos.getZ() + 0.5);
            stand.setInvisible(true);
            stand.setNoGravity(true);
            stand.addTag(SEAT_TAG);
            level.addFreshEntity(stand);
            seat = stand.getUUID();
        }
        player.startRiding(stand, true, false);
        lastStep = level.getGameTime();
        setChanged();
        level.scheduleTick(this.worldPosition, getBlockState().getBlock(), 5);
        return true;
    }

    private @Nullable Player sitter(ServerLevel level) {
        Entity stand = seat == null ? null : level.getEntity(seat);
        if (stand instanceof ArmorStand s && s.getFirstPassenger() instanceof Player p) return p;
        return null;
    }

    private void dropSeat(ServerLevel level) {
        Entity stand = seat == null ? null : level.getEntity(seat);
        if (stand != null) stand.discard();
        seat = null;
    }

    /** Called when the block goes away (broken, replaced, hatched): the seat must not outlive it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel server) dropSeat(server);
    }

    // ------------------------------------------------------------------------------------------------ progress

    /** The scheduled tick: reads the sitter and steps. */
    public void tickScheduled(ServerLevel level) {
        long now = level.getGameTime();
        Player sitter = sitter(level);
        if (seat != null && sitter == null && now - lastStep > GRACE) dropSeat(level);
        advance(level, now, sitter == null ? null : sitter.getUUID());
        BlockState state = level.getBlockState(this.worldPosition);
        if (!(state.getBlock() instanceof DinoEggBlock block)) return; // hatched: the block is gone
        if (brooded()) level.scheduleTick(this.worldPosition, block, 10);
        else if (sitter != null) level.scheduleTick(this.worldPosition, block, 5);
        else if (progress.value() > 0) level.scheduleTick(this.worldPosition, block, 10);
    }

    /** One step at game time {@code now} with an optional sitter (testable: no real ticks needed). */
    public void advance(ServerLevel level, long now, @Nullable UUID sitter) {
        if (hatchAt > 0) {
            stepBrooded(level, now);
            return;
        }
        int total = DinosConfig.incubateTicks();
        long dt = Math.max(0L, Math.min(now - lastStep, MAX_STEP));
        lastStep = now;
        if (sitter != null && dt > 0) {
            progress.add(now, (int) dt, total);
            sitting.merge(sitter, (int) dt, Integer::sum);
        } else {
            progress.settle(now);
        }
        setChanged();
        if (progress.value() >= total) {
            incubatedAt = now;
            hatchAt = now + DinosConfig.hatchTicks();
            owner = bestSitter();
            nextCrack = now + 200;
            dropSeat(level);
            level.playSound(null, this.worldPosition, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        updatePhase(level, now);
    }

    private @Nullable UUID bestSitter() {
        UUID best = null;
        int most = -1;
        for (Map.Entry<UUID, Integer> e : sitting.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** The state's phase for this moment: brooding step, or the crack step of a brooded egg. */
    public int phase(long now) {
        if (hatchAt <= 0) {
            int total = Math.max(1, DinosConfig.incubateTicks());
            return Math.min(DinoEggBlock.BROOD_PHASES - 1, (int) ((long) progress.value() * DinoEggBlock.BROOD_PHASES / total));
        }
        long span = Math.max(1L, hatchAt - incubatedAt);
        long percent = Math.max(0L, (now - incubatedAt) * 100L / span);
        if (percent >= CRACK_FIRST) return 14;
        if (percent >= CRACK_SECOND) return 13;
        if (percent >= CRACK_THIRD) return 12;
        return DinoEggBlock.BROOD_PHASES - 1;
    }

    private void updatePhase(ServerLevel level, long now) {
        BlockState state = getBlockState();
        int phase = phase(now);
        if (state.getValue(DinoEggBlock.PHASE) != phase) level.setBlock(this.worldPosition, state.setValue(DinoEggBlock.PHASE, phase), Block.UPDATE_CLIENTS);
    }

    private void stepBrooded(ServerLevel level, long now) {
        updatePhase(level, now);
        if (now >= nextCrack) {
            level.playSound(null, this.worldPosition, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.3F);
            nextCrack = now + 400 + level.getRandom().nextInt(800);
        }
        if (now >= hatchAt) hatch(level);
    }

    /** Hatches now if a player is near (the owner if online, else the nearest player within 16 blocks). Returns true when it hatched. */
    public boolean hatch(ServerLevel level) {
        EntityType<?> type = kind().entity();
        if (type == null) return false; // this kind has no dinosaur yet: the egg stays as it is
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            Player near = level.getNearestPlayer(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5, 16.0, false);
            if (near instanceof ServerPlayer sp) player = sp;
        }
        if (player == null) return false;
        Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (!(entity instanceof Dino dino)) return false;
        BlockPos pos = this.worldPosition;
        dino.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        dino.setCoat(DinoVariants.pick(level.getBiome(pos), level.getRandom())); // rearing biome: where the egg lay
        dino.hatch(player);
        level.removeBlock(pos, false);
        level.addFreshEntity(dino);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    // ------------------------------------------------------------------------------------------------ storage

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress.set(input.getIntOr("progress", 0), input.getLongOr("last_sat", 0L), input.getLongOr("settled", 0L));
        this.lastStep = input.getLongOr("last_step", 0L);
        this.incubatedAt = input.getLongOr("incubated_at", 0L);
        this.hatchAt = input.getLongOr("hatch_at", 0L);
        this.seat = input.read("seat", UUIDUtil.CODEC).orElse(null);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.sitting.clear();
        for (String part : input.getStringOr("sitters", "").split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            try {
                sitting.put(UUID.fromString(kv[0]), Integer.parseInt(kv[1]));
            } catch (IllegalArgumentException ignored) {
                // a damaged entry is dropped
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", this.progress.value());
        output.putLong("last_sat", this.progress.lastWork());
        output.putLong("settled", this.progress.settledAt());
        output.putLong("last_step", this.lastStep);
        output.putLong("incubated_at", this.incubatedAt);
        output.putLong("hatch_at", this.hatchAt);
        if (this.seat != null) output.store("seat", UUIDUtil.CODEC, this.seat);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        StringBuilder sb = new StringBuilder();
        sitting.forEach((id, ticks) -> sb.append(id).append(':').append(ticks).append(';'));
        output.putString("sitters", sb.toString());
    }
}

package com.simplebuilding.modules.simplemobs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Invisible marker of a running brood (Brood hollow K1), saved with the chunk. Every second it checks the hollow; when
 * the figure breaks (a soul torch gone, the purpur mined) the brood stops and every used item drops back. At the end the
 * Vanilla spawn egg of the species pops out of the hollow.
 */
public class BroodRitualEntity extends Entity {
    private @Nullable EntityType<?> species;
    private int age, duration = BroodRitual.TICKS[1];
    private List<ItemStack> refund = new ArrayList<>();

    public BroodRitualEntity(EntityType<? extends BroodRitualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    void setup(BlockPos hollow, EntityType<?> species, int duration, List<ItemStack> refund) {
        this.snapTo(hollow.getX() + 0.5, hollow.getY() + 1.0, hollow.getZ() + 0.5, 0f, 0f);
        this.species = species;
        this.duration = duration;
        this.refund = new ArrayList<>(refund);
    }

    public int age() { return age; }
    public int duration() { return duration; }
    public @Nullable EntityType<?> species() { return species; }

    /** Test hook: jump close to the end. */
    void skipTo(int ticksLeft) { this.age = Math.max(0, duration - ticksLeft); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
    @Override public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        this.age = in.getIntOr("Age", 0);
        this.duration = in.getIntOr("Duration", BroodRitual.TICKS[1]);
        Identifier id = Identifier.tryParse(in.getStringOr("Species", ""));
        this.species = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        this.refund = new ArrayList<>();
        in.listOrEmpty("Refund", ItemStack.CODEC).forEach(refund::add);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("Age", age);
        out.putInt("Duration", duration);
        if (species != null) out.putString("Species", BuiltInRegistries.ENTITY_TYPE.getKey(species).toString());
        ValueOutput.TypedOutputList<ItemStack> list = out.list("Refund", ItemStack.CODEC);
        for (ItemStack st : refund) if (!st.isEmpty()) list.add(st);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        BlockPos hollow = this.blockPosition().below();
        if (species == null) { this.discard(); return; }
        if (age % 20 == 0 && !BroodRitual.figure(level, hollow)) {
            abort(level, hollow);
            return;
        }
        age++;
        if (age % 4 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.1, this.getZ(), 2, 0.25, 0.05, 0.25, 0.01);
            if (age % 40 == 0) level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
        }
        if (age % 100 == 0) level.playSound(null, hollow, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.6F, 0.7F);
        if (age >= duration) finish(level, hollow);
    }

    private void finish(ServerLevel level, BlockPos hollow) {
        ItemStack egg = species == null ? ItemStack.EMPTY : BroodRitual.eggOf(species);
        if (egg.isEmpty()) { abort(level, hollow); return; }
        drop(level, egg);
        level.playSound(null, hollow, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        refund.clear();
        this.discard();
    }

    /** The figure broke: everything comes back, nothing is lost. */
    void abort(ServerLevel level, BlockPos hollow) {
        for (ItemStack st : refund) if (!st.isEmpty()) drop(level, st.copy());
        refund.clear();
        BossRituals.fizzle(level, hollow);
        this.discard();
    }

    private void drop(ServerLevel level, ItemStack st) {
        ItemEntity e = new ItemEntity(level, this.getX(), this.getY() + 0.2, this.getZ(), st);
        e.setDefaultPickUpDelay();
        level.addFreshEntity(e);
    }
}

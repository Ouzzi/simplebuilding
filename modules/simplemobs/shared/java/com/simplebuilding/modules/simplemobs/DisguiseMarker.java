package com.simplebuilding.modules.simplemobs;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Invisible, never saved companion of a disguised player. It carries the disguise (spec + shown gear) in its own synced
 * data, so vanilla entity tracking brings it to every client that sees the player - no loader networking and no synced
 * data on the foreign {@link Player} class (NeoForge forbids that). The server keeps it on the player's position.
 */
public class DisguiseMarker extends Entity {
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SPEC = SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.STRING);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<ItemStack>[] DATA_GEAR = new EntityDataAccessor[] {
            SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(DisguiseMarker.class, EntityDataSerializers.ITEM_STACK)};
    /** Client side only: owner entity id -> its marker. */
    private static final Map<Integer, DisguiseMarker> CLIENT = new HashMap<>();

    public DisguiseMarker(EntityType<? extends DisguiseMarker> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    /** The client-side marker of {@code owner}, or null. */
    public static @Nullable DisguiseMarker of(Player owner) {
        DisguiseMarker m = CLIENT.get(owner.getId());
        return m != null && !m.isRemoved() && m.level() == owner.level() ? m : null;
    }

    public void setup(Player owner, String spec, ItemStack[] gear) {
        this.entityData.set(DATA_OWNER, owner.getId());
        this.entityData.set(DATA_SPEC, spec);
        for (int i = 0; i < DATA_GEAR.length; i++) this.entityData.set(DATA_GEAR[i], gear[i]);
        this.snapTo(owner.getX(), owner.getY(), owner.getZ(), 0f, 0f);
    }

    public String spec() { return this.entityData.get(DATA_SPEC); }

    public ItemStack gear(int slot) { return this.entityData.get(DATA_GEAR[slot]); }

    public int ownerId() { return this.entityData.get(DATA_OWNER); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_SPEC, "");
        for (EntityDataAccessor<ItemStack> g : DATA_GEAR) builder.define(g, ItemStack.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!this.level().isClientSide()) return;
        if (DATA_OWNER.equals(key)) CLIENT.put(ownerId(), this);
        if (DATA_OWNER.equals(key) || DATA_SPEC.equals(key)) refreshOwner();
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        if (CLIENT.get(ownerId()) == this) CLIENT.remove(ownerId());
        refreshOwner();
    }

    private void refreshOwner() {
        if (this.level().getEntity(ownerId()) instanceof Player p) p.refreshDimensions();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            CLIENT.putIfAbsent(ownerId(), this);
            return;
        }
        Entity owner = this.level().getEntity(ownerId());
        if (!(owner instanceof Player p) || !Disguise.disguised(p) || Disguise.marker(p) != this) {
            this.discard();
            return;
        }
        this.setPos(p.getX(), p.getY(), p.getZ());
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(ValueInput in) { }
    @Override protected void addAdditionalSaveData(ValueOutput out) { }
}

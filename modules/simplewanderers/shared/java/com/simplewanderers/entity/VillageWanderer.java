package com.simplewanderers.entity;

import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.WandererRegistry;
import com.simplewanderers.config.WandererConfig;
import com.simplewanderers.logic.WandererTrades;
import com.simplewanderers.logic.WandererVillages;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Village Wanderer: a Wandering Trader that belongs to one village (owner answers W1-W10). He stays at the bell, is never
 * invisible, never despawns, brings one leashed horse, gets a fresh seeded stock every {@code rerollDays} and a restock every
 * {@code restockDays}, and his range follows the village biome. Killing him is allowed; the village record then waits
 * {@code respawnDays} while a player is at the bell (see WandererSpawner).
 */
public class VillageWanderer extends WanderingTrader {
    /** No village: summoned or spawned from an egg; such a wanderer is never recorded or counted. */
    public static final long NO_VILLAGE = Long.MIN_VALUE;
    public static final int OCHRE = 0xB5762E;

    /** What the rotation did this time. */
    public enum Rotation { NONE, RESTOCK, REROLL }

    private long villageId = NO_VILLAGE;
    private String villagerType = "plains";
    private long lastPeriod = Long.MIN_VALUE;
    private long lastHalf = Long.MIN_VALUE;
    private @Nullable UUID horse;
    private long horseGoneSince = -1L;
    private int horseMissing;
    private boolean duplicateCheck = true;

    public VillageWanderer(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Like the Wandering Trader, minus the invisibility potion and the wander target: he stays visible and near the bell.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Evoker.class, 12.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vindicator.class, 8.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vex.class, 8.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Pillager.class, 15.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Illusioner.class, 12.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zoglin.class, 10.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    // --- village binding -------------------------------------------------------------------------------------------------------

    /** Ties this wanderer to a village; called by the spawner right after the spawn. */
    public void bind(long village, String type, BlockPos bell) {
        this.villageId = village;
        this.villagerType = WandererTrades.TYPES.contains(type) ? type : "plains";
        setHomeTo(bell, WandererConfig.homeRadius);
        setPersistenceRequired();
    }

    public long villageId() {
        return villageId;
    }

    public String villagerType() {
        return villagerType;
    }

    /** Test hook: pretend this is another village type (biome assortment). */
    public void setVillagerType(String type) {
        this.villagerType = type;
    }

    public @Nullable UUID horseId() {
        return horse;
    }

    public long horseGoneSince() {
        return horseGoneSince;
    }

    // --- trades ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void updateTrades(ServerLevel level) {
        long period = lastPeriod == Long.MIN_VALUE ? WandererTrades.period(level.getGameTime()) : lastPeriod;
        long id = villageId == NO_VILLAGE ? getUUID().getLeastSignificantBits() : villageId;
        // AbstractVillager.getOffers assigns the list before it calls updateTrades.
        WandererTrades.draw(level, this, this.offers, id, period, villagerType);
    }

    /**
     * The once-a-second rotation check. A new reroll period drops and redraws the offers (a reroll is a restock, too); a new
     * restock period only resets the uses. Both are skipped while somebody trades, and an absence of many periods
     * costs one reroll, never a burst.
     */
    public Rotation tickRotation(long gameTime) {
        if (isTrading() || !(level() instanceof ServerLevel)) return Rotation.NONE;
        long period = WandererTrades.period(gameTime), half = WandererTrades.half(gameTime);
        if (lastPeriod == Long.MIN_VALUE) {
            lastPeriod = period;
            lastHalf = half;
            return Rotation.NONE;
        }
        if (period != lastPeriod) {
            lastPeriod = period;
            lastHalf = half;
            this.offers = null;
            return Rotation.REROLL;
        }
        if (half != lastHalf) {
            lastHalf = half;
            if (this.offers != null) for (MerchantOffer offer : this.offers) offer.resetUses();
            return Rotation.RESTOCK;
        }
        return Rotation.NONE;
    }

    // --- ticking ---------------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && !isRemoved()) {
            if (duplicateCheck) {
                duplicateCheck = false;
                if (removeIfDuplicate(server)) return;
            }
            if (tickCount % 20 == 0 && SimpleWanderers.on()) tickRotation(server.getGameTime());
            if (tickCount % 100 == 50 && SimpleWanderers.on()) horseUpkeep(server);
        }
    }

    /** Third guard of "exactly one per village": a loaded second wanderer for a recorded village removes itself. */
    private boolean removeIfDuplicate(ServerLevel server) {
        if (villageId == NO_VILLAGE) return false;
        WandererVillages data = WandererVillages.get(server.getServer());
        String key = WandererVillages.key(server, villageId);
        WandererVillages.Entry entry = data.get(key);
        if (entry == null || entry.wanderer().isEmpty()) {
            data.setAlive(key, getUUID());
            return false;
        }
        if (entry.wanderer().get().equals(getUUID())) return false;
        Entity other = server.getEntity(entry.wanderer().get());
        if (other instanceof VillageWanderer w && w.isAlive() && !w.isRemoved()) {
            SimpleWanderers.LOG.info("Removed a duplicate Village Wanderer {} (village {})", getUUID(), key);
            discard();
            return true;
        }
        // The recorded one is not loaded: this one is the village's wanderer from now on.
        data.setAlive(key, getUUID());
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        markVillageDead();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy()) markVillageDead();
        super.remove(reason);
    }

    private void markVillageDead() {
        if (villageId != NO_VILLAGE && level() instanceof ServerLevel server) {
            WandererVillages.get(server.getServer()).markDead(WandererVillages.key(server, villageId), getUUID(), server.getGameTime());
        }
    }

    // --- horse -----------------------------------------------------------------------------------------------------------------

    /** Spawns the leashed horse next to this wanderer; false when there is no room (the upkeep retries). */
    public boolean spawnHorse(ServerLevel level) {
        BlockPos at = com.simplewanderers.logic.WandererSpawner.findSpot(level, blockPosition(), 3, random);
        if (at == null) return false;
        WandererHorse h = WandererRegistry.HORSE.spawn(level, at, EntitySpawnReason.EVENT);
        if (h == null) return false;
        h.setPersistenceRequired();
        h.setHomeTo(blockPosition(), WandererConfig.homeRadius);
        ItemStack armor = new ItemStack(Items.LEATHER_HORSE_ARMOR);
        armor.set(DataComponents.DYED_COLOR, new DyedItemColor(OCHRE));
        h.setItemSlot(EquipmentSlot.BODY, armor);
        h.setDropChance(EquipmentSlot.BODY, 0.0F);
        h.setLeashedTo(this, true);
        this.horse = h.getUUID();
        this.horseGoneSince = -1L;
        this.horseMissing = 0;
        return true;
    }

    /** Test hook: the horse spawn failed or was never wanted, so the first upkeep may replace it at once. */
    public void horseLostAt(long gameTime) {
        this.horse = null;
        this.horseGoneSince = gameTime;
    }

    /**
     * Notices a horse that was taken (leash moved to a player), killed or lost, starts the replacement timer, and brings a
     * new horse after {@code respawnDays} while a player is near. The first missing horse of a fresh wanderer is replaced at once.
     */
    public void horseUpkeep(ServerLevel server) {
        if (!WandererConfig.horse) return;
        Entity entity = horse == null ? null : server.getEntity(horse);
        if (entity instanceof WandererHorse h && h.isAlive() && h.getLeashHolder() == this) {
            horseMissing = 0;
            return;
        }
        if (horse != null) {
            if (entity == null && ++horseMissing < 3) return;
            horse = null;
            horseGoneSince = server.getGameTime();
        }
        horseMissing = 0;
        if (horseGoneSince < 0L) {
            spawnHorse(server);
            if (horse == null) horseGoneSince = server.getGameTime();
            return;
        }
        long wait = WandererConfig.respawnTicks();
        if (wait > 0 && server.getGameTime() - horseGoneSince >= wait && server.hasNearbyAlivePlayer(getX(), getY(), getZ(), WandererConfig.villageRadius)) {
            spawnHorse(server);
        }
    }

    // --- saving ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("VillageId", villageId);
        output.putString("VillagerType", villagerType);
        output.putLong("LastPeriod", lastPeriod);
        output.putLong("LastHalf", lastHalf);
        output.putLong("HorseGoneSince", horseGoneSince);
        output.storeNullable("Horse", UUIDUtil.CODEC, horse);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        villageId = input.getLongOr("VillageId", NO_VILLAGE);
        villagerType = input.getStringOr("VillagerType", "plains");
        lastPeriod = input.getLongOr("LastPeriod", Long.MIN_VALUE);
        lastHalf = input.getLongOr("LastHalf", Long.MIN_VALUE);
        horseGoneSince = input.getLongOr("HorseGoneSince", -1L);
        horse = input.read("Horse", UUIDUtil.CODEC).orElse(null);
    }
}

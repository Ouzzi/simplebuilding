package com.simplebuilding.modules.simpletools.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simpletools.SimpleTools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Death bookkeeping of the Death Vault (owner 2026-10-12), one server-wide {@link SavedData} of the overworld (Vanilla
 * writes it atomically). Per player all deaths (newest last, at most {@code deathVaultMaxDeaths}); per death the stacks
 * that left the inventory as item entities, each with the entity UUID and a snapshot of the stack.
 *
 * <p>An entry is {@link #LYING} while its entity exists. When the entity is removed for good it becomes {@link #LOST}
 * (redeemable) only if it still holds the unchanged stack and it despawned in its own tick, burned or blew up, or fell into
 * the void; every other removal (picked up by anyone, hopper, mob, part taken, /kill, cactus ...) strikes the entry. A
 * death caused by another player only counts despawns and destructions caused by that player. Fail-closed throughout.
 */
public final class DeathLedger extends SavedData {
    public static final int LYING = 0, LOST = 1;
    /** Vanilla item lifetime in ticks. */
    public static final int LIFETIME = 6000;

    /** One dropped stack. */
    public static final class Entry {
        public final UUID entity;
        public final ItemStack stack;
        public int state;

        public Entry(UUID entity, ItemStack stack, int state) {
            this.entity = entity;
            this.stack = stack;
            this.state = state;
        }

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("entity").forGetter(e -> e.entity),
                ItemStack.CODEC.fieldOf("stack").forGetter(e -> e.stack),
                Codec.INT.fieldOf("state").forGetter(e -> e.state)).apply(i, Entry::new));
    }

    /** One death. */
    public static final class Death {
        public final long time;
        public final Optional<UUID> killer;
        public final List<Entry> entries;

        public Death(long time, Optional<UUID> killer, List<Entry> entries) {
            this.time = time;
            this.killer = killer;
            this.entries = new ArrayList<>(entries);
        }

        static final Codec<Death> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("time").forGetter(d -> d.time),
                UUIDUtil.CODEC.optionalFieldOf("killer").forGetter(d -> d.killer),
                Entry.CODEC.listOf().fieldOf("entries").forGetter(d -> d.entries)).apply(i, Death::new));
    }

    public static final Codec<DeathLedger> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Death.CODEC.listOf())
            .xmap(DeathLedger::new, l -> l.players).fieldOf("players").codec();

    public static final SavedDataType<DeathLedger> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, "death_ledger"), DeathLedger::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, List<Death>> players = new LinkedHashMap<>();
    /** entity UUID -> owner, death, entry (rebuilt from {@link #players}). */
    private final Map<UUID, Ref> index = new HashMap<>();

    private record Ref(UUID owner, Death death, Entry entry) {}

    public DeathLedger() {}

    private DeathLedger(Map<UUID, List<Death>> loaded) {
        loaded.forEach((player, deaths) -> players.put(player, new ArrayList<>(deaths)));
        reindex();
        setDirty(false);
    }

    private void reindex() {
        index.clear();
        players.forEach((player, deaths) -> deaths.forEach(d -> d.entries.forEach(e -> {
            if (e.state == LYING) index.put(e.entity, new Ref(player, d, e));
        })));
    }

    public static DeathLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The ledger of the entity's server, or null on the client / without a server. */
    public static DeathLedger of(Entity entity) {
        if (entity.level().isClientSide()) return null;
        MinecraftServer server = entity.level().getServer();
        return server == null ? null : get(server);
    }

    /** All deaths of a player, newest last (live list; do not modify outside this class). */
    public List<Death> deaths(UUID player) {
        return players.getOrDefault(player, List.of());
    }

    /** Records one death with its dropped item entities; old deaths beyond the limit are forgotten. */
    public Death record(UUID player, long time, Optional<UUID> killer, List<ItemEntity> drops) {
        List<Entry> entries = new ArrayList<>();
        for (ItemEntity drop : drops) {
            if (!drop.getItem().isEmpty()) entries.add(new Entry(drop.getUUID(), drop.getItem().copy(), LYING));
        }
        Death death = new Death(time, killer, entries);
        if (entries.isEmpty()) return death;
        List<Death> list = players.computeIfAbsent(player, k -> new ArrayList<>());
        list.add(death);
        int max = SimpleTools.config().deathVaultMaxDeaths;
        while (list.size() > max) list.removeFirst();
        reindex();
        setDirty();
        return death;
    }

    /** True while the entity is a recorded death drop that is still lying (it must not merge). */
    public boolean tracked(UUID entity) {
        return index.containsKey(entity);
    }

    /**
     * The tracked entity leaves the world for good. {@code despawn}: removed in its own tick at full age; {@code hurt}: the
     * damage that destroyed it (null if none); {@code voidFall}: below the world.
     */
    public void removed(ItemEntity entity, boolean despawn, DamageSource hurt, boolean voidFall) {
        Ref ref = index.remove(entity.getUUID());
        if (ref == null) return;
        ItemStack now = entity.getItem();
        boolean intact = !now.isEmpty() && now.getCount() == ref.entry.stack.getCount() && ItemStack.isSameItemSameComponents(now, ref.entry.stack);
        boolean lost = intact && lostBy(ref.death, despawn, hurt, voidFall);
        if (lost) ref.entry.state = LOST;
        else strike(ref.owner, ref.death, ref.entry);
        setDirty();
    }

    /** Rules of the owner answers: despawn, fire/lava, explosion or void; a PvP death only despawn or the killer's damage. */
    static boolean lostBy(Death death, boolean despawn, DamageSource hurt, boolean voidFall) {
        if (despawn) return true;
        if (death.killer.isPresent()) {
            return hurt != null && hurt.getEntity() != null && death.killer.get().equals(hurt.getEntity().getUUID())
                    && (hurt.is(DamageTypeTags.IS_FIRE) || hurt.is(DamageTypeTags.IS_EXPLOSION));
        }
        if (voidFall) return true;
        return hurt != null && (hurt.is(DamageTypeTags.IS_FIRE) || hurt.is(DamageTypeTags.IS_EXPLOSION));
    }

    private void strike(UUID owner, Death death, Entry entry) {
        death.entries.remove(entry);
        if (death.entries.isEmpty()) {
            List<Death> list = players.get(owner);
            if (list != null) {
                list.remove(death);
                if (list.isEmpty()) players.remove(owner);
            }
        }
    }

    /** Redeemed: the entry leaves the list for good. */
    public void take(UUID owner, Death death, Entry entry) {
        index.remove(entry.entity);
        strike(owner, death, entry);
        setDirty();
    }

    /** Forgets everything of a player (admin). */
    public void clear(UUID player) {
        if (players.remove(player) != null) {
            reindex();
            setDirty();
        }
    }
}

package com.simplebuilding.modules.simpletools.vault;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Death bookkeeping of the Death Vault (owner 2026-10-12, reworked 2026-10-14), one server-wide {@link SavedData} of the
 * overworld (Vanilla writes it atomically). Per player the deaths (newest last; only the last one unless the future redeem
 * option is on, then at most {@code deathVaultMaxDeaths}); per death the stacks that left the inventory as item entities,
 * each with the entity UUID, a snapshot of the stack and its {@link Fate}.
 *
 * <p>An entry is {@link Fate#LYING} while its entity exists. The fate is final once the entity is gone for good: picked up
 * (by whom), burned (lava, fire, cactus), despawned, fallen into the void, or gone another way. Entries stay in the list so
 * the vault can show where every item went; the future redeem option may return only entries flagged {@code redeemable}:
 * unchanged stacks that despawned, burned in fire, blew up or fell into the void (after a death by another player only
 * despawns and destructions caused by that player). Fail-closed throughout.
 */
public final class DeathLedger extends SavedData {
    /** Vanilla item lifetime in ticks. */
    public static final int LIFETIME = 6000;
    /**
     * Despawn age of an item entity in ticks. Vanilla: {@link #LIFETIME}; the NeoForge entrypoint swaps in the entity's
     * own {@code lifespan} (items with {@code getEntityLifespan}, saved "Lifespan"), so the timer and the despawn check follow it.
     */
    public static volatile java.util.function.ToIntFunction<ItemEntity> lifespanOf = e -> LIFETIME;

    public static int lifespan(ItemEntity entity) {
        return Math.clamp(lifespanOf.applyAsInt(entity), 1, Short.MAX_VALUE);
    }
    /** Ledger state of the first version: "lost" (redeemable); read as {@link Fate#OTHER} + redeemable. */
    private static final int LEGACY_LOST = 1;

    /** One dropped stack. */
    public static final class Entry {
        public final UUID entity;
        public final ItemStack stack;
        /** {@link Fate#id}. */
        public int state;
        /** Name of the other player who picked it up, empty otherwise. */
        public String who;
        /** May the future redeem option return it. */
        public boolean redeemable;
        /** Age of the entity in ticks when it was last seen (unloaded or shown), 0 at the death. */
        public int age;
        /** Despawn age of the entity in ticks when it was last seen ({@link #LIFETIME} for Vanilla items). */
        public int lifespan = LIFETIME;

        public Entry(UUID entity, ItemStack stack, int state, String who, boolean redeemable, int age, int lifespan) {
            this(entity, stack, state, who, redeemable, age);
            this.lifespan = lifespan;
        }

        public Entry(UUID entity, ItemStack stack, int state, String who, boolean redeemable, int age) {
            this.entity = entity;
            this.stack = stack;
            if (state == LEGACY_LOST) {
                state = Fate.OTHER.id;
                redeemable = true;
            }
            this.state = state;
            this.who = who;
            this.redeemable = redeemable;
            this.age = age;
        }

        public Fate fate() {
            return Fate.of(state);
        }

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("entity").forGetter(e -> e.entity),
                ItemStack.CODEC.fieldOf("stack").forGetter(e -> e.stack),
                Codec.INT.fieldOf("state").forGetter(e -> e.state),
                Codec.STRING.optionalFieldOf("who", "").forGetter(e -> e.who),
                Codec.BOOL.optionalFieldOf("redeemable", false).forGetter(e -> e.redeemable),
                Codec.INT.optionalFieldOf("age", 0).forGetter(e -> e.age),
                Codec.INT.optionalFieldOf("lifespan", LIFETIME).forGetter(e -> e.lifespan)).apply(i, Entry::new));
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
                lenientList(Entry.CODEC, "entry").fieldOf("entries").forGetter(d -> d.entries)).apply(i, Death::new));
    }

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /**
     * Like {@code codec.listOf()}, but a broken element is logged and skipped instead of failing the whole list (and with it
     * the whole ledger, which Vanilla would then replace by an empty one). Encoding is unchanged, so old saves still load.
     */
    private static <T> Codec<List<T>> lenientList(Codec<T> element, String what) {
        return Codec.of(element.listOf(), new Decoder<List<T>>() {
            @Override
            public <O> DataResult<Pair<List<T>, O>> decode(DynamicOps<O> ops, O input) {
                return ops.getStream(input).map(stream -> {
                    List<T> out = new ArrayList<>();
                    stream.forEach(item -> element.parse(ops, item).resultOrPartial(
                            err -> LOGGER.error("Death ledger: skipping broken {}: {}", what, err)).ifPresent(out::add));
                    return Pair.of(out, input);
                });
            }
        });
    }

    /** Players: a broken player key or death list is logged and skipped, the other players stay. */
    private static final Codec<Map<UUID, List<Death>>> PLAYERS = Codec.of(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Death.CODEC.listOf()),
            new Decoder<Map<UUID, List<Death>>>() {
                private final Codec<List<Death>> deaths = lenientList(Death.CODEC, "death");

                @Override
                public <O> DataResult<Pair<Map<UUID, List<Death>>, O>> decode(DynamicOps<O> ops, O input) {
                    return ops.getMapValues(input).map(entries -> {
                        Map<UUID, List<Death>> out = new LinkedHashMap<>();
                        entries.forEach(e -> {
                            var key = UUIDUtil.STRING_CODEC.parse(ops, e.getFirst());
                            var value = deaths.parse(ops, e.getSecond());
                            if (key.result().isPresent() && value.result().isPresent()) out.put(key.result().get(), value.result().get());
                            else LOGGER.error("Death ledger: skipping broken player entry {}: {}", e.getFirst(),
                                    key.error().isPresent() ? key.error().get().message() : value.error().map(DataResult.Error::message).orElse("?"));
                        });
                        return Pair.of(out, input);
                    });
                }
            });

    public static final Codec<DeathLedger> CODEC = PLAYERS.xmap(DeathLedger::new, l -> l.players).fieldOf("players").codec();

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
            if (e.state == Fate.LYING.id) index.put(e.entity, new Ref(player, d, e));
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

    /** Deaths kept per player: only the last one, unless the future redeem option is on. */
    private static int maxDeaths() {
        return SimpleTools.config().deathVaultRedeem ? SimpleTools.config().deathVaultMaxDeaths : 1;
    }

    /**
     * Records one death with its dropped item entities (also a death without drops: the "last death" is then empty); old
     * deaths beyond the limit are forgotten.
     */
    public Death record(UUID player, long time, Optional<UUID> killer, List<ItemEntity> drops) {
        List<Entry> entries = new ArrayList<>();
        for (ItemEntity drop : drops) {
            if (!drop.getItem().isEmpty()) entries.add(new Entry(drop.getUUID(), drop.getItem().copy(), Fate.LYING.id, "", false, 0));
        }
        Death death = new Death(time, killer, entries);
        List<Death> list = players.computeIfAbsent(player, k -> new ArrayList<>());
        list.add(death);
        int max = maxDeaths();
        while (list.size() > max) list.removeFirst();
        reindex();
        setDirty();
        return death;
    }

    /** The last death of a player, if any. */
    public Optional<Death> last(UUID player) {
        List<Death> list = players.get(player);
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.getLast());
    }

    /** True while the entity is a recorded death drop that is still lying (it must not merge). */
    public boolean tracked(UUID entity) {
        return index.containsKey(entity);
    }

    /**
     * The tracked entity leaves the world for good (not picked up by a player). {@code despawn}: removed in its own tick at
     * full age; {@code hurt}: the damage that destroyed it (null if none); {@code voidFall}: below the world.
     */
    public void removed(ItemEntity entity, boolean despawn, DamageSource hurt, boolean voidFall) {
        Ref ref = index.remove(entity.getUUID());
        if (ref == null) return;
        ItemStack now = entity.getItem();
        boolean intact = !now.isEmpty() && now.getCount() == ref.entry.stack.getCount() && ItemStack.isSameItemSameComponents(now, ref.entry.stack);
        Fate fate;
        if (despawn) fate = Fate.DESPAWNED;
        else if (hurt != null) fate = hurt.is(DamageTypeTags.IS_FIRE) || hurt.is(DamageTypes.CACTUS) ? Fate.BURNED : Fate.OTHER;
        else if (voidFall) fate = Fate.VOID;
        else fate = Fate.OTHER;
        ref.entry.state = fate.id;
        ref.entry.redeemable = intact && lostBy(ref.death, despawn, hurt, voidFall);
        ref.entry.age = Math.max(0, entity.getAge());
        ref.entry.lifespan = lifespan(entity);
        setDirty();
    }

    /** A player picked up the tracked entity (whole or part): the entry is final, never redeemable. */
    public void pickedUp(UUID entity, Player player) {
        Ref ref = index.remove(entity);
        if (ref == null) return;
        boolean own = ref.owner.equals(player.getUUID());
        ref.entry.state = (own ? Fate.PICKED_OWNER : Fate.PICKED_OTHER).id;
        ref.entry.who = own ? "" : player.getName().getString();
        ref.entry.redeemable = false;
        setDirty();
    }

    /** The tracked entity unloads with its chunk: remember its age, the timer stands until it loads again. */
    public void unloaded(ItemEntity entity) {
        Ref ref = index.get(entity.getUUID());
        if (ref == null) return;
        ref.entry.age = Math.max(0, entity.getAge());
        ref.entry.lifespan = lifespan(entity);
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

    /** Redeemed (future option): the entry stays listed as bought back and can never be returned again. */
    public void take(UUID owner, Death death, Entry entry) {
        index.remove(entry.entity);
        entry.state = Fate.REDEEMED.id;
        entry.redeemable = false;
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

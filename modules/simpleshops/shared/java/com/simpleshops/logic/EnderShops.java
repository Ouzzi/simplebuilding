package com.simpleshops.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.EarningsStore;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlockEntity;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Ender shops (KONZ20 S1 A): the contents of every shop block live here, in the world save, not in the chunk. A block is only
 * the window onto its entry, like the ender chest onto the player's ender inventory: price row and earnings of a sales block,
 * goods of a display case, earnings of a sales storage. So a trade terminal can reach a shop whose chunk is not loaded,
 * without loading it.
 * <p>Rules: one entry ("vault") per placed block, id {@code p000001 ...} (never reused); the entry knows its dimension and
 * position, and a block entity whose saved id points at an entry somewhere else (copied block data) gets a fresh empty
 * entry, so nothing exists twice. Breaking the block drops the entry's items once at the block and deletes it. An entry
 * without its block (removed without side effects) stays "orphaned" with all its items until an operator clears it.
 * The one-player lock and the revision are runtime only (never saved), like before on the block entity.
 */
public final class EnderShops extends SavedData {
    public static final int MAX_VAULTS = 200_000, MAX_TRANSIT = 64, MAX_STORAGE_LINKS = ShopStorage.MAX_CHAIN, NAME_MAX = 24;

    public enum Kind {
        SALES(EarningsStore.EARNINGS_SIZE), CASE(DisplayCaseBlockEntity.SIZE), STORAGE(EarningsStore.EARNINGS_SIZE);

        public final int size;

        Kind(int size) {
            this.size = size;
        }

        public static final Codec<Kind> CODEC = Codec.STRING.xmap(s -> Kind.valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT));
    }

    /** One stack at a slot index (saving keeps the positions, empty slots are not written). */
    record SlotStack(int slot, ItemStack stack) {
        static final Codec<SlotStack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 63).fieldOf("slot").forGetter(SlotStack::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(SlotStack::stack)).apply(i, SlotStack::new));

        static List<SlotStack> of(NonNullList<ItemStack> list) {
            List<SlotStack> out = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) if (!list.get(i).isEmpty()) out.add(new SlotStack(i, list.get(i).copy()));
            return out;
        }

        static void into(List<SlotStack> stacks, NonNullList<ItemStack> list) {
            for (SlotStack s : stacks) if (s.slot() < list.size() && !s.stack().isEmpty()) list.set(s.slot(), s.stack().copy());
        }
    }

    /** The saved part of a vault. */
    record Stored(String id, Kind kind, Optional<UUID> owner, String ownerName, String dimension, long pos, List<SlotStack> items,
                  List<SlotStack> prices, String serverId, boolean listed, String name, String caseId, List<String> storages, List<ItemStack> transit) {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Stored::id),
                Kind.CODEC.fieldOf("kind").forGetter(Stored::kind),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Stored::owner),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Stored::ownerName),
                Codec.STRING.fieldOf("dimension").forGetter(Stored::dimension),
                Codec.LONG.fieldOf("pos").forGetter(Stored::pos),
                SlotStack.CODEC.listOf(0, 64).optionalFieldOf("items", List.of()).forGetter(Stored::items),
                SlotStack.CODEC.listOf(0, PriceRow.SIZE).optionalFieldOf("prices", List.of()).forGetter(Stored::prices),
                Codec.STRING.optionalFieldOf("server_id", "").forGetter(Stored::serverId),
                Codec.BOOL.optionalFieldOf("public", true).forGetter(Stored::listed),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Stored::name),
                Codec.STRING.optionalFieldOf("case", "").forGetter(Stored::caseId),
                Codec.STRING.listOf(0, MAX_STORAGE_LINKS).optionalFieldOf("storages", List.of()).forGetter(Stored::storages),
                ItemStack.CODEC.listOf(0, MAX_TRANSIT).optionalFieldOf("transit", List.of()).forGetter(Stored::transit)).apply(i, Stored::new));
    }

    /** One shop block's contents and links. */
    public static final class Vault {
        public final String id;
        public final Kind kind;
        @Nullable UUID owner;
        String ownerName;
        String dimension;
        long pos;
        /** Earnings (sales block, storage) or goods (display case). */
        public final NonNullList<ItemStack> items;
        /** Price templates (sales block only). */
        public final NonNullList<ItemStack> prices = PriceRow.empty();
        String serverId = "";
        boolean listed = true;
        String name = "";
        /** Sales block: the display case above it and the storages of its chain, as last seen on site. */
        String caseId = "";
        final List<String> storages = new ArrayList<>();
        /** Display case: goods on their way to a trade terminal (reserved, back into the goods if the delivery fails). */
        final List<ItemStack> transit = new ArrayList<>();
        // --- runtime only ---
        int revision;
        boolean deleted;
        private @Nullable WeakReference<ServerPlayer> lockPlayer;
        private @Nullable WeakReference<AbstractContainerMenu> lockMenu;
        private boolean lockRemote;

        Vault(String id, Kind kind, String dimension, long pos) {
            this.id = id;
            this.kind = kind;
            this.dimension = dimension;
            this.pos = pos;
            this.ownerName = "";
            this.items = NonNullList.withSize(kind.size, ItemStack.EMPTY);
        }

        public @Nullable UUID owner() {
            return this.owner;
        }

        public String ownerName() {
            return this.ownerName;
        }

        public String dimension() {
            return this.dimension;
        }

        public BlockPos blockPos() {
            return BlockPos.of(this.pos);
        }

        public boolean isServer() {
            return !this.serverId.isEmpty();
        }

        public String serverId() {
            return this.serverId;
        }

        public boolean listed() {
            return this.listed;
        }

        public void setListed(boolean listed) {
            this.listed = listed;
        }

        public String name() {
            return this.name;
        }

        public void setName(String name) {
            String n = name == null ? "" : name.strip();
            this.name = n.length() > NAME_MAX ? n.substring(0, NAME_MAX) : n;
        }

        public String caseId() {
            return this.caseId;
        }

        public List<String> storages() {
            return List.copyOf(this.storages);
        }

        public List<ItemStack> transit() {
            return this.transit;
        }

        public int revision() {
            return this.revision;
        }

        public void bumpRevision() {
            this.revision++;
        }

        public boolean deleted() {
            return this.deleted;
        }

        public boolean isOwner(UUID player) {
            return this.owner != null && this.owner.equals(player);
        }

        /** Whether this entry belongs to the block at {@code pos} in {@code level}. */
        public boolean at(ServerLevel level, BlockPos at) {
            return this.pos == at.asLong() && this.dimension.equals(dim(level));
        }

        public void setOwner(@Nullable UUID owner, String ownerName) {
            this.owner = owner;
            this.ownerName = ownerName == null ? "" : ownerName;
        }

        public void setServerId(String serverId) {
            this.serverId = serverId == null ? "" : serverId;
        }

        /** Puts reserved goods back into free goods slots; what finds no place stays reserved (never lost, dropped with the case). */
        public boolean settle() {
            boolean moved = false;
            for (var it = this.transit.iterator(); it.hasNext();) {
                ItemStack s = it.next();
                int free = -1;
                for (int i = 0; i < this.items.size(); i++) {
                    if (this.items.get(i).isEmpty()) {
                        free = i;
                        break;
                    }
                }
                if (free < 0) break;
                this.items.set(free, s);
                it.remove();
                moved = true;
            }
            return moved;
        }

        // --- one-player lock (runtime) ------------------------------------------------------------------------------

        /** The player holding the shop, or {@code null}; a stale lock is dropped here. On site: same level, within reach; remote: still selected. */
        public @Nullable ServerPlayer lockHolder() {
            ServerPlayer p = this.lockPlayer == null ? null : this.lockPlayer.get();
            AbstractContainerMenu menu = this.lockMenu == null ? null : this.lockMenu.get();
            boolean live = p != null && menu != null && !this.deleted && !p.isRemoved() && p.isAlive() && p.containerMenu == menu;
            if (live && this.lockRemote) {
                live = menu instanceof RemoteHolder r && r.holdsShop(this.id);
            } else if (live) {
                live = p.level().dimension().identifier().toString().equals(this.dimension)
                        && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(blockPos())) <= 64.0 + 4.0;
            }
            if (!live) {
                this.lockPlayer = null;
                this.lockMenu = null;
                return null;
            }
            return p;
        }

        public boolean lockFree() {
            return lockHolder() == null;
        }

        public void lock(ServerPlayer player, AbstractContainerMenu menu, boolean remote) {
            this.lockPlayer = new WeakReference<>(player);
            this.lockMenu = new WeakReference<>(menu);
            this.lockRemote = remote;
        }

        public void unlock(AbstractContainerMenu menu) {
            if (this.lockMenu != null && this.lockMenu.get() == menu) {
                this.lockPlayer = null;
                this.lockMenu = null;
            }
        }

        Stored stored() {
            return new Stored(this.id, this.kind, Optional.ofNullable(this.owner), this.ownerName, this.dimension, this.pos, SlotStack.of(this.items),
                    SlotStack.of(this.prices), this.serverId, this.listed, this.name, this.caseId, List.copyOf(this.storages),
                    this.transit.stream().map(ItemStack::copy).toList());
        }

        static Vault of(Stored s) {
            Vault v = new Vault(s.id(), s.kind(), s.dimension(), s.pos());
            v.owner = s.owner().orElse(null);
            v.ownerName = s.ownerName();
            SlotStack.into(s.items(), v.items);
            if (v.kind == Kind.SALES) {
                SlotStack.into(s.prices(), v.prices);
                if (!PriceRow.valid(v.prices)) for (int i = 0; i < PriceRow.SIZE; i++) v.prices.set(i, ItemStack.EMPTY);
            }
            v.serverId = s.serverId();
            v.listed = s.listed();
            v.setName(s.name());
            v.caseId = s.caseId();
            v.storages.addAll(s.storages());
            for (ItemStack t : s.transit()) if (!t.isEmpty()) v.transit.add(t.copy());
            // A restart ends every delivery: reserved goods go back first.
            v.settle();
            return v;
        }
    }

    /** A menu that holds a shop lock from afar (the trade terminal): the lock lives only while the menu still has that shop selected. */
    public interface RemoteHolder {
        boolean holdsShop(String vaultId);
    }

    public static final Codec<EnderShops> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("next", 0L).forGetter(e -> e.next),
            Stored.CODEC.listOf(0, MAX_VAULTS).optionalFieldOf("vaults", List.of()).forGetter(e -> e.vaults.values().stream().map(Vault::stored).toList()))
            .apply(i, EnderShops::new));
    public static final SavedDataType<EnderShops> TYPE = new SavedDataType<>(Identifier.parse("simpleshops:ender_shops"), EnderShops::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private long next;
    private final Map<String, Vault> vaults = new LinkedHashMap<>();

    public EnderShops() {}

    private EnderShops(long next, List<Stored> stored) {
        this.next = next;
        for (Stored s : stored) this.vaults.put(s.id(), Vault.of(s));
    }

    public static EnderShops get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    static String dim(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public @Nullable Vault find(String id) {
        if (id == null || id.isEmpty()) return null;
        return this.vaults.get(id);
    }

    public Collection<Vault> all() {
        return this.vaults.values();
    }

    public int size() {
        return this.vaults.size();
    }

    /** A new empty entry for a block at {@code pos}; {@code null} only when the hard limit is reached (the block then keeps its items itself). */
    public @Nullable Vault create(Kind kind, ServerLevel level, BlockPos pos) {
        if (this.vaults.size() >= MAX_VAULTS) return null;
        String id;
        do {
            id = String.format(Locale.ROOT, "p%06d", ++this.next);
        } while (this.vaults.containsKey(id));
        Vault v = new Vault(id, kind, dim(level), pos.asLong());
        this.vaults.put(id, v);
        setDirty();
        return v;
    }

    /** Deletes an entry (its block went away after dropping the items); links to it are cleared. */
    public void delete(Vault v) {
        if (this.vaults.remove(v.id) == null) return;
        v.deleted = true;
        v.lockPlayer = null;
        v.lockMenu = null;
        for (Vault o : this.vaults.values()) {
            if (o.kind != Kind.SALES) continue;
            if (o.caseId.equals(v.id)) o.caseId = "";
            o.storages.remove(v.id);
        }
        setDirty();
    }

    // --- links ----------------------------------------------------------------------------------------------------------

    /** Records what stands on site around a sales block (case above, storage chain below), for the trade terminal. */
    public static void link(SalesBlockEntity sales) {
        Vault v = sales.vault();
        if (v == null) return;
        String caseId = sales.getLevel() != null && sales.getLevel().getBlockEntity(sales.getBlockPos().above()) instanceof DisplayCaseBlockEntity c
                && c.vault() != null && c.isServerShop() == sales.isServerShop() ? c.vault().id : "";
        List<String> chain = new ArrayList<>();
        for (EarningsStore s : ShopStorage.chain(sales)) {
            if (s instanceof SalesStorageBlockEntity st && st.vault() != null) chain.add(st.vault().id);
        }
        if (!v.caseId.equals(caseId) || !v.storages.equals(chain)) {
            v.caseId = caseId;
            v.storages.clear();
            v.storages.addAll(chain);
        }
    }

    /** Links a case entry to a sales entry directly (operator tools and tests; on site {@link #link} does it). */
    public void linkCase(Vault sales, Vault box) {
        if (sales.kind == Kind.SALES && box.kind == Kind.CASE) {
            sales.caseId = box.id;
            setDirty();
        }
    }

    /** The display case entry of a sales entry (same kind of shop), or {@code null}. */
    public @Nullable Vault caseOf(Vault sales) {
        Vault c = find(sales.caseId);
        if (c == null || c.kind != Kind.CASE || !c.dimension.equals(sales.dimension) || c.pos != BlockPos.of(sales.pos).above().asLong()) return null;
        return c;
    }

    /** The earnings lists of a sales entry: its own, then its linked storages with the same owner. */
    public List<NonNullList<ItemStack>> earningsChain(Vault sales) {
        List<NonNullList<ItemStack>> out = new ArrayList<>();
        out.add(sales.items);
        if (sales.owner == null || sales.isServer()) return out;
        for (String id : sales.storages) {
            Vault s = find(id);
            if (s != null && s.kind == Kind.STORAGE && sales.owner.equals(s.owner)) out.add(s.items);
        }
        return out;
    }

    /** Pushes a changed entry to its block (client view), if that block's chunk is loaded; never loads a chunk. */
    public static void touch(MinecraftServer server, Vault v) {
        get(server).setDirty();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(v.dimension)));
        if (level == null) return;
        BlockPos pos = v.blockPos();
        if (!level.hasChunkAt(pos)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SalesBlockEntity s && s.vault() == v) s.changed();
        if (be instanceof DisplayCaseBlockEntity c && c.vault() == v) c.changed();
        if (be instanceof SalesStorageBlockEntity t && t.vault() == v) t.changed();
    }

    // --- orphans --------------------------------------------------------------------------------------------------------

    /**
     * Entries whose block is gone (removed without side effects, e.g. by a structure or a mod). Only loaded chunks are checked:
     * an entry in an unloaded chunk is never guessed to be an orphan.
     */
    public List<Vault> orphans(MinecraftServer server) {
        List<Vault> out = new ArrayList<>();
        for (Vault v : this.vaults.values()) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(v.dimension)));
            if (level == null) {
                out.add(v);
                continue;
            }
            BlockPos pos = v.blockPos();
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            Vault there = be instanceof SalesBlockEntity s ? s.peekVault() : be instanceof DisplayCaseBlockEntity c ? c.peekVault()
                    : be instanceof SalesStorageBlockEntity t ? t.peekVault() : null;
            if (there != v) out.add(v);
        }
        return out;
    }

    /** All items of an entry (goods/earnings and reserved goods; never the price templates), taken out of it. */
    public static List<ItemStack> takeAll(Vault v) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < v.items.size(); i++) {
            if (!v.items.get(i).isEmpty()) out.add(v.items.get(i));
            v.items.set(i, ItemStack.EMPTY);
        }
        out.addAll(v.transit);
        v.transit.clear();
        return out;
    }
}

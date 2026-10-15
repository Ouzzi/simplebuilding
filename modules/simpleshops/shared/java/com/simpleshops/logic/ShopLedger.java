package com.simpleshops.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Who placed which shop block, for the per-player limit (the counter lives here, not in the blocks). Sites in loaded
 * chunks are checked against the world while counting, so a block removed by a command or another mod stops counting;
 * sites in unloaded chunks count as they are (nothing is guessed).
 */
public final class ShopLedger extends SavedData {
    public static final int MAX_SITES = 200000;

    public record Site(UUID owner, String dimension, long pos, boolean sales) {
        public static final Codec<Site> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Site::owner),
                Codec.STRING.fieldOf("dimension").forGetter(Site::dimension),
                Codec.LONG.fieldOf("pos").forGetter(Site::pos),
                Codec.BOOL.fieldOf("sales").forGetter(Site::sales)).apply(i, Site::new));
    }

    public static final Codec<ShopLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
            Site.CODEC.listOf(0, MAX_SITES).fieldOf("sites").forGetter(ShopLedger::sites)).apply(i, ShopLedger::new));
    public static final SavedDataType<ShopLedger> TYPE = new SavedDataType<>(Identifier.parse("simpleshops:ledger"),
            ShopLedger::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<Site> sites = new ArrayList<>();

    public ShopLedger() {}

    private ShopLedger(List<Site> sites) {
        this.sites.addAll(sites);
    }

    public List<Site> sites() {
        return new ArrayList<>(this.sites);
    }

    public static ShopLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static String dim(Level level) {
        return level.dimension().identifier().toString();
    }

    public static void record(ServerLevel level, BlockPos pos, UUID owner, boolean sales) {
        ShopLedger ledger = get(level.getServer());
        String dimension = dim(level);
        long p = pos.asLong();
        ledger.sites.removeIf(s -> s.pos() == p && s.dimension().equals(dimension));
        ledger.sites.add(new Site(owner, dimension, p, sales));
        ledger.setDirty();
    }

    public static void forget(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        ShopLedger ledger = get(server.getServer());
        String dimension = dim(level);
        long p = pos.asLong();
        if (ledger.sites.removeIf(s -> s.pos() == p && s.dimension().equals(dimension))) ledger.setDirty();
    }

    /** How many sales blocks ({@code sales}) or display cases {@code owner} has placed; stale sites in loaded chunks are dropped. */
    public static int count(MinecraftServer server, UUID owner, boolean sales) {
        ShopLedger ledger = get(server);
        int n = 0;
        boolean dirty = false;
        for (var it = ledger.sites.iterator(); it.hasNext();) {
            Site s = it.next();
            if (!s.owner().equals(owner) || s.sales() != sales) continue;
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(s.dimension())));
            BlockPos pos = BlockPos.of(s.pos());
            if (level != null && level.hasChunkAt(pos)) {
                var be = level.getBlockEntity(pos);
                boolean valid = sales ? be instanceof SalesBlockEntity sb && sb.isOwner(owner)
                        : be instanceof DisplayCaseBlockEntity cb && cb.isOwner(owner);
                if (!valid) {
                    it.remove();
                    dirty = true;
                    continue;
                }
            } else if (level == null) {
                it.remove();
                dirty = true;
                continue;
            }
            n++;
        }
        if (dirty) ledger.setDirty();
        return n;
    }
}

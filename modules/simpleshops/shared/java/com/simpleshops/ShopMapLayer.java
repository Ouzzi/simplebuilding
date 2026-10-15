package com.simpleshops;

import com.simplebuilding.framework.api.MapLayerSources.Marker;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The shops layer of the wayfinder map: every active shop (a sales block with a price and a stocked display case right
 * above it) in a loaded chunk, with what it offers and what it costs. Only what the shop shows to everyone: no earnings,
 * no stock counts beyond the offer.
 */
final class ShopMapLayer {
    /** Shops per answer; the map asks for small areas, this only bounds a crowded one. */
    static final int MAX = 128;

    private ShopMapLayer() {}

    static List<Marker> collect(Object level, int minX, int minZ, int maxX, int maxZ) {
        List<Marker> out = new ArrayList<>();
        if (!SimpleShops.on() || !(level instanceof ServerLevel serverLevel)) return out;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (out.size() >= MAX) return out;
                    if (!(be instanceof SalesBlockEntity sales) || sales.isRemoved() || !sales.hasPrice()) continue;
                    BlockPos pos = sales.getBlockPos();
                    if (pos.getX() < minX || pos.getX() > maxX || pos.getZ() < minZ || pos.getZ() > maxZ) continue;
                    if (!(serverLevel.getBlockEntity(pos.above()) instanceof DisplayCaseBlockEntity shopCase)
                            || shopCase.stocked() == 0 || shopCase.isServerShop() != sales.isServerShop()) continue;
                    List<String> lines = new ArrayList<>();
                    lines.add(sales.isServerShop() ? "Server shop" : "Shop of " + sales.ownerName());
                    lines.add("Offer: " + join(shopCase.goods(), false));
                    lines.add("Price: " + join(sales.prices(), true));
                    out.add(new Marker(pos.getX(), pos.getZ(), "shop", lines));
                }
            }
        }
        return out;
    }

    /** Names with counts (the price); the offer shows names only, a sale hands out one piece, the stock stays private. */
    private static String join(List<ItemStack> stacks, boolean counts) {
        StringBuilder b = new StringBuilder();
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            if (b.length() > 0) b.append(", ");
            if (counts) b.append(s.getCount()).append("x ");
            b.append(s.getHoverName().getString());
        }
        return b.toString();
    }
}

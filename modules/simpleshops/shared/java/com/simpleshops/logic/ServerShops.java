package com.simpleshops.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlock;
import com.simpleshops.block.SalesBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Registry of the server (creative, unlimited) shops, kept in the world save: the ids {@code s001, s002 ...} (never reused, the
 * counter only grows), where each one stands, and the optional admin cash. A server shop is a normal sales block with a display
 * case above it, flagged by the operator command: no owner, goods are copied per purchase (never used up), price items are a sink
 * unless the config collects them here. Nothing is ever dropped from a server shop.
 */
public final class ServerShops extends SavedData {
    public static final int MAX_SHOPS = 999_999, MAX_CASH_ENTRIES = 256;

    /** Where the sales block of one server shop stands (the case is always directly above). */
    public record Shop(String id, String dimension, long pos) {
        public static final Codec<Shop> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Shop::id),
                Codec.STRING.fieldOf("dimension").forGetter(Shop::dimension),
                Codec.LONG.fieldOf("pos").forGetter(Shop::pos)).apply(i, Shop::new));

        public BlockPos blockPos() {
            return BlockPos.of(this.pos);
        }
    }

    /** One kind of price item collected in the admin cash (a one-piece template and a total). */
    public record Cash(ItemStack stack, long count) {
        public static final Codec<Cash> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("item").forGetter(Cash::stack),
                Codec.LONG.fieldOf("count").forGetter(Cash::count)).apply(i, Cash::new));
    }

    public static final Codec<ServerShops> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("next", 0).forGetter(s -> s.next),
            Shop.CODEC.listOf(0, MAX_SHOPS).optionalFieldOf("shops", List.of()).forGetter(ServerShops::shops),
            Cash.CODEC.listOf(0, MAX_CASH_ENTRIES).optionalFieldOf("cash", List.of()).forGetter(ServerShops::cash)).apply(i, ServerShops::new));
    public static final SavedDataType<ServerShops> TYPE = new SavedDataType<>(Identifier.parse("simpleshops:server_shops"),
            ServerShops::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private int next;
    private final List<Shop> shops = new ArrayList<>();
    private final List<Cash> cash = new ArrayList<>();

    public ServerShops() {}

    private ServerShops(int next, List<Shop> shops, List<Cash> cash) {
        this.next = next;
        this.shops.addAll(shops);
        this.cash.addAll(cash);
    }

    public static ServerShops get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Shop> shops() {
        return new ArrayList<>(this.shops);
    }

    public List<Cash> cash() {
        return new ArrayList<>(this.cash);
    }

    public @Nullable Shop find(String id) {
        String wanted = id.toLowerCase(Locale.ROOT);
        for (Shop s : this.shops) if (s.id().equals(wanted)) return s;
        return null;
    }

    public static String formatId(int n) {
        return String.format(Locale.ROOT, "s%03d", n);
    }

    /** Called by the sales block entity when it goes away (break, command, replacement): the entry disappears, the id stays used. */
    public static void forgetAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        ServerShops registry = get(server.getServer());
        String dimension = level.dimension().identifier().toString();
        long p = pos.asLong();
        if (registry.shops.removeIf(s -> s.pos() == p && s.dimension().equals(dimension))) registry.setDirty();
    }

    // --- cash (optional) ------------------------------------------------------------------------------------------------------

    /** Adds the price items of one purchase; more than {@link #MAX_CASH_ENTRIES} kinds are not tracked (they vanish like the sink). */
    public void collect(List<ItemStack> prices) {
        boolean changed = false;
        for (ItemStack price : prices) {
            if (price.isEmpty()) continue;
            boolean merged = false;
            for (int i = 0; i < this.cash.size(); i++) {
                Cash c = this.cash.get(i);
                if (ItemStack.isSameItemSameComponents(c.stack(), price)) {
                    this.cash.set(i, new Cash(c.stack(), c.count() + price.getCount()));
                    merged = true;
                    break;
                }
            }
            if (!merged && this.cash.size() < MAX_CASH_ENTRIES) this.cash.add(new Cash(price.copyWithCount(1), price.getCount()));
            changed = true;
        }
        if (changed) setDirty();
    }

    public void clearCash() {
        if (!this.cash.isEmpty()) setDirty();
        this.cash.clear();
    }

    // --- create / remove ----------------------------------------------------------------------------------------------------

    public enum Created { OK, OCCUPIED, OUT_OF_WORLD, LIMIT }

    public record CreateResult(Created status, @Nullable String id) {}

    /** Places a sales block at {@code pos} and a display case above it, flags both and registers the id. Both places must be replaceable. */
    public static CreateResult create(ServerLevel level, BlockPos pos, Direction facing) {
        ServerShops registry = get(level.getServer());
        BlockPos above = pos.above();
        if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(above)) return new CreateResult(Created.OUT_OF_WORLD, null);
        if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(above).canBeReplaced()) return new CreateResult(Created.OCCUPIED, null);
        if (registry.next >= MAX_SHOPS) return new CreateResult(Created.LIMIT, null);
        String id;
        do {
            id = formatId(++registry.next);
        } while (registry.find(id) != null);
        level.setBlock(pos, ShopsRegistry.SALES_BLOCK.defaultBlockState().setValue(SalesBlock.FACING, facing), Block.UPDATE_ALL);
        level.setBlock(above, ShopsRegistry.DISPLAY_CASE.defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof SalesBlockEntity sales) || !(level.getBlockEntity(above) instanceof DisplayCaseBlockEntity shopCase)) {
            return new CreateResult(Created.OCCUPIED, null);
        }
        sales.initServer(id);
        shopCase.initServer();
        registry.shops.add(new Shop(id, level.dimension().identifier().toString(), pos.asLong()));
        registry.setDirty();
        return new CreateResult(Created.OK, id);
    }

    /** What stands at a registered shop (chunk loaded by the caller). */
    public record Located(Shop shop, ServerLevel level, @Nullable SalesBlockEntity sales, @Nullable DisplayCaseBlockEntity shopCase) {
        public boolean complete() {
            return this.sales != null && this.shopCase != null && this.sales.serverId().equals(this.shop.id()) && this.shopCase.isServerShop();
        }
    }

    /** Looks the shop up in its dimension; {@code load} loads the chunk (operator commands), otherwise an unloaded shop has no entities. */
    public static @Nullable Located locate(MinecraftServer server, Shop shop, boolean load) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(shop.dimension())));
        if (level == null) return null;
        BlockPos pos = shop.blockPos();
        if (load) level.getChunk(pos);
        SalesBlockEntity sales = level.getBlockEntity(pos) instanceof SalesBlockEntity s ? s : null;
        DisplayCaseBlockEntity shopCase = level.getBlockEntity(pos.above()) instanceof DisplayCaseBlockEntity c ? c : null;
        return new Located(shop, level, sales, shopCase);
    }

    /** Removes the blocks of a server shop without any drop and forgets the entry. */
    public static void remove(MinecraftServer server, Shop shop) {
        Located at = locate(server, shop, true);
        if (at != null) {
            BlockPos pos = shop.blockPos();
            // Only blocks that are still flagged as this shop (a replaced position is left alone).
            if (at.shopCase() != null && at.shopCase().isServerShop()) at.level().removeBlock(pos.above(), false);
            if (at.sales() != null && at.sales().serverId().equals(shop.id())) at.level().removeBlock(pos, false);
        }
        ServerShops registry = get(server);
        if (registry.shops.removeIf(s -> s.id().equals(shop.id()))) registry.setDirty();
    }
}

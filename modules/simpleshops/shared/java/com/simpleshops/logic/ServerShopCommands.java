package com.simpleshops.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.simpleshops.SimpleShops;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlock;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.StockMenu;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /servershop}: the operator (permission level 2) commands of the server shops. Only operators create, change or remove one;
 * buying is the normal shop menu for everyone. Rights are checked by the command tree and again by the menus.
 * <pre>
 * create [pos]   remove &lt;id&gt;   list [coords|items]   info &lt;id&gt;   tp &lt;id&gt;   edit &lt;id&gt; price|goods
 * price &lt;id&gt; add &lt;item&gt; [count] | remove &lt;position 1-5&gt; | clear
 * goods &lt;id&gt; add &lt;item&gt; [count] | remove &lt;slot 1-5&gt; | clear
 * cash [clear]
 * orphans [clear]   (ender shop entries whose block is gone: list, or hand their items to the operator and delete them)
 * </pre>
 */
public final class ServerShopCommands {
    private static final String K = "commands.simpleshops.servershop.";
    private static final SimpleCommandExceptionType NO_SHOP = error("unknown"), BROKEN = error("broken"), NEEDS_PLAYER = error("needs_player"),
            OFF = error("off"), OCCUPIED = error("occupied"), NO_ITEM = error("no_item"), FULL = error("full"), NOTHING = error("nothing"),
            LIMIT = error("limit");
    private static final SuggestionProvider<CommandSourceStack> IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ServerShops.get(ctx.getSource().getServer()).shops().stream().map(ServerShops.Shop::id), builder);

    private static SimpleCommandExceptionType error(String key) {
        return new SimpleCommandExceptionType(Component.translatable(K + key));
    }

    /** {@code name <id> ...}: a literal with the id argument below it; {@code body} adds the rest to the id node. */
    private static LiteralArgumentBuilder<CommandSourceStack> withId(String name, UnaryOperator<RequiredArgumentBuilder<CommandSourceStack, String>> body) {
        return Commands.literal(name).then(body.apply(Commands.argument("id", StringArgumentType.word()).suggests(IDS)));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("servershop").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("create").executes(c -> create(c.getSource(), null))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> create(c.getSource(), BlockPosArgument.getBlockPos(c, "pos")))))
                .then(withId("remove", n -> n.executes(c -> remove(c.getSource(), id(c)))))
                .then(Commands.literal("list").executes(c -> list(c.getSource(), 0))
                        .then(Commands.literal("coords").executes(c -> list(c.getSource(), 1)))
                        .then(Commands.literal("items").executes(c -> list(c.getSource(), 2))))
                .then(withId("info", n -> n.executes(c -> info(c.getSource(), id(c)))))
                .then(withId("tp", n -> n.executes(c -> tp(c.getSource(), id(c)))))
                .then(withId("edit", n -> n
                        .then(Commands.literal("price").executes(c -> editPrice(c.getSource(), id(c))))
                        .then(Commands.literal("goods").executes(c -> editGoods(c.getSource(), id(c))))))
                .then(withId("price", n -> n
                        .then(Commands.literal("add").then(Commands.argument("item", ItemArgument.item(context)).executes(c -> addPrice(c, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 99)).executes(c -> addPrice(c, IntegerArgumentType.getInteger(c, "count"))))))
                        .then(Commands.literal("remove").then(Commands.argument("position", IntegerArgumentType.integer(1, PriceRow.SIZE))
                                .executes(c -> removePrice(c.getSource(), id(c), IntegerArgumentType.getInteger(c, "position") - 1))))
                        .then(Commands.literal("clear").executes(c -> clearPrice(c.getSource(), id(c))))))
                .then(withId("goods", n -> n
                        .then(Commands.literal("add").then(Commands.argument("item", ItemArgument.item(context)).executes(c -> addGoods(c, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 99)).executes(c -> addGoods(c, IntegerArgumentType.getInteger(c, "count"))))))
                        .then(Commands.literal("remove").then(Commands.argument("slot", IntegerArgumentType.integer(1, DisplayCaseBlockEntity.SIZE))
                                .executes(c -> removeGoods(c.getSource(), id(c), IntegerArgumentType.getInteger(c, "slot") - 1))))
                        .then(Commands.literal("clear").executes(c -> clearGoods(c.getSource(), id(c))))))
                .then(Commands.literal("cash").executes(c -> cash(c.getSource(), false))
                        .then(Commands.literal("clear").executes(c -> cash(c.getSource(), true))))
                .then(Commands.literal("orphans").executes(c -> orphans(c.getSource(), false))
                        .then(Commands.literal("clear").executes(c -> orphans(c.getSource(), true)))));
    }

    /**
     * Ender shop entries without their block (KONZ20: never a silent loss). Listing is free; clearing gives every item to the
     * operator who runs it (a full inventory drops at the operator's feet: the operator asked for it) and deletes the entries.
     */
    public static int orphans(CommandSourceStack source, boolean clear) throws CommandSyntaxException {
        EnderShops data = EnderShops.get(source.getServer());
        List<EnderShops.Vault> found = data.orphans(source.getServer());
        if (!clear) {
            int items = 0;
            for (EnderShops.Vault v : found) {
                for (ItemStack s : v.items) items += s.getCount();
                for (ItemStack s : v.transit()) items += s.getCount();
            }
            int n = found.size(), sum = items;
            source.sendSuccess(() -> Component.translatable(K + "orphans", n, sum), false);
            for (EnderShops.Vault v : found.subList(0, Math.min(found.size(), 20))) {
                BlockPos p = v.blockPos();
                source.sendSuccess(() -> Component.literal("  " + v.id + " " + v.kind.name().toLowerCase(Locale.ROOT) + " " + v.dimension() + " "
                        + p.getX() + " " + p.getY() + " " + p.getZ() + " " + v.ownerName()), false);
            }
            return n;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) throw NEEDS_PLAYER.create();
        int n = 0;
        for (EnderShops.Vault v : found) {
            if (!v.isServer()) for (ItemStack s : EnderShops.takeAll(v)) player.getInventory().placeItemBackInInventory(s, true, net.minecraft.util.Prediction.SERVER_ONLY);
            data.delete(v);
            n++;
        }
        int cleared = n;
        source.sendSuccess(() -> Component.translatable(K + "orphans_cleared", cleared), true);
        return n;
    }

    private static String id(CommandContext<CommandSourceStack> c) {
        return StringArgumentType.getString(c, "id").toLowerCase(Locale.ROOT);
    }

    // --- create / remove ----------------------------------------------------------------------------------------------------

    private static int create(CommandSourceStack source, BlockPos where) throws CommandSyntaxException {
        if (!SimpleShops.on()) throw OFF.create();
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getPlayer();
        BlockPos pos = where;
        Direction facing = Direction.NORTH;
        if (player != null) {
            facing = player.getDirection().getOpposite();
            // Default place: the block in front of the player (never the one the player stands in).
            if (pos == null) pos = player.blockPosition().relative(player.getDirection());
        }
        if (pos == null) throw NEEDS_PLAYER.create();
        ServerShops.CreateResult result = ServerShops.create(level, pos, facing);
        switch (result.status()) {
            case OCCUPIED, OUT_OF_WORLD -> throw OCCUPIED.create();
            case LIMIT -> throw LIMIT.create();
            default -> {}
        }
        String id = result.id();
        BlockPos at = pos;
        source.sendSuccess(() -> Component.translatable(K + "created", id, at.getX() + " " + at.getY() + " " + at.getZ()), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerShops.Shop shop = shop(source, id);
        ServerShops.remove(source.getServer(), shop);
        source.sendSuccess(() -> Component.translatable(K + "removed", shop.id()), true);
        return 1;
    }

    // --- list / info / tp ---------------------------------------------------------------------------------------------------

    /** @param mode 0 ids, 1 with coordinates, 2 with goods and prices */
    private static int list(CommandSourceStack source, int mode) {
        List<ServerShops.Shop> shops = ServerShops.get(source.getServer()).shops();
        if (shops.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(K + "list.empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(K + "list.header", shops.size()), false);
        for (ServerShops.Shop shop : shops) {
            ServerShops.Located at = ServerShops.locate(source.getServer(), shop, mode == 2);
            MutableComponent line = Component.literal(shop.id()).withStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent.RunCommand("/servershop tp " + shop.id())));
            if (mode >= 1) line.append(Component.literal("  " + where(shop)));
            if (mode == 2 && at != null && at.complete()) {
                line.append(Component.literal("  ")).append(goodsText(at.shopCase())).append(Component.literal(" <- ")).append(priceText(at.sales().prices()));
            }
            // A loaded, damaged shop is marked; an unloaded one is not judged.
            if (at != null && at.level().hasChunkAt(shop.blockPos()) && !at.complete()) {
                line.append(Component.translatable(K + "list.broken").withStyle(net.minecraft.ChatFormatting.RED));
            }
            source.sendSuccess(() -> line, false);
        }
        return shops.size();
    }

    private static int info(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerShops.Shop shop = shop(source, id);
        ServerShops.Located at = ServerShops.locate(source.getServer(), shop, true);
        source.sendSuccess(() -> Component.translatable(K + "info", shop.id(), shop.dimension(), where(shop)), false);
        if (at == null || !at.complete()) {
            source.sendSuccess(() -> Component.translatable(K + "broken").withStyle(net.minecraft.ChatFormatting.RED), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(K + "info.goods", goodsText(at.shopCase())), false);
        source.sendSuccess(() -> Component.translatable(K + "info.price", priceText(at.sales().prices())), false);
        return 1;
    }

    private static int tp(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerShops.Shop shop = shop(source, id);
        ServerShops.Located at = ServerShops.locate(source.getServer(), shop, true);
        if (at == null) throw BROKEN.create();
        ServerLevel level = at.level();
        BlockPos pos = shop.blockPos();
        Direction facing = level.getBlockState(pos).getBlock() instanceof SalesBlock ? level.getBlockState(pos).getValue(SalesBlock.FACING) : Direction.NORTH;
        BlockPos front = pos.relative(facing);
        boolean free = level.getBlockState(front).getCollisionShape(level, front).isEmpty()
                && level.getBlockState(front.above()).getCollisionShape(level, front.above()).isEmpty();
        BlockPos stand = free ? front : pos.above(2);
        player.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), free ? facing.getOpposite().toYRot() : player.getYRot(), 0.0F, true);
        source.sendSuccess(() -> Component.translatable(K + "tp", shop.id()), false);
        return 1;
    }

    // --- edit (menus) --------------------------------------------------------------------------------------------------------

    private static int editPrice(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerShops.Located at = complete(source, id);
        player.openMenu(new SimpleMenuProvider((cid, inv, p) -> new SalesOwnerMenu(cid, inv, at.sales(), player), Component.translatable("container.simpleshops.sales")));
        return 1;
    }

    private static int editGoods(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerShops.Located at = complete(source, id);
        player.openMenu(new SimpleMenuProvider((cid, inv, p) -> new StockMenu(cid, inv, at.shopCase(), at.sales(), player), Component.translatable("container.simpleshops.stock")));
        return 1;
    }

    // --- price / goods ---------------------------------------------------------------------------------------------------------

    private static ItemStack stack(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        ItemInput input = ItemArgument.getItem(c, "item");
        // createItemStack refuses counts above the stack size (like /give); air gives an empty stack.
        ItemStack stack = input.createItemStack(count);
        if (stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()) throw NO_ITEM.create();
        return stack;
    }

    private static int addPrice(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        ServerShops.Located at = complete(c.getSource(), id(c));
        ItemStack price = stack(c, count);
        NonNullList<ItemStack> row = at.sales().prices();
        int index;
        if (PriceRow.filled(row) == 0) index = PriceRow.CENTER;
        else if (PriceRow.high(row) < PriceRow.SIZE - 1) index = PriceRow.high(row) + 1;
        else if (PriceRow.low(row) > 0) index = PriceRow.low(row) - 1;
        else throw FULL.create();
        row.set(index, price);
        at.sales().bumpRevision();
        at.sales().changed();
        c.getSource().sendSuccess(() -> Component.translatable(K + "price.set", at.shop().id(), index + 1, describe(price)), true);
        return 1;
    }

    private static int removePrice(CommandSourceStack source, String id, int index) throws CommandSyntaxException {
        ServerShops.Located at = complete(source, id);
        if (at.sales().prices().get(index).isEmpty()) throw NOTHING.create();
        PriceRow.remove(at.sales().prices(), index);
        at.sales().bumpRevision();
        at.sales().changed();
        source.sendSuccess(() -> Component.translatable(K + "price.removed", at.shop().id()), true);
        return 1;
    }

    private static int clearPrice(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerShops.Located at = complete(source, id);
        for (int i = 0; i < PriceRow.SIZE; i++) at.sales().prices().set(i, ItemStack.EMPTY);
        at.sales().bumpRevision();
        at.sales().changed();
        source.sendSuccess(() -> Component.translatable(K + "price.cleared", at.shop().id()), true);
        return 1;
    }

    private static int addGoods(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        ServerShops.Located at = complete(c.getSource(), id(c));
        ItemStack goods = stack(c, count);
        int slot = at.shopCase().freeSlot();
        if (slot < 0) throw FULL.create();
        at.shopCase().goods().set(slot, goods);
        at.shopCase().changed();
        at.sales().bumpRevision();
        at.sales().changed();
        c.getSource().sendSuccess(() -> Component.translatable(K + "goods.set", at.shop().id(), slot + 1, describe(goods)), true);
        return 1;
    }

    private static int removeGoods(CommandSourceStack source, String id, int slot) throws CommandSyntaxException {
        ServerShops.Located at = complete(source, id);
        if (at.shopCase().goods().get(slot).isEmpty()) throw NOTHING.create();
        at.shopCase().goods().set(slot, ItemStack.EMPTY);
        at.shopCase().changed();
        at.sales().bumpRevision();
        at.sales().changed();
        source.sendSuccess(() -> Component.translatable(K + "goods.removed", at.shop().id()), true);
        return 1;
    }

    private static int clearGoods(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerShops.Located at = complete(source, id);
        for (int i = 0; i < DisplayCaseBlockEntity.SIZE; i++) at.shopCase().goods().set(i, ItemStack.EMPTY);
        at.shopCase().changed();
        at.sales().bumpRevision();
        at.sales().changed();
        source.sendSuccess(() -> Component.translatable(K + "goods.cleared", at.shop().id()), true);
        return 1;
    }

    // --- cash ----------------------------------------------------------------------------------------------------------------

    private static int cash(CommandSourceStack source, boolean clear) {
        ServerShops registry = ServerShops.get(source.getServer());
        List<ServerShops.Cash> cash = registry.cash();
        if (clear) {
            registry.clearCash();
            source.sendSuccess(() -> Component.translatable(K + "cash.cleared"), true);
            return cash.size();
        }
        source.sendSuccess(() -> Component.translatable(K + (com.simpleshops.config.ShopsConfig.collectServerShopIncome ? "cash.on" : "cash.off")), false);
        if (cash.isEmpty()) source.sendSuccess(() -> Component.translatable(K + "cash.empty"), false);
        for (ServerShops.Cash c : cash) source.sendSuccess(() -> Component.literal(c.count() + "x ").append(c.stack().getHoverName()), false);
        return cash.size();
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    private static ServerShops.Shop shop(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerShops.Shop shop = ServerShops.get(source.getServer()).find(id);
        if (shop == null) throw NO_SHOP.create();
        return shop;
    }

    /** The shop with both blocks present and flagged (chunk loaded); else the "broken" error. */
    private static ServerShops.Located complete(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerShops.Located at = ServerShops.locate(source.getServer(), shop(source, id), true);
        if (at == null || !at.complete()) throw BROKEN.create();
        return at;
    }

    private static String where(ServerShops.Shop shop) {
        BlockPos p = shop.blockPos();
        return shop.dimension() + " " + p.getX() + " " + p.getY() + " " + p.getZ();
    }

    private static Component describe(ItemStack stack) {
        return Component.literal(stack.getCount() + "x ").append(stack.getHoverName());
    }

    private static Component goodsText(DisplayCaseBlockEntity shopCase) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (ItemStack s : shopCase.goods()) {
            if (s.isEmpty()) continue;
            if (!first) out.append(Component.literal(", "));
            out.append(describe(s));
            first = false;
        }
        return first ? Component.literal("-") : out;
    }

    private static Component priceText(NonNullList<ItemStack> row) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (ItemStack s : row) {
            if (s.isEmpty()) continue;
            if (!first) out.append(Component.literal(" + "));
            out.append(describe(s));
            first = false;
        }
        return first ? Component.literal("-") : out;
    }

    private ServerShopCommands() {}
}

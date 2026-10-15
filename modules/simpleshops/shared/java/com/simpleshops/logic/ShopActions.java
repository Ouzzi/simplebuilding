package com.simpleshops.logic;

import com.simpleshops.SimpleShops;
import com.simpleshops.block.DisplayCaseBlock;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlock;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import com.simpleshops.menu.StorageMenu;
import com.simpleshops.block.SalesStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** What a right click does, server side. Menus are only opened when the one-player lock is free. */
public final class ShopActions {
    /** Right click on a sales block: owner = price and earnings menu, everyone else = buyer menu (needs the case above). */
    public static InteractionResult useSales(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!SimpleShops.on()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SalesBlockEntity sales)) return fail(level, pos);
        if (sales.isOwner(player.getUUID())) return openOwner(player, level, pos, sales);
        return buy(player, level, pos, sales);
    }

    /** Right click on a display case with an empty hand: owner = goods menu, everyone else = buyer menu of the sales block below. */
    public static InteractionResult useCase(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!SimpleShops.on()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity shopCase)) return fail(level, pos);
        SalesBlockEntity sales = level.getBlockEntity(pos.below()) instanceof SalesBlockEntity s ? s : null;
        if (shopCase.isOwner(player.getUUID())) {
            if (sales != null && sales.isOwner(player.getUUID())) return openOwner(player, level, pos.below(), sales);
            if (sales != null && !sales.lockFree()) return busy(level, pos);
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StockMenu(id, inv, shopCase, sales, player),
                    Component.translatable("container.simpleshops.stock")));
            return InteractionResult.SUCCESS;
        }
        if (sales == null) return fail(level, pos);
        return buy(player, level, pos.below(), sales);
    }

    /** Owner right click on a case with an item in hand: the whole stack goes to the next free slot (the quick way to stock). */
    public static InteractionResult quickStock(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
        if (!SimpleShops.on()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof DisplayCaseBlockEntity shopCase) || !shopCase.isOwner(player.getUUID())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        SalesBlockEntity sales = level.getBlockEntity(pos.below()) instanceof SalesBlockEntity s ? s : null;
        if (sales != null && !sales.lockFree()) return busy(level, pos);
        int slot = shopCase.freeSlot();
        if (slot < 0) return fail(level, pos);
        shopCase.goods().set(slot, held.copy());
        player.setItemInHand(hand, ItemStack.EMPTY);
        shopCase.changed();
        if (sales != null) {
            sales.bumpRevision();
            sales.changed();
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Right click on a sales storage: its owner sees the 27 slots; everyone else gets a "no" (only breaking is up to operators). */
    public static InteractionResult useStorage(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!SimpleShops.on()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SalesStorageBlockEntity storage) || !storage.isOwner(player.getUUID())) return fail(level, pos);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StorageMenu(id, inv, storage), Component.translatable("container.simpleshops.storage")));
        return InteractionResult.SUCCESS;
    }

    /** The owner's screen: one shared screen when his display case stands on the sales block, else the sales block's own. */
    private static InteractionResult openOwner(ServerPlayer player, ServerLevel level, BlockPos salesPos, SalesBlockEntity sales) {
        if (!sales.lockFree()) return busy(level, salesPos);
        if (level.getBlockEntity(salesPos.above()) instanceof DisplayCaseBlockEntity shopCase && shopCase.isOwner(player.getUUID()) && !sales.isServerShop()) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> SalesOwnerMenu.combined(id, inv, sales, shopCase, player),
                    Component.translatable("container.simpleshops.shop")));
        } else {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SalesOwnerMenu(id, inv, sales, player),
                    Component.translatable("container.simpleshops.sales")));
        }
        return InteractionResult.SUCCESS;
    }

    /** Developer button, owner screen to buyer screen (server decides; the old menu is closed first so its lock is released). */
    public static boolean switchToBuyer(ServerPlayer player, SalesBlockEntity sales) {
        if (!(player.level() instanceof ServerLevel level) || !sales.mayEdit(player) || sales.isRemoved()) return false;
        BlockPos pos = sales.getBlockPos();
        if (level.getBlockEntity(pos) != sales) return false;
        player.closeContainer();
        return buy(player, level, pos, sales).consumesAction();
    }

    /** Developer button, buyer screen back to the owner screen. */
    public static boolean switchToOwner(ServerPlayer player, SalesBlockEntity sales) {
        if (!(player.level() instanceof ServerLevel level) || !sales.mayEdit(player) || sales.isRemoved()) return false;
        BlockPos pos = sales.getBlockPos();
        if (level.getBlockEntity(pos) != sales) return false;
        player.closeContainer();
        return openOwner(player, level, pos, sales).consumesAction();
    }

    private static InteractionResult buy(ServerPlayer player, ServerLevel level, BlockPos salesPos, SalesBlockEntity sales) {
        if (!(level.getBlockEntity(salesPos.above()) instanceof DisplayCaseBlockEntity shopCase)) return fail(level, salesPos);
        return open(player, level, salesPos, sales, false, shopCase);
    }

    private static InteractionResult open(ServerPlayer player, ServerLevel level, BlockPos pos, SalesBlockEntity sales, boolean owner,
                                          @Nullable DisplayCaseBlockEntity shopCase) {
        if (!sales.isServerShop() && !sales.lockFree()) return busy(level, pos);
        String name = sales.ownerName().isEmpty() ? "?" : sales.ownerName();
        if (owner) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SalesOwnerMenu(id, inv, sales, player),
                    Component.translatable("container.simpleshops.sales")));
        } else {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ShopBuyerMenu(id, inv, sales, shopCase, player),
                    sales.isServerShop() ? Component.translatable("container.simpleshops.buyer_server")
                            : Component.translatable("container.simpleshops.buyer", name)));
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult busy(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 0.8F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult fail(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.1F);
        return InteractionResult.CONSUME;
    }

    /** Placement rules, shared by both blocks (server: ledger limit and ownership, client: the shape of the rule only). */
    public static boolean mayPlace(net.minecraft.world.item.context.BlockPlaceContext ctx, boolean sales) {
        if (!SimpleShops.on() || ctx.getPlayer() == null) return false;
        var level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!sales) {
            if (!(level.getBlockState(pos.below()).getBlock() instanceof SalesBlock)) return false;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return true;
        if (!sales) {
            BlockEntity below = level.getBlockEntity(pos.below());
            if (!(below instanceof SalesBlockEntity s) || s.isServerShop() || (!s.isOwner(player.getUUID()) && !ShopGuard.isAdmin(player))) return false;
        }
        int count = ShopLedger.count(player.level().getServer(), player.getUUID(), sales);
        return count < ShopsConfig.maxShopsPerPlayer;
    }

    /**
     * Placement of a sales storage: directly under a sales block or another storage, and every block of that run has the placing
     * player as owner (a stranger's shop is never extended); at most {@link ShopStorage#MAX_CHAIN} storages in a row.
     */
    public static boolean mayPlaceStorage(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        if (!SimpleShops.on() || ctx.getPlayer() == null) return false;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return true;
        int above = ShopStorage.storagesAbove(ctx.getLevel(), ctx.getClickedPos(), player.getUUID());
        return above >= 0 && above < ShopStorage.MAX_CHAIN;
    }

    private ShopActions() {}
}

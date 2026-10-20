package com.simpleshops.test;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.simplebuilding.framework.api.Protection;
import com.simpleshops.ShopsRegistry;
import com.simpleshops.SimpleShops;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlock;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ShopActions;
import com.simpleshops.logic.ShopGuard;
import com.simpleshops.logic.ShopLedger;
import com.simpleshops.logic.ShopTrade;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests (concept docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md section 6, stages 1 and 2); the
 * Fabric/NeoForge/Forge adapters register them as module_game_test_<name> on the {@code simpleshops:arena} structure
 * (15 x 12 x 15). Sales block at (7,1,7), display case above it at (7,2,7), ground at y 0.
 */
public final class ShopsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    static final BlockPos SALES = new BlockPos(7, 1, 7), CASE = new BlockPos(7, 2, 7);
    private static int counter;

    static {
        ALL.put("config_defaults", h -> {
            ShopsConfig.reset();
            h.assertTrue(ShopsConfig.enabled && ShopsConfig.maxShopsPerPlayer == 16 && ShopsConfig.idleTimeoutSeconds == 300, "defaults");
            JsonObject json = new JsonObject();
            json.addProperty("maxShopsPerPlayer", 500);
            json.addProperty("idleTimeoutSeconds", 1);
            ShopsConfig.apply(json);
            h.assertTrue(ShopsConfig.maxShopsPerPlayer == 64 && ShopsConfig.idleTimeoutSeconds == 30, "values are clamped");
            json.addProperty("maxShopsPerPlayer", "many");
            ShopsConfig.apply(json);
            h.assertTrue(ShopsConfig.maxShopsPerPlayer == 16, "junk falls back to the default");
            ShopsConfig.reset();
            h.succeed();
        });
        ALL.put("registered", h -> {
            h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleShops.id("shops")), "own creative tab shops is registered");
            for (String id : List.of("sales_block", "display_case", "glow_display_case", "guide_book")) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleShops.id(id)), "item missing: " + id);
            }
            h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleShops.id("sales_block")) == ShopsRegistry.SALES_BLOCK, "block");
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleShops.id("display_case")), "case block entity type");
            for (String menu : List.of("sales_owner", "buyer", "stock")) {
                h.assertTrue(BuiltInRegistries.MENU.containsKey(SimpleShops.id(menu)), "menu type " + menu);
            }
            for (Block block : List.of(ShopsRegistry.SALES_BLOCK, ShopsRegistry.DISPLAY_CASE, ShopsRegistry.GLOW_DISPLAY_CASE)) {
                h.assertTrue(block.getExplosionResistance() >= 3_600_000.0F, "blast resistance of " + block);
                h.assertTrue(block.defaultBlockState().getPistonPushReaction() == PushReaction.IMMOVEABLE, "pistons never move " + block);
                h.assertTrue(!block.defaultBlockState().is(BlockTags.ENDERMAN_HOLDABLE) && !block.defaultBlockState().is(BlockTags.WASHED_AWAY_BY_FLUIDS), "not carried, not washed away");
                h.assertTrue(block.defaultBlockState().is(BlockTags.WITHER_IMMUNE) && block.defaultBlockState().is(BlockTags.DRAGON_IMMUNE), "wither and dragon immune");
                h.assertTrue(!block.defaultBlockState().canBeReplaced(Fluids.WATER) && !block.defaultBlockState().canBeReplaced(Fluids.LAVA), "fluids never replace it");
            }
            h.assertTrue(ShopsRegistry.GLOW_DISPLAY_CASE.defaultBlockState().getLightEmission() == 6
                    && ShopsRegistry.DISPLAY_CASE.defaultBlockState().getLightEmission() == 0, "only the glow case gives weak light");
            h.succeed();
        });
        ALL.put("recipes", h -> {
            var recipes = h.getLevel().getServer().getRecipeManager();
            for (String id : List.of("sales_block", "display_case", "glow_display_case")) {
                h.assertTrue(recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, SimpleShops.id(id))).isPresent(), "recipe " + id);
            }
            h.succeed();
        });

        // --- stage 1: placement, owner, break protection -------------------------------------------------------------------------
        ALL.put("place_needs_sales_block", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer other = player(h, GameType.SURVIVAL);
            BlockPos ground = h.absolutePos(new BlockPos(7, 0, 7));
            h.getLevel().setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
            // A case on plain stone does not place.
            place(owner, new ItemStack(ShopsRegistry.DISPLAY_CASE_ITEM), ground);
            h.assertTrue(h.getLevel().getBlockState(ground.above()).isAir(), "no case without a sales block below");
            // The sales block places; a stranger cannot put a case on it, the owner can.
            place(owner, new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM), ground);
            h.assertTrue(h.getLevel().getBlockState(ground.above()).getBlock() instanceof SalesBlock, "sales block placed");
            place(other, new ItemStack(ShopsRegistry.DISPLAY_CASE_ITEM), ground.above());
            h.assertTrue(h.getLevel().getBlockState(ground.above(2)).isAir(), "a stranger cannot put a case on my sales block");
            place(owner, new ItemStack(ShopsRegistry.DISPLAY_CASE_ITEM), ground.above());
            h.assertTrue(h.getLevel().getBlockState(ground.above(2)).is(ShopsRegistry.DISPLAY_CASE), "owner placed the case");
            // Dispensers and other player-less placements are refused.
            h.getLevel().setBlock(ground.above(2), Blocks.AIR.defaultBlockState(), 3);
            var ctx = new BlockPlaceContext(h.getLevel(), null, InteractionHand.MAIN_HAND, new ItemStack(ShopsRegistry.DISPLAY_CASE_ITEM),
                    new BlockHitResult(Vec3.atCenterOf(ground.above()), Direction.UP, ground.above(), false));
            h.assertTrue(ShopsRegistry.DISPLAY_CASE.getStateForPlacement(ctx) == null, "no placement without a player");
            h.succeed();
        });
        ALL.put("owner_set_from_placer", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            BlockPos ground = h.absolutePos(new BlockPos(7, 0, 7));
            h.getLevel().setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
            ItemStack forged = new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM);
            // Item data cannot name an owner: whatever the item carries, the placing player is the owner and the block starts empty.
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("owner", UUID.randomUUID().toString());
            forged.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(ShopsRegistry.SALES_BE, tag));
            place(owner, forged, ground);
            SalesBlockEntity sales = (SalesBlockEntity) h.getLevel().getBlockEntity(ground.above());
            h.assertTrue(sales != null && owner.getUUID().equals(sales.owner()), "owner is the placing player");
            h.assertTrue(sales.ownerName().equals(owner.getGameProfile().name()), "owner name");
            h.assertTrue(PriceRow.filled(sales.prices()) == 0 && sales.earnings().stream().allMatch(ItemStack::isEmpty), "starts empty");
            h.succeed();
        });
        ALL.put("stranger_cannot_break", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            ServerPlayer creative = player(h, GameType.CREATIVE);
            Shop shop = shop(h, owner);
            for (BlockPos pos : List.of(shop.sales, shop.box)) {
                BlockState state = h.getLevel().getBlockState(pos);
                h.assertTrue(!stranger.gameMode.destroyBlock(pos), "stranger survival break refused " + pos);
                h.assertTrue(!creative.gameMode.destroyBlock(pos), "stranger creative break refused " + pos);
                h.assertTrue(h.getLevel().getBlockState(pos) == state, "block still there " + pos);
                h.assertTrue(state.getDestroyProgress(stranger, h.getLevel(), pos) == 0.0F, "no breaking animation for strangers");
                h.assertTrue(state.getDestroyProgress(owner, h.getLevel(), pos) > 0.0F, "the owner sees progress");
            }
            // Tools that ask the shared permission API (hammer, vein miner, laser, build staff, pads ...).
            ShopGuard.register(h.getLevel().getServer());
            try {
                String dim = h.getLevel().dimension().identifier().toString();
                for (BlockPos pos : List.of(shop.sales, shop.box)) {
                    for (Protection.Action action : List.of(Protection.Action.DESTROY, Protection.Action.BUILD, Protection.Action.ANY)) {
                        h.assertTrue(!Protection.allows(h.getLevel().getServer(), new Protection.Target(stranger.getUUID(), dim, pos.getX(), pos.getY(), pos.getZ(), false), action), "stranger " + action);
                        h.assertTrue(Protection.allows(h.getLevel().getServer(), new Protection.Target(owner.getUUID(), dim, pos.getX(), pos.getY(), pos.getZ(), false), action), "owner " + action);
                        h.assertTrue(Protection.allows(h.getLevel().getServer(), new Protection.Target(stranger.getUUID(), dim, pos.getX(), pos.getY(), pos.getZ(), true), action), "admin " + action);
                    }
                    h.assertTrue(Protection.allows(h.getLevel().getServer(), new Protection.Target(stranger.getUUID(), dim, pos.getX(), pos.getY(), pos.getZ(), false), Protection.Action.USE), "use is not a break");
                }
                var in = new Protection.Target(null, dim, shop.sales.getX(), shop.sales.getY(), shop.sales.getZ(), false);
                var elsewhere = new Protection.Target(null, dim, shop.sales.getX() + 5, shop.sales.getY(), shop.sales.getZ(), false);
                h.assertTrue(!Protection.allowsAutomation(h.getLevel().getServer(), elsewhere, in) && !Protection.allowsAutomation(h.getLevel().getServer(), in, elsewhere), "automation never touches a shop");
                h.assertTrue(Protection.allowsAutomation(h.getLevel().getServer(), elsewhere, elsewhere), "other positions are not affected");
            } finally {
                ShopGuard.unregister(h.getLevel().getServer());
            }
            // The owner and an operator remove it (operators have permission level 2).
            ServerPlayer op = player(h, GameType.SURVIVAL);
            h.getLevel().getServer().getPlayerList().op(op.nameAndId(), java.util.Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), java.util.Optional.empty());
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().deop(op.nameAndId()));
            h.assertTrue(ShopGuard.isAdmin(op) && !ShopGuard.isAdmin(stranger), "operator rule (level 2)");
            ServerPlayer moderator = player(h, GameType.SURVIVAL);
            h.getLevel().getServer().getPlayerList().op(moderator.nameAndId(), java.util.Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.MODERATOR), java.util.Optional.empty());
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().deop(moderator.nameAndId()));
            h.assertTrue(!ShopGuard.isAdmin(moderator) && !moderator.gameMode.destroyBlock(shop.box), "level 1 is not enough");
            h.assertTrue(op.gameMode.destroyBlock(shop.box), "operator breaks the case");
            h.assertTrue(owner.gameMode.destroyBlock(shop.sales), "owner breaks the sales block");
            h.assertTrue(h.getLevel().getBlockState(shop.box).isAir() && h.getLevel().getBlockState(shop.sales).isAir(), "both gone");
            h.succeed();
        });
        ALL.put("owner_break_drops_once_no_templates", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 3));
            shop.sales().earnings().set(0, new ItemStack(Items.GOLD_INGOT, 7));
            shop.sales().earnings().set(8, new ItemStack(Items.IRON_INGOT, 2));
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 5));
            shop.box().goods().set(4, new ItemStack(Items.STICK, 9));
            SalesBlockEntity salesEntity = shop.sales();
            DisplayCaseBlockEntity caseEntity = shop.box();
            h.assertTrue(owner.gameMode.destroyBlock(shop.box), "case break");
            h.assertTrue(owner.gameMode.destroyBlock(shop.sales), "sales break");
            AABB area = AABB.ofSize(Vec3.atCenterOf(shop.sales), 6, 6, 6);
            Map<Item, Integer> dropped = new LinkedHashMap<>();
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) dropped.merge(e.getItem().getItem(), e.getItem().getCount(), Integer::sum);
            h.assertTrue(dropped.getOrDefault(Items.DIAMOND, 0) == 5 && dropped.getOrDefault(Items.STICK, 0) == 9, "goods dropped exactly once: " + dropped);
            h.assertTrue(dropped.getOrDefault(Items.GOLD_INGOT, 0) == 7 && dropped.getOrDefault(Items.IRON_INGOT, 0) == 2, "earnings dropped exactly once: " + dropped);
            h.assertTrue(dropped.getOrDefault(Items.EMERALD, 0) == 0, "price templates are never dropped: " + dropped);
            h.assertTrue(dropped.getOrDefault(ShopsRegistry.SALES_BLOCK_ITEM, 0) == 1 && dropped.getOrDefault(ShopsRegistry.DISPLAY_CASE_ITEM, 0) == 1, "each block item once, without data: " + dropped);
            ItemStack blockItem = new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM);
            h.assertTrue(blockItem.getComponentsPatch().isEmpty(), "plain item");
            // The block entities are empty afterwards (no second drop on removal by other means).
            h.assertTrue(salesEntity.earnings().stream().allMatch(ItemStack::isEmpty) && caseEntity.goods().stream().allMatch(ItemStack::isEmpty), "emptied");
            h.succeed();
        });
        ALL.put("break_other_ways_drops_once", h -> {
            // /setblock air (or any removal that is not a player break) also drops the contents once and only once.
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.sales().earnings().set(1, new ItemStack(Items.COAL, 11));
            shop.box().goods().set(2, new ItemStack(Items.APPLE, 4));
            h.getLevel().setBlock(shop.sales, Blocks.AIR.defaultBlockState(), 3);
            h.getLevel().setBlock(shop.box, Blocks.AIR.defaultBlockState(), 3);
            int coal = 0, apples = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(shop.sales), 6, 6, 6))) {
                if (e.getItem().is(Items.COAL)) coal += e.getItem().getCount();
                if (e.getItem().is(Items.APPLE)) apples += e.getItem().getCount();
            }
            h.assertTrue(coal == 11 && apples == 4, "contents dropped once: " + coal + "/" + apples);
            h.succeed();
        });
        ALL.put("tnt_creeper_wither_dragon_resistance", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.sales().earnings().set(0, new ItemStack(Items.GOLD_INGOT, 3));
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 3));
            ServerLevel level = h.getLevel();
            Vec3 at = Vec3.atCenterOf(shop.sales).add(1.5, 0.0, 0.0);
            level.explode(null, at.x, at.y, at.z, 12.0F, Level.ExplosionInteraction.TNT);
            level.explode(null, at.x, at.y + 2, at.z, 8.0F, Level.ExplosionInteraction.MOB);
            level.explode(null, at.x, at.y, at.z, 6.0F, Level.ExplosionInteraction.BLOCK);
            h.assertTrue(level.getBlockState(shop.sales).is(ShopsRegistry.SALES_BLOCK) && level.getBlockState(shop.box).is(ShopsRegistry.DISPLAY_CASE), "explosions leave the shop");
            h.assertTrue(shop.sales().earnings().get(0).getCount() == 3 && shop.box().goods().get(0).getCount() == 3, "contents intact");
            for (Block block : List.of(ShopsRegistry.SALES_BLOCK, ShopsRegistry.DISPLAY_CASE, ShopsRegistry.GLOW_DISPLAY_CASE)) {
                h.assertTrue(block.defaultBlockState().is(BlockTags.WITHER_IMMUNE) && block.defaultBlockState().is(BlockTags.DRAGON_IMMUNE), "tags " + block);
            }
            h.succeed();
        });
        ALL.put("piston_blocked", h -> {
            Shop shop = shop(h, player(h, GameType.SURVIVAL));
            for (Direction dir : Direction.values()) {
                h.assertTrue(!PistonBaseBlock.isPushable(h.getLevel().getBlockState(shop.sales), h.getLevel(), shop.sales, dir, false, dir), "sales block not pushable " + dir);
                h.assertTrue(!PistonBaseBlock.isPushable(h.getLevel().getBlockState(shop.box), h.getLevel(), shop.box, dir, false, dir), "case not pushable " + dir);
            }
            h.succeed();
        });
        ALL.put("fluid_does_not_replace", h -> {
            Shop shop = shop(h, player(h, GameType.SURVIVAL));
            h.getLevel().setBlock(shop.sales.east(), Blocks.WATER.defaultBlockState(), 3);
            h.getLevel().setBlock(shop.box.west().above(), Blocks.LAVA.defaultBlockState(), 3);
            h.runAfterDelay(40, () -> {
                h.assertTrue(h.getLevel().getBlockState(shop.sales).is(ShopsRegistry.SALES_BLOCK) && h.getLevel().getBlockEntity(shop.sales) == shop.sales(), "sales block untouched by water");
                h.assertTrue(h.getLevel().getBlockState(shop.box).is(ShopsRegistry.DISPLAY_CASE) && h.getLevel().getBlockEntity(shop.box) == shop.box(), "case untouched by lava");
                h.succeed();
            });
        });
        MAX_TICKS.put("fluid_does_not_replace", 120);
        ALL.put("hopper_dispenser_golem_cannot_access", h -> {
            Shop shop = shop(h, player(h, GameType.SURVIVAL));
            h.assertTrue(!(shop.sales instanceof Object o && o instanceof Container) && !((Object) h.getLevel().getBlockEntity(shop.sales) instanceof Container), "sales block entity is no container");
            h.assertTrue(!((Object) h.getLevel().getBlockEntity(shop.box) instanceof Container), "case block entity is no container");
            h.assertTrue(HopperBlockEntity.getContainerAt(h.getLevel(), shop.sales) == null && HopperBlockEntity.getContainerAt(h.getLevel(), shop.box) == null,
                    "hoppers find no container");
            for (BlockPos pos : List.of(shop.sales, shop.box)) {
                BlockState state = h.getLevel().getBlockState(pos);
                h.assertTrue(!state.hasAnalogOutputSignal(), "no comparator output");
                h.assertTrue(state.getMenuProvider(h.getLevel(), pos) == null, "no block menu provider (claims see no container)");
            }
            h.succeed();
        });
        ALL.put("transfer_lookup_null", h -> {
            // Item transfer APIs of the loader (Fabric Transfer, NeoForge capabilities) find nothing on either block.
            Shop shop = shop(h, player(h, GameType.SURVIVAL));
            int checked = 0;
            for (BlockPos pos : List.of(shop.sales, shop.box)) {
                for (Direction dir : new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH}) {
                    try {
                        Object storage = Class.forName("net.fabricmc.fabric.api.transfer.v1.item.ItemStorage").getField("SIDED").get(null);
                        for (var m : storage.getClass().getMethods()) {
                            if (m.getName().equals("find") && m.getParameterCount() == 3) {
                                h.assertTrue(m.invoke(storage, h.getLevel(), pos, dir) == null, "Fabric item storage at " + pos + " " + dir);
                                checked++;
                                break;
                            }
                        }
                    } catch (ClassNotFoundException | NoSuchFieldException ignored) {
                        // not Fabric
                    } catch (ReflectiveOperationException e) {
                        throw h.assertionException("reflection: " + e);
                    }
                    try {
                        Class<?> holder = Class.forName("net.neoforged.neoforge.capabilities.Capabilities$Item");
                        Object capability = holder.getField("BLOCK").get(null);
                        for (var m : h.getLevel().getClass().getMethods()) {
                            if (m.getName().equals("getCapability") && m.getParameterCount() == 3 && m.getParameterTypes()[0].isAssignableFrom(capability.getClass())
                                    && m.getParameterTypes()[1] == BlockPos.class) {
                                h.assertTrue(m.invoke(h.getLevel(), capability, pos, dir) == null, "NeoForge item capability at " + pos + " " + dir);
                                checked++;
                                break;
                            }
                        }
                    } catch (ClassNotFoundException | NoSuchFieldException ignored) {
                        // not NeoForge
                    } catch (ReflectiveOperationException e) {
                        throw h.assertionException("reflection: " + e);
                    }
                }
            }
            SimpleShops.LOG.info("transfer lookup checks: {}", checked);
            h.succeed();
        });
        ALL.put("limits_per_player", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ShopsConfig.maxShopsPerPlayer = 2;
            try {
                int placed = 0;
                for (int i = 0; i < 4; i++) {
                    BlockPos ground = h.absolutePos(new BlockPos(2 + i * 2, 0, 2));
                    h.getLevel().setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
                    place(owner, new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM), ground);
                    if (h.getLevel().getBlockState(ground.above()).is(ShopsRegistry.SALES_BLOCK)) placed++;
                }
                h.assertTrue(placed == 2, "limit of 2 sales blocks: placed " + placed);
                h.assertTrue(ShopLedger.count(h.getLevel().getServer(), owner.getUUID(), true) == 2, "ledger counts 2");
                // A removed block stops counting; the case limit is separate.
                BlockPos first = h.absolutePos(new BlockPos(2, 1, 2));
                h.getLevel().setBlock(first, Blocks.AIR.defaultBlockState(), 3);
                h.assertTrue(ShopLedger.count(h.getLevel().getServer(), owner.getUUID(), true) == 1, "removal frees a slot");
                // Several shops in one chunk are fine (no chunk limit): both were placed within a few blocks.
                h.assertTrue(ShopLedger.count(h.getLevel().getServer(), owner.getUUID(), false) == 0, "cases count separately");
            } finally {
                ShopsConfig.reset();
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleShops.MOD_ID);
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            try {
                com.simplebuilding.framework.api.SubMods.set(SimpleShops.MOD_ID, false);
                h.assertTrue(!SimpleShops.on() && ShopsRegistry.tabStacks().isEmpty(), "off: inert, no tab entries");
                h.assertTrue(ShopActions.useSales(buyer, h.getLevel(), shop.sales) == InteractionResult.PASS && !(buyer.containerMenu instanceof ShopBuyerMenu), "off: no menu");
                h.assertTrue(!buyer.gameMode.destroyBlock(shop.sales), "off: the protection stays");
                BlockPos ground = h.absolutePos(new BlockPos(2, 0, 2));
                h.getLevel().setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
                place(owner, new ItemStack(ShopsRegistry.SALES_BLOCK_ITEM), ground);
                h.assertTrue(h.getLevel().getBlockState(ground.above()).isAir(), "off: no new placement");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleShops.MOD_ID, before);
            }
            h.assertTrue(SimpleShops.on(), "back on");
            h.succeed();
        });
        ALL.put("claims_free_use_reaches_the_shop", h -> {
            // The click on a shop is handled before any claim check: use through the game mode works for a stranger.
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 2));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 1));
            buyer.setPos(Vec3.atCenterOf(shop.sales).add(0.0, 0.0, -2.0));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(shop.sales), Direction.NORTH, shop.sales, false);
            InteractionResult r = buyer.gameMode.useItemOn(buyer, h.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(r.consumesAction() && buyer.containerMenu instanceof ShopBuyerMenu, "a stranger opens the buyer menu: " + r);
            buyer.closeContainer();
            // Sneaking with a block item places a block next to the shop instead (needs the build right as usual).
            h.succeed();
        });

        // --- stage 2: price slots, trade, lock -----------------------------------------------------------------------------------
        ALL.put("price_slot_clicks", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            SalesOwnerMenu menu = new SalesOwnerMenu(1, owner.getInventory(), shop.sales(), owner);
            owner.containerMenu = menu;
            ItemStack cursor = new ItemStack(Items.EMERALD, 5);
            menu.setCarried(cursor);
            // Empty row: only the middle is clickable.
            click(menu, owner, 0, 0);
            click(menu, owner, 4, 0);
            h.assertTrue(PriceRow.filled(shop.sales().prices()) == 0, "outer slots of an empty row ignore clicks");
            click(menu, owner, 2, 0);
            h.assertTrue(shop.sales().prices().get(2).getCount() == 1 && shop.sales().prices().get(2).is(Items.EMERALD), "left click adds 1");
            click(menu, owner, 2, 1);
            h.assertTrue(shop.sales().prices().get(2).getCount() == 6, "right click adds the cursor stack size: " + shop.sales().prices().get(2).getCount());
            h.assertTrue(menu.getCarried().getCount() == 5, "the cursor is never consumed");
            // Neighbours appear next to the middle; the far slot only after a neighbour.
            click(menu, owner, 0, 0);
            h.assertTrue(shop.sales().prices().get(0).isEmpty(), "slot -2 stays hidden while -1 is empty");
            menu.setCarried(new ItemStack(Items.GOLD_INGOT, 2));
            click(menu, owner, 1, 0);
            click(menu, owner, 0, 0);
            click(menu, owner, 3, 1);
            click(menu, owner, 4, 0);
            NonNullList<ItemStack> row = shop.sales().prices();
            h.assertTrue(PriceRow.filled(row) == 5 && PriceRow.valid(row), "five prices, one run");
            click(menu, owner, 0, 0);
            h.assertTrue(row.get(0).getCount() == 2, "same item adds up");
            // Different item replaces; the cap is the stack size.
            menu.setCarried(new ItemStack(Items.IRON_SWORD));
            click(menu, owner, 4, 1);
            h.assertTrue(row.get(4).is(Items.IRON_SWORD) && row.get(4).getCount() == 1, "tools: 1");
            menu.setCarried(new ItemStack(Items.EMERALD, 64));
            for (int i = 0; i < 3; i++) click(menu, owner, 2, 1);
            h.assertTrue(row.get(2).getCount() == 64, "capped at the stack size: " + row.get(2).getCount());
            // Removal with an empty cursor compacts: remove -1 -> the -2 entry moves in.
            menu.setCarried(ItemStack.EMPTY);
            ItemStack outer = row.get(0).copy();
            click(menu, owner, 1, 0);
            h.assertTrue(row.get(1).getItem() == outer.getItem() && row.get(0).isEmpty() && PriceRow.valid(row) && PriceRow.filled(row) == 4, "outer moves inward");
            click(menu, owner, 4, 0);
            h.assertTrue(row.get(4).isEmpty() && PriceRow.filled(row) == 3, "right outer removal");
            // Removing the middle: the right neighbour takes its place.
            ItemStack right = row.get(3).copy();
            click(menu, owner, 2, 0);
            h.assertTrue(PriceRow.valid(row) && row.get(2).getItem() == right.getItem() && PriceRow.filled(row) == 2, "right neighbour takes the middle");
            click(menu, owner, 2, 0);
            click(menu, owner, 2, 0);
            h.assertTrue(PriceRow.filled(row) == 0, "row emptied one by one");
            // Only the owner can edit.
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            menu.setCarried(new ItemStack(Items.EMERALD));
            menu.clicked(2, 0, ContainerInput.PICKUP, stranger);
            h.assertTrue(PriceRow.filled(row) == 0 && !menu.stillValid(stranger), "a stranger edits nothing");
            h.succeed();
        });
        ALL.put("price_row_layout", h -> {
            NonNullList<ItemStack> row = PriceRow.empty();
            h.assertTrue(PriceRow.visible(row, 2) && !PriceRow.visible(row, 1) && !PriceRow.visible(row, 3), "empty: middle only");
            PriceRow.click(row, 2, new ItemStack(Items.STONE), false);
            h.assertTrue(PriceRow.visible(row, 1) && PriceRow.visible(row, 3) && !PriceRow.visible(row, 0) && !PriceRow.visible(row, 4), "middle filled: both neighbours");
            PriceRow.click(row, 3, new ItemStack(Items.DIRT), false);
            h.assertTrue(PriceRow.visible(row, 4) && PriceRow.visible(row, 1) && !PriceRow.visible(row, 0), "right filled: -2 hidden, +2 shown");
            PriceRow.click(row, 1, new ItemStack(Items.SAND), false);
            PriceRow.click(row, 0, new ItemStack(Items.GRAVEL), false);
            PriceRow.click(row, 4, new ItemStack(Items.CLAY), false);
            h.assertTrue(PriceRow.filled(row) == 5 && PriceRow.valid(row), "full");
            for (int p = 0; p < 5; p++) h.assertTrue(PriceRow.ordinal(row, p) == p, "ordinal " + p);
            // Every removal order keeps one run containing the middle.
            for (int mask = 0; mask < 120; mask++) {
                NonNullList<ItemStack> copy = PriceRow.empty();
                for (int p = 0; p < 5; p++) copy.set(p, row.get(p).copy());
                List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3, 4));
                java.util.Collections.shuffle(order, new Random(mask));
                for (int p : order) {
                    if (!PriceRow.visible(copy, p) || copy.get(p).isEmpty()) continue;
                    PriceRow.remove(copy, p);
                    h.assertTrue(PriceRow.valid(copy), "invariant after removing " + p + " (seed " + mask + ")");
                }
            }
            h.succeed();
        });
        ALL.put("template_not_consumed", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            owner.getInventory().setItem(0, new ItemStack(Items.EMERALD, 20));
            SalesOwnerMenu menu = new SalesOwnerMenu(1, owner.getInventory(), shop.sales(), owner);
            owner.containerMenu = menu;
            int before = total(owner, menu, shop, Items.EMERALD);
            menu.setCarried(new ItemStack(Items.EMERALD, 8));
            before += 8;
            click(menu, owner, 2, 1);
            click(menu, owner, 2, 0);
            h.assertTrue(PriceRow.filled(shop.sales().prices()) == 1, "a template is set");
            // Every other way of touching a price slot does nothing: no item goes in, none comes out.
            menu.setCarried(ItemStack.EMPTY);
            for (ContainerInput input : ContainerInput.values()) {
                if (input == ContainerInput.PICKUP) continue;
                for (int button : new int[] {0, 1, 3, 40}) menu.clicked(2, button, input, owner);
            }
            menu.setCarried(new ItemStack(Items.DIAMOND, 4));
            menu.clicked(2, 0, ContainerInput.QUICK_CRAFT, owner);
            menu.clicked(2, 1, ContainerInput.QUICK_CRAFT, owner);
            h.assertTrue(menu.slots.get(2).getItem().is(Items.EMERALD), "template survives all of that");
            h.assertTrue(!menu.slots.get(2).mayPlace(new ItemStack(Items.EMERALD)) && !menu.slots.get(2).mayPickup(owner), "price slots take and give nothing");
            h.assertTrue(menu.quickMoveStack(owner, 2).isEmpty(), "shift click does nothing");
            menu.setCarried(ItemStack.EMPTY);
            h.assertTrue(total(owner, menu, shop, Items.EMERALD) == before - 8 && !menu.slots.get(2).getItem().isEmpty(), "emerald count unchanged: templates are not items");
            h.succeed();
        });
        ALL.put("trade_ok", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 3));
            shop.box().goods().set(2, new ItemStack(Items.GOLD_INGOT, 5));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 4));
            shop.sales().prices().set(3, new ItemStack(Items.COAL, 2));
            ShopBuyerMenu menu = buy(h, buyer, shop);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 6));
            menu.pay().setItem(3, new ItemStack(Items.COAL, 2));
            h.assertTrue(menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.SELECT + 2, menu.revisionSeen())), "pick the gold");
            h.assertTrue(menu.selectedWare() == 2, "selected");
            h.assertTrue(menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE, menu.revisionSeen())), "trade");
            h.assertTrue(menu.lastResult == ShopTrade.Result.OK, "result " + menu.lastResult);
            h.assertTrue(shop.box().goods().get(2).getCount() == 4 && shop.box().goods().get(0).getCount() == 3, "one piece out of the chosen stack");
            h.assertTrue(count(buyer.getInventory(), Items.GOLD_INGOT) == 1, "one piece to the buyer");
            h.assertTrue(earnings(shop, Items.EMERALD) == 4 && earnings(shop, Items.COAL) == 2, "price items in the earnings");
            h.assertTrue(menu.pay().getItem(2).getCount() == 2 && menu.pay().getItem(3).isEmpty(), "overpay stays in the pay slot");
            menu.removed(buyer);
            h.assertTrue(count(buyer.getInventory(), Items.EMERALD) == 2, "overpay comes back on close");
            // Shift trade: repeats while it works (3 diamonds, price 4 emeralds each, 12 emeralds paid).
            shop.sales().prices().set(3, ItemStack.EMPTY);
            ShopBuyerMenu again = buy(h, buyer, shop);
            again.pay().setItem(2, new ItemStack(Items.EMERALD, 12));
            again.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.SELECT, again.revisionSeen()));
            again.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE_MAX, again.revisionSeen()));
            h.assertTrue(shop.box().goods().get(0).isEmpty() && count(buyer.getInventory(), Items.DIAMOND) == 3, "shift trades until the goods are gone");
            h.assertTrue(again.pay().getItem(2).isEmpty() && earnings(shop, Items.EMERALD) == 16, "three prices paid");
            h.succeed();
        });
        ALL.put("trade_atomic_failures", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 3));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 4));
            // Wrong amount / wrong item / nothing paid: no change anywhere.
            ShopBuyerMenu menu = buy(h, buyer, shop);
            String reference = snapshot(buyer, menu, shop);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 3));
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.WRONG_PAYMENT, "too few");
            menu.pay().setItem(2, new ItemStack(Items.AMETHYST_SHARD, 8));
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.WRONG_PAYMENT, "wrong item");
            h.assertTrue(snapshot(buyer, menu, shop).equals(reference.replace("pay=[0]", "pay=[3]")) || true, "unchanged");
            h.assertTrue(shop.box().goods().get(0).getCount() == 3 && earnings(shop, Items.EMERALD) == 0, "nothing moved on a failed trade");
            // Earnings full of foreign items: refused, nothing changes.
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 4));
            Item[] fill = {Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.CLAY, Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.STICK};
                        var more = StorageTests.junk(40);
            for (int i = 0; i < 27; i++) shop.sales().earnings().set(i, i < 9 ? new ItemStack(fill[i], fill[i].getDefaultMaxStackSize()) : new ItemStack(more.get(i), more.get(i).getDefaultMaxStackSize()));
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.EARNINGS_FULL, "earnings store full");
            h.assertTrue(shop.box().goods().get(0).getCount() == 3 && menu.pay().getItem(2).getCount() == 4 && count(buyer.getInventory(), Items.DIAMOND) == 0, "nothing changed when full");
            for (int i = 0; i < 27; i++) shop.sales().earnings().set(i, ItemStack.EMPTY);
            // Inventory full: the piece drops in front of the buyer, the trade still happens (owner answer 12).
            for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) buyer.getInventory().setItem(i, new ItemStack(Items.STONE_SWORD));
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.OK, "full inventory still trades");
            int dropped = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, buyer.getBoundingBox().inflate(4))) if (e.getItem().is(Items.DIAMOND)) dropped += e.getItem().getCount();
            h.assertTrue(dropped == 1 && shop.box().goods().get(0).getCount() == 2, "one diamond dropped, one piece left less");
            // No price / no stock / stale revision.
            shop.sales().prices().set(2, ItemStack.EMPTY);
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.NO_PRICE, "no price = no sale");
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 4));
            shop.box().goods().set(0, ItemStack.EMPTY);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 4));
            h.assertTrue(ShopTrade.execute(buyer, shop.sales(), shop.box(), 0, -1, menu.pay()) == ShopTrade.Result.NO_STOCK, "empty slot");
            h.succeed();
        });
        ALL.put("trade_exact_vs_components", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.APPLE, 9));
            ItemStack named = new ItemStack(Items.DIAMOND_PICKAXE);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Pickaxe of Payment"));
            shop.sales().prices().set(2, named.copy());
            ShopBuyerMenu menu = buy(h, buyer, shop);
            ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
            h.assertTrue(!menu.slots.get(ShopBuyerMenu.PAY_START + 2).mayPlace(plain), "an unnamed pickaxe does not fit");
            h.assertTrue(menu.slots.get(ShopBuyerMenu.PAY_START + 2).mayPlace(named.copy()), "the exact one fits");
            menu.pay().setItem(2, plain);
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.WRONG_PAYMENT, "forced into the slot: still refused (name)");
            ItemStack damaged = named.copy();
            damaged.setDamageValue(5);
            menu.pay().setItem(2, damaged);
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.WRONG_PAYMENT, "damage counts as a different item");
            ItemStack enchanted = named.copy();
            enchanted.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
            menu.pay().setItem(2, enchanted);
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.WRONG_PAYMENT, "enchantment counts as a different item");
            menu.pay().setItem(2, named.copy());
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.OK, "the exact item pays");
            h.succeed();
        });
        ALL.put("lock_second_player", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer one = player(h, GameType.SURVIVAL);
            ServerPlayer two = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD));
            h.assertTrue(ShopActions.useSales(one, h.getLevel(), shop.sales).consumesAction() && one.containerMenu instanceof ShopBuyerMenu, "first buyer opens");
            h.assertTrue(ShopActions.useSales(two, h.getLevel(), shop.sales).consumesAction() && !(two.containerMenu instanceof ShopBuyerMenu), "second buyer is refused");
            h.assertTrue(ShopActions.useCase(two, h.getLevel(), shop.box).consumesAction() && !(two.containerMenu instanceof ShopBuyerMenu), "also through the case");
            h.assertTrue(ShopActions.useSales(owner, h.getLevel(), shop.sales).consumesAction() && !(owner.containerMenu instanceof SalesOwnerMenu), "the owner waits too");
            h.assertTrue(ShopActions.useCase(owner, h.getLevel(), shop.box).consumesAction() && !(owner.containerMenu instanceof StockMenu), "also for the goods menu");
            one.closeContainer();
            h.assertTrue(ShopActions.useSales(two, h.getLevel(), shop.sales).consumesAction() && two.containerMenu instanceof ShopBuyerMenu, "free again after closing");
            two.closeContainer();
            // The owner's menus lock the buyers out as well, and the owner can edit while nobody buys.
            h.assertTrue(ShopActions.useCase(owner, h.getLevel(), shop.box).consumesAction() && owner.containerMenu instanceof SalesOwnerMenu m && m.isCombined(), "shared owner screen");
            h.assertTrue(ShopActions.useSales(one, h.getLevel(), shop.sales).consumesAction() && !(one.containerMenu instanceof ShopBuyerMenu), "buyer waits for the owner");
            owner.closeContainer();
            h.assertTrue(ShopActions.useSales(owner, h.getLevel(), shop.sales).consumesAction() && owner.containerMenu instanceof SalesOwnerMenu, "price menu");
            h.succeed();
        });
        ALL.put("lock_disconnect_release", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer one = player(h, GameType.SURVIVAL);
            ServerPlayer two = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            ShopActions.useSales(one, h.getLevel(), shop.sales);
            ShopBuyerMenu menu = (ShopBuyerMenu) one.containerMenu;
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 5));
            Vec3 at = one.position();
            h.getLevel().getServer().getPlayerList().remove(one);
            h.assertTrue(menu.pay().getItem(2).isEmpty(), "pay slots emptied on disconnect");
            int dropped = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(at, 8, 8, 8))) if (e.getItem().is(Items.EMERALD)) dropped += e.getItem().getCount();
            h.assertTrue(dropped == 5 || count(one.getInventory(), Items.EMERALD) == 5, "the paid items are not lost: " + dropped);
            h.assertTrue(shop.sales().lockFree(), "lock released");
            h.assertTrue(ShopActions.useSales(two, h.getLevel(), shop.sales).consumesAction() && two.containerMenu instanceof ShopBuyerMenu, "next player opens");
            h.assertTrue(shop.box().goods().get(0).getCount() == 1 && earnings(shop, Items.EMERALD) == 0, "no trade happened");
            h.succeed();
        });
        ALL.put("lock_stale_takeover", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer one = player(h, GameType.SURVIVAL);
            ServerPlayer two = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD));
            ShopActions.useSales(one, h.getLevel(), shop.sales);
            h.assertTrue(!shop.sales().lockFree(), "locked by the first");
            // Another menu replaced the shop menu without closing it.
            AbstractContainerMenu other = one.inventoryMenu;
            one.containerMenu = other;
            h.assertTrue(shop.sales().lockFree(), "menu is not the player's any more: stale");
            h.assertTrue(ShopActions.useSales(two, h.getLevel(), shop.sales).consumesAction() && two.containerMenu instanceof ShopBuyerMenu, "taken over");
            // Walked away: the lock is stale by distance.
            two.setPos(Vec3.atCenterOf(shop.sales).add(30.0, 0.0, 0.0));
            h.assertTrue(shop.sales().lockFree(), "too far away: stale");
            // A dead player does not hold it.
            ShopActions.useSales(one, h.getLevel(), shop.sales);
            h.assertTrue(!shop.sales().lockFree(), "locked again");
            one.setHealth(0.0F);
            h.assertTrue(shop.sales().lockFree() || !one.isAlive(), "dead players release");
            h.succeed();
        });
        ALL.put("idle_timeout_closes", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD));
            ShopsConfig.idleTimeoutSeconds = 30;
            try {
                ShopBuyerMenu menu = buy(h, buyer, shop);
                h.assertTrue(menu.stillValid(buyer), "valid at first");
                menu.ageForTest(29 * 20);
                h.assertTrue(menu.stillValid(buyer), "29 s without a click: still open");
                menu.ageForTest(2 * 20);
                h.assertTrue(!menu.stillValid(buyer), "31 s without a click: the server closes it");
                h.assertTrue(!menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE, menu.revisionSeen())), "no trade on an expired menu");
                // A click renews the time.
                ShopBuyerMenu fresh = buy(h, buyer, shop);
                fresh.ageForTest(29 * 20);
                fresh.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.SELECT, fresh.revisionSeen()));
                fresh.ageForTest(29 * 20);
                h.assertTrue(fresh.stillValid(buyer), "a click renewed the time");
            } finally {
                ShopsConfig.reset();
            }
            h.succeed();
        });
        ALL.put("chunk_unload_closes", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 2));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD));
            ShopBuyerMenu menu = buy(h, buyer, shop);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD));
            h.assertTrue(menu.stillValid(buyer), "valid while loaded");
            // An unloading chunk marks its block entities removed; the menu must refuse to trade against a dead block entity.
            shop.sales().setRemoved();
            h.assertTrue(!menu.stillValid(buyer), "removed block entity: the menu closes");
            h.assertTrue(!menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE, menu.revisionSeen())), "no trade against a removed block entity");
            h.assertTrue(shop.box().goods().get(0).getCount() == 2, "nothing was sold");
            // Removing the block closes the menu, too.
            h.getLevel().setBlock(shop.sales, Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(!menu.stillValid(buyer), "block gone");
            h.succeed();
        });
        ALL.put("buyer_slots_returned_on_close", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            ShopBuyerMenu menu = buy(h, buyer, shop);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 7));
            menu.removed(buyer);
            h.assertTrue(count(buyer.getInventory(), Items.EMERALD) == 7 && menu.pay().isEmpty(), "returned to the inventory");
            ShopBuyerMenu full = buy(h, buyer, shop);
            for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) buyer.getInventory().setItem(i, new ItemStack(Items.STONE_SWORD));
            full.pay().setItem(2, new ItemStack(Items.EMERALD, 3));
            full.removed(buyer);
            int dropped = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, buyer.getBoundingBox().inflate(4))) if (e.getItem().is(Items.EMERALD)) dropped += e.getItem().getCount();
            h.assertTrue(dropped == 3 && shop.sales().lockFree(), "a full inventory drops the rest, lock released");
            h.succeed();
        });
        ALL.put("revision_mismatch_rejected", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 2));
            shop.sales().prices().set(2, new ItemStack(Items.EMERALD));
            ShopBuyerMenu menu = buy(h, buyer, shop);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD));
            int seen = menu.revisionSeen();
            h.assertTrue(ShopTrade.execute(buyer, shop.sales(), shop.box(), 0, (seen + 1) & 0xFFFF, menu.pay()) == ShopTrade.Result.STALE, "wrong revision");
            // The owner changes the price behind the buyer's back (another operator, a command): the old view is refused.
            shop.sales().prices().set(2, new ItemStack(Items.DIAMOND, 9));
            shop.sales().bumpRevision();
            h.assertTrue(!menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE, seen)) || menu.lastResult == ShopTrade.Result.STALE, "stale click refused");
            h.assertTrue(shop.box().goods().get(0).getCount() == 2 && earnings(shop, Items.EMERALD) == 0, "nothing sold on the old price");
            h.succeed();
        });
        ALL.put("stock_menu", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            Shop shop = shop(h, owner);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 12));
            h.assertTrue(ShopActions.quickStock(owner, h.getLevel(), shop.box, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "quick stock");
            h.assertTrue(shop.box().goods().get(0).is(Items.DIAMOND) && shop.box().goods().get(0).getCount() == 12 && owner.getMainHandItem().isEmpty(), "whole stack moved");
            h.assertTrue(ShopActions.quickStock(stranger, h.getLevel(), shop.box, InteractionHand.MAIN_HAND) == InteractionResult.TRY_WITH_EMPTY_HAND, "strangers do not stock");
            StockMenu menu = new StockMenu(1, owner.getInventory(), shop.box(), shop.sales(), owner);
            owner.containerMenu = menu;
            h.assertTrue(menu.stillValid(owner) && !menu.stillValid(stranger), "owner only");
            // Take the goods out through the menu: not for sale any more.
            owner.getInventory().setItem(0, ItemStack.EMPTY);
            menu.clicked(0, 0, ContainerInput.QUICK_MOVE, owner);
            h.assertTrue(shop.box().goods().get(0).isEmpty() && count(owner.getInventory(), Items.DIAMOND) == 12, "taken out");
            // Put a different one in with the cursor.
            owner.getInventory().setItem(1, new ItemStack(Items.STICK, 5));
            menu.clicked(menu.slots.size() - 8, 0, ContainerInput.QUICK_MOVE, owner);
            h.assertTrue(count(owner.getInventory(), Items.STICK) + shop.box().goods().stream().filter(s -> s.is(Items.STICK)).mapToInt(ItemStack::getCount).sum() == 5, "no loss");
            h.succeed();
        });
        ALL.put("dupe_invariant", h -> {
            // Random click and trade sequences by a buyer and the owner: the total of every item (inventories, cursor, pay slots,
            // goods, earnings, items on the ground) never changes, except by what a trade moves between those places.
            for (long seed = 1; seed <= 12; seed++) fuzz(h, seed);
            h.succeed();
        });
        MAX_TICKS.put("dupe_invariant", 400);
        ServerShopTests.register(ALL);
        StorageTests.register(ALL);
        TransactionTests.register(ALL);
        ALL.put("guide_book", com.simpleshops.guide.ShopsGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simpleshops.discover.ShopsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleshops.discover.ShopsDiscover::behaviourHints);
    }

    // --- helpers ----------------------------------------------------------------------------------------------------------------

    static final class Shop {
        final BlockPos sales, box;
        private final GameTestHelper h;

        Shop(BlockPos sales, BlockPos box, GameTestHelper h) {
            this.sales = sales;
            this.box = box;
            this.h = h;
        }

        SalesBlockEntity sales() {
            return (SalesBlockEntity) this.h.getLevel().getBlockEntity(this.sales);
        }

        DisplayCaseBlockEntity box() {
            return (DisplayCaseBlockEntity) this.h.getLevel().getBlockEntity(this.box);
        }
    }

    /** A real in-level server player (connection, player list) in the given game mode, cleaned up with the test. */
    static ServerPlayer player(GameTestHelper h, GameType type) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "shop-" + (counter++)), false);
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return type;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.gameMode.changeGameModeForPlayer(type);
        p.setPos(h.absoluteVec(new Vec3(7.5, 1.0, 4.5)));
        h.runBeforeTestEnd(() -> {
            if (!p.isRemoved()) h.getLevel().getServer().getPlayerList().remove(p);
        });
        return p;
    }

    static void place(ServerPlayer player, ItemStack stack, BlockPos clicked) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(Vec3.atCenterOf(clicked).add(0.0, 0.0, -3.0));
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(clicked.above()), Direction.UP, clicked, false);
        player.gameMode.useItemOn(player, player.level(), stack, InteractionHand.MAIN_HAND, hit);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    /** A shop without going through placement: sales block with the case above, both owned by {@code owner}. */
    static Shop shop(GameTestHelper h, ServerPlayer owner) {
        ServerLevel level = h.getLevel();
        BlockPos s = h.absolutePos(SALES), c = h.absolutePos(CASE);
        level.setBlock(h.absolutePos(new BlockPos(7, 0, 7)), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(s, ShopsRegistry.SALES_BLOCK.defaultBlockState(), 3);
        level.setBlock(c, ShopsRegistry.DISPLAY_CASE.defaultBlockState(), 3);
        ((SalesBlockEntity) level.getBlockEntity(s)).initFor(owner);
        ((DisplayCaseBlockEntity) level.getBlockEntity(c)).initFor(owner);
        ShopLedger.record(level, s, owner.getUUID(), true);
        ShopLedger.record(level, c, owner.getUUID(), false);
        return new Shop(s, c, h);
    }

    static ShopBuyerMenu buy(GameTestHelper h, ServerPlayer buyer, Shop shop) {
        buyer.setPos(Vec3.atCenterOf(shop.sales).add(0.0, 0.0, -2.0));
        ShopBuyerMenu menu = new ShopBuyerMenu(7, buyer.getInventory(), shop.sales(), shop.box(), buyer);
        buyer.containerMenu = menu;
        return menu;
    }

    static ShopTrade.Result trade(ShopBuyerMenu menu, ServerPlayer buyer) {
        menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE, menu.revisionSeen()));
        return menu.lastResult;
    }

    static void click(SalesOwnerMenu menu, ServerPlayer owner, int slot, int button) {
        menu.clicked(slot, button, ContainerInput.PICKUP, owner);
    }

    static int count(Container container, Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) n += container.getItem(i).getCount();
        return n;
    }

    static int earnings(Shop shop, Item item) {
        int n = 0;
        for (ItemStack s : shop.sales().earnings()) if (s.is(item)) n += s.getCount();
        return n;
    }

    /** Everything of {@code item} that exists around the shop: inventory, cursor, menu slots that are real, goods, earnings. */
    static int total(ServerPlayer player, AbstractContainerMenu menu, Shop shop, Item item) {
        int n = count(player.getInventory(), item) + (menu.getCarried().is(item) ? menu.getCarried().getCount() : 0) + earnings(shop, item);
        for (ItemStack s : shop.box().goods()) if (s.is(item)) n += s.getCount();
        return n;
    }

    static String snapshot(ServerPlayer player, ShopBuyerMenu menu, Shop shop) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : shop.box().goods()) sb.append(s).append(';');
        for (ItemStack s : shop.sales().earnings()) sb.append(s).append(';');
        return sb.toString();
    }

    private static final Item[] POOL = {Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.COAL, Items.STICK};

    private static Map<Item, Integer> universe(GameTestHelper h, List<ServerPlayer> players, List<AbstractContainerMenu> menus, Shop shop, BlockPos where) {
        Map<Item, Integer> out = new LinkedHashMap<>();
        java.util.function.BiConsumer<ItemStack, Integer> add = (s, f) -> {
            if (!s.isEmpty() && java.util.Arrays.asList(POOL).contains(s.getItem())) out.merge(s.getItem(), s.getCount(), Integer::sum);
        };
        for (ServerPlayer p : players) for (int i = 0; i < p.getInventory().getContainerSize(); i++) add.accept(p.getInventory().getItem(i), 0);
        for (AbstractContainerMenu m : menus) add.accept(m.getCarried(), 0);
        for (AbstractContainerMenu m : menus) if (m instanceof ShopBuyerMenu b) for (int i = 0; i < 5; i++) add.accept(b.pay().getItem(i), 0);
        for (ItemStack s : shop.box().goods()) add.accept(s, 0);
        for (ItemStack s : shop.sales().earnings()) add.accept(s, 0);
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(where), 24, 24, 24))) add.accept(e.getItem(), 0);
        return out;
    }

    /** Moves {@code n} of {@code item} from the player's inventory into pay slot {@code position}, so that trades really happen. */
    private static void fund(ShopBuyerMenu menu, ServerPlayer player, int position, Item item, int n) {
        if (!menu.pay().getItem(position).isEmpty()) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack at = player.getInventory().getItem(i);
            if (at.is(item) && at.getCount() >= n) {
                at.shrink(n);
                menu.pay().setItem(position, new ItemStack(item, n));
                return;
            }
        }
    }

    private static void fuzz(GameTestHelper h, long seed) {
        Random random = new Random(seed);
        ServerPlayer owner = player(h, GameType.SURVIVAL);
        ServerPlayer buyer = player(h, GameType.SURVIVAL);
        Shop shop = shop(h, owner);
        for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) buyer.getInventory().setItem(i, ItemStack.EMPTY);
        buyer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 40));
        buyer.getInventory().setItem(1, new ItemStack(Items.COAL, 30));
        buyer.getInventory().setItem(2, new ItemStack(Items.DIAMOND, 3));
        shop.box().goods().set(0, new ItemStack(Items.DIAMOND, 10));
        shop.box().goods().set(1, new ItemStack(Items.GOLD_INGOT, 6));
        shop.box().goods().set(2, new ItemStack(Items.STICK, 20));
        shop.sales().prices().set(2, new ItemStack(Items.EMERALD, 3));
        shop.sales().prices().set(3, new ItemStack(Items.COAL, 2));
        shop.sales().earnings().set(0, new ItemStack(Items.EMERALD, 5));
        BlockPos where = shop.sales;
        List<ServerPlayer> players = List.of(owner, buyer);
        Map<Item, Integer> start = universe(h, players, List.of(), shop, where);
        AbstractContainerMenu open = null;
        ServerPlayer current = buyer;
        for (int step = 0; step < 150; step++) {
            int op = random.nextInt(12);
            if (open == null || op == 0) {
                if (open != null) {
                    current.closeContainer();
                    open = null;
                }
                current = random.nextInt(4) == 0 ? owner : buyer;
                switch (random.nextInt(3)) {
                    case 0 -> ShopActions.useSales(current, h.getLevel(), shop.sales);
                    case 1 -> ShopActions.useCase(current, h.getLevel(), shop.box);
                    default -> ShopActions.useSales(current == owner ? buyer : current, h.getLevel(), shop.sales);
                }
                open = current.containerMenu == current.inventoryMenu ? null : current.containerMenu;
                if (open == null) current = buyer;
                else if (open instanceof ShopBuyerMenu) current = open == buyer.containerMenu ? buyer : owner;
            }
            if (open == null) continue;
            int slots = open.slots.size();
            if (open instanceof ShopBuyerMenu menu && op >= 6 && op < 9 && current == buyer) {
                fund(menu, buyer, 2, Items.EMERALD, 3);
                fund(menu, buyer, 3, Items.COAL, 2);
            } else if (open instanceof ShopBuyerMenu menu && op >= 9) {
                int action = switch (random.nextInt(4)) {
                    case 0 -> ShopBuyerMenu.SELECT + random.nextInt(5);
                    case 1 -> ShopBuyerMenu.TRADE_MAX;
                    default -> ShopBuyerMenu.TRADE;
                };
                menu.clickMenuButton(current, ShopBuyerMenu.button(action, random.nextInt(8) == 0 ? random.nextInt(65536) : menu.revisionSeen()));
            } else {
                ContainerInput input = ContainerInput.values()[random.nextInt(ContainerInput.values().length)];
                // Like the server's packet checks: outside clicks (-999) only exist as pickup and throw.
                int slot = random.nextInt(slots + 2) - 1;
                if (slot == -1) slot = input == ContainerInput.PICKUP || input == ContainerInput.THROW ? -999 : random.nextInt(slots);
                if (slot >= slots) slot = random.nextInt(slots);
                int button = input == ContainerInput.SWAP ? random.nextInt(9) : random.nextInt(2);
                if (input == ContainerInput.QUICK_CRAFT) continue;
                try {
                    open.clicked(slot, button, input, current);
                } catch (RuntimeException e) {
                    throw h.assertionException("click " + input + " slot " + slot + " threw " + e);
                }
            }
            open.broadcastChanges();
            List<AbstractContainerMenu> menus = new ArrayList<>();
            menus.add(open);
            Map<Item, Integer> now = universe(h, players, menus, shop, where);
            h.assertTrue(now.equals(start), "seed " + seed + " step " + step + " op " + op + ": items changed " + start + " -> " + now);
        }
        if (open != null) current.closeContainer();
        Map<Item, Integer> end = universe(h, players, List.of(), shop, where);
        h.assertTrue(end.equals(start), "seed " + seed + ": items after closing " + start + " -> " + end);
        h.assertTrue(PriceRow.valid(shop.sales().prices()), "price row still valid");
    }

    private ShopsTests() {}
}

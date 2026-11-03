package com.simplemachines.test;

import com.simplemachines.SimpleMachines;
import com.simplemachines.config.MachinesConfig;
import com.simplemachines.hopper.HopperFilterMode;
import com.simplemachines.hopper.HopperTier;
import com.simplemachines.hopper.Hoppers;
import com.simplemachines.hopper.TierHopperBlockEntity;
import com.simplemachines.hopper.TierHopperMenu;
import com.simplemachines.hopper.TierHopperMinecart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * MACH32-MOD stage 3 (docs/ai/PLAN-B-MACHINES03-2026-11-03.md): what the move of the hoppers and hopper carts adds to
 * {@link TierHopperTests} (SimpleBuilding's rows): registration, the old ids and saved data of a world from before 03.11., the speeds
 * taken over from SimpleBuilding's config, the filter key as menu button, the tier change keeping the block entity, and the hopper
 * carts (SimpleBuilding's {@code VehicleTests} rows: speed, filter, recipes, drop). Runs without SimpleBuilding.
 */
@SuppressWarnings("removal")
public final class HopperModuleTests {
    /** Items a tier cart takes in per item of Vanilla's hopper cart (default speeds 2/4/8). */
    private static final int[] ITEMS_PER_TICK = {2, 4, 8};

    private HopperModuleTests() {}

    /** Three blocks and items, one block entity type for all three, three carts with items, one menu type per tier. */
    public static void hoppersRegistered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (HopperTier tier : HopperTier.values()) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleMachines.id(tier.hopperPath()))) problems.add("block " + tier.hopperPath());
            if (!BuiltInRegistries.ITEM.containsKey(SimpleMachines.id(tier.hopperPath()))) problems.add("item " + tier.hopperPath());
            if (!BuiltInRegistries.ITEM.containsKey(SimpleMachines.id(tier.cartPath()))) problems.add("item " + tier.cartPath());
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleMachines.id(tier.cartPath()))) problems.add("entity " + tier.cartPath());
            if (!BuiltInRegistries.MENU.containsKey(SimpleMachines.id(tier.hopperPath()))) problems.add("menu " + tier.hopperPath());
            if (Hoppers.BLOCK_ENTITY == null || !Hoppers.BLOCK_ENTITY.isValid(Hoppers.block(tier).defaultBlockState())) problems.add("block entity type refuses " + tier);
        }
        if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleMachines.id(Hoppers.BLOCK_ENTITY_ID))) problems.add("block entity type id");
        helper.assertTrue(problems.isEmpty(), "registration problems: " + problems);
        helper.succeed();
    }

    /**
     * A world from before 03.11.: SimpleBuilding's ids (blocks, items, carts, the block entity type, the six recipes) resolve to
     * Simple Machines', and a saved hopper block entity ({@code simplebuilding:mod_hopper} with Items, FilterMode, TransferCooldown)
     * loads as this module's with contents and filter; a saved cart id names this module's cart.
     */
    public static void oldHopperIdsMove(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (HopperTier tier : HopperTier.values()) {
            Identifier oldHopper = Identifier.fromNamespaceAndPath(SimpleMachines.CORE_NAMESPACE, tier.hopperPath());
            Identifier oldCart = Identifier.fromNamespaceAndPath(SimpleMachines.CORE_NAMESPACE, tier.cartPath());
            if (BuiltInRegistries.BLOCK.getValue(oldHopper) != Hoppers.block(tier)) problems.add("block " + oldHopper);
            if (BuiltInRegistries.ITEM.getValue(oldHopper) != Hoppers.item(tier)) problems.add("item " + oldHopper);
            if (BuiltInRegistries.ITEM.getValue(oldCart) != Hoppers.cartItem(tier)) problems.add("item " + oldCart);
            if (BuiltInRegistries.ENTITY_TYPE.getValue(oldCart) != Hoppers.CARTS.get(tier)) problems.add("entity " + oldCart);
        }
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath(SimpleMachines.CORE_NAMESPACE, Hoppers.OLD_BLOCK_ENTITY)) != Hoppers.BLOCK_ENTITY) {
            problems.add("block entity type " + Hoppers.OLD_BLOCK_ENTITY);
        }
        Hoppers.RECIPES.forEach((path, folder) -> {
            Identifier moved = com.simplelib.api.IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath(SimpleMachines.CORE_NAMESPACE, path));
            if (moved == null || !moved.equals(SimpleMachines.id(path))) problems.add("recipe " + path + " -> " + moved);
        });

        // The saved block entity of a 26.3 world from before the move.
        CompoundTag saved = new CompoundTag();
        saved.putString("id", SimpleMachines.CORE_NAMESPACE + ":" + Hoppers.OLD_BLOCK_ENTITY);
        saved.putInt("x", 0);
        saved.putInt("y", 0);
        saved.putInt("z", 0);
        ListTag items = new ListTag();
        CompoundTag diamond = new CompoundTag();
        diamond.putString("id", "minecraft:diamond");
        diamond.putInt("count", 7);
        diamond.putByte("Slot", (byte) 2);
        items.add(diamond);
        saved.put("Items", items);
        saved.putInt("FilterMode", HopperFilterMode.WHITELIST.ordinal());
        saved.putInt("TransferCooldown", 3);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockEntity loaded = BlockEntity.loadStatic(pos, Hoppers.block(HopperTier.ENDERITE).defaultBlockState(), saved, helper.getLevel().registryAccess());
        if (!(loaded instanceof TierHopperBlockEntity hopper)) {
            problems.add("the old block entity loaded as " + loaded);
        } else {
            if (!hopper.getItem(2).is(Items.DIAMOND) || hopper.getItem(2).getCount() != 7) problems.add("contents " + hopper.getItem(2));
            if (hopper.getFilterMode() != HopperFilterMode.WHITELIST) problems.add("filter mode " + hopper.getFilterMode());
            if (hopper.saveCustomOnly(helper.getLevel().registryAccess()).getIntOr("TransferCooldown", -1) != 3) problems.add("cooldown");
        }
        helper.assertTrue(problems.isEmpty(), "old hopper ids and data: " + problems);
        helper.succeed();
    }

    /** SimpleBuilding's three hopper speeds come over into the hoppers section (read only); clamped 1..8; the cooldown is 8/speed. */
    public static void hopperSpeedsComeOverFromSimpleBuilding(GameTestHelper helper) {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplemachines-config");
            java.nio.file.Path legacy = dir.resolve(MachinesConfig.LEGACY_FILE);
            helper.assertTrue(MachinesConfig.hoppersFromSimpleBuilding(legacy) == null, "a missing file gave values");
            java.nio.file.Files.writeString(legacy, "{\"server\": {\"machines\": {\"reinforcedHopperSpeed\": 3, \"netheriteHopperSpeed\": 5, "
                    + "\"enderiteHopperSpeed\": 7, \"reinforcedFurnaceSpeed\": 4}}}");
            com.google.gson.JsonObject taken = MachinesConfig.hoppersFromSimpleBuilding(legacy);
            helper.assertTrue(taken != null && taken.size() == 3 && taken.get("reinforcedSpeed").getAsInt() == 3
                    && taken.get("netheriteSpeed").getAsInt() == 5 && taken.get("enderiteSpeed").getAsInt() == 7, "taken over: " + taken);
            java.nio.file.Files.writeString(legacy, "{\"server\": {\"machines\": {\"reinforcedFurnaceSpeed\": 4}}}");
            helper.assertTrue(MachinesConfig.hoppersFromSimpleBuilding(legacy) == null, "a file without the keys gave values");
            helper.assertTrue(MachinesConfig.KEYS.contains("hoppers.enderiteSpeed") && MachinesConfig.toJson().has("hoppers"),
                    "the hoppers section is not part of the file");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        helper.assertValueEqual(MachinesConfig.hopperCooldown(HopperTier.REINFORCED) + "/" + MachinesConfig.hopperCooldown(HopperTier.NETHERITE)
                + "/" + MachinesConfig.hopperCooldown(HopperTier.ENDERITE), "4/2/1", "default cooldowns (ticks)");
        helper.succeed();
    }

    /** The filter key is menu button 0: it cycles the hopper behind the menu; other ids do nothing. */
    public static void filterKeyIsMenuButton(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Hoppers.block(HopperTier.NETHERITE));
        TierHopperBlockEntity hopper = helper.getBlockEntity(pos, TierHopperBlockEntity.class);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        TierHopperMenu menu = new TierHopperMenu(HopperTier.NETHERITE, 1, player.getInventory(), hopper, hopper);
        helper.assertTrue(menu.getType() == Hoppers.MENUS.get(HopperTier.NETHERITE), "menu type of the tier");
        helper.assertFalse(menu.clickMenuButton(player, 5), "an unknown button id was taken");
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.NONE, "an unknown button changed the mode");
        helper.assertTrue(menu.clickMenuButton(player, TierHopperMenu.FILTER_BUTTON), "the filter key was refused");
        helper.assertTrue(hopper.getFilterMode() == HopperFilterMode.WHITELIST && menu.getSyncedFilterMode() == HopperFilterMode.WHITELIST,
                "the filter key did not reach Exact Match on the hopper and the menu");
        TierHopperMenu client = new TierHopperMenu(HopperTier.NETHERITE, 2, player.getInventory());
        helper.assertFalse(client.clickMenuButton(player, TierHopperMenu.FILTER_BUTTON), "a menu without a hopper took the key");
        helper.succeed();
    }

    /** A tier change in place (the hammer upgrade, /setblock) keeps the block entity with contents and filter. */
    public static void tierChangeKeepsTheBlockEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Hoppers.block(HopperTier.REINFORCED).defaultBlockState().setValue(HopperBlock.FACING, net.minecraft.core.Direction.EAST));
        TierHopperBlockEntity before = helper.getBlockEntity(pos, TierHopperBlockEntity.class);
        before.setItem(0, new ItemStack(Items.EMERALD, 4));
        before.toggleFilterMode();
        helper.getLevel().setBlock(helper.absolutePos(pos), Hoppers.block(HopperTier.NETHERITE).withPropertiesOf(helper.getBlockState(pos)), 3);
        TierHopperBlockEntity after = helper.getBlockEntity(pos, TierHopperBlockEntity.class);
        helper.assertTrue(after == before, "the block entity was replaced");
        helper.assertTrue(after.tier() == HopperTier.NETHERITE, "tier after the change: " + after.tier());
        helper.assertTrue(after.getItem(0).is(Items.EMERALD) && after.getItem(0).getCount() == 4 && after.getFilterMode() == HopperFilterMode.WHITELIST,
                "contents or filter lost: " + after.getItem(0) + " " + after.getFilterMode());
        helper.assertTrue(helper.getBlockState(pos).getValue(HopperBlock.FACING) == net.minecraft.core.Direction.EAST, "facing lost");
        after.clearContent();
        helper.succeed();
    }

    /** Under a full chest each tier cart takes in 2/4/8 times what Vanilla's hopper cart takes in the same ticks. */
    public static void hopperCartsTakeInAtTheHopperSpeed(GameTestHelper helper) {
        List<AbstractMinecartContainer> carts = new ArrayList<>();
        for (int lane = 0; lane < 4; lane++) {
            BlockPos rail = new BlockPos(1 + lane * 2, 1, 2);
            helper.setBlock(rail.below(), Blocks.STONE);
            helper.setBlock(rail, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
            helper.setBlock(rail.above(), Blocks.CHEST);
            ChestBlockEntity chest = helper.getBlockEntity(rail.above(), ChestBlockEntity.class);
            for (int slot = 0; slot < 27; slot++) chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            if (lane == 0) {
                MinecartHopper vanilla = helper.spawn(net.minecraft.world.entity.EntityTypes.HOPPER_MINECART, rail);
                vanilla.setEnabled(false);
                carts.add(vanilla);
            } else {
                TierHopperMinecart cart = helper.spawn(Hoppers.CARTS.get(HopperTier.values()[lane - 1]), rail);
                cart.setEnabled(false);
                carts.add(cart);
            }
        }
        helper.startSequence().thenIdle(2).thenExecute(() -> carts.forEach(c -> setEnabled(c, true)))
                .thenIdle(3).thenExecute(() -> {
                    carts.forEach(c -> setEnabled(c, false));
                    int[] taken = new int[4];
                    for (int lane = 0; lane < 4; lane++) {
                        for (int slot = 0; slot < 5; slot++) taken[lane] += carts.get(lane).getItem(slot).getCount();
                    }
                    helper.assertTrue(taken[0] > 0, "Vanilla's hopper cart took nothing");
                    for (int i = 0; i < 3; i++) {
                        helper.assertValueEqual(taken[i + 1], taken[0] * ITEMS_PER_TICK[i],
                                carts.get(i + 1).getType().toShortString() + " items taken while Vanilla took " + taken[0]);
                    }
                    carts.forEach(c -> {
                        c.clearContent();
                        c.discard();
                    });
                }).thenSucceed();
    }

    private static void setEnabled(Entity cart, boolean enabled) {
        if (cart instanceof MinecartHopper vanilla) vanilla.setEnabled(enabled);
        if (cart instanceof TierHopperMinecart tiered) tiered.setEnabled(enabled);
    }

    /**
     * Filter principle on the cart: with Exact Match on, only the slot holding cobblestone fills - from a chest of cobblestone and
     * dirt - no dirt comes in, and a hopper below may take everything but the last cobblestone. The cart's menu is the hopper menu.
     */
    public static void hopperCartFiltersLikeTheHopperBlock(GameTestHelper helper) {
        BlockPos rail = new BlockPos(2, 2, 2);
        helper.setBlock(rail.below(), Blocks.STONE);
        helper.setBlock(rail, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        helper.setBlock(rail.above(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(rail.above(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.DIRT, 64));
        chest.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
        TierHopperMinecart cart = helper.spawn(Hoppers.CARTS.get(HopperTier.NETHERITE), rail);
        cart.setEnabled(false);
        cart.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        cart.toggleFilterMode();
        helper.assertTrue(cart.getFilterMode() == HopperFilterMode.WHITELIST, "the filter key did not reach Exact Match");
        helper.assertTrue(!cart.canPlaceItem(1, new ItemStack(Items.DIRT)), "an empty slot takes dirt with the filter on");
        helper.assertTrue(!cart.canTakeItem(chest, 0, cart.getItem(0)), "a hopper could take the last (filter) item");
        helper.assertTrue(cart.mayPlayerPlace(1, new ItemStack(Items.DIRT)) && !cart.mayPlayerPlace(0, new ItemStack(Items.DIRT)),
                "a player may set a filter in an empty slot but not put dirt onto the cobblestone filter");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        var menu = cart.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof TierHopperMenu hopperMenu && hopperMenu.tier() == HopperTier.NETHERITE
                && hopperMenu.filterHopper() == cart, "the cart opens " + menu + " instead of its hopper menu");
        helper.startSequence().thenExecute(() -> cart.setEnabled(true)).thenIdle(10).thenExecute(() -> {
            cart.setEnabled(false);
            helper.assertTrue(cart.getItem(0).is(Items.COBBLESTONE) && cart.getItem(0).getCount() == 17,
                    "the filter slot holds " + cart.getItem(0) + " instead of all 17 cobblestone");
            for (int slot = 1; slot < 5; slot++) {
                helper.assertTrue(cart.getItem(slot).isEmpty(), "slot " + slot + " took " + cart.getItem(slot));
            }
            helper.assertValueEqual(chest.getItem(0).getCount(), 64, "dirt left in the chest");
            helper.assertTrue(cart.canTakeItem(chest, 0, cart.getItem(0)), "hoppers may take the cobblestone above the filter item");
            cart.clearContent();
            cart.discard();
        }).thenSucceed();
    }

    /** Crafting: minecart + tier hopper gives the tier cart; smithing Reinforced -> Netherite; a broken cart drops its item and contents. */
    public static void hopperCartRecipesAndDrop(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (HopperTier tier : HopperTier.values()) {
            CraftingInput grid = CraftingInput.of(2, 1, List.of(new ItemStack(Items.MINECART), new ItemStack(Hoppers.item(tier))));
            ItemStack got = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                    .map(m -> m.value().assemble(grid)).orElse(ItemStack.EMPTY);
            if (!got.is(Hoppers.cartItem(tier))) wrong.add(tier.cartPath() + " got " + got);
        }
        var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Hoppers.cartItem(HopperTier.REINFORCED)), new ItemStack(Items.NETHERITE_INGOT));
        ItemStack netherite = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
                .map(m -> m.value().assemble(input)).orElse(ItemStack.EMPTY);
        if (!netherite.is(Hoppers.cartItem(HopperTier.NETHERITE))) wrong.add("smithing reinforced hopper minecart gave " + netherite);
        helper.assertTrue(wrong.isEmpty(), "hopper cart recipes: " + wrong);

        var level = helper.getLevel();
        TierHopperMinecart cart = helper.spawn(Hoppers.CARTS.get(HopperTier.REINFORCED), new BlockPos(4, 2, 4));
        cart.setItem(0, new ItemStack(Items.EMERALD, 3));
        cart.hurtServer(level, level.damageSources().generic(), 100.0F);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, cart.getBoundingBox().inflate(2.0));
        Item own = Hoppers.cartItem(HopperTier.REINFORCED);
        helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(own)), "the cart dropped " + drops.stream().map(d -> d.getItem().toString()).toList());
        helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(Items.EMERALD)), "the cart lost its contents");
        drops.forEach(Entity::discard);
        helper.succeed();
    }

    /** The item tooltip names the speed (from the config) and the filter, like SimpleBuilding's did. */
    public static void hopperTooltipsNameSpeedAndFilter(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        String[] speed = {"Moves an item every 4 ticks", "Moves an item every 2 ticks", "Moves an item every tick"};
        for (HopperTier tier : HopperTier.values()) {
            List<String> lines = new ArrayList<>();
            ItemStack stack = new ItemStack(Hoppers.item(tier));
            stack.getItem().appendHoverText(stack, net.minecraft.world.item.Item.TooltipContext.of(helper.getLevel()),
                    net.minecraft.world.item.component.TooltipDisplay.DEFAULT, c -> lines.add(c.getString()), net.minecraft.world.item.TooltipFlag.NORMAL);
            if (!lines.contains(speed[tier.ordinal()]) || !lines.contains("Filter in its menu: exact or type match")) problems.add(tier + ": " + lines);
        }
        helper.assertTrue(problems.isEmpty(), "hopper tooltips: " + problems);
        helper.succeed();
    }
}

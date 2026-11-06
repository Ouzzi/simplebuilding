package com.simplestorage.test;

import com.simplelib.api.BarrelDock;
import com.simplelib.api.IdMoves;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import com.simplestorage.SimpleStorage;
import com.simplestorage.barrel.BarrelBlock;
import com.simplestorage.barrel.BarrelBlockEntity;
import com.simplestorage.barrel.BarrelMenu;
import com.simplestorage.barrel.BarrelTier;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
import com.simplestorage.registry.StorageRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * WH26-6 (owner 06.11.: "Fässer gehören in Simple Storage"): the barrels moved here from Simple Crucibles. Registered with their
 * tags and recipe, old {@code simplecrucibles:} / {@code simplelib:} / {@code simplebuilding:} worlds migrate (blocks, items,
 * block entity, recipe, unlock advancement), a loose barrel behaves like Vanilla's, the axe upgrade keeps the contents. The
 * docking onto a crucible (Simple Crucibles) is tested there; here only the contract that works without a host. Every test
 * runs standalone and with SimpleBuilding.
 */
final class BarrelTests {
    private BarrelTests() {}

    private static BarrelBlock barrel(BarrelTier tier) {
        return StorageBlocks.BARRELS.get(tier);
    }

    static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (BarrelTier tier : BarrelTier.values()) {
            String path = tier.id() + "_barrel";
            if (!SimpleStorage.BARRELS.contains(path)) problems.add("not in SimpleStorage.BARRELS: " + path);
            if (BuiltInRegistries.BLOCK.getValue(SimpleStorage.id(path)) != barrel(tier)) problems.add("block " + path);
            if (BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path)) != StorageItems.BARRELS.get(tier)) problems.add("item " + path);
            if (StorageRegistry.barrelStacks().stream().noneMatch(s -> s.is(StorageItems.BARRELS.get(tier)))) problems.add("not in the tab " + path);
            if (StorageBlocks.BARRELS.get(tier).tier() != tier) problems.add("tier of " + path);
            if (StorageMenus.forBarrel(tier) == null) problems.add("menu type " + path);
        }
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleStorage.id("barrel")) != StorageBlockEntities.BARREL) problems.add("block entity type");
        if (SimpleStorage.BARRELS.size() != 4) problems.add("four barrels, got " + SimpleStorage.BARRELS);
        int[] rows = {3, 4, 5, 6};
        int[] stacks = {1, 1, 1, 2};
        for (BarrelTier tier : BarrelTier.values()) {
            if (tier.rows() != rows[tier.ordinal()] || tier.stackMultiplier() != stacks[tier.ordinal()]) problems.add("shape of " + tier);
            Block block = barrel(tier);
            BlockState state = block.defaultBlockState();
            if (!state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) problems.add("not pickaxe " + tier);
            if (!state.is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "barrels")))) problems.add("not in c:barrels " + tier);
            if (state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL) != (tier == BarrelTier.ENDERITE)) problems.add("diamond tool " + tier);
            if (!(block instanceof BarrelDock.Dockable dock) || dock.barrelStackMultiplier() != stacks[tier.ordinal()]) problems.add("dockable " + tier);
        }
        h.assertTrue(problems.isEmpty(), "barrels: " + problems);
        h.succeed();
    }

    /**
     * Old worlds: the ids of Simple Crucibles, SimpleLib (copper, reinforced, netherite) and SimpleBuilding (Enderite) load as the
     * module's - registry lookups, item stacks, block state palettes, the block entity (with contents) - and are written back under the
     * new id; the copper barrel's recipe and unlock advancement move too.
     */
    static void migration(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (String ns : SimpleStorage.BARREL_OLD_NAMESPACES) {
            for (String path : SimpleStorage.BARRELS) {
                boolean existed = switch (ns) {
                    case "simplelib" -> !path.equals("enderite_barrel");
                    case "simplebuilding" -> path.equals("enderite_barrel");
                    default -> true;
                };
                if (!existed) continue;
                Identifier from = Identifier.fromNamespaceAndPath(ns, path);
                Identifier to = SimpleStorage.id(path);
                Block block = BuiltInRegistries.BLOCK.getValue(to);
                if (BuiltInRegistries.BLOCK.getValue(from) != block) problems.add("BLOCK.getValue(" + from + ")");
                Item item = BuiltInRegistries.ITEM.getValue(to);
                if (BuiltInRegistries.ITEM.getValue(from) != item) problems.add("ITEM.getValue(" + from + ")");
                CompoundTag saved = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(item, 3)).getOrThrow();
                saved.putString("id", from.toString());
                ItemStack loaded = ItemStack.CODEC.parse(ops, saved).result().orElse(ItemStack.EMPTY);
                if (loaded.getItem() != item || loaded.getCount() != 3) problems.add("stack " + saved + " loads as " + loaded);
                var resaved = ItemStack.CODEC.encodeStart(ops, loaded).result().orElse(null);
                if (resaved == null || resaved.toString().contains(from.toString())) problems.add("stack saved back as " + resaved);
                CompoundTag state = NbtUtils.writeBlockState(block.defaultBlockState());
                state.remove("Name");
                state.putString("id", from.toString());
                BlockState decoded = BlockState.CODEC.parse(NbtOps.INSTANCE, state).result().orElse(null);
                if (decoded == null || !decoded.is(block)) problems.add("chunk codec loads " + state + " as " + decoded);
            }
        }
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        BlockState copper = barrel(BarrelTier.COPPER).defaultBlockState();
        BarrelBlockEntity original = new BarrelBlockEntity(pos, copper);
        original.setItem(20, new ItemStack(Items.APPLE, 3));
        for (String oldId : List.of("simplelib:barrel", "simplecrucibles:barrel")) {
            CompoundTag beTag = original.saveWithFullMetadata(h.getLevel().registryAccess());
            beTag.putString("id", oldId);
            BlockEntity be = BlockEntity.loadStatic(pos, copper, beTag, h.getLevel().registryAccess());
            if (!(be instanceof BarrelBlockEntity loadedBe) || !loadedBe.getItem(20).is(Items.APPLE) || loadedBe.getItem(20).getCount() != 3) {
                problems.add("barrel entity " + oldId + " loads as " + be);
            } else if (!be.saveWithFullMetadata(h.getLevel().registryAccess()).getStringOr("id", "").equals("simplestorage:barrel")) {
                problems.add("barrel entity saved back wrongly");
            }
        }
        for (String ns : List.of("simplelib", "simplecrucibles")) {
            Identifier oldRecipe = Identifier.fromNamespaceAndPath(ns, "copper_barrel");
            if (!SimpleStorage.id("copper_barrel").equals(IdMoves.target(Registries.RECIPE, oldRecipe))) problems.add("recipe move from " + ns);
            Identifier oldAdv = Identifier.fromNamespaceAndPath(ns, "recipes/misc/copper_barrel");
            if (h.getLevel().getServer().getAdvancements().get(oldAdv) != h.getLevel().getServer().getAdvancements().get(SimpleStorage.id("recipes/misc/copper_barrel"))) {
                problems.add("the recipe advancement does not resolve under " + oldAdv);
            }
        }
        if (h.getLevel().getServer().getAdvancements().get(SimpleStorage.id("recipes/misc/copper_barrel")) == null) problems.add("no recipe advancement for the copper barrel");
        h.assertTrue(problems.isEmpty(), "barrel migration: " + problems);
        h.succeed();
    }

    /** The copper barrel: copper ingots around a Vanilla barrel; no other barrel has a recipe (they are upgrades). */
    static void recipe(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        var manager = h.getLevel().getServer().getRecipeManager();
        if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id("copper_barrel"))).isEmpty()) problems.add("recipe simplestorage:copper_barrel missing");
        for (String old : List.of("simplelib", "simplecrucibles")) {
            if (manager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(old, "copper_barrel"))).isPresent()) {
                // an old id resolves through IdMoves, so this finds the same recipe; it must not be a second one
                var viaOld = manager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(old, "copper_barrel"))).get();
                var viaNew = manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id("copper_barrel"))).get();
                if (viaOld != viaNew) problems.add("a second recipe under " + old);
            }
        }
        ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
        ItemStack vanilla = new ItemStack(Items.BARREL);
        List<ItemStack> grid = List.of(ItemStack.EMPTY, copperIngot, ItemStack.EMPTY, copperIngot, vanilla, copperIngot, ItemStack.EMPTY, copperIngot, ItemStack.EMPTY);
        var found = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid), h.getLevel());
        ItemStack result = found.map(r -> r.value().assemble(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid))).orElse(ItemStack.EMPTY);
        if (!result.is(StorageItems.BARRELS.get(BarrelTier.COPPER))) problems.add("four copper ingots around a barrel craft " + result);
        h.assertTrue(problems.isEmpty(), "barrel recipe: " + problems);
        h.succeed();
    }

    /** Placing: every tier opens as a chest of its size, with the raised limit (Enderite 128), comparator signal, hopper-ready container. */
    static void placement(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        BlockPos rel = new BlockPos(2, 2, 2);
        int[] sizes = {27, 36, 45, 54};
        for (BarrelTier tier : BarrelTier.values()) {
            h.setBlock(rel, barrel(tier));
            BlockEntity be = h.getLevel().getBlockEntity(h.absolutePos(rel));
            if (!(be instanceof BarrelBlockEntity barrel)) {
                problems.add("no block entity for " + tier);
                continue;
            }
            if (barrel.getContainerSize() != sizes[tier.ordinal()]) problems.add(tier + " size " + barrel.getContainerSize());
            if (barrel.tier() != tier) problems.add("tier " + tier);
            ItemStack cobble = new ItemStack(Items.COBBLESTONE);
            int expected = tier == BarrelTier.ENDERITE ? 128 : 64;
            if (barrel.getMaxStackSize(cobble) != expected) problems.add(tier + " limit " + barrel.getMaxStackSize(cobble));
            if (barrel.attached()) problems.add(tier + " attached by itself");
            barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            BlockState state = h.getLevel().getBlockState(h.absolutePos(rel));
            if (net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(barrel) < 1) problems.add(tier + " comparator signal");
            if (BarrelDock.hostSide(h.getLevel(), h.absolutePos(rel), state) != null) problems.add(tier + " found a host without a crucible");
        }
        h.assertTrue(problems.isEmpty(), "barrel placement: " + problems);
        h.succeed();
    }

    /**
     * DW03C-4: a loose barrel turns its lid to all six sides like Vanilla's barrel ({@code top}, default up - old worlds without it
     * keep standing), opens while a player looks in and closes after, and is a {@code c:barrels} barrel (the chest interface mounts on it).
     */
    static void looseBarrelFacesLikeVanillaAndOpens(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Block copperBlock = barrel(BarrelTier.COPPER);
        BlockState plain = copperBlock.defaultBlockState();
        if (plain.getValue(BarrelBlock.TOP) != Direction.UP) problems.add("default top " + plain);
        if (!plain.is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "barrels")))) problems.add("not in c:barrels");
        for (Direction d : Direction.values()) {
            if (plain.setValue(BarrelBlock.TOP, d).getValue(BarrelBlock.TOP) != d) problems.add("top " + d);
        }
        BlockState turned = plain.setValue(BarrelBlock.TOP, Direction.NORTH).rotate(Rotation.CLOCKWISE_90);
        if (turned.getValue(BarrelBlock.TOP) != Direction.EAST) problems.add("rotated top " + turned);
        // an old world's state (facing, attached, open - no top) loads standing
        CompoundTag tag = NbtUtils.writeBlockState(plain.setValue(BarrelBlock.FACING, Direction.EAST).setValue(BarrelBlock.TOP, Direction.NORTH));
        boolean removed = false;
        for (String key : tag.keySet()) {
            if (tag.get(key) instanceof CompoundTag properties && properties.contains("top")) {
                properties.remove("top");
                removed = true;
            }
        }
        if (!removed) problems.add("no top in the written state " + tag);
        BlockState old = NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), tag);
        if (old.getBlock() != copperBlock || old.getValue(BarrelBlock.TOP) != Direction.UP || old.getValue(BarrelBlock.FACING) != Direction.EAST) problems.add("old state " + old);
        // open while a player looks in, closed after
        BlockPos rel = new BlockPos(2, 2, 2);
        h.setBlock(rel, plain.setValue(BarrelBlock.TOP, Direction.SOUTH));
        BarrelBlockEntity be = h.getBlockEntity(rel, BarrelBlockEntity.class);
        var player = h.makeMockServerPlayerInLevel();
        be.startOpen(player);
        if (!h.getBlockState(rel).getValue(BarrelBlock.OPEN)) problems.add("not open while looked into");
        be.stopOpen(player);
        if (h.getBlockState(rel).getValue(BarrelBlock.OPEN)) problems.add("still open after closing");
        if (h.getBlockState(rel).getValue(BarrelBlock.TOP) != Direction.SOUTH) problems.add("opening turned the lid");
        h.assertTrue(problems.isEmpty(), "loose barrel: " + problems);
        h.succeed();
    }

    /**
     * Owner N15: a loose barrel opens a chest menu whose slots keep the barrel's limit on both sides - the Enderite mirror takes 128,
     * a shift-click of two full stacks fills one slot, picking up gives a normal stack, and a copper barrel stays at 64.
     */
    static void looseBarrelMenu(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        BarrelTier tier = BarrelTier.ENDERITE;
        var client = BarrelMenu.client(tier, 0, player.getInventory());
        h.assertTrue(client.slots.get(0).getMaxStackSize(cobble) == 128 && client.getRowCount() == 6,
                "enderite barrel on the client: 6 rows of 128, got " + client.getRowCount() + " rows of " + client.slots.get(0).getMaxStackSize(cobble));
        var barrel = com.simplelib.api.StackLimits.mirror(tier.slots(), tier::stackMultiplier);
        var menu = new BarrelMenu(tier, 0, player.getInventory(), barrel);
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        int hotbar = tier.slots() + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        h.assertTrue(barrel.getItem(0).getCount() == 128 && barrel.getItem(1).isEmpty(),
                "shift-click fills one enderite slot to 128, got " + barrel.getItem(0).getCount() + "+" + barrel.getItem(1).getCount());
        var taken = menu.slots.get(0).tryRemove(128, Integer.MAX_VALUE, player);
        h.assertTrue(taken.isPresent() && taken.get().getCount() == 64 && barrel.getItem(0).getCount() == 64,
                "picking up takes one normal stack, got " + taken.map(ItemStack::getCount).orElse(0));
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, barrel(BarrelTier.COPPER));
        var copper = (BarrelBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        var copperMenu = new BarrelMenu(copper.tier(), 0, player.getInventory(), copper);
        h.assertTrue(copperMenu.slots.get(0).getMaxStackSize(cobble) == 64 && copperMenu.getRowCount() == 3,
                "copper barrel: 3 rows of 64, got " + copperMenu.getRowCount() + " rows of " + copperMenu.slots.get(0).getMaxStackSize(cobble));
        h.succeed();
    }

    /**
     * The barrel line is data-driven (in_world_upgrades/barrels.json): copper -> reinforced -> netherite -> enderite in SimpleLib's table,
     * the handler keeps contents and lid, an axe with the material in the off hand takes the click (while the axe ways are on).
     */
    static void upgradeKeepsContents(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Item cracked = SimpleLibApi.materialItem("cracked_diamond");
        Item netherite = SimpleLibApi.materialItem("netherite_nugget");
        boolean ores = SimpleLibApi.materialPresent("enderite");
        Item enderite = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleores", "enderite_nugget"));
        Object[][] steps = {{BarrelTier.COPPER, BarrelTier.REINFORCED, cracked}, {BarrelTier.REINFORCED, BarrelTier.NETHERITE, netherite},
                {BarrelTier.NETHERITE, BarrelTier.ENDERITE, enderite}};
        for (Object[] step : steps) {
            var entry = InWorldUpgrades.entryFor(barrel((BarrelTier) step[0]));
            if (step[2] == enderite && !ores) {
                if (entry != null) problems.add("Enderite step without Simple Ores: " + entry);
                continue;
            }
            if (entry == null || entry.to() != barrel((BarrelTier) step[1]) || entry.material() != step[2] || entry.materialCount() != 2) {
                problems.add("step from " + step[0] + ": " + entry);
            }
        }
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos abs = h.absolutePos(rel);
        h.setBlock(rel, barrel(BarrelTier.REINFORCED).defaultBlockState().setValue(BarrelBlock.TOP, Direction.EAST));
        ((net.minecraft.world.Container) h.getLevel().getBlockEntity(abs)).setItem(30, new ItemStack(Items.COAL, 7));
        if (!InWorldUpgrades.hasHandler(barrel(BarrelTier.NETHERITE))) problems.add("no barrel handler registered");
        if (InWorldUpgrades.replace(h.getLevel(), abs, barrel(BarrelTier.NETHERITE)) != 1) problems.add("blocks changed");
        BlockState after = h.getLevel().getBlockState(abs);
        if (!after.is(barrel(BarrelTier.NETHERITE)) || after.getValue(BarrelBlock.TOP) != Direction.EAST) problems.add("upgraded state " + after);
        if (!(h.getLevel().getBlockEntity(abs) instanceof BarrelBlockEntity fresh) || fresh.getContainerSize() != 45
                || !fresh.getItem(30).is(Items.COAL) || fresh.getItem(30).getCount() != 7) problems.add("45 slots, contents kept");
        // the axe way (principle 5a): an axe + two cracked diamonds on a copper barrel takes the click exactly when the axe ways are on
        h.setBlock(rel, barrel(BarrelTier.COPPER));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(cracked, 2));
        BlockState copper = h.getLevel().getBlockState(abs);
        if (InWorldUpgrades.axeWants(copper, player, InteractionHand.MAIN_HAND) != SimpleLibApi.axeWaysEnabled()) problems.add("axe click on the copper barrel");
        if (SimpleLibApi.axeWaysEnabled() && !InWorldUpgrades.axeStrike(h.getLevel(), abs, player)) problems.add("the first axe strike was refused");
        h.assertTrue(problems.isEmpty(), "barrel upgrade: " + problems);
        h.succeed();
    }

    /** The attached barrel's outline (the body element turned to the docking side) and the dock contract without a host. */
    static void dockContract(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        BlockState north = barrel(BarrelTier.COPPER).defaultBlockState().setValue(BarrelBlock.ATTACHED, true).setValue(BarrelBlock.FACING, Direction.NORTH);
        var nb = north.getShape(level, pos).bounds();
        if (!(nb.minZ == 0 && nb.maxZ == 13 / 16.0 && nb.maxY == 11 / 16.0)) problems.add("attached barrel north box: " + nb);
        var loose = barrel(BarrelTier.COPPER).defaultBlockState().getShape(level, pos).bounds();
        if (!(loose.maxY == 1.0 && loose.maxZ == 1.0)) problems.add("loose barrel is a full cube: " + loose);
        if (BarrelBlock.ATTACHED != BarrelDock.ATTACHED || BarrelBlock.FACING != BarrelDock.FACING) problems.add("the block uses SimpleLib's dock properties");
        if (BarrelDock.ATTACH_STRIKES != 6 || BarrelDock.MAX_HOST_SLOTS != 30) problems.add("dock constants");
        if (!BarrelDock.isBarrel(north) || !BarrelDock.isAttached(north) || BarrelDock.isAttached(barrel(BarrelTier.COPPER).defaultBlockState())) problems.add("isBarrel/isAttached");
        if (BarrelDock.isHost(north) || BarrelDock.isBarrel(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())) problems.add("stone or a barrel is no host");
        h.assertTrue(problems.isEmpty(), "dock contract: " + problems);
        h.succeed();
    }
}

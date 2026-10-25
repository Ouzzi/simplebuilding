package com.simplecrucibles.test;

import com.google.gson.JsonObject;
import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.config.CrucibleConfig;
import com.simplecrucibles.crucible.CrucibleBlankBlock;
import com.simplecrucibles.crucible.CrucibleBlock;
import com.simplecrucibles.crucible.CrucibleBlockEntity;
import com.simplecrucibles.crucible.CrucibleTier;
import com.simplecrucibles.crucible.Heat;
import com.simplecrucibles.crucible.HeatLevel;
import com.simplecrucibles.registry.CrucibleBlockEntities;
import com.simplecrucibles.registry.CrucibleBlocks;
import com.simplelib.registry.LibComponents;
import com.simplelib.api.SimpleLibApi;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GameTests of Simple Crucibles (split stage 5; they were SimpleLib's). One body per id; the Fabric adapter ({@code ModuleGameTest}) and the
 * NeoForge/Forge registrations use the same names ({@code module_game_test_<name>}). Crucible ticks
 * are driven directly (no waiting), so every test finishes in its first tick.
 */
public final class CrucibleTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    /** Longer tick budgets (default 100). */
    public static final Map<String, Integer> MAX_TICKS = new java.util.HashMap<>();

    static {
        ALL.put("config_bounds", CrucibleTests::configBounds);
        // CMD24: a nested group (soulLava.*) through /simplecrucibles config, clamp and the restart hint.
        ALL.put("config_command", h -> {
            com.simplelib.test.ConfigCommandTests.roundTrip(h, SimpleCrucibles.MOD_ID, "soulLava.burnSeconds", "20", "20", "1.5");
            var spec = com.simplelib.api.config.ConfigCommands.spec(SimpleCrucibles.MOD_ID, false);
            String before = com.simplelib.api.config.ConfigCommands.show(spec.value("soulLava.burnSeconds"));
            var r = spec.set("soulLava.burnSeconds", "999");
            spec.set("soulLava.burnSeconds", before);
            h.assertTrue(r.status() == com.simplelib.api.config.ConfigCommands.Status.CLAMPED && r.value().equals("60"), "clamp " + r);
            h.assertTrue(spec.restart("soulLava.fuelMultiplier") && !spec.restart("soulLava.burnSeconds"), "restart hint");
            h.succeed();
        });
        ALL.put("config_adopts_simplelib_options", CrucibleTests::configAdoptsSimpleLibOptions);
        ALL.put("broken_config_is_quarantined", CrucibleTests::brokenConfigIsQuarantined);
        ALL.put("registered", CrucibleTests::registered);
        ALL.put("migration_from_simplelib_and_simplebuilding", CrucibleTests::migration);
        ALL.put("crucible_upgrade_handler", CrucibleTests::upgradeHandler);
        ALL.put("creative_tab_routing", CrucibleTests::creativeTabRouting);
        ALL.put("tier_layout", CrucibleTests::tierLayout);
        ALL.put("heat_sources", CrucibleTests::heatSources);
        ALL.put("heat_two_below", CrucibleTests::heatTwoBelow);
        ALL.put("cooks_into_slot_below", CrucibleTests::cooksIntoSlotBelow);
        ALL.put("medium_heat_blocks_smelting", CrucibleTests::mediumHeatBlocksSmelting);
        ALL.put("reserved_slot_blocks_and_keeps_progress", CrucibleTests::reservedSlotBlocks);
        ALL.put("result_not_recooked", CrucibleTests::resultNotRecooked);
        ALL.put("warms_food_and_eats_faster", CrucibleTests::warmsFood);
        ALL.put("afterglow_keeps_heat", CrucibleTests::afterglow);
        ALL.put("hopper_rules", CrucibleTests::hopperRules);
        ALL.put("crucible_hitboxes_match_model", CrucibleTests::crucibleHitboxes);
        ALL.put("axe_builds_iron_crucible", CrucibleTests::axeBuild);
        ALL.put("break_drops_contents", CrucibleTests::breakDrops);
        ALL.put("load_keeps_readable_slots_around_an_unreadable_one", CrucibleTests::loadKeepsReadableSlots);
        ALL.put("axe_upgrade_keeps_contents", CrucibleTests::axeUpgrade);
        ALL.put("village_kitchen_in_pools", CrucibleTests::villageKitchen);
        ALL.put("barrel_attach_and_results_first", CrucibleTests::barrelAttach);
        ALL.put("barrel_attached_later_takes_over_reservations", CrucibleTests::barrelAttachedLater);
        ALL.put("client_menu_shows_the_raised_stack_limit", CrucibleTests::clientMenuStackLimit);
        ALL.put("axe_click_reaches_axe_not_menu", CrucibleTests::axeClickReachesAxe);
        ALL.put("axe_upgrades_cauldron", CrucibleTests::axeUpgradesCauldron);
        ALL.put("barrel_strike_counters_are_separate", CrucibleTests::barrelStrikeCountersAreSeparate);
        ALL.put("reinforced_cauldron_holds_buckets", CrucibleTests::reinforcedCauldronBuckets);
        ALL.put("reinforced_cauldron_inherits_vanilla", CrucibleTests::reinforcedCauldronInheritsVanilla);
        ALL.put("axe_upgrades_barrel_to_netherite", CrucibleTests::axeUpgradesBarrelToNetherite);
        ALL.put("crucible_burn_damage_follows_config", CrucibleTests::crucibleBurnDamageFollowsConfig);
        ALL.put("loose_barrel_menu_keeps_the_raised_limit", CrucibleTests::looseBarrelMenu);
        ALL.put("reinforced_crucible_loads_old_nine_slots", CrucibleTests::reinforcedCrucibleLoadsOldNineSlots);
        ALL.put("crucible_barrel_beside_inventory_centred", CrucibleTests::barrelBesideInventoryCentred);
        ALL.put("hammer_upgrade_steps_ship_with_the_module", CrucibleTests::hammerUpgradeStepsShipWithTheModule);
        // Split stage 5b: soul lava, the buckets and the JEI/Jade data (were SimpleBuilding's tests).
        BucketTests.register(ALL, MAX_TICKS);
    }

    // ------------------------------------------------------------ helpers

    private static CrucibleBlockEntity crucible(GameTestHelper h, BlockPos rel, BlockState below, CrucibleBlock block) {
        h.setBlock(rel.below(), below);
        h.setBlock(rel, block.defaultBlockState());
        CrucibleBlockEntity be = (CrucibleBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        h.assertTrue(be != null, "no crucible block entity");
        return be;
    }

    private static void run(GameTestHelper h, CrucibleBlockEntity be, int ticks) {
        ServerLevel level = h.getLevel();
        for (int i = 0; i < ticks; i++) {
            CrucibleBlockEntity.serverTick(level, be.getBlockPos(), level.getBlockState(be.getBlockPos()), be);
        }
    }

    private static String debug(CrucibleBlockEntity be) {
        StringBuilder sb = new StringBuilder("[heat=" + be.heat() + " mul=" + be.heatMultiplier());
        for (int i = 0; i < be.getContainerSize(); i++) {
            sb.append(" | ").append(i).append(':').append(be.getItem(i)).append(" st=").append(be.slotState(i))
                    .append(" p=").append(be.percent(i)).append(" t=").append(be.target(i)).append(be.isResult(i) ? " R" : "");
        }
        return sb.append(']').toString();
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    // ------------------------------------------------------------ tests

    // ------------------------------------------------------------ split stage 5 (module level)

    /** Every crucible option is clamped to its hard bounds (they were SimpleLib's options before stage 5). */
    private static void configBounds(GameTestHelper h) {
        JsonObject json = new JsonObject();
        json.addProperty("reinforcedSpeed", 999);
        json.addProperty("factorMedium", -3.0);
        json.addProperty("afterglowEnderite", 10_000);
        json.addProperty("crucibleBurnDamage", 50.0);
        json.addProperty("villageKitchenWeight", 99);
        CrucibleConfig.apply(json);
        check(h, CrucibleConfig.reinforcedSpeed == CrucibleConfig.MAX_SPEED, "speed not clamped");
        check(h, CrucibleConfig.factorMedium == CrucibleConfig.FACTOR_MIN, "factor not clamped");
        check(h, CrucibleConfig.afterglowSeconds[3] == CrucibleConfig.AFTERGLOW_MAX_SECONDS, "afterglow not clamped");
        check(h, CrucibleConfig.crucibleBurnDamage == CrucibleConfig.BURN_DAMAGE_MAX, "crucible burn damage not clamped");
        check(h, CrucibleConfig.villageKitchenWeight == CrucibleConfig.VILLAGE_WEIGHT_MAX, "kitchen weight not clamped");
        CrucibleConfig.reset();
        check(h, CrucibleConfig.factorHigh == 0.75 && CrucibleConfig.afterglowSeconds[0] == 2, "defaults wrong");
        check(h, CrucibleConfig.crucibleBurnDamage == 1.0, "crucible burn damage default is not magma's 1");
        h.succeed();
    }

    /** First start without an own file: the crucible options of SimpleLib's old file come over once, the old file stays, an own file wins. */
    private static void configAdoptsSimpleLibOptions(GameTestHelper h) {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplecrucibles-adopt");
            String oldText = "{ \"reinforcedSpeed\": 3, \"crucibleBurnDamage\": 2.5, \"villageKitchenWeight\": 0, \"warmBaseTicks\": 300 }";
            java.nio.file.Files.writeString(dir.resolve(CrucibleConfig.OLD_FILE), oldText);
            CrucibleConfig.load(dir);
            check(h, CrucibleConfig.reinforcedSpeed == 3 && CrucibleConfig.crucibleBurnDamage == 2.5 && CrucibleConfig.villageKitchenWeight == 0,
                    "options not taken over: " + CrucibleConfig.reinforcedSpeed + " " + CrucibleConfig.crucibleBurnDamage);
            var written = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(dir.resolve(CrucibleConfig.FILE))).getAsJsonObject();
            check(h, written.get("reinforcedSpeed").getAsInt() == 3 && !written.has("warmBaseTicks"), "own file wrong (warm options are SimpleLib's): " + written);
            check(h, java.nio.file.Files.readString(dir.resolve(CrucibleConfig.OLD_FILE)).equals(oldText), "the old file was changed");
            java.nio.file.Files.writeString(dir.resolve(CrucibleConfig.OLD_FILE), "{ \"reinforcedSpeed\": 5 }");
            CrucibleConfig.load(dir);
            check(h, CrucibleConfig.reinforcedSpeed == 3, "an existing own file did not win");
        } catch (java.io.IOException | RuntimeException e) {
            h.fail("config takeover threw " + e);
            return;
        } finally {
            CrucibleConfig.reset();
        }
        h.succeed();
    }

    /** Shared policy (framework ConfigFiles): a broken server config never crashes, it is moved to *.broken-<time> and defaults are written. */
    private static void brokenConfigIsQuarantined(GameTestHelper h) {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplecrucibles-broken");
            java.nio.file.Path file = dir.resolve(CrucibleConfig.FILE);
            java.nio.file.Files.writeString(file, "{ \"reinforcedSpeed\": 3,, }");
            CrucibleConfig.load(dir);
            check(h, CrucibleConfig.factorHigh == 0.75, "defaults not used after a broken file");
            try (var files = java.nio.file.Files.list(dir)) {
                var names = files.map(x -> x.getFileName().toString()).toList();
                check(h, names.stream().anyMatch(n -> n.startsWith(CrucibleConfig.FILE + ".broken-")), "broken file not kept: " + names);
            }
            com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file));
        } catch (java.io.IOException | RuntimeException e) {
            h.fail("broken config handling threw " + e);
            return;
        }
        CrucibleConfig.reset();
        h.succeed();
    }

    /** Everything the module owns is registered under its namespace, the Enderite stage included. */
    private static void registered(GameTestHelper h) {
        for (String path : SimpleCrucibles.MOVED_FROM_LIB) {
            check(h, net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(SimpleCrucibles.id(path)), "no block " + path);
            check(h, net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(SimpleCrucibles.id(path)), "no item " + path);
        }
        for (String path : SimpleCrucibles.MOVED_FROM_SB) {
            check(h, net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(SimpleCrucibles.id(path)), "no block " + path);
            check(h, net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(SimpleCrucibles.id(path)), "no item " + path);
        }
        check(h, net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(SimpleCrucibles.id("crucible_blank")), "no crucible blank");
        check(h, net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleCrucibles.id("crucible")) == CrucibleBlockEntities.CRUCIBLE, "crucible block entity type");
        check(h, net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleCrucibles.id("barrel")) == CrucibleBlockEntities.BARREL, "barrel block entity type");
        check(h, CrucibleBlocks.ENDERITE_CRUCIBLE.tier() == CrucibleTier.ENDERITE && CrucibleTier.ENDERITE.slots() == 30
                && CrucibleTier.ENDERITE.stackMultiplier() == 2, "the Enderite crucible is not the 30-slot double-stack tier");
        check(h, CrucibleBlocks.CRUCIBLES.contains(CrucibleBlocks.ENDERITE_CRUCIBLE) && CrucibleBlocks.BARRELS.contains(CrucibleBlocks.ENDERITE_BARREL),
                "the Enderite tiers are not in the block lists");
        check(h, CrucibleBlocks.ENDERITE_CRUCIBLE.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                && CrucibleBlocks.ENDERITE_CRUCIBLE.defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL), "Enderite crucible tags (pickaxe, diamond)");
        h.succeed();
    }

    /**
     * Old worlds (split stage 5): the ids of SimpleLib and SimpleBuilding load as the module's - registry lookups, item stacks, block
     * state palettes, block entity ids - and are written back under the new id; recipe and advancement ids move too.
     */
    private static void migration(GameTestHelper h) {
        var problems = new java.util.ArrayList<String>();
        record Move(String from, String to) {}
        java.util.List<Move> moves = new java.util.ArrayList<>();
        for (String path : SimpleCrucibles.MOVED_FROM_LIB) moves.add(new Move("simplelib:" + path, "simplecrucibles:" + path));
        for (String path : SimpleCrucibles.MOVED_FROM_SB) moves.add(new Move("simplebuilding:" + path, "simplecrucibles:" + path));
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        for (Move move : moves) {
            var from = net.minecraft.resources.Identifier.parse(move.from());
            var to = net.minecraft.resources.Identifier.parse(move.to());
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(to);
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(from) != block) problems.add("BLOCK.getValue(" + from + ")");
            if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(from)) problems.add("BLOCK.containsKey(" + from + ")");
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(to);
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(from) != item) problems.add("ITEM.getValue(" + from + ")");
            // item stack as saved by an old world
            net.minecraft.nbt.CompoundTag saved = (net.minecraft.nbt.CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(item, 3)).getOrThrow();
            saved.putString("id", from.toString());
            ItemStack loaded = ItemStack.CODEC.parse(ops, saved).result().orElse(ItemStack.EMPTY);
            if (loaded.getItem() != item || loaded.getCount() != 3) problems.add("stack " + saved + " loads as " + loaded);
            var resaved = ItemStack.CODEC.encodeStart(ops, loaded).result().orElse(null);
            if (resaved == null || resaved.toString().contains(from.toString())) problems.add("stack saved back as " + resaved);
            // block state palette entry of a chunk
            net.minecraft.nbt.CompoundTag state = net.minecraft.nbt.NbtUtils.writeBlockState(block.defaultBlockState());
            // 26.3 palette entries name the block under "id" (BlockState.CODEC, NbtUtils); the old probe set "Name" and never read the old id.
            if (!state.contains("id")) problems.add("palette entry without id: " + state);
            state.remove("Name");
            state.putString("id", from.toString());
            if (!net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), state).is(block))
                problems.add("palette " + state);
            // The chunk section codec decodes the palette through BlockState.CODEC.
            BlockState decoded = BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, state).result().orElse(null);
            if (decoded == null || !decoded.is(block)) problems.add("chunk codec loads " + state + " as " + decoded);
        }
        var blank = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("simplelib:crucible_blank"));
        if (blank != CrucibleBlocks.CRUCIBLE_BLANK) problems.add("the old crucible blank does not resolve");
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        // block entities, saved under the old type id, with contents
        for (String oldId : java.util.List.of("simplelib:crucible")) {
            BlockState crucibleState = CrucibleBlocks.IRON_CRUCIBLE.defaultBlockState();
            CrucibleBlockEntity original = new CrucibleBlockEntity(pos, crucibleState);
            original.setItem(2, new ItemStack(Items.RAW_COPPER, 5));
            net.minecraft.nbt.CompoundTag beTag = original.saveWithFullMetadata(h.getLevel().registryAccess());
            beTag.putString("id", oldId);
            var be = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, crucibleState, beTag, h.getLevel().registryAccess());
            if (!(be instanceof CrucibleBlockEntity loadedBe) || !loadedBe.getItem(2).is(Items.RAW_COPPER) || loadedBe.getItem(2).getCount() != 5) problems.add("crucible entity " + oldId + " loads as " + be);
            else if (!be.saveWithFullMetadata(h.getLevel().registryAccess()).getStringOr("id", "").equals("simplecrucibles:crucible")) problems.add("crucible entity saved back wrongly");
        }
        BlockState barrelState = CrucibleBlocks.COPPER_BARREL.defaultBlockState();
        net.minecraft.nbt.CompoundTag barrelTag = new com.simplecrucibles.crucible.CrucibleBarrelBlockEntity(pos, barrelState).saveWithFullMetadata(h.getLevel().registryAccess());
        barrelTag.putString("id", "simplelib:barrel");
        if (!(net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, barrelState, barrelTag, h.getLevel().registryAccess()) instanceof com.simplecrucibles.crucible.CrucibleBarrelBlockEntity))
            problems.add("barrel entity simplelib:barrel does not load");
        // recipe book and advancement progress
        var oldRecipe = net.minecraft.resources.Identifier.parse("simplelib:copper_barrel");
        var newRecipe = net.minecraft.resources.Identifier.parse("simplecrucibles:copper_barrel");
        if (!newRecipe.equals(com.simplelib.api.IdMoves.target(net.minecraft.core.registries.Registries.RECIPE, oldRecipe))) problems.add("recipe move");
        var oldAdv = net.minecraft.resources.Identifier.parse("simplelib:recipes/misc/copper_barrel");
        if (h.getLevel().getServer().getAdvancements().get(oldAdv) != h.getLevel().getServer().getAdvancements().get(net.minecraft.resources.Identifier.parse("simplecrucibles:recipes/misc/copper_barrel")))
            problems.add("the recipe advancement does not resolve under its old id");
        if (h.getLevel().getServer().getAdvancements().get(net.minecraft.resources.Identifier.parse("simplecrucibles:recipes/misc/copper_barrel")) == null) problems.add("no recipe advancement for the copper barrel");
        check(h, problems.isEmpty(), "migration: " + problems);
        h.succeed();
    }

    /** The module's upgrade handler moves a crucible's contents into the next tier (InWorldUpgrades.replace); it was SimpleLib's before stage 5. */
    private static void upgradeHandler(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, CrucibleBlocks.IRON_CRUCIBLE.defaultBlockState());
        BlockPos abs = h.absolutePos(rel);
        CrucibleBlockEntity be = (CrucibleBlockEntity) h.getLevel().getBlockEntity(abs);
        check(h, be != null, "no crucible block entity");
        be.setItem(4, new ItemStack(Items.RAW_COPPER, 3));
        check(h, com.simplelib.api.InWorldUpgrades.hasHandler(CrucibleBlocks.REINFORCED_CRUCIBLE), "no crucible handler registered");
        h.assertValueEqual(com.simplelib.api.InWorldUpgrades.replace(h.getLevel(), abs, CrucibleBlocks.REINFORCED_CRUCIBLE), 1, "crucible blocks changed");
        CrucibleBlockEntity fresh = (CrucibleBlockEntity) h.getLevel().getBlockEntity(abs);
        check(h, h.getLevel().getBlockState(abs).is(CrucibleBlocks.REINFORCED_CRUCIBLE) && fresh != null
                && fresh.getItem(4).is(Items.RAW_COPPER) && fresh.getItem(4).getCount() == 3, "the crucible handler lost the contents");
        h.succeed();
    }

    private static void creativeTabRouting(GameTestHelper h) {
        var vanilla = net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS;
        var sb = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                com.simplecrucibles.registry.CrucibleRegistry.SB_FUNCTIONAL_TAB);
        var other = net.minecraft.world.item.CreativeModeTabs.COMBAT;
        h.assertTrue(com.simplecrucibles.registry.CrucibleRegistry.wantsStacks(vanilla, true), "Vanilla functional tab gets no crucibles");
        h.assertTrue(!com.simplecrucibles.registry.CrucibleRegistry.wantsStacks(vanilla, false), "Vanilla tab filled although switched off");
        h.assertTrue(com.simplecrucibles.registry.CrucibleRegistry.wantsStacks(sb, false), "SimpleBuilding's tab skipped");
        h.assertTrue(!com.simplecrucibles.registry.CrucibleRegistry.wantsStacks(other, true), "an unrelated tab gets crucibles");
        h.assertTrue(!com.simplecrucibles.registry.CrucibleRegistry.tabStacks().isEmpty(), "nothing to route");
        h.succeed();
    }





    /** Owner N11 P6: a high-heat crucible burns like magma for crucibleBurnDamage; 0 switches the burn off. */
    /** Owner 2026-10-11: outline/collision follow the model (handles turn with the facing, blank stages grow wall by wall, attached barrel box). */
    private static void crucibleHitboxes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            boolean xSides = d.getAxis() == net.minecraft.core.Direction.Axis.Z;
            for (var block : new net.minecraft.world.level.block.Block[] {CrucibleBlocks.IRON_CRUCIBLE, CrucibleBlocks.NETHERITE_CRUCIBLE, CrucibleBlocks.REINFORCED_CRUCIBLE}) {
                BlockState st = block.defaultBlockState().setValue(CrucibleBlock.FACING, d);
                var outline = st.getShape(level, pos).bounds();
                var coll = st.getCollisionShape(level, pos).bounds();
                check(h, xSides ? (outline.minX == 0 && outline.maxX == 1 && outline.minZ == 1 / 16.0 && outline.maxZ == 15 / 16.0)
                        : (outline.minZ == 0 && outline.maxZ == 1 && outline.minX == 1 / 16.0 && outline.maxX == 15 / 16.0),
                        "handles in the outline on the " + (xSides ? "x" : "z") + " sides for " + d + ": " + outline);
                check(h, outline.maxY == 14 / 16.0 && outline.minY == 0, "outline height 14/16: " + outline);
                check(h, coll.maxY == 9 / 16.0 && !st.getShape(level, pos).isEmpty() && !st.getInteractionShape(level, pos).isEmpty(),
                        "collision stays 9/16 (handles outline only), interior shape set: " + coll);
            }
            for (int stage = 1; stage < CrucibleBlankBlock.STRIKES; stage++) {
                var b = CrucibleBlocks.CRUCIBLE_BLANK.defaultBlockState().setValue(CrucibleBlankBlock.STAGE, stage)
                        .setValue(CrucibleBlankBlock.FACING, d).getShape(level, pos).bounds();
                check(h, b.minY == 0 && b.maxY == (stage > 12 ? 14 : stage > 1 ? 11 : 8) / 16.0 && b.maxX <= 1 && b.minX >= 0, "blank stage " + stage + " " + d + " is no full cube: " + b);
            }
        }
        BlockState north = CrucibleBlocks.COPPER_BARREL.defaultBlockState().setValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACHED, true)
                .setValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.FACING, net.minecraft.core.Direction.NORTH);
        var nb = north.getShape(level, pos).bounds();
        check(h, nb.minZ == 0 && nb.maxZ == 13 / 16.0 && nb.maxY == 11 / 16.0, "attached barrel north box: " + nb);
        h.succeed();
    }

    private static void crucibleBurnDamageFollowsConfig(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        run(h, be, 1);
        check(h, be.heat().atLeast(HeatLevel.HIGH), "lava cauldron below: high heat");
        ServerLevel level = h.getLevel();
        BlockPos pos = be.getBlockPos();
        BlockState state = level.getBlockState(pos);
        try {
            var pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 3, 1));
            float before = pig.getHealth();
            state.getBlock().stepOn(level, pos, state, pig);
            check(h, Math.abs(before - pig.getHealth() - 1.0F) < 1.0E-4, "default burn is 1 like magma: " + pig.getHealth());
            var spared = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 3, 1));
            CrucibleConfig.crucibleBurnDamage = 0;
            float full = spared.getHealth();
            state.getBlock().stepOn(level, pos, state, spared);
            check(h, spared.getHealth() == full, "crucibleBurnDamage 0 still burns");
        } finally {
            CrucibleConfig.crucibleBurnDamage = CrucibleConfig.BURN_DAMAGE_DEFAULT;
        }
        h.succeed();
    }

    /**
     * N37 (owner, mockup a of N34): every tier - enderite too - shows the barrel box beside the crucible box and the
     * inventory box under both, the inventory centred (same margin left and right). Purely the layout numbers.
     */
    private static void barrelBesideInventoryCentred(GameTestHelper h) {
        for (CrucibleTier tier : CrucibleTier.values()) {
            com.simplecrucibles.crucible.CrucibleMenu.Layout compact = com.simplecrucibles.crucible.CrucibleMenu.layout(tier, false);
            com.simplecrucibles.crucible.CrucibleMenu.Layout wide = com.simplecrucibles.crucible.CrucibleMenu.layout(tier, true);
            int crucibleBottom = wide.y() + wide.sectionHeight();
            int barrelBottom = wide.barrelBoxTop() + wide.barrelBoxHeight();
            check(h, wide.barrelBox() >= wide.crucibleLeft() + wide.crucibleWidth(), tier + ": barrel box not beside the crucible box");
            // N37 (owner): Vanilla's 7 px from every box edge to its grid, left and right - compact boxes.
            for (com.simplecrucibles.crucible.CrucibleMenu.Layout l : new com.simplecrucibles.crucible.CrucibleMenu.Layout[] {compact, wide}) {
                int gridLeft = l.slotX(tier, 0) - 1, gridRight = gridLeft + com.simplecrucibles.crucible.CrucibleMenu.gridsWidth(tier);
                check(h, gridLeft - l.crucibleLeft() == 7 && l.crucibleLeft() + l.crucibleWidth() - gridRight == 7,
                        tier + ": crucible margins " + (gridLeft - l.crucibleLeft()) + " / " + (l.crucibleLeft() + l.crucibleWidth() - gridRight));
            }
            int barrelGrid = wide.barrelX(tier, 0) - 1;
            check(h, barrelGrid - wide.barrelBox() == 7 && wide.barrelBox() + wide.barrelBoxWidth() - barrelGrid - com.simplecrucibles.crucible.CrucibleMenu.gridsWidth(tier) == 7,
                    tier + ": barrel margins not 7 px");
            check(h, wide.inventoryBoxTop() >= barrelBottom && wide.inventoryBoxTop() >= crucibleBottom, tier + ": inventory box not under both boxes");
            for (com.simplecrucibles.crucible.CrucibleMenu.Layout l : new com.simplecrucibles.crucible.CrucibleMenu.Layout[] {compact, wide}) {
                int leftGap = l.inventoryLeft() - 1 - l.inventoryBoxLeft();
                int rightGap = l.inventoryBoxLeft() + l.inventoryBoxWidth() - (l.inventoryLeft() - 1 + 162);
                check(h, leftGap == 7 && rightGap == 7, tier + ": inventory margins " + leftGap + " / " + rightGap + " (Vanilla 7)");
            }
            check(h, com.simplecrucibles.crucible.CrucibleMenu.imageWidth(tier) <= 424, tier + ": window wider than 424 px (854x480 at scale 2)");
            check(h, com.simplecrucibles.crucible.CrucibleMenu.imageHeight(tier) <= 240, tier + ": panel does not fit 240 px (854x480 at scale 2)");
            check(h, wide.y() + wide.height() <= com.simplecrucibles.crucible.CrucibleMenu.imageHeight(tier)
                    && compact.y() + compact.height() <= com.simplecrucibles.crucible.CrucibleMenu.imageHeight(tier), tier + ": panel taller than the image");
            for (int i = 0; i < tier.slots(); i++) {
                int bx = wide.barrelX(tier, i), by = wide.barrelY(tier, i);
                check(h, bx > wide.barrelBox() && bx + 16 < wide.barrelBox() + wide.barrelBoxWidth()
                        && by > wide.barrelBoxTop() && by + 16 < barrelBottom, tier + ": barrel field " + i + " outside its box");
                check(h, wide.slotY(tier, i) + 16 < crucibleBottom, tier + ": crucible field " + i + " outside its box");
                check(h, by == wide.slotY(tier, i), tier + ": barrel field " + i + " not level with its crucible field");
            }
            check(h, wide.inventoryTop() + 58 + 16 < wide.inventoryBoxTop() + wide.inventoryBoxHeight(), tier + ": hotbar outside the inventory box");
            // SP22-6 (owner 22.10., height "1/4"): 12 + 5 px under the lowest slot row, 10 px of it free inside the 7 px bottom frame.
            int lowest = wide.slotY(tier, tier.slots() - 1) + 17;
            check(h, wide.fireTop(tier) == lowest && crucibleBottom - 7 - lowest >= 10,
                    tier + ": fire zone under the grid " + (crucibleBottom - 7 - lowest) + " px (want >= 10)");
        }
        h.succeed();
    }

    private static void tierLayout(GameTestHelper h) {
        check(h, CrucibleTier.IRON.slots() == 6 && CrucibleTier.REINFORCED.slots() == 12
                && CrucibleTier.NETHERITE.slots() == 18 && CrucibleTier.ENDERITE.slots() == 30, "slot counts (owner F4, reinforced 12 since N30, enderite 30 since N37)");
        check(h, CrucibleTier.REINFORCED.grids() == 2 && CrucibleTier.REINFORCED.rows() == 2 && CrucibleTier.REINFORCED.below(0) == 3
                && CrucibleTier.REINFORCED.below(3) == -1 && CrucibleTier.REINFORCED.below(6) == 9, "reinforced 2x6 (N37: two 2x3 grids)");
        check(h, CrucibleTier.IRON.below(0) == 3 && CrucibleTier.IRON.below(3) == -1, "iron below");
        check(h, CrucibleTier.NETHERITE.below(6) == -1 && CrucibleTier.NETHERITE.below(9) == 12, "second grid below");
        check(h, CrucibleTier.ENDERITE.stackMultiplier() == 2 && CrucibleTier.NETHERITE.stackMultiplier() == 1, "stack multiplier");
        h.succeed();
    }

    /**
     * N30: the reinforced crucible grew from 9 (3x3) to 12 slots (3x4). A crucible saved with 9 slots (its arrays 9
     * long) loads with every stack, reservation and progress in the same place; the fourth row starts empty.
     */
    private static void reinforcedCrucibleLoadsOldNineSlots(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity old = crucible(h, rel, Blocks.STONE.defaultBlockState(), CrucibleBlocks.REINFORCED_CRUCIBLE);
        check(h, old.getContainerSize() == 12, "reinforced crucible has 12 slots, has " + old.getContainerSize());
        for (int i = 0; i < 9; i++) old.setItem(i, new ItemStack(Items.COBBLESTONE, i + 1));
        net.minecraft.nbt.CompoundTag tag = old.saveCustomOnly(h.getLevel().registryAccess());
        // The old save: arrays of 9 (Progress, Targets, Results), one reservation 2 -> 5.
        int[] targets = new int[9];
        java.util.Arrays.fill(targets, -1);
        targets[2] = 5;
        tag.putIntArray("Progress", new int[] {0, 0, 7, 0, 0, 0, 0, 0, 0});
        tag.putIntArray("Targets", targets);
        tag.putIntArray("Results", new int[9]);
        BlockPos rel2 = new BlockPos(3, 2, 1);
        CrucibleBlockEntity loaded = crucible(h, rel2, Blocks.STONE.defaultBlockState(), CrucibleBlocks.REINFORCED_CRUCIBLE);
        loaded.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        for (int i = 0; i < 9; i++) {
            check(h, loaded.getItem(i).is(Items.COBBLESTONE) && loaded.getItem(i).getCount() == i + 1,
                    "old slot " + i + " kept its stack: " + debug(loaded));
        }
        for (int i = 9; i < 12; i++) check(h, loaded.getItem(i).isEmpty(), "new fourth row starts empty: " + debug(loaded));
        check(h, loaded.target(2) == 5 && loaded.target(9) == -1, "reservation kept, new slots free: " + debug(loaded));
        old.clearContent();
        loaded.clearContent();
        h.succeed();
    }

    private static void heatSources(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        check(h, Heat.of(level, pos, Blocks.CAMPFIRE.defaultBlockState()) == HeatLevel.MEDIUM, "campfire medium");
        check(h, Heat.of(level, pos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false)) == HeatLevel.NONE, "unlit campfire");
        check(h, Heat.of(level, pos, Blocks.SOUL_CAMPFIRE.defaultBlockState()) == HeatLevel.MEDIUM, "soul campfire medium (owner 25)");
        check(h, Heat.of(level, pos, Blocks.MAGMA_BLOCK.defaultBlockState()) == HeatLevel.MEDIUM, "magma medium");
        check(h, Heat.of(level, pos, Blocks.LAVA.defaultBlockState()) == HeatLevel.HIGH, "lava source high");
        check(h, Heat.of(level, pos, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 3)) == HeatLevel.MEDIUM, "flowing lava one lower (F30)");
        check(h, Heat.of(level, pos, Blocks.LAVA_CAULDRON.defaultBlockState()) == HeatLevel.HIGH, "lava cauldron like source (F30)");
        check(h, Heat.of(level, pos, Blocks.TORCH.defaultBlockState()) == HeatLevel.NONE, "torch gives no heat (owner 51)");
        check(h, Heat.of(level, pos, Blocks.FIRE.defaultBlockState()) == HeatLevel.NONE, "fire gives no heat (owner 51)");
        h.succeed();
    }

    private static void heatTwoBelow(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 3, 1);
        h.setBlock(rel.below(2), Blocks.LAVA_CAULDRON);
        h.setBlock(rel.below(), Blocks.HOPPER);
        h.setBlock(rel, CrucibleBlocks.IRON_CRUCIBLE);
        Heat.Reading reading = Heat.at(h.getLevel(), h.absolutePos(rel));
        check(h, reading.level() == HeatLevel.HIGH && Math.abs(reading.multiplier() - 0.9) < 1e-9, "hopper in between, 10 % slower (F26)");
        h.setBlock(rel.below(), Blocks.STONE);
        check(h, Heat.at(h.getLevel(), h.absolutePos(rel)).level() == HeatLevel.NONE, "full block blocks the heat");
        h.succeed();
    }

    private static void cooksIntoSlotBelow(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        be.setItem(1, new ItemStack(Items.SAND));
        // 26.3 cooking recipes all take 200 ticks: / 0.75 (high) = 267 ticks per item at iron speed.
        run(h, be, 270);
        check(h, be.getItem(3).is(Items.IRON_INGOT) && be.getItem(3).getCount() == 1, "first ingot in the slot below (F13) ");
        check(h, be.isResult(3), "result locked");
        run(h, be, 270);
        check(h, be.getItem(3).getCount() == 2 && be.getItem(0).isEmpty(), "second ingot merged");
        check(h, be.getItem(4).is(Items.GLASS), "sand smelted in parallel into its own slot below");
        check(h, be.storedExperience() > 0, "experience stored");
        h.succeed();
    }

    private static void mediumHeatBlocksSmelting(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.CAMPFIRE.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_IRON));
        be.setItem(1, new ItemStack(Items.BEEF));
        // Beef: smoking 200 ticks / 0.5 (medium) = 400 ticks.
        run(h, be, 405);
        check(h, be.slotState(0) == CrucibleBlockEntity.COLD, "raw iron needs high heat (blue)");
        check(h, be.getItem(4).is(Items.COOKED_BEEF), "beef cooked on a campfire");
        check(h, SimpleLibApi.isWarm(be.getItem(4), h.getLevel()), "cooked food comes out warm");
        h.succeed();
    }

    private static void reservedSlotBlocks(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_IRON));
        run(h, be, 20);
        check(h, be.target(0) == 3 && be.ghost(3).is(Items.IRON_INGOT), "reserved below with ghost");
        int before = be.percent(0);
        be.setItem(3, new ItemStack(Items.DIRT));
        run(h, be, 200);
        check(h, be.slotState(0) == CrucibleBlockEntity.BLOCKED, "foreign item blocks (F18)");
        check(h, be.percent(0) == before && be.getItem(0).is(Items.RAW_IRON), "progress kept");
        be.setItem(3, ItemStack.EMPTY);
        run(h, be, 260);
        check(h, be.getItem(3).is(Items.IRON_INGOT), "continues after taking it out");
        h.succeed();
    }

    private static void resultNotRecooked(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.COBBLESTONE));
        run(h, be, 600);
        check(h, be.getItem(3).is(Items.STONE), "stone stays stone, never smooth stone (F14)");
        h.succeed();
    }

    private static void warmsFood(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.CAMPFIRE.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(2, new ItemStack(Items.COOKED_PORKCHOP, 5));
        run(h, be, 210);
        ItemStack warm = be.getItem(2);
        check(h, warm.getCount() == 5 && warm.has(LibComponents.WARM), "whole stack warmed in its slot");
        check(h, be.isResult(2), "warm food is a result");
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        int cold = new ItemStack(Items.COOKED_PORKCHOP).getUseDuration(player);
        int fast = warm.getUseDuration(player);
        check(h, fast == SimpleLibApi.warmFasterUse(cold) && fast < cold, "warm food is eaten 15 % faster");
        long until = warm.get(LibComponents.WARM).until();
        run(h, be, 40);
        check(h, be.getItem(2).get(LibComponents.WARM).until() >= until, "does not cool inside the crucible (owner 39)");
        h.succeed();
    }

    private static void afterglow(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        run(h, be, 2);
        check(h, be.heat() == HeatLevel.HIGH, "heated");
        h.setBlock(rel.below(), Blocks.STONE);
        be.markHeatDirty();
        run(h, be, 20);
        check(h, be.heat() == HeatLevel.HIGH, "afterglow keeps the heat (iron: 2 s)");
        run(h, be, 30);
        check(h, be.heat() == HeatLevel.NONE, "cold after the afterglow");
        h.succeed();
    }

    private static void hopperRules(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        check(h, be.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.UP), "insert cookable from above");
        check(h, !be.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.DOWN), "never from below");
        check(h, !be.canPlaceItemThroughFace(0, new ItemStack(Items.DIRT), Direction.UP), "only cookable items");
        be.setItem(0, new ItemStack(Items.RAW_IRON));
        run(h, be, 270);
        check(h, be.canTakeItemThroughFace(3, be.getItem(3), Direction.DOWN), "results go out below");
        check(h, !be.canTakeItemThroughFace(0, be.getItem(0), Direction.DOWN) || be.getItem(0).isEmpty(), "inputs stay");
        check(h, !be.canPlaceItemThroughFace(3, new ItemStack(Items.IRON_INGOT), Direction.UP), "no insert into results");
        h.succeed();
    }

    /**
     * Owner N51: on an empty Vanilla cauldron, 16 iron ingots, one per strike - plates half/full, handles half/full, rim;
     * every stage is its own state. A filled cauldron is not taken; a broken blank gives the cauldron and the ingots back,
     * an older blank (iron block base) its iron block.
     */
    private static void axeBuild(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, Blocks.CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 16));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        if (com.simplelib.api.SimpleLibApi.axeWaysEnabled()) {
            // REV2-A: the axe way runs at the head of ItemStack.useOn (before Vanilla's Adventure check) - it must not build for non-builders.
            net.minecraft.world.entity.player.Player adventurer = h.makeMockPlayer(GameType.ADVENTURE);
            adventurer.getAbilities().mayBuild = false; // the mock player keeps default abilities
            adventurer.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 16));
            ItemStack adventureAxe = new ItemStack(Items.IRON_AXE);
            adventurer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, adventureAxe);
            adventureAxe.useOn(new net.minecraft.world.item.context.UseOnContext(adventurer, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            check(h, h.getLevel().getBlockState(abs).is(Blocks.CAULDRON) && adventurer.getOffhandItem().getCount() == 16,
                    "Adventure mode builds nothing: " + h.getLevel().getBlockState(abs));
        }
        check(h, !CrucibleBlankBlock.fitsNext(0, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)), "pressure plates are no longer a part");
        check(h, CrucibleBlankBlock.strikesAt(Blocks.WATER_CAULDRON.defaultBlockState()) < 0
                && CrucibleBlankBlock.strikesAt(Blocks.IRON_BLOCK.defaultBlockState()) < 0, "only an empty cauldron starts the build");
        for (int i = 1; i <= CrucibleBlankBlock.STRIKES; i++) {
            check(h, CrucibleBlankBlock.strike(h.getLevel(), abs, player, axe, 1), "strike " + i);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
            BlockState state = h.getLevel().getBlockState(abs);
            check(h, i < CrucibleBlankBlock.STRIKES ? state.is(CrucibleBlocks.CRUCIBLE_BLANK) && state.getValue(CrucibleBlankBlock.STAGE) == i
                    && state.getValue(CrucibleBlankBlock.CAULDRON) : state.is(CrucibleBlocks.IRON_CRUCIBLE), "after strike " + i + ": " + state);
            check(h, player.getOffhandItem().getCount() == 16 - i, "one ingot per strike, " + player.getOffhandItem().getCount() + " left after " + i);
        }
        check(h, axe.getDamageValue() == CrucibleBlankBlock.STRIKES, "one durability per strike: " + axe.getDamageValue());
        BlockState cauldronBlank = CrucibleBlocks.CRUCIBLE_BLANK.defaultBlockState().setValue(CrucibleBlankBlock.STAGE, 7).setValue(CrucibleBlankBlock.CAULDRON, true);
        check(h, dropsOf(h, cauldronBlank, new BlockPos(3, 1, 1)).equals("cauldron 1, iron_ingot 7"), "blank stage 7 gives the cauldron and 7 ingots back: "
                + dropsOf(h, cauldronBlank, new BlockPos(3, 1, 1)));
        BlockState oldBlank = CrucibleBlocks.CRUCIBLE_BLANK.defaultBlockState().setValue(CrucibleBlankBlock.STAGE, 3);
        check(h, dropsOf(h, oldBlank, new BlockPos(5, 1, 1)).equals("iron_block 1, iron_ingot 3"), "an older iron-block blank still gives its iron block: "
                + dropsOf(h, oldBlank, new BlockPos(5, 1, 1)));
        h.succeed();
    }

    /** What breaking {@code state} at {@code rel} drops, sorted: "cauldron 1, iron_ingot 7". */
    private static String dropsOf(GameTestHelper h, BlockState state, BlockPos rel) {
        java.util.Map<String, Integer> out = new java.util.TreeMap<>();
        for (ItemStack stack : net.minecraft.world.level.block.Block.getDrops(state, h.getLevel(), h.absolutePos(rel), null)) {
            out.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), stack.getCount(), Integer::sum);
        }
        StringBuilder line = new StringBuilder();
        out.forEach((k, v) -> line.append(line.isEmpty() ? "" : ", ").append(k).append(' ').append(v));
        return line.toString();
    }

    /**
     * Review H-4: one saved stack that no longer decodes (item of a removed mod) must not empty the crucible. Vanilla's
     * {@code ValueInput.read} keeps the partial list, so the other slots load.
     */
    private static void loadKeepsReadableSlots(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.STONE.defaultBlockState(), CrucibleBlocks.REINFORCED_CRUCIBLE);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (Object[] slot : new Object[][] {{0, "minecraft:raw_gold", 7}, {1, "nomod:vanished_ore", 3}, {2, "minecraft:raw_iron", 5}}) {
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putInt("Slot", (Integer) slot[0]);
            net.minecraft.nbt.CompoundTag item = new net.minecraft.nbt.CompoundTag();
            item.putString("id", (String) slot[1]);
            item.putInt("count", (Integer) slot[2]);
            entry.put("Item", item);
            entry.putInt("Count", (Integer) slot[2]);
            list.add(entry);
        }
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put("Contents", list);
        be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        check(h, be.getItem(0).is(Items.RAW_GOLD) && be.getItem(0).getCount() == 7, "slot before the unreadable one: " + be.getItem(0));
        check(h, be.getItem(2).is(Items.RAW_IRON) && be.getItem(2).getCount() == 5, "slot after the unreadable one: " + be.getItem(2));
        h.succeed();
    }

    private static void breakDrops(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.STONE.defaultBlockState(), CrucibleBlocks.REINFORCED_CRUCIBLE);
        be.setItem(5, new ItemStack(Items.RAW_GOLD, 7));
        check(h, be.drops().size() == 1 && be.drops().get(0).getCount() == 7, "drops contents");
        h.getLevel().destroyBlock(h.absolutePos(rel), true);
        h.assertItemEntityPresent(Items.RAW_GOLD, rel, 2.0);
        h.succeed();
    }

    /**
     * Owner N51: 12 strikes, one cracked diamond every 3 strikes, each shows as a band (state {@code upgrade}); contents
     * stay; a crucible broken mid-way gives the struck-in units back.
     */
    private static void axeUpgrade(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.STONE.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(4, new ItemStack(Items.RAW_COPPER, 3));
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.item.Item cracked = com.simplelib.api.SimpleLibApi.materialItem("cracked_diamond");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(cracked, 5));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        int strikes = com.simplecrucibles.crucible.CrucibleUpgrades.CRUCIBLE_STRIKES;
        for (int i = 1; i <= strikes; i++) {
            check(h, com.simplecrucibles.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + i);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
            if (i < strikes) {
                BlockState state = h.getLevel().getBlockState(abs);
                check(h, state.is(CrucibleBlocks.IRON_CRUCIBLE) && state.getValue(CrucibleBlock.UPGRADE) == i / 3,
                        "after strike " + i + " the crucible shows " + (i / 3) + " bands: " + state);
                check(h, player.getOffhandItem().getCount() == 5 - i / 3, "units used after strike " + i + ": " + player.getOffhandItem().getCount());
            }
        }
        check(h, h.getLevel().getBlockState(abs).is(CrucibleBlocks.REINFORCED_CRUCIBLE)
                && h.getLevel().getBlockState(abs).getValue(CrucibleBlock.UPGRADE) == 0, "upgraded to reinforced (owner N51: 4 cracked diamonds, 12 strikes)");
        CrucibleBlockEntity fresh = (CrucibleBlockEntity) h.getLevel().getBlockEntity(abs);
        check(h, fresh != null && fresh.getContainerSize() == 12 && fresh.getItem(4).is(Items.RAW_COPPER) && fresh.getItem(4).getCount() == 3
                && fresh.upgradeUnits().isEmpty(), "contents kept in the same slot, no units left over");
        check(h, player.getOffhandItem().getCount() == 1, "four cracked diamonds used");
        // Broken after 6 strikes: the two struck-in units come back with the crucible.
        BlockPos rel2 = new BlockPos(3, 2, 1);
        crucible(h, rel2, Blocks.STONE.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        BlockPos abs2 = h.absolutePos(rel2);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 2));
        for (int i = 1; i <= 6; i++) {
            check(h, com.simplecrucibles.crucible.CrucibleUpgrades.strike(h.getLevel(), abs2, player, axe), "second crucible strike " + i);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs2).getValue(CrucibleBlock.UPGRADE) == 2 && player.getOffhandItem().isEmpty(), "two diamonds in");
        h.getLevel().destroyBlock(abs2, true);
        int diamonds = 0;
        for (var item : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(abs2).inflate(2))) {
            if (item.getItem().is(Items.DIAMOND)) diamonds += item.getItem().getCount();
        }
        check(h, diamonds == 2, "breaking it mid-way gives the 2 diamonds back, got " + diamonds);
        h.succeed();
    }



    private static void villageKitchen(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        com.simplecrucibles.village.VillageKitchen.inject(access);
        var pools = access.lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        for (String type : com.simplecrucibles.village.VillageKitchen.TYPES) {
            var pool = pools.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("village/" + type + "/houses"));
            check(h, pool != null, "pool " + type);
            long copies = ((com.simplecrucibles.mixin.TemplatePoolAccessor) pool).simplecrucibles$templates().stream()
                    .filter(e -> e.toString().contains("simplecrucibles:village/" + type + "/field_kitchen")).count();
            check(h, copies == CrucibleConfig.villageKitchenWeight, type + ": kitchen added once with weight " + CrucibleConfig.villageKitchenWeight + ", found " + copies);
            var template = h.getLevel().getStructureTemplateManager().get(com.simplecrucibles.SimpleCrucibles.id("village/" + type + "/field_kitchen"));
            check(h, template.isPresent(), type + ": structure file loads");
            // The kitchen really holds its crucible (old "Name" palette keys loaded as air in 26.3, found by TZ22-3).
            check(h, !template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                    CrucibleBlocks.IRON_CRUCIBLE).isEmpty(), type + ": kitchen template holds no iron crucible");
            // WD25-20: campfire in the ground layer (y 0), crucible directly above it (y 1)
            var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
            var fires = template.get().filterBlocks(BlockPos.ZERO, settings, Blocks.CAMPFIRE);
            var crucibles = template.get().filterBlocks(BlockPos.ZERO, settings, CrucibleBlocks.IRON_CRUCIBLE);
            check(h, fires.size() == 1 && crucibles.size() == 1, type + ": one campfire and one crucible");
            check(h, fires.get(0).pos().getY() == 0 && crucibles.get(0).pos().equals(fires.get(0).pos().above()),
                    type + ": campfire at ground level, crucible on it, got " + fires.get(0).pos() + " / " + crucibles.get(0).pos());
        }
        // 10 % of the campfires become magma blocks (position-seeded, so the share over many positions is stable)
        var list = access.lookupOrThrow(net.minecraft.core.registries.Registries.PROCESSOR_LIST).get(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.PROCESSOR_LIST, com.simplecrucibles.SimpleCrucibles.id("field_kitchen")));
        check(h, list.isPresent(), "field_kitchen processor list is registered");
        var info = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                BlockPos.ZERO, Blocks.CAMPFIRE.defaultBlockState(), null);
        int magma = 0;
        int total = 4000;
        for (int n = 0; n < total; n++) {
            BlockPos at = new BlockPos(n % 80, 64, n / 80);
            var moved = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(at, info.state(), null);
            var out = list.get().value().list().get(0).processBlock(h.getLevel(), BlockPos.ZERO, at, moved, moved,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings());
            if (out != null && out.state().is(Blocks.MAGMA_BLOCK)) magma++;
        }
        check(h, magma > total * 0.07 && magma < total * 0.13, "about 10 % magma, got " + magma + " of " + total);
        // magma counts as a heat source for the crucible
        check(h, h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK).getOrThrow(
                net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.builtInRegistryHolder().key()).is(
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, com.simplecrucibles.SimpleCrucibles.id("heat_source/medium"))),
                "magma block is a medium heat source");
        h.succeed();
    }

    private static void barrelAttach(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        BlockPos barrelRel = rel.east();
        h.setBlock(barrelRel, CrucibleBlocks.COPPER_BARREL);
        BlockPos barrelAbs = h.absolutePos(barrelRel);
        var barrel = (com.simplecrucibles.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(barrelAbs);
        check(h, barrel.getContainerSize() == 27, "on its own a copper barrel has 27 slots (owner 58)");
        barrel.setItem(20, new ItemStack(Items.APPLE, 3));
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        check(h, com.simplecrucibles.crucible.CrucibleBarrelBlock.crackStage(1) == 0 && com.simplecrucibles.crucible.CrucibleBarrelBlock.crackStage(5) == 7,
                "cracks grow with the strikes (owner addition 11)");
        for (int i = 0; i < com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACH_STRIKES; i++) {
            check(h, com.simplecrucibles.crucible.CrucibleBarrelBlock.attachStrike(h.getLevel(), barrelAbs, player, axe, 1), "attach strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
            // Owner 2026-10-10: every strike visibly grows the target (shared preview); the last one removes it.
            int previews = h.getLevel().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.Display.BlockDisplay.class),
                    new net.minecraft.world.phys.AABB(barrelAbs).inflate(1), d -> d.entityTags().contains(com.simplelib.api.InWorldStrikes.PENDING_TAG)).size();
            boolean last = i == com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACH_STRIKES - 1;
            check(h, previews == (last ? 0 : 1), "growing preview after strike " + (i + 1) + ": " + previews);
        }
        var state = h.getLevel().getBlockState(barrelAbs);
        check(h, state.getValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACHED)
                && state.getValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.FACING) == Direction.WEST, "attached, facing the crucible");
        // Owner N12c: attached, the barrel offers as many slots as the crucible (iron: 6); the rest stays stored, hidden.
        check(h, h.getLevel().getBlockEntity(barrelAbs) == barrel && barrel.getContainerSize() == 6, "attached to the iron crucible: 6 slots");
        check(h, h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(barrelAbs).inflate(2), e -> e.getItem().is(Items.APPLE)).isEmpty(), "nothing dropped when attaching");
        var menu = new com.simplecrucibles.crucible.CrucibleMenu(0, player.getInventory(), be);
        check(h, menu.slots.size() == 5 * 6 + 72, "menu: 6 barrel fields for the 6-slot crucible, got " + (menu.slots.size() - 72 - 24));
        be.markHeatDirty();
        be.setItem(0, new ItemStack(Items.RAW_GOLD));
        run(h, be, 270);
        check(h, barrel.getItem(0).is(Items.GOLD_INGOT), "result went into the barrel first (owner wish)");
        check(h, be.getItem(3).isEmpty(), "not into the slot below");
        menu.slots.get(menu.barrelStart() + 5).set(new ItemStack(Items.STICK));
        check(h, barrel.getItem(5).is(Items.STICK), "the 6th field is the barrel's slot 6");
        check(h, menu.slots.get(menu.barrelStart() + 5).container.getContainerSize() == 6 && barrel.getContainerSize() == 6,
                "the menu reaches no barrel slot beyond 6");
        h.setBlock(rel, Blocks.AIR);
        var after = h.getLevel().getBlockState(barrelAbs);
        check(h, !after.getValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACHED) && barrel.getContainerSize() == 27,
                "crucible gone: a normal barrel again");
        check(h, barrel.getItem(0).is(Items.GOLD_INGOT), "contents stay in the barrel");
        check(h, barrel.getItem(20).is(Items.APPLE) && barrel.getItem(20).getCount() == 3, "the hidden slot 21 is back with its apples");
        h.succeed();
    }

    /**
     * Owner N15: a loose barrel opens a chest menu whose slots keep the barrel's limit on both sides - the Enderite
     * mirror takes 128, a shift-click of two full stacks fills one slot, picking up gives a normal stack, and a
     * copper barrel stays at 64.
     */
    private static void looseBarrelMenu(GameTestHelper h) {
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        var tier = com.simplecrucibles.crucible.BarrelTier.ENDERITE;
        var client = com.simplecrucibles.crucible.BarrelMenu.client(tier, 0, player.getInventory());
        check(h, client.slots.get(0).getMaxStackSize(cobble) == 128 && client.getRowCount() == 6,
                "enderite barrel on the client: 6 rows of 128, got " + client.getRowCount() + " rows of " + client.slots.get(0).getMaxStackSize(cobble));
        var barrel = com.simplelib.api.StackLimits.mirror(tier.slots(), tier::stackMultiplier);
        var menu = new com.simplecrucibles.crucible.BarrelMenu(tier, 0, player.getInventory(), barrel);
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        int hotbar = tier.slots() + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        check(h, barrel.getItem(0).getCount() == 128 && barrel.getItem(1).isEmpty(),
                "shift-click fills one enderite slot to 128, got " + barrel.getItem(0).getCount() + "+" + barrel.getItem(1).getCount());
        var taken = menu.slots.get(0).tryRemove(128, Integer.MAX_VALUE, player);
        check(h, taken.isPresent() && taken.get().getCount() == 64 && barrel.getItem(0).getCount() == 64,
                "picking up takes one normal stack, got " + taken.map(ItemStack::getCount).orElse(0));
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, CrucibleBlocks.COPPER_BARREL);
        var copper = (com.simplecrucibles.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        var copperMenu = new com.simplecrucibles.crucible.BarrelMenu(copper.tier(), 0, player.getInventory(), copper);
        check(h, copperMenu.slots.get(0).getMaxStackSize(cobble) == 64 && copperMenu.getRowCount() == 3,
                "copper barrel: 3 rows of 64, got " + copperMenu.getRowCount() + " rows of " + copperMenu.slots.get(0).getMaxStackSize(cobble));
        h.succeed();
    }


    /** Owner N15: the client's menu mirror allows the same raised stacks as the server (Enderite: 128, not 64). */
    private static void clientMenuStackLimit(GameTestHelper h) {
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        var enderite = com.simplecrucibles.crucible.CrucibleMenu.client(com.simplecrucibles.crucible.CrucibleTier.ENDERITE, 0, player.getInventory());
        check(h, enderite.slots.get(0).getMaxStackSize(cobble) == 128, "enderite crucible slot on the client: 128, got " + enderite.slots.get(0).getMaxStackSize(cobble));
        var iron = com.simplecrucibles.crucible.CrucibleMenu.client(com.simplecrucibles.crucible.CrucibleTier.IRON, 0, player.getInventory());
        check(h, iron.slots.get(0).getMaxStackSize(cobble) == 64, "iron crucible slot on the client: 64");
        check(h, com.simplelib.api.StackLimits.max(new ItemStack(Items.IRON_SWORD), 2) == 1, "unstackable items stay single");
        h.succeed();
    }

    /** Owner N16: items already cooking when the barrel is attached send their results to the barrel too. */
    private static void barrelAttachedLater(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.LAVA_CAULDRON.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_GOLD, 2));
        be.setItem(1, new ItemStack(Items.RAW_IRON));
        run(h, be, 20);
        check(h, be.target(0) >= 0 && be.target(0) < CrucibleBlockEntity.BARREL, "without a barrel the gold reserves a crucible slot, got " + be.target(0));
        BlockPos barrelAbs = h.absolutePos(rel.east());
        h.setBlock(rel.east(), CrucibleBlocks.COPPER_BARREL);
        var barrel = (com.simplecrucibles.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(barrelAbs);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACH_STRIKES; i++) {
            com.simplecrucibles.crucible.CrucibleBarrelBlock.attachStrike(h.getLevel(), barrelAbs, player, axe, 1);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        run(h, be, 1);
        check(h, be.target(0) >= CrucibleBlockEntity.BARREL && be.target(1) >= CrucibleBlockEntity.BARREL,
                "after attaching, both running jobs reserve barrel slots, got " + be.target(0) + "/" + be.target(1));
        for (int s = 2; s < 6; s++) check(h, be.ghost(s).isEmpty(), "no crucible slot keeps a stale reservation: " + s);
        run(h, be, 700);
        int gold = 0, iron = 0;
        for (int s = 0; s < barrel.getContainerSize(); s++) {
            if (barrel.getItem(s).is(Items.GOLD_INGOT)) gold += barrel.getItem(s).getCount();
            if (barrel.getItem(s).is(Items.IRON_INGOT)) iron += barrel.getItem(s).getCount();
        }
        check(h, gold == 2 && iron == 1, "all results went into the barrel, gold " + gold + ", iron " + iron);
        for (int s = 0; s < 6; s++) check(h, be.getItem(s).isEmpty(), "crucible slot " + s + " is empty afterwards");
        h.succeed();
    }

    /**
     * a-small22: the 7 upgrade steps (crucibles, barrels, cauldron) are the module's own file
     * ({@code data/simplecrucibles/in_world_upgrades/crucibles.json}), not SimpleBuilding's - so with Simple Hammers alone a
     * hammer in the smithing stance takes the click on every step (combo target with Simple Hammers, without SimpleBuilding).
     */
    private static void hammerUpgradeStepsShipWithTheModule(GameTestHelper h) {
        check(h, com.simplelib.api.InWorldUpgrades.isLoaded(), "SimpleLib's in-world upgrade table is loaded");
        net.minecraft.world.item.Item cracked = com.simplelib.api.SimpleLibApi.materialItem("cracked_diamond");
        net.minecraft.world.item.Item netherite = com.simplelib.api.SimpleLibApi.materialItem("netherite_nugget");
        net.minecraft.world.item.Item enderite = com.simplelib.api.SimpleLibApi.materialItem("enderite_nugget");
        Object[][] steps = {
                {Blocks.CAULDRON, CrucibleBlocks.REINFORCED_CAULDRON, cracked},
                {CrucibleBlocks.IRON_CRUCIBLE, CrucibleBlocks.REINFORCED_CRUCIBLE, cracked},
                {CrucibleBlocks.REINFORCED_CRUCIBLE, CrucibleBlocks.NETHERITE_CRUCIBLE, netherite},
                {CrucibleBlocks.NETHERITE_CRUCIBLE, CrucibleBlocks.ENDERITE_CRUCIBLE, enderite},
                {CrucibleBlocks.COPPER_BARREL, CrucibleBlocks.REINFORCED_BARREL, cracked},
                {CrucibleBlocks.REINFORCED_BARREL, CrucibleBlocks.NETHERITE_BARREL, netherite},
                {CrucibleBlocks.NETHERITE_BARREL, CrucibleBlocks.ENDERITE_BARREL, enderite}};
        for (Object[] step : steps) {
            var entry = com.simplelib.api.InWorldUpgrades.entryFor((net.minecraft.world.level.block.Block) step[0]);
            check(h, entry != null && entry.to() == step[1] && entry.material() == step[2],
                    "upgrade step from " + step[0] + " (to " + step[1] + " with " + step[2] + "): " + entry);
        }
        // With a hammer mod (Simple Hammers fills #simplelib:hammers) the hammer really takes each click - SimpleBuilding or not.
        net.minecraft.world.item.Item hammer = null;
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(com.simplelib.api.InWorldUpgrades.HAMMERS)) {
            if (hammer == null || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.value()).getPath().startsWith("enderite")) hammer = holder.value();
        }
        if (hammer != null) {
            net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(hammer));
            BlockPos rel = new BlockPos(1, 2, 1);
            BlockPos abs = h.absolutePos(rel);
            for (Object[] step : steps) {
                h.setBlock(rel, ((net.minecraft.world.level.block.Block) step[0]).defaultBlockState());
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack((net.minecraft.world.item.Item) step[2], 16));
                check(h, com.simplelib.api.HammerActions.upgradeTakesClick(h.getLevel().getBlockState(abs), h.getLevel(), abs, player,
                        net.minecraft.world.InteractionHand.MAIN_HAND), "the hammer " + hammer + " takes the upgrade click on " + step[0]);
            }
        }
        h.succeed();
    }

    /** Principle 5a: with the axe way on, an axe + material click reaches the axe instead of opening the menu. */
    private static void axeClickReachesAxe(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        crucible(h, rel, Blocks.STONE.defaultBlockState(), CrucibleBlocks.IRON_CRUCIBLE);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        var state = h.getLevel().getBlockState(abs);
        check(h, !com.simplelib.api.SimpleLibApi.toolWants(state, h.getLevel(), abs, player, net.minecraft.world.InteractionHand.MAIN_HAND),
                "an axe alone opens the menu");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 2));
        boolean expected = com.simplelib.api.SimpleLibApi.axeWaysEnabled();
        check(h, com.simplelib.api.SimpleLibApi.toolWants(state, h.getLevel(), abs, player, net.minecraft.world.InteractionHand.MAIN_HAND) == expected,
                "axe + 2 diamonds reaches the axe exactly when the axe way is on (" + expected + ")");
        h.succeed();
    }

    /** Owner 56 B: without SimpleBuilding a cauldron becomes reinforced with an axe and 8 diamonds (doubled 2026-10-06). */
    /** Attaching and upgrading a barrel with the axe count their strikes apart (they used to share one counter). */
    private static void barrelStrikeCountersAreSeparate(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        h.setBlock(rel, CrucibleBlocks.COPPER_BARREL);
        var barrel = (com.simplecrucibles.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        check(h, barrel.addAttachStrike() == 1 && barrel.addAttachStrike() == 2, "attach strikes count up");
        check(h, barrel.addUpgradeStrike() == 1, "upgrade strikes start at 1, not after the attach strikes");
        check(h, barrel.addAttachStrike() == 3 && barrel.addUpgradeStrike() == 2, "each counter keeps its own count");
        h.succeed();
    }

    private static void axeUpgradesCauldron(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, Blocks.CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, com.simplecrucibles.crucible.CrucibleUpgrades.CAULDRON_COST));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplecrucibles.crucible.CrucibleUpgrades.CAULDRON_STRIKES; i++) {
            check(h, com.simplecrucibles.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs).is(CrucibleBlocks.REINFORCED_CAULDRON), "reinforced cauldron built");
        check(h, player.getOffhandItem().isEmpty(), "eight diamonds used");
        h.succeed();
    }

    /** The reinforced cauldron takes and gives lava/water by bucket; lava in it heats a crucible like a lava source (owner F30). */
    private static void reinforcedCauldronBuckets(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        h.setBlock(rel, CrucibleBlocks.REINFORCED_CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
        h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        check(h, "lava".equals(com.simplecrucibles.api.SimpleCruciblesApi.cauldronContent(h.getLevel().getBlockState(abs))), "lava poured in");
        check(h, player.getMainHandItem().is(Items.BUCKET), "empty bucket back");
        check(h, Heat.at(h.getLevel(), abs.above()).level() == HeatLevel.HIGH, "lava in the cauldron heats high");
        h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        check(h, player.getMainHandItem().is(Items.LAVA_BUCKET), "lava taken back");
        h.succeed();
    }

    /** Owner addition 11: the reinforced cauldron runs the Vanilla cauldron interactions (bottles, levels) and stays reinforced. */
    private static void reinforcedCauldronInheritsVanilla(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        h.setBlock(rel, CrucibleBlocks.REINFORCED_CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        var level = h.getLevel();
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
        java.util.function.Consumer<ItemStack> use = stack -> {
            player.setItemInHand(hand, stack);
            level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, hand, hit);
        };
        use.accept(new ItemStack(Items.WATER_BUCKET));
        var state = level.getBlockState(abs);
        check(h, state.is(CrucibleBlocks.REINFORCED_CAULDRON) && "water".equals(com.simplecrucibles.api.SimpleCruciblesApi.cauldronContent(state))
                && state.getValue(com.simplecrucibles.cauldron.ReinforcedCauldronBlock.LEVEL) == 3, "water bucket: full water, still reinforced");
        use.accept(new ItemStack(Items.GLASS_BOTTLE));
        state = level.getBlockState(abs);
        check(h, player.getMainHandItem().is(Items.POTION), "bottle filled with water");
        check(h, state.is(CrucibleBlocks.REINFORCED_CAULDRON) && state.getValue(com.simplecrucibles.cauldron.ReinforcedCauldronBlock.LEVEL) == 2, "level 3 -> 2");
        use.accept(new ItemStack(Items.GLASS_BOTTLE));
        use.accept(new ItemStack(Items.GLASS_BOTTLE));
        state = level.getBlockState(abs);
        check(h, state.is(CrucibleBlocks.REINFORCED_CAULDRON) && "empty".equals(com.simplecrucibles.api.SimpleCruciblesApi.cauldronContent(state)),
                "three bottles empty it, it stays reinforced");
        use.accept(new ItemStack(Items.POWDER_SNOW_BUCKET));
        check(h, "powder_snow".equals(com.simplecrucibles.api.SimpleCruciblesApi.cauldronContent(level.getBlockState(abs))), "powder snow bucket");
        use.accept(new ItemStack(Items.BUCKET));
        check(h, player.getMainHandItem().is(Items.POWDER_SNOW_BUCKET)
                && "empty".equals(com.simplecrucibles.api.SimpleCruciblesApi.cauldronContent(level.getBlockState(abs))), "powder snow taken back");
        h.succeed();
    }

    /** Owner addition 11: reinforced barrel -> netherite barrel (axe, 1 netherite ingot, ten strikes), 45 slots, contents kept. */
    private static void axeUpgradesBarrelToNetherite(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, CrucibleBlocks.REINFORCED_BARREL);
        BlockPos abs = h.absolutePos(rel);
        ((net.minecraft.world.Container) h.getLevel().getBlockEntity(abs)).setItem(30, new ItemStack(Items.COAL, 7));
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.NETHERITE_INGOT));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplecrucibles.crucible.CrucibleUpgrades.STRIKES; i++) {
            check(h, com.simplecrucibles.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs).is(CrucibleBlocks.NETHERITE_BARREL), "netherite barrel built");
        var barrel = (com.simplecrucibles.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(abs);
        check(h, barrel.getContainerSize() == 45 && barrel.getItem(30).is(Items.COAL) && barrel.getItem(30).getCount() == 7, "45 slots, contents kept");
        check(h, player.getOffhandItem().isEmpty(), "netherite ingot used");
        h.succeed();
    }

    private CrucibleTests() {}

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplecrucibles.discover.CruciblesDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplecrucibles.discover.CruciblesDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplecrucibles.discover.CruciblesDiscover::firstPickupTips);
    }

    // JEI24: recipe viewer data against its sources.
    static {
        ALL.put("jei_build_attach_and_upgrades", CrucibleJeiTests::buildAttachAndUpgrades);
    }
}

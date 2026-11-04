package com.simplearrows.test;

import com.simplearrows.ArrowsRegistry;
import com.simplearrows.SimpleArrows;
import com.simplearrows.config.ArrowsConfig;
import com.simplearrows.entity.LightArrow;
import com.simplearrows.entity.SilverArrow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Arrows (docs/ai/PLAN-B-ARROWS02-2026-11-03.md): the Silver Arrow moved from Simple Ores (ids, recipe only with Simple Ores,
 * +3 against undead) and the Light Arrow (lights the air in front of the block it sticks in, the light goes with the arrow: Vanilla
 * despawn, pick-up, falling out). Loader-neutral; the Fabric adapter registers {@link #ALL} as module_game_test_<name> on
 * {@code simplearrows:empty}; every test runs standalone too (the silver parts then check that there is no recipe).
 */
public final class ArrowsTests {
    /** Test name -> body, in catalogue order. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", ArrowsTests::registered);
        ALL.put("old_silver_arrow_ids_move", ArrowsTests::oldSilverArrowIdsMove);
        ALL.put("sub_mod_switch", ArrowsTests::subModSwitch);
        ALL.put("silver_arrow_recipe_needs_simple_ores", ArrowsTests::silverArrowRecipeNeedsSimpleOres);
        ALL.put("silver_arrow_undead_bonus", ArrowsTests::silverArrowUndeadBonus);
        ALL.put("light_arrow_recipe", ArrowsTests::lightArrowRecipe);
        ALL.put("light_arrow_sets_and_removes_light", ArrowsTests::lightArrowSetsAndRemovesLight);
        ALL.put("light_arrow_despawns_like_an_arrow", ArrowsTests::lightArrowDespawnsLikeAnArrow);
        ALL.put("light_arrow_light_is_shared", ArrowsTests::lightArrowLightIsShared);
        ALL.put("light_arrow_light_follows_save_data", ArrowsTests::lightArrowLightFollowsSaveData);
        ALL.put("light_arrow_switch_off", ArrowsTests::lightArrowSwitchOff);
        // Stage 2 (b-arrows03, 04.11.): the Fletching Table from the SimpleBuilding core.
        ALL.put("fletching_each_tip_adds_its_damage_against_its_targets", FletchingTests::eachTipAddsItsDamageAgainstItsTargets);
        ALL.put("fletching_shafts_and_fletchings_change_the_flight", FletchingTests::shaftsAndFletchingsChangeTheFlight);
        ALL.put("fletching_material_shafts_are_only_the_diamond_rod", FletchingTests::materialShaftsAreOnlyTheDiamondRod);
        ALL.put("fletching_old_netherite_and_enderite_shafts_load_as_sticks", FletchingTests::oldNetheriteAndEnderiteShaftsLoadAsSticks);
        ALL.put("fletching_the_table_makes_four_arrows_from_three_parts", FletchingTests::theTableMakesFourArrowsFromThreeParts);
        ALL.put("fletching_recipe_book_places_the_parts_from_the_inventory", FletchingTests::recipeBookPlacesThePartsFromTheInventory);
        ALL.put("fletching_every_part_has_its_recipe_book_entry", FletchingTests::everyCombinationHasItsRecipeBookEntry);
        ALL.put("fletching_right_clicking_the_fletching_table_opens_the_menu", FletchingTests::rightClickingTheFletchingTableOpensTheMenu);
        ALL.put("fletching_potion_slot_makes_tipped_arrows", FletchingTests::potionSlotMakesTippedArrows);
        ALL.put("old_fletching_ids_move", FletchingTests::oldFletchingIdsMove);
        ALL.put("fletching_layout", FletchingTests::fletchingLayout);
        ALL.put("amethyst_arrow_is_not_recovered", FletchingTests::amethystArrowIsNotRecovered);
        ALL.put("dummy_head_counts_for_tips", FletchingTests::dummyHeadCountsForTips);
        ALL.put("tip_tags_are_data", FletchingTests::tipTagsAreData);
        ALL.put("discover_recipe_unlocks", com.simplearrows.discover.ArrowsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplearrows.discover.ArrowsDiscover::behaviourHints);
        ALL.put("guide_book", com.simplearrows.guide.ArrowsGuide::gameTest);
    }

    private ArrowsTests() {}

    private static ItemStack craft(GameTestHelper h, CraftingInput input) {
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(x -> x.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    public static void registered(GameTestHelper h) {
        for (String path : List.of("silver_arrow", "light_arrow", "crafted_arrow")) {
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleArrows.id(path)), "item " + path);
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleArrows.id(path)), "entity type " + path);
        }
        h.assertTrue(new ItemStack(ArrowsRegistry.SILVER_ARROW).is(ItemTags.ARROWS) && new ItemStack(ArrowsRegistry.LIGHT_ARROW).is(ItemTags.ARROWS)
                && new ItemStack(com.simplearrows.fletching.Fletchings.CRAFTED_ARROW).is(ItemTags.ARROWS), "bows take all three as arrows");
        h.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(ArrowsRegistry.SILVER_ARROW_ENTITY).is(EntityTypeTags.ARROWS)
                && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(ArrowsRegistry.LIGHT_ARROW_ENTITY).is(EntityTypeTags.ARROWS), "arrow entities");
        h.assertTrue(ArrowsConfig.KEYS.equals(List.of("silverArrowUndeadBonus", "lightArrow")) && ArrowsConfig.silverArrowUndeadBonus == 3
                && ArrowsConfig.lightArrow, "config defaults");
        h.succeed();
    }

    /** Worlds from before 03.11.: {@code simpleores:silver_arrow} stacks, entities, recipe book entries and recipe advancements load as Simple Arrows'. */
    public static void oldSilverArrowIdsMove(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Identifier old = Identifier.fromNamespaceAndPath(SimpleArrows.ORES_NAMESPACE, "silver_arrow");
        if (BuiltInRegistries.ITEM.getValue(old) != ArrowsRegistry.SILVER_ARROW) problems.add("item");
        if (BuiltInRegistries.ENTITY_TYPE.getValue(old) != ArrowsRegistry.SILVER_ARROW_ENTITY) problems.add("entity type");
        if (!SimpleArrows.id("silver_arrow").equals(com.simplelib.api.IdMoves.target(Registries.RECIPE, old))) problems.add("recipe");
        if (!SimpleArrows.id("recipes/combat/silver_arrow").equals(com.simplelib.api.IdMoves.advancement(
                Identifier.fromNamespaceAndPath(SimpleArrows.ORES_NAMESPACE, "recipes/combat/silver_arrow")))) problems.add("advancement");
        // A saved stack with the old id
        com.google.gson.JsonObject stack = new com.google.gson.JsonObject();
        stack.addProperty("id", old.toString());
        stack.addProperty("count", 5);
        ItemStack decoded = ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), stack)
                .result().orElse(ItemStack.EMPTY);
        if (!decoded.is(ArrowsRegistry.SILVER_ARROW) || decoded.getCount() != 5) problems.add("saved stack " + decoded);
        h.assertTrue(problems.isEmpty(), "old silver arrow ids: " + problems);
        h.succeed();
    }

    public static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimpleArrows.MOD_ID, false);
        try {
            h.assertTrue(!SimpleArrows.on() && ArrowsRegistry.stacks().isEmpty(), "no tab stacks while off");
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleArrows.id("light_arrow")), "registrations stay");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleArrows.MOD_ID, true);
        }
        h.assertTrue(SimpleArrows.on() && !ArrowsRegistry.stacks().isEmpty(), "back on");
        h.succeed();
    }

    /** Nugget / stick / feather -> 4 silver arrows, only when Simple Ores is there (its silver); standalone there is no recipe. */
    public static void silverArrowRecipeNeedsSimpleOres(GameTestHelper h) {
        boolean ores = ArrowsRegistry.silverAvailable();
        var recipe = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleArrows.id("silver_arrow")));
        h.assertValueEqual(recipe.isPresent(), ores, "silver arrow recipe loaded (Simple Ores present: " + ores + ")");
        h.assertValueEqual(ArrowsRegistry.stacks().stream().anyMatch(s -> s.is(ArrowsRegistry.SILVER_ARROW)), ores, "silver arrow in the tab");
        if (ores) {
            ItemStack nugget = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(SimpleArrows.ORES_NAMESPACE, "silver_nugget")));
            ItemStack out = craft(h, CraftingInput.of(1, 3, List.of(nugget, new ItemStack(Items.STICK), new ItemStack(Items.FEATHER))));
            h.assertTrue(out.is(ArrowsRegistry.SILVER_ARROW) && out.getCount() == 4, "nugget / stick / feather -> 4 silver arrows: " + out);
        }
        h.succeed();
    }

    public static void silverArrowUndeadBonus(GameTestHelper h) {
        try {
            ArrowsConfig.silverArrowUndeadBonus = 3;
            float silverZombie = damage(h, EntityTypes.ZOMBIE, true, 1);
            float vanillaZombie = damage(h, EntityTypes.ZOMBIE, false, 1);
            h.assertTrue(silverZombie - vanillaZombie > 2.5F && silverZombie - vanillaZombie < 3.5F, "undead: +3 damage: silver " + silverZombie + " vs " + vanillaZombie);
            float silverSheep = damage(h, EntityTypes.SHEEP, true, 5);
            float vanillaSheep = damage(h, EntityTypes.SHEEP, false, 5);
            h.assertTrue(silverSheep == vanillaSheep && vanillaSheep > 0.0F, "a sheep takes the Vanilla damage: " + silverSheep + " vs " + vanillaSheep);
            ArrowsConfig.silverArrowUndeadBonus = 0;
            float off = damage(h, EntityTypes.ZOMBIE, true, 9);
            h.assertTrue(Math.abs(off - vanillaZombie) < 0.01F, "bonus 0 = Vanilla damage: " + off + " vs " + vanillaZombie);
        } finally {
            ArrowsConfig.reset();
        }
        h.succeed();
    }

    /** Health a fresh mob loses when a silver or Vanilla arrow at speed 3 hits it (mobs 4 cells apart per call). */
    private static <E extends Mob> float damage(GameTestHelper h, EntityType<E> type, boolean silver, int slot) {
        E mob = h.spawnWithNoFreeWill(type, slot % 7, 1, 1 + (silver ? 0 : 4));
        float before = mob.getHealth();
        AbstractArrow arrow = silver
                ? new SilverArrow(h.getLevel(), mob.getX() - 3.0, mob.getY() + 1.0, mob.getZ(), new ItemStack(ArrowsRegistry.SILVER_ARROW), null)
                : new HitArrow(h.getLevel(), mob.getX() - 3.0, mob.getY() + 1.0, mob.getZ());
        arrow.setDeltaMovement(3.0, 0.0, 0.0);
        if (arrow instanceof SilverArrow silverArrow) silverArrow.onHitEntity(new EntityHitResult(mob));
        else ((HitArrow) arrow).hit(new EntityHitResult(mob));
        float lost = before - mob.getHealth();
        mob.discard();
        return lost;
    }

    /** A Vanilla arrow that can be made to hit without flying (onHitEntity is protected). */
    private static final class HitArrow extends Arrow {
        HitArrow(net.minecraft.world.level.Level level, double x, double y, double z) {
            super(level, x, y, z, new ItemStack(Items.ARROW), null);
        }

        void hit(EntityHitResult result) {
            this.onHitEntity(result);
        }
    }

    /** Glowstone dust / stick / feather -> 4 Light Arrows (Vanilla's arrow shape), Vanilla ingredients only. */
    public static void lightArrowRecipe(GameTestHelper h) {
        ItemStack out = craft(h, CraftingInput.of(1, 3, List.of(new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.STICK), new ItemStack(Items.FEATHER))));
        h.assertTrue(out.is(ArrowsRegistry.LIGHT_ARROW) && out.getCount() == 4, "glowstone dust / stick / feather -> 4 light arrows: " + out);
        h.assertTrue(craft(h, CraftingInput.of(1, 3, List.of(new ItemStack(Items.FEATHER), new ItemStack(Items.STICK), new ItemStack(Items.GLOWSTONE_DUST)))).isEmpty(),
                "upside down: nothing");
        h.succeed();
    }

    /** A Light Arrow stuck in the west face of a stone block at (3, 2, 3) like a flying one (at the face, then 0.05 back); the air west of it gets the light. */
    private static LightArrow stick(GameTestHelper h, BlockPos stone) {
        h.setBlock(stone, Blocks.STONE);
        BlockPos abs = h.absolutePos(stone);
        LightArrow arrow = new LightArrow(h.getLevel(), abs.getX(), abs.getY() + 0.5, abs.getZ() + 0.5, new ItemStack(ArrowsRegistry.LIGHT_ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        h.getLevel().addFreshEntity(arrow);
        arrow.setDeltaMovement(1.0, 0.0, 0.0);
        arrow.onHitBlock(new BlockHitResult(new Vec3(abs.getX(), abs.getY() + 0.5, abs.getZ() + 0.5), Direction.WEST, abs, false));
        return arrow;
    }

    public static void lightArrowSetsAndRemovesLight(GameTestHelper h) {
        BlockPos stone = new BlockPos(3, 2, 3);
        LightArrow arrow = stick(h, stone);
        h.assertTrue(h.getBlockState(stone.west()).is(Blocks.LIGHT), "light in front of the hit face");
        h.assertValueEqual(arrow.lightPos(), h.absolutePos(stone.west()), "the arrow owns the light");
        h.assertValueEqual(h.getBlockState(stone.west()).getLightEmission(), 15, "light level");
        arrow.discard(); // what a pick-up does
        h.assertTrue(h.getBlockState(stone.west()).isAir(), "picked up: the light is gone");
        // The block it sticks in goes: the arrow falls out (Vanilla) and its light goes with it.
        LightArrow second = stick(h, stone);
        second.tick();
        h.assertTrue(second.lightPos() != null && h.getBlockState(stone.west()).is(Blocks.LIGHT), "still stuck after a tick");
        h.setBlock(stone, Blocks.AIR);
        second.tick();
        h.assertTrue(second.lightPos() == null && !h.getBlockState(stone.west()).is(Blocks.LIGHT), "fell out: the light is gone");
        second.discard();
        h.assertFalse(h.getBlockState(stone.west()).is(Blocks.LIGHT), "no light left behind");
        h.succeed();
    }

    /** Owner 03.11.: the Light Arrow despawns like a normal arrow (Vanilla clock: 1200 ticks in the ground) and its light goes with it. */
    public static void lightArrowDespawnsLikeAnArrow(GameTestHelper h) {
        BlockPos stone = new BlockPos(3, 2, 3);
        LightArrow arrow = stick(h, stone);
        for (int i = 0; i < 1199; i++) arrow.tickDespawn();
        h.assertTrue(arrow.isAlive() && h.getBlockState(stone.west()).is(Blocks.LIGHT), "still there after 1199 ticks");
        arrow.tickDespawn();
        h.assertTrue(arrow.isRemoved(), "despawned after 1200 ticks like a Vanilla arrow");
        h.assertTrue(h.getBlockState(stone.west()).isAir(), "despawned: the light is gone");
        h.succeed();
    }

    /** Two Light Arrows in the same air: the light stays until the last one goes. A light that was replaced is left alone. */
    public static void lightArrowLightIsShared(GameTestHelper h) {
        BlockPos stone = new BlockPos(3, 2, 3);
        LightArrow first = stick(h, stone);
        LightArrow second = stick(h, stone);
        h.assertValueEqual(second.lightPos(), first.lightPos(), "both hold the same light");
        first.discard();
        h.assertTrue(h.getBlockState(stone.west()).is(Blocks.LIGHT), "one arrow left: the light stays");
        h.setBlock(stone.west(), Blocks.GLASS);
        second.discard();
        h.assertTrue(h.getBlockState(stone.west()).is(Blocks.GLASS), "a block placed into the light is not removed");
        h.succeed();
    }

    /** The light position is saved with the arrow; an arrow unloaded with its chunk keeps the light. */
    public static void lightArrowLightFollowsSaveData(GameTestHelper h) {
        BlockPos stone = new BlockPos(3, 2, 3);
        LightArrow arrow = stick(h, stone);
        net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        arrow.saveWithoutId(out);
        net.minecraft.nbt.CompoundTag tag = out.buildResult();
        h.assertTrue(tag.contains("light_pos"), "light_pos saved: " + tag);
        LightArrow loaded = new LightArrow(ArrowsRegistry.LIGHT_ARROW_ENTITY, h.getLevel());
        loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        h.assertValueEqual(loaded.lightPos(), arrow.lightPos(), "light_pos loaded");
        arrow.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertTrue(h.getBlockState(stone.west()).is(Blocks.LIGHT), "unloading keeps the light");
        loaded.setUUID(java.util.UUID.randomUUID());
        h.getLevel().addFreshEntity(loaded);
        loaded.discard();
        h.assertTrue(h.getBlockState(stone.west()).isAir(), "the reloaded arrow takes its light away");
        h.succeed();
    }

    public static void lightArrowSwitchOff(GameTestHelper h) {
        try {
            ArrowsConfig.lightArrow = false;
            BlockPos stone = new BlockPos(3, 2, 3);
            LightArrow arrow = stick(h, stone);
            h.assertTrue(arrow.lightPos() == null && h.getBlockState(stone.west()).isAir(), "switched off: a plain arrow");
            arrow.discard();
        } finally {
            ArrowsConfig.reset();
        }
        h.succeed();
    }
}

package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SilentDandelionTests {
    private SilentDandelionTests() {}

    private static boolean enabled(GameTestHelper helper) {
        if (McVersion.SILENT_DANDELION) return true;
        helper.succeed();
        return false;
    }

    public static void useTogglesMobsWithVanillaCooldownAndConsumption(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 2));
        cow.setAge(0);
        var baby = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 2));
        baby.setAge(-24000);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 2));
        var villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(5, 2, 2));
        List<net.minecraft.world.entity.Mob> mobs = List.of(cow, baby, zombie, villager);
        ItemStack flowers = new ItemStack(ModItems.SILENT_DANDELION, 12);
        player.setItemInHand(InteractionHand.MAIN_HAND, flowers);
        for (var mob : mobs) {
            var result = player.interactOn(mob, InteractionHand.MAIN_HAND, mob.position());
            helper.assertTrue(result == net.minecraft.world.InteractionResult.SUCCESS, "use must return vanilla SUCCESS for arm swing");
            helper.assertTrue(mob.isSilent() && mob.isPersistenceRequired(), "use must silence and preserve mob");
        }
        helper.assertTrue(flowers.getCount() == 8, "one flower consumed per mob");
        helper.assertTrue(cow.getAge() == 0 && baby.getAge() == -24000, "silence must not change age");
        // Only retry on the cow: other mobs may have a normal fallback interaction.
        player.interactOn(cow, InteractionHand.MAIN_HAND, cow.position());
        helper.assertTrue(cow.isSilent() && flowers.getCount() == 8, "golden-style cooldown must prevent immediate reuse");
        helper.runAfterDelay(41, () -> {
            for (var mob : mobs) {
                player.interactOn(mob, InteractionHand.MAIN_HAND, mob.position());
                helper.assertTrue(!mob.isSilent(), "second use must restore sound");
                mob.discard();
            }
            helper.assertTrue(flowers.getCount() == 4, "restoring sound also consumes one flower");
            helper.succeed();
        });
    }

    public static void creativeOffhandUseDoesNotConsume(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        net.minecraft.world.level.GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 2));
        ItemStack flower = new ItemStack(ModItems.SILENT_DANDELION, 2);
        player.setItemInHand(InteractionHand.OFF_HAND, flower);
        player.interactOn(cow, InteractionHand.OFF_HAND, cow.position());
        helper.assertTrue(cow.isSilent(), "creative offhand use must silence");
        helper.assertTrue(flower.getCount() == 2, "creative offhand use must not consume");
        helper.runAfterDelay(41, () -> {
            player.interactOn(cow, InteractionHand.OFF_HAND, cow.position());
            helper.assertTrue(!cow.isSilent() && flower.getCount() == 2, "creative reuse must not consume");
            cow.discard();
            helper.succeed();
        });
    }

    public static void playersStandsBossesAndDeadMobsAreUnchanged(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var other = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(2, 2, 2));
        var wither = helper.spawn(EntityTypes.WITHER, new BlockPos(3, 2, 2));
        var dragon = helper.spawn(EntityTypes.ENDER_DRAGON, new BlockPos(4, 2, 2));
        var dead = helper.spawn(EntityTypes.COW, new BlockPos(5, 2, 2));
        dead.setHealth(0);
        ItemStack flowers = new ItemStack(ModItems.SILENT_DANDELION, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, flowers);
        for (var entity : List.of(other, stand, wither, dragon, dead)) {
            player.interactOn(entity, InteractionHand.MAIN_HAND, entity.position());
            helper.assertTrue(!entity.isSilent(), "excluded entity was silenced: " + entity.getType());
            entity.setSilent(true);
            player.interactOn(entity, InteractionHand.MAIN_HAND, entity.position());
            helper.assertTrue(entity.isSilent(), "excluded entity lost existing silence");
        }
        helper.assertTrue(flowers.getCount() == 8, "excluded entities must not consume flowers");
        stand.discard();
        wither.discard();
        dragon.discard();
        dead.discard();
        helper.succeed();
    }

    public static void pottingAndUnpottingUseVanillaInteraction(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos soil = new BlockPos(4, 1, 4);
        helper.setBlock(soil, Blocks.DIRT);
        ItemStack planting = new ItemStack(ModItems.SILENT_DANDELION);
        player.setItemInHand(InteractionHand.MAIN_HAND, planting);
        BlockPos soilAbsolute = helper.absolutePos(soil);
        planting.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soilAbsolute).add(0, 0.5, 0), Direction.UP, soilAbsolute, false)));
        helper.assertTrue(helper.getBlockState(soil.above()).is(ModBlocks.SILENT_DANDELION), "block item must plant on soil");
        var nearby = helper.spawn(EntityTypes.COW, soil.above().east());
        helper.assertTrue(!nearby.isSilent(), "planted flower must not silence nearby mobs");
        nearby.discard();
        helper.setBlock(soil.above(), Blocks.AIR);
        BlockPos pot = new BlockPos(2, 2, 2);
        helper.setBlock(pot.below(), Blocks.STONE);
        helper.setBlock(pot, Blocks.FLOWER_POT);
        ItemStack flower = new ItemStack(ModItems.SILENT_DANDELION, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, flower);
        BlockPos absolute = helper.absolutePos(pot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        helper.getBlockState(pot).useItemOn(flower, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(helper.getBlockState(pot).is(ModBlocks.POTTED_SILENT_DANDELION), "flower did not enter vanilla pot");
        var cow = helper.spawn(EntityTypes.COW, pot.east());
        helper.assertTrue(!cow.isSilent(), "potted flower must be decoration without an aura");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(pot).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(helper.getBlockState(pot).is(Blocks.FLOWER_POT), "empty-hand use must return empty pot");
        helper.assertTrue(!cow.isSilent(), "unpotting must not change silence");
        helper.assertTrue(player.getMainHandItem().is(ModItems.SILENT_DANDELION), "unpotting must return the flower");
        cow.discard();
        helper.succeed();
    }

    public static void recipesUseFourStringOrAnyWoolAndEightYarn(GameTestHelper helper) {
        if (!enabled(helper)) return;
        // Diamond arrangement leaves vanilla's 2x2 white wool recipe unambiguous.
        craft(helper, "yarn_ball_from_string", CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.STRING),
                ItemStack.EMPTY, new ItemStack(Items.STRING), ItemStack.EMPTY, new ItemStack(Items.STRING),
                ItemStack.EMPTY, new ItemStack(Items.STRING), ItemStack.EMPTY)), ModItems.YARN_BALL, 1);
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            Item wool = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    net.minecraft.resources.Identifier.withDefaultNamespace(color.getName() + "_wool"));
            craft(helper, "yarn_ball_from_wool", CraftingInput.of(1, 1, List.of(new ItemStack(wool))), ModItems.YARN_BALL, 2);
        }
        ItemStack yarn = new ItemStack(ModItems.YARN_BALL);
        craft(helper, "silent_dandelion", CraftingInput.of(3, 3, List.of(yarn.copy(), yarn.copy(), yarn.copy(), yarn.copy(),
                new ItemStack(Items.DANDELION), yarn.copy(), yarn.copy(), yarn.copy(), yarn.copy())), ModItems.SILENT_DANDELION, 1);
        helper.succeed();
    }

    private static void craft(GameTestHelper helper, String id, CraftingInput grid, Item item, int count) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", id));
        var holder = helper.getLevel().getServer().getRecipeManager().byKey(key).orElseThrow();
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) holder.value();
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "recipe does not match " + id);
        ItemStack result = recipe.assemble(grid);
        helper.assertTrue(result.is(item) && result.getCount() == count, "wrong recipe result for " + id + ": " + result);
    }

    public static void toggledSilenceSurvivesVanillaSaveAndLoad(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack flowers = new ItemStack(ModItems.SILENT_DANDELION, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, flowers);
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 2));
        player.interactOn(cow, InteractionHand.MAIN_HAND, cow.position());
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        cow.saveWithoutId(output);
        helper.assertTrue(output.buildResult().getBooleanOr("Silent", false), "vanilla Silent NBT must persist use");
        cow.discard();
        var loaded = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 2));
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(loaded.isSilent(), "silence must survive reload");
        player.interactOn(loaded, InteractionHand.MAIN_HAND, loaded.position());
        helper.assertTrue(!loaded.isSilent() && flowers.isEmpty(), "reloaded silence must toggle off with one flower");
        output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        loaded.saveWithoutId(output);
        helper.assertTrue(!output.buildResult().getBooleanOr("Silent", false), "restored sounds must persist as not silent");
        loaded.setSilent(true);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(!loaded.isSilent(), "loading the second save must restore sound");
        loaded.discard();
        helper.succeed();
    }

    public static void yarnPlacesInSmallPartsAndDropsItself(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.STONE);
        ItemStack yarn = new ItemStack(ModItems.YARN_BALL, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, yarn);
        BlockPos absolute = helper.absolutePos(floor);
        var result = yarn.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false)));
        helper.assertTrue(result.consumesAction(), "yarn use did not place anything");
        helper.assertTrue(yarn.getCount() == 1, "placing yarn must consume exactly one item");
        helper.assertTrue(helper.getBlockState(floor.above()).is(ModBlocks.PLACED_SMALL_PARTS), "yarn must reuse small parts block");
        var be = helper.getBlockEntity(floor.above(), PlacedSmallPartsBlockEntity.class);
        helper.assertTrue(be.parts().size() == 1 && be.parts().getFirst().is(ModItems.YARN_BALL), "placed block lost yarn");
        helper.getLevel().destroyBlock(helper.absolutePos(floor.above()), true);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(floor.above())).inflate(1)).stream()
                .anyMatch(e -> e.getItem().is(ModItems.YARN_BALL) && e.getItem().getCount() == 1), "broken yarn did not drop itself");
        helper.succeed();
    }
}

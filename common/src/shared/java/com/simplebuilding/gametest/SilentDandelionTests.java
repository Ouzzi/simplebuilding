package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
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

    public static void configIsBoundedAndCanDisableTheArea(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerTuningConfig parsed = ServerTuning.copyOf("{\"silentDandelion\":{\"radius\":999}} ");
        helper.assertTrue(parsed.silentDandelion.radius == 16, "radius must be capped at 16");
        helper.assertTrue(ServerTuning.copyOf("{\"silentDandelion\":{\"radius\":-9}}").silentDandelion.radius == 1,
                "radius must be at least one");
        helper.assertTrue(ServerTuning.copyOf("{\"silentDandelion\":null}").silentDandelion.radius == 8,
                "missing group must use defaults");
        var config = ServerTuning.local().silentDandelion;
        boolean before = config.enabled;
        try {
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.DIRT);
            helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.SILENT_DANDELION);
            var cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 2));
            config.enabled = false;
            helper.assertTrue(!cow.isSilent(), "disabled flower silenced a mob");
            config.enabled = true;
            helper.assertTrue(cow.isSilent(), "enabled flower did not silence a mob");
            cow.discard();
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        } finally {
            config.enabled = before;
        }
        helper.succeed();
    }

    public static void onlyMobsInsideTheSphereAreSilent(GameTestHelper helper) {
        if (!enabled(helper)) return;
        BlockPos flower = new BlockPos(2, 2, 2);
        helper.setBlock(flower.below(), Blocks.DIRT);
        helper.setBlock(flower, ModBlocks.SILENT_DANDELION);
        var cow = helper.spawn(EntityTypes.COW, flower.east());
        var zombie = helper.spawn(EntityTypes.ZOMBIE, flower.north());
        helper.assertTrue(cow.isSilent() && zombie.isSilent(), "passive and hostile mobs must be silent");
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(flower));
        int radius = ServerTuning.silentDandelionRadius();
        cow.setPos(center.add(radius, 0, 0));
        helper.assertTrue(cow.isSilent(), "exact radius boundary must be included");
        cow.setPos(center.add(radius + 0.01, 0, 0));
        helper.assertTrue(!cow.isSilent(), "leaving the sphere must immediately restore sound");
        cow.setPos(center.add(radius, radius, 0));
        helper.assertTrue(!cow.isSilent(), "radius must be spherical, not a cube");
        cow.setPos(center.add(0, 1, 0));
        helper.assertTrue(cow.isSilent(), "reentering must silence the mob again");
        var stand = helper.spawn(EntityTypes.ARMOR_STAND, flower.east());
        helper.assertTrue(!stand.isSilent(), "non-mobs must remain unaffected");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(center);
        helper.assertTrue(!player.isSilent(), "players must remain unaffected");
        helper.setBlock(flower, Blocks.AIR);
        helper.assertTrue(!cow.isSilent() && !zombie.isSilent(), "removing flower must restore sound immediately");
        cow.discard();
        zombie.discard();
        stand.discard();
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
        helper.assertTrue(cow.isSilent(), "potted flower must have the same aura");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(pot).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(helper.getBlockState(pot).is(Blocks.FLOWER_POT), "empty-hand use must return empty pot");
        helper.assertTrue(!cow.isSilent(), "unpotting must restore sound");
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

    public static void temporarySilenceIsNeverSavedAndExplicitSilenceSurvives(GameTestHelper helper) {
        if (!enabled(helper)) return;
        BlockPos flower = new BlockPos(2, 2, 2);
        helper.setBlock(flower.below(), Blocks.DIRT);
        helper.setBlock(flower, ModBlocks.SILENT_DANDELION);
        var cow = helper.spawn(EntityTypes.COW, flower.east());
        helper.assertTrue(cow.isSilent(), "precondition: cow must be inside area");
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        cow.saveWithoutId(output);
        helper.assertTrue(!output.buildResult().getBooleanOr("Silent", false), "area silence leaked into saved entity data");
        helper.setBlock(flower, Blocks.AIR);
        cow.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(!cow.isSilent(), "reloaded cow stayed silent outside area");
        cow.setSilent(true);
        helper.setBlock(flower, ModBlocks.SILENT_DANDELION);
        output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        cow.saveWithoutId(output);
        helper.assertTrue(output.buildResult().getBooleanOr("Silent", false), "explicit silent flag was lost");
        helper.setBlock(flower, Blocks.AIR);
        helper.assertTrue(cow.isSilent(), "removing flower cleared explicit silence");
        cow.discard();
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

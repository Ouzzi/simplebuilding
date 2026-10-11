package com.simplebuilding.gametest;

import com.simplebuilding.blocks.custom.ThemedFireBlock;
import com.simplebuilding.version.McVersion;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Sulfur fire, copper fire and gunpowder from sulfur (QUEUE N33, claude-fires). */
public final class FireTests {
    public static final int FIRE_MAX_TICKS = 100;

    private FireTests() {
    }

    private static BlockState ignite(GameTestHelper helper, Block base, Block expectedFire) {
        BlockPos basePos = new BlockPos(1, 1, 1);
        BlockPos firePos = basePos.above();
        helper.setBlock(basePos, base);
        BlockState lit = BaseFireBlock.getState(helper.getLevel(), helper.absolutePos(firePos));
        helper.assertTrue(lit.is(expectedFire), "fire above " + base + " is " + lit);
        helper.setBlock(firePos, lit);
        return lit;
    }

    /** Fire lit on sulfur blocks is sulfur fire (light 12, a fire); an entity standing in it gets nausea. */
    public static void sulfurFireBurnsOnSulfurAndGivesNausea(GameTestHelper helper) {
        if (!McVersion.FIRES) {
            helper.succeed();
            return;
        }
        for (Block base : List.of(Blocks.SULFUR, Blocks.POLISHED_SULFUR, Blocks.SULFUR_BRICKS, Blocks.CHISELED_SULFUR, Blocks.POTENT_SULFUR)) {
            helper.setBlock(new BlockPos(1, 1, 1), base);
            helper.assertTrue(BaseFireBlock.getState(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1))).is(ThemedFireBlock.SULFUR_FIRE),
                    "no sulfur fire on " + base);
        }
        BlockState lit = ignite(helper, Blocks.SULFUR, ThemedFireBlock.SULFUR_FIRE);
        helper.assertTrue(lit.is(BlockTags.FIRE), "sulfur fire is not in #minecraft:fire");
        helper.assertValueEqual(lit.getLightEmission(), ThemedFireBlock.LIGHT, "sulfur fire light");
        helper.assertTrue(lit.getLightEmission() < Blocks.FIRE.defaultBlockState().getLightEmission()
                && lit.getLightEmission() > Blocks.SOUL_FIRE.defaultBlockState().getLightEmission(), "light is not between fire and soul fire");
        LivingEntity golem = (LivingEntity) helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(1, 2, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.hasEffect(MobEffects.NAUSEA), "standing in sulfur fire gave no nausea");
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "sulfur fire did no damage");
        });
    }

    /** Copper blocks give copper fire: same damage, but no effect. */
    public static void copperFireBurnsOnCopperWithoutEffect(GameTestHelper helper) {
        if (!McVersion.FIRES) {
            helper.succeed();
            return;
        }
        Block copper = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("copper_block"));
        Block oxidized = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("oxidized_copper"));
        helper.setBlock(new BlockPos(2, 1, 1), oxidized);
        helper.assertTrue(BaseFireBlock.getState(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 1))).is(ThemedFireBlock.COPPER_FIRE),
                "no copper fire on oxidized copper");
        BlockState lit = ignite(helper, copper, ThemedFireBlock.COPPER_FIRE);
        helper.assertTrue(lit.is(BlockTags.FIRE), "copper fire is not in #minecraft:fire");
        LivingEntity golem = (LivingEntity) helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(1, 2, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "copper fire did no damage");
            helper.assertFalse(golem.hasEffect(MobEffects.NAUSEA), "copper fire gave nausea");
        });
    }

    /** Other bases keep their fire (stone: normal fire, soul sand: soul fire); a themed fire dies when its base goes. */
    public static void firesKeepTheirBaseBlocks(GameTestHelper helper) {
        if (!McVersion.FIRES) {
            helper.succeed();
            return;
        }
        BlockPos base = new BlockPos(1, 1, 1);
        BlockPos above = base.above();
        helper.setBlock(base, Blocks.STONE);
        helper.assertTrue(BaseFireBlock.getState(helper.getLevel(), helper.absolutePos(above)).is(Blocks.FIRE), "stone no longer burns with normal fire");
        helper.setBlock(base, Blocks.SOUL_SAND);
        helper.assertTrue(BaseFireBlock.getState(helper.getLevel(), helper.absolutePos(above)).is(Blocks.SOUL_FIRE), "soul sand no longer burns with soul fire");
        helper.setBlock(base, Blocks.SULFUR);
        helper.setBlock(above, ThemedFireBlock.SULFUR_FIRE.defaultBlockState());
        helper.setBlock(base, Blocks.STONE);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(above).isAir(), "sulfur fire stayed after its base was replaced: " + helper.getBlockState(above)));
    }

    /** 3 sulfur + 1 coal or charcoal = 1 gunpowder; fewer sulfur or other fuel craft nothing. */
    public static void gunpowderCraftsFromSulfurAndCoal(GameTestHelper helper) {
        if (!McVersion.FIRES) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        for (var fuel : List.of(Items.COAL, Items.CHARCOAL)) {
            CraftingInput grid = CraftingInput.of(2, 2, List.of(new ItemStack(Items.SULFUR), new ItemStack(Items.SULFUR),
                    new ItemStack(Items.SULFUR), new ItemStack(fuel)));
            Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
            helper.assertTrue(match.isPresent(), "3 sulfur + " + fuel + " craft nothing");
            helper.assertValueEqual(match.get().id().identifier().toString(), "simplebuilding:gunpowder_from_sulfur", "recipe");
            ItemStack out = match.get().value().assemble(grid);
            helper.assertTrue(out.is(Items.GUNPOWDER) && out.getCount() == 1, "result is " + out);
        }
        CraftingInput two = CraftingInput.of(2, 2, List.of(new ItemStack(Items.SULFUR), new ItemStack(Items.SULFUR), new ItemStack(Items.COAL), ItemStack.EMPTY));
        helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, two, level).isEmpty(), "2 sulfur + coal craft something");
        helper.succeed();
    }
}

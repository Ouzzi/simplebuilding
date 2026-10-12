package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.block.EndBudBlock;
import com.simplebuilding.modules.simpleend.block.EndSticksBlock;
import com.simplebuilding.modules.simpleend.mixin.FireBlockInvoker;
import com.simplebuilding.modules.simpleend.world.EndBiomes;
import com.simplebuilding.modules.simpleend.world.EndFlora;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Test bodies shared by the Fabric @GameTest wrappers and the NeoForge/Forge registration (structure simpleend:grove). */
public final class EndTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private EndTests() {}

    private static void add(String name, Consumer<GameTestHelper> body) { ALL.put(name, body); }

    private static final BlockPos BUD = new BlockPos(12, 2, 12);

    private static void ground(GameTestHelper h, BlockPos at, int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) h.setBlock(at.offset(dx, -1, dz), Blocks.END_STONE);
    }

    private static int count(GameTestHelper h, Block block, BlockPos from, BlockPos to) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(from, to)) if (h.getBlockState(p).is(block)) n++;
        return n;
    }

    private static void waitForLight(GameTestHelper h, BlockPos rel, boolean bright, Runnable then) {
        h.succeedWhen(() -> {
            h.assertTrue(EndBudBlock.bright(h.getLevel(), h.absolutePos(rel)) == bright, "light around the bud not settled yet");
            then.run();
        });
    }

    static {
        add("guide_book", com.simplebuilding.modules.simpleend.guide.EndGuide::gameTest);
        add("registered", h -> {
            for (EndWood w : EndWood.ALL) {
                h.assertTrue(w.blockIds.size() == 21, w.name + ": 21 blocks, got " + w.blockIds.size());
                h.assertTrue(w.itemIds.size() == 20, w.name + ": 20 items, got " + w.itemIds.size());
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleEnd.id(w.id("W_boat"))), w.name + " boat entity");
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleEnd.id(w.id("W_chest_boat"))), w.name + " chest boat entity");
                h.assertTrue(BlockEntityTypes.SIGN.isValid(w.sign.defaultBlockState()) && BlockEntityTypes.SIGN.isValid(w.wallSign.defaultBlockState()), w.name + " sign block entity");
                h.assertTrue(BlockEntityTypes.HANGING_SIGN.isValid(w.hangingSign.defaultBlockState()), w.name + " hanging sign block entity");
            }
            h.assertTrue(BuiltInRegistries.FEATURE_TYPE.containsKey(SimpleEnd.id("end_flora")), "feature type end_flora");
            var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
            h.assertTrue(biomes.get(EndBiomes.ASH_VALLEY).isPresent() && biomes.get(EndBiomes.DAWNLANDS).isPresent(), "Ash Valley and Dawnlands biomes");
            h.succeed();
        });
        add("wood_tags_and_recipes", h -> {
            var recipes = h.getLevel().recipeAccess();
            for (EndWood w : EndWood.ALL) {
                h.assertTrue(w.planks.defaultBlockState().is(BlockTags.PLANKS), "planks tag");
                h.assertTrue(w.log.defaultBlockState().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("logs_that_burn"))) && w.log.defaultBlockState().is(BlockTags.LOGS), "logs tags");
                h.assertTrue(w.leaves.defaultBlockState().is(BlockTags.LEAVES), "leaves tag");
                h.assertTrue(new ItemStack(w.boat).is(ItemTags.BOATS) && new ItemStack(w.chestBoat).is(ItemTags.CHEST_BOATS), "boat tags");
                h.assertTrue(new ItemStack(w.itemIds.get(w.id("W_sign"))).is(ItemTags.SIGNS), "sign tag");
                h.assertTrue(new ItemStack(w.log).is(EndRegistry.VOID_SAFE), "void-safe tag");
                for (String r : new String[] {"W_planks", "W_wood", "stripped_W_wood", "W_stairs", "W_slab", "W_fence", "W_fence_gate", "W_door",
                        "W_trapdoor", "W_button", "W_pressure_plate", "W_sign", "W_hanging_sign", "W_boat", "W_chest_boat"}) {
                    ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = ResourceKey.create(Registries.RECIPE, SimpleEnd.id(w.id(r)));
                    h.assertTrue(recipes.byKey(key).isPresent(), "recipe " + key.identifier());
                }
            }
            h.succeed();
        });
        add("axe_strips_and_wood_burns", h -> {
            FireBlockInvoker fire = (FireBlockInvoker) Blocks.FIRE;
            for (EndWood w : EndWood.ALL) {
                BlockPos at = new BlockPos(2 + EndWood.ALL.indexOf(w) * 2, 2, 2);
                h.setBlock(at, w.log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
                Player p = h.makeMockPlayer(GameType.SURVIVAL);
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                BlockPos abs = h.absolutePos(at);
                Items.IRON_AXE.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
                BlockState now = h.getBlockState(at);
                h.assertTrue(now.is(w.strippedLog) && now.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, w.name + ": axe strips the log, got " + now);
                for (Block b : new Block[] {w.log, w.planks, w.leaves, w.sticks}) h.assertTrue(fire.simpleend$canBurn(b.defaultBlockState()), BuiltInRegistries.BLOCK.getKey(b) + " burns");
            }
            h.assertTrue(fire.simpleend$canBurn(EndRegistry.END_GRASS.defaultBlockState()), "End grass burns");
            h.succeed();
        });
        add("bud_light_grows_tree", h -> {
            EndWood w = EndWood.ASHWOOD;
            ground(h, BUD, 3);
            h.setBlock(BUD, w.bud);
            h.setBlock(BUD.offset(2, 0, 0), Blocks.GLOWSTONE);
            waitForLight(h, BUD, true, () -> {
                boolean grown = ((EndBudBlock) w.bud).grow(h.getLevel(), h.absolutePos(BUD), h.getLevel().getRandom());
                h.assertTrue(grown, "a lit bud grows");
                h.assertTrue(h.getBlockState(BUD).is(w.log) && h.getBlockState(BUD.above(7)).is(w.log), "trunk of at least 8 logs");
                h.assertTrue(count(h, w.leaves, BUD.offset(-7, 0, -7), BUD.offset(7, 16, 7)) > 20, "leaf clusters");
                h.assertTrue(count(h, w.sticks, BUD.offset(-3, 0, -3), BUD.offset(3, 14, 3)) == 0, "no scrub in the light");
            });
        });
        add("bud_dark_grows_scrub", h -> {
            EndWood w = EndWood.DUSKWOOD;
            ground(h, BUD, 3);
            for (BlockPos p : BlockPos.betweenClosed(BUD.offset(-2, -1, -2), BUD.offset(2, 15, 2))) {
                BlockPos r = p.immutable();
                boolean shell = Math.abs(r.getX() - BUD.getX()) == 2 || Math.abs(r.getZ() - BUD.getZ()) == 2 || r.getY() == BUD.getY() + 15;
                if (shell) h.setBlock(r, Blocks.STONE);
            }
            h.setBlock(BUD, w.bud);
            waitForLight(h, BUD, false, () -> {
                BlockState before = h.getBlockState(BUD);
                for (int i = 0; i < 60; i++) before.randomTick(h.getLevel(), h.absolutePos(BUD), h.getLevel().getRandom());
                h.assertTrue(h.getBlockState(BUD).is(w.bud), "a dark bud never grows by itself");
                boolean grown = ((EndBudBlock) w.bud).grow(h.getLevel(), h.absolutePos(BUD), h.getLevel().getRandom());
                h.assertTrue(grown && h.getBlockState(BUD).is(w.sticks), "bone meal in the dark gives End sticks");
                h.assertTrue(h.getBlockState(BUD).getValue(EndSticksBlock.THICKNESS) == 3, "thick base");
                h.assertTrue(count(h, w.log, BUD.offset(-1, 0, -1), BUD.offset(1, 14, 1)) == 0, "no tree in the dark");
            });
        });
        add("bud_square_grows_huge_tree", h -> {
            EndWood w = EndWood.ASHWOOD;
            ground(h, BUD, 4);
            for (BlockPos p : new BlockPos[] {BUD, BUD.east(), BUD.south(), BUD.south().east()}) h.setBlock(p, w.bud);
            h.setBlock(BUD.offset(-2, 0, 0), Blocks.GLOWSTONE);
            waitForLight(h, BUD, true, () -> {
                boolean grown = ((EndBudBlock) w.bud).grow(h.getLevel(), h.absolutePos(BUD.south().east()), h.getLevel().getRandom());
                h.assertTrue(grown, "2x2 buds grow");
                for (BlockPos p : new BlockPos[] {BUD, BUD.east(), BUD.south(), BUD.south().east()})
                    h.assertTrue(h.getBlockState(p.above(12)).is(w.log), "2x2 trunk at least 13 high at " + p);
            });
        });
        add("sticks_taper_like_dripstone", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = new BlockPos(3, 1, 3);
            h.setBlock(at.below(), Blocks.END_STONE);
            for (int i = 0; i < 4; i++) {
                BlockPos p = h.absolutePos(at.above(i));
                ServerLevel level = h.getLevel();
                level.setBlockAndUpdate(p, ((EndSticksBlock) w.sticks).shaped(level.getBlockState(p.above())));
            }
            h.assertTrue(h.getBlockState(at.above(3)).getValue(EndSticksBlock.THICKNESS) == 1, "tip on top");
            h.assertTrue(h.getBlockState(at.above(2)).getValue(EndSticksBlock.THICKNESS) == 2, "middle under the tip");
            h.assertTrue(h.getBlockState(at).getValue(EndSticksBlock.THICKNESS) == 3, "thick base");
            h.succeed();
        });
        add("natural_tree_feature", h -> {
            BlockPos at = new BlockPos(12, 2, 12);
            ground(h, at, 3);
            boolean placed = new EndFlora("natural_tree", "duskwood").place(h.getLevel(), null, h.getLevel().getRandom(), h.absolutePos(at));
            h.assertTrue(placed && h.getBlockState(at).is(EndWood.DUSKWOOD.log) && h.getBlockState(at.above(3)).is(EndWood.DUSKWOOD.log), "narrow natural tree");
            h.assertTrue(count(h, EndWood.DUSKWOOD.log, at.offset(-3, 0, -3), at.offset(3, 12, 3)) == count(h, EndWood.DUSKWOOD.log, at.offset(-12, 0, -12), at.offset(12, 20, 12)), "stays narrow (within 3 blocks)");
            h.succeed();
        });
        add("leaves_drop_bud_sticks_fruit", h -> {
            EndWood w = EndWood.ASHWOOD;
            BlockPos at = new BlockPos(5, 2, 5);
            h.setBlock(at, w.leaves);
            BlockPos abs = h.absolutePos(at);
            boolean bud = false, stick = false, fruit = false;
            for (int i = 0; i < 3000 && !(bud && stick && fruit); i++) {
                for (ItemStack s : Block.getDrops(h.getLevel().getBlockState(abs), h.getLevel(), abs, null)) {
                    bud |= s.is(w.bud.asItem());
                    stick |= s.is(Items.STICK);
                    fruit |= s.is(EndRegistry.DRIFT_FRUIT);
                }
            }
            h.assertTrue(bud && stick && fruit, "leaves drop bud " + bud + ", sticks " + stick + ", Drift Fruit " + fruit);
            h.succeed();
        });
        add("drift_fruit_glide", h -> {
            Player p = h.makeMockPlayer(GameType.SURVIVAL);
            BlockPos abs = h.absolutePos(new BlockPos(4, 2, 4));
            p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
            p.setDeltaMovement(Vec3.ZERO);
            ItemStack fruit = new ItemStack(EndRegistry.DRIFT_FRUIT);
            fruit.finishUsingItem(h.getLevel(), p);
            h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling after eating");
            int t = p.getEffect(MobEffects.SLOW_FALLING).getDuration();
            h.assertTrue(t >= 60 && t <= 100, "3-5 s of Slow Falling, got " + t + " ticks");
            h.assertTrue(p.getDeltaMovement().y > 0.4, "small hop up");
            h.succeed();
        });
        add("void_safe_items", h -> {
            ServerLevel level = h.getLevel();
            BlockPos abs = h.absolutePos(new BlockPos(6, 2, 6));
            ItemEntity item = new ItemEntity(level, abs.getX() + 0.5, level.getMinY() - 6, abs.getZ() + 0.5, new ItemStack(EndWood.ASHWOOD.log));
            item.setDeltaMovement(0, -0.5, 0);
            level.addFreshEntity(item);
            h.succeedWhen(() -> {
                h.assertTrue(item.isAlive(), "item survives the void");
                h.assertTrue(item.getAge() < 0, "despawn timer doubled (age starts at -6000), age " + item.getAge());
                // Rising from 6 below the bottom; in the test world bedrock at minY may stop it from below.
                h.assertTrue(item.isNoGravity() && item.getY() > level.getMinY() - 2, "floats back up to the world bottom, y " + item.getY());
                item.discard();
            });
        });
        add("biome_patches", h -> {
            EndBiomes.Noise flat = (x, z) -> 0.1;
            int some = 0;
            for (int x = 0; x < EndBiomes.CELL * 6; x++)
                for (int z = 0; z < EndBiomes.CELL * 6; z++) {
                    h.assertTrue(EndBiomes.pick(x, z, flat, 0.0) == EndBiomes.NONE, "chance 0 means no patch");
                    if (EndBiomes.pick(x, z, flat, 1.0) != EndBiomes.NONE) some++;
                    h.assertTrue(EndBiomes.pick(x, z, flat, 1.0) == EndBiomes.pick(x, z, flat, 1.0), "deterministic");
                }
            // 36 cells, each patch 3-6 chunks across (area ~7..29 chunks)
            h.assertTrue(some >= 36 * 5 && some <= 36 * 32, "patch area per cell, got " + some + " chunks in 36 cells");
            var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
            TheEndBiomeSource source = TheEndBiomeSource.create(biomes);
            boolean ash = source.possibleBiomes().stream().anyMatch(b -> b.is(EndBiomes.ASH_VALLEY));
            boolean dawn = source.possibleBiomes().stream().anyMatch(b -> b.is(EndBiomes.DAWNLANDS));
            h.assertTrue(ash && dawn, "End biome source lists Ash Valley and Dawnlands");
            h.succeed();
        });
        add("submod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleEnd.ID);
            try {
                com.simplebuilding.framework.api.SubMods.set(SimpleEnd.ID, false);
                SimpleEnd.applySubSwitches();
                h.assertTrue(!SimpleEnd.on(), "Simple End off");
                h.assertTrue(!com.simplebuilding.framework.api.SubMods.enabled(SimpleEnd.DRAGON_FIGHT), "nested Dragon Fight follows Simple End");
                com.simplebuilding.framework.api.SubMods.set(SimpleEnd.ID, true);
                SimpleEnd.applySubSwitches();
                h.assertTrue(com.simplebuilding.framework.api.SubMods.enabled(SimpleEnd.DRAGON_FIGHT) == SimpleEndConfig.enableSimpleEndDragonFight, "Dragon Fight back on");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleEnd.ID, before);
                SimpleEnd.applySubSwitches();
            }
            h.assertTrue(SimpleEndConfig.KEYS.size() == 7, "config keys");
            h.succeed();
        });
        add("nibblemite_nibbles_end_stone", h -> {
            BlockPos at = new BlockPos(6, 2, 6);
            h.setBlock(at.below(), Blocks.END_STONE);
            var mite = EndRegistry.NIBBLEMITE.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            h.assertTrue(mite != null, "nibblemite entity");
            BlockPos abs = h.absolutePos(at);
            mite.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
            mite.setNoAi(true);
            h.getLevel().addFreshEntity(mite);
            h.assertTrue(mite.nibble(), "nibbles the End stone below");
            h.assertTrue(h.getBlockState(at.below()).is(EndRegistry.BURROWED_END_STONE), "End stone became Burrowed End Stone");
            h.assertTrue(!mite.nibble(), "burrowed stone is not nibbled again");
            h.assertTrue(com.simplebuilding.modules.simpleend.entity.Nibblemite.isEndGround(h.getBlockState(at.below())), "burrowed stone still counts as End ground");
            mite.discard();
            h.succeed();
        });
        add("burrowed_end_stone_recovers", h -> {
            BlockPos at = new BlockPos(7, 2, 7);
            h.setBlock(at, EndRegistry.BURROWED_END_STONE);
            for (int i = 0; i < 200 && h.getBlockState(at).is(EndRegistry.BURROWED_END_STONE); i++)
                h.getBlockState(at).randomTick(h.getLevel(), h.absolutePos(at), h.getLevel().getRandom());
            h.assertTrue(h.getBlockState(at).is(Blocks.END_STONE), "Burrowed End Stone recovers to End stone");
            h.assertTrue(EndRegistry.BURROWED_END_STONE.defaultBlockState().isRandomlyTicking(), "recovers on random ticks");
            h.succeed();
        });
        add("nibblemite_is_passive", h -> {
            var mite = EndRegistry.NIBBLEMITE.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            h.assertTrue(mite != null, "nibblemite entity");
            h.assertTrue(!((Object) mite instanceof net.minecraft.world.entity.monster.Enemy), "not an enemy");
            h.assertTrue(!((Object) mite instanceof net.minecraft.world.entity.monster.Endermite), "not an Endermite (Endermen ignore it)");
            h.assertTrue(EndRegistry.NIBBLEMITE.getCategory() == net.minecraft.world.entity.MobCategory.CREATURE, "creature category");
            h.assertTrue(mite.isFood(new ItemStack(Items.CHORUS_FRUIT)) && !mite.isFood(new ItemStack(Items.WHEAT)), "fed with chorus fruit");
            h.assertTrue(!mite.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "no attack damage");
            h.succeed();
        });
        add("end_grass_on_end_stone", h -> {
            BlockPos at = new BlockPos(8, 2, 8);
            h.setBlock(at.below(), Blocks.END_STONE);
            h.assertTrue(EndRegistry.END_GRASS.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(at)), "End grass stays on End stone");
            h.assertTrue(EndWood.ASHWOOD.bud.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(at)), "bud stays on End stone");
            h.setBlock(at.below(), Blocks.STONE);
            h.assertTrue(!EndRegistry.END_GRASS.defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(at)), "not on stone");
            h.succeed();
        });
    }
}

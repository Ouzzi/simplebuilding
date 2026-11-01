package com.simplebuilding.modules.simplemobs;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;

/** Spawner Fragment, Chunk and Makeshift Spawner (WF25-6; Fabric wrappers in ModuleGameTest). Standalone-safe: Vanilla items only. */
final class SpawnerPartsTests {
    private SpawnerPartsTests() {}

    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    @SuppressWarnings("unchecked")
    private static ItemStack craft(GameTestHelper h, String recipe, ItemStack ingredient) {
        var holder = h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, recipe)))
                .orElseThrow(() -> new IllegalStateException("missing recipe " + recipe));
        var r = (Recipe<CraftingInput>) holder.value();
        var input = CraftingInput.of(3, 3, java.util.Collections.nCopies(9, ingredient));
        check(r.matches(input, h.getLevel()), "recipe matches " + recipe);
        return r.assemble(input);
    }

    private static int fragmentsAround(GameTestHelper h, BlockPos pos) {
        List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0));
        int n = 0;
        for (ItemEntity e : items) {
            if (e.getItem().is(SpawnerParts.FRAGMENT)) n += e.getItem().getCount();
            e.discard();
        }
        return n;
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("spawner_parts_registered", h -> {
            check(BuiltInRegistries.ITEM.getKey(SpawnerParts.FRAGMENT).equals(Identifier.fromNamespaceAndPath(MobsRegistry.ID, "spawner_fragment")), "fragment registered");
            check(BuiltInRegistries.ITEM.getKey(SpawnerParts.CHUNK).equals(Identifier.fromNamespaceAndPath(MobsRegistry.ID, "spawner_chunk")), "chunk registered");
            check(BuiltInRegistries.BLOCK.getKey(SpawnerParts.MAKESHIFT_SPAWNER).equals(Identifier.fromNamespaceAndPath(MobsRegistry.ID, "makeshift_spawner"))
                    && SpawnerParts.MAKESHIFT_SPAWNER_ITEM.getDefaultInstance().getItem() == SpawnerParts.MAKESHIFT_SPAWNER_ITEM, "makeshift spawner block and item registered");
            ItemStack chunk = craft(h, "spawner_chunk", new ItemStack(SpawnerParts.FRAGMENT));
            check(chunk.is(SpawnerParts.CHUNK) && chunk.getCount() == 1, "9 fragments make 1 chunk");
            ItemStack spawner = craft(h, "makeshift_spawner", new ItemStack(SpawnerParts.CHUNK));
            check(spawner.is(SpawnerParts.MAKESHIFT_SPAWNER_ITEM) && spawner.getCount() == 1, "9 chunks make 1 makeshift spawner");
            h.succeed();
        });
        add.accept("spawner_makeshift_drops_itself", h -> {
            var level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, SpawnerParts.MAKESHIFT_SPAWNER.defaultBlockState(), 3);
            var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos), null, new ItemStack(Items.IRON_PICKAXE));
            int n = 0;
            for (ItemStack d : drops) if (d.is(SpawnerParts.MAKESHIFT_SPAWNER_ITEM)) n += d.getCount();
            check(n == 1, "breaking the makeshift spawner drops exactly itself (loot table in 26 format), got " + n);
            h.succeed();
        });
        add.accept("spawner_makeshift_place", h -> {
            var level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, SpawnerParts.MAKESHIFT_SPAWNER.defaultBlockState(), 3);
            check(level.getBlockEntity(pos) instanceof SpawnerBlockEntity, "the placed block has a spawner block entity");
            check(level.getBlockState(pos).getTicker(level, net.minecraft.world.level.block.entity.BlockEntityTypes.MOB_SPAWNER) != null, "it has a server ticker");
            // empty without a spawn egg: ticking it next to a player spawns nothing
            var player = h.makeMockServerPlayerInLevel();
            player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            var be = (SpawnerBlockEntity) level.getBlockEntity(pos);
            for (int i = 0; i < 40; i++) SpawnerBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
            check(level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(8.0)).isEmpty(), "without a spawn egg it stays empty");
            // slowdown: every nth tick runs
            int old = MobsConfig.makeshiftSpawnerSlowdown;
            try {
                MobsConfig.makeshiftSpawnerSlowdown = 3;
                int runs = 0;
                for (long t = 0; t < 30; t++) if (MakeshiftSpawnerBlock.runsOn(t)) runs++;
                check(runs == 10, "slowdown 3: 10 of 30 ticks run, got " + runs);
                MobsConfig.makeshiftSpawnerSlowdown = 1;
                runs = 0;
                for (long t = 0; t < 30; t++) if (MakeshiftSpawnerBlock.runsOn(t)) runs++;
                check(runs == 30, "slowdown 1: every tick runs");
            } finally {
                MobsConfig.makeshiftSpawnerSlowdown = old;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            h.succeed();
        });
        add.accept("spawner_fragment_roll", h -> {
            var level = h.getLevel();
            ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
            ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
            silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            RandomSource r = RandomSource.create(42L);
            check(!SpawnerParts.roll(r, iron, 0.0f), "chance 0 never drops");
            check(SpawnerParts.roll(r, iron, 1.0f), "chance 1 always drops with a pickaxe");
            check(!SpawnerParts.roll(r, ItemStack.EMPTY, 1.0f) && !SpawnerParts.roll(r, new ItemStack(Items.IRON_AXE), 1.0f), "no pickaxe, no fragment");
            check(!SpawnerParts.roll(r, silk, 1.0f), "Silk Touch chips nothing off");
            int hits = 0;
            for (int i = 0; i < 10000; i++) if (SpawnerParts.roll(r, iron, 0.3f)) hits++;
            check(hits > 2800 && hits < 3200, "about 30 % of 10000 rolls: " + hits);
            check(MobsConfig.spawnerFragmentChance == 0.3f, "default chance 30 %");
            h.succeed();
        });
        add.accept("spawner_fragment_break", h -> {
            var level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            float old = MobsConfig.spawnerFragmentChance;
            try {
                level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 3);
                var state = level.getBlockState(pos);
                ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
                MobsConfig.spawnerFragmentChance = 1.0f;
                state.spawnAfterBreak(level, pos, pick, false);
                check(fragmentsAround(h, pos) == 1, "a mined Vanilla spawner drops one fragment (chance 1)");
                state.spawnAfterBreak(level, pos, ItemStack.EMPTY, false);
                check(fragmentsAround(h, pos) == 0, "bare hands / explosions drop none");
                MobsConfig.spawnerFragmentChance = 0.0f;
                state.spawnAfterBreak(level, pos, pick, false);
                check(fragmentsAround(h, pos) == 0, "chance 0 drops none");
                MobsConfig.spawnerFragmentChance = 1.0f;
                level.setBlock(pos, SpawnerParts.MAKESHIFT_SPAWNER.defaultBlockState(), 3);
                level.getBlockState(pos).spawnAfterBreak(level, pos, pick, false);
                check(fragmentsAround(h, pos) == 0, "the makeshift spawner itself yields no fragment (no loop)");
            } finally {
                MobsConfig.spawnerFragmentChance = old;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            h.succeed();
        });
    }
}

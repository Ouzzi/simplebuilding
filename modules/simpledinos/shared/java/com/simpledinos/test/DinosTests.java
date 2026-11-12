package com.simpledinos.test;

import com.simpledinos.DinoKind;
import com.simpledinos.DinoVariants;
import com.simpledinos.DinosRegistry;
import com.simpledinos.Progress;
import com.simpledinos.SimpleDinos;
import com.simpledinos.block.DinoEggBlock;
import com.simpledinos.block.DinoEggBlockEntity;
import com.simpledinos.block.FossilBlock;
import com.simpledinos.block.FossilBlockEntity;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.Dino;
import com.simpledinos.entity.Trex;
import com.simpledinos.entity.Triceratops;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Module GameTests (concept WP-18 section 3, owner choice ABN29b-4). All on the 16 x 8 x 16 stone arena
 * ({@code simpledinos:arena}). Time is passed in as game-time stamps, so a minute of brushing or five minutes of brooding run
 * in a few calls: the progress, the rollback, the stages, the hatching and the growth are tested through the blocks' own steps.
 */
public final class DinosTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();

    private static BlockPos at(int x, int z) {
        return new BlockPos(x, 1, z);
    }

    private static ServerPlayer player(GameTestHelper h, int x, int z) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(p));
        Vec3 pos = h.absoluteVec(new Vec3(x + 0.5, 1.0, z + 0.5));
        p.setPos(pos.x, pos.y, pos.z);
        return p;
    }

    private static Holder<Biome> biome(GameTestHelper h, ResourceKey<Biome> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
    }

    /** One coat at home in one biome: nobody else is, the weighted pick favours it and every other coat still appears now and then. */
    private static void checkHome(GameTestHelper h, int coat, Holder<Biome> biome, String what) {
        h.assertTrue(DinoVariants.atHome(coat, biome), what + " is at home");
        for (int other = 0; other < DinoVariants.COATS.size(); other++) {
            if (other != coat) h.assertTrue(!DinoVariants.atHome(other, biome), what + ": coat " + other + " is not at home too");
        }
        RandomSource random = RandomSource.create(777L + coat);
        int[] count = new int[DinoVariants.COATS.size()];
        for (int i = 0; i < 1000; i++) count[DinoVariants.pick(biome, random)]++;
        h.assertTrue(count[coat] * 100 >= 1000 * 55, what + ": the home coat is the most common (" + count[coat] + " of 1000)");
        for (int i = 0; i < count.length; i++) h.assertTrue(count[i] > 0, what + ": coat " + i + " still appears now and then");
    }

    private static FossilBlockEntity fossil(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, DinosRegistry.FOSSIL);
        return (FossilBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
    }

    /** Broods the egg with {@code sitter} as the only sitter (time as game-time stamps). */
    private static void brood(ServerLevel level, DinoEggBlockEntity be, UUID sitter) {
        long t = 3000;
        be.advance(level, t, null);
        while (!be.brooded() && t < 3000 + 8000) be.advance(level, t += 40, sitter);
    }

    private static DinoEggBlockEntity egg(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, DinosRegistry.EGGS.get(DinoKind.TREX));
        return (DinoEggBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
    }

    private static Trex adult(GameTestHelper h, ServerPlayer owner, int x, int z) {
        Trex trex = h.spawn(DinosRegistry.TREX, at(x, z));
        trex.setPersistenceRequired();
        trex.setTamed(true);
        trex.setOwner(owner);
        trex.setAge(0);
        return trex;
    }

    /** a-dino36b: an adult T. rex without AI at a block, body and head turned to +z (yaw 0), so its head and tail boxes stay put. */
    private static Trex standing(GameTestHelper h, int x, int z) {
        Trex trex = h.spawn(DinosRegistry.TREX, at(x, z));
        trex.setPersistenceRequired();
        trex.setTamed(true);
        trex.setAge(0);
        trex.setNoAi(true);
        Vec3 p = trex.position();
        trex.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
        trex.setYBodyRot(0.0F);
        trex.setYHeadRot(0.0F);
        return trex;
    }

    /** A wall of {@code block} across x 5..11 at row z, from the floor up to y 5 (the head of an adult reaches y 4.1..5.04 here). */
    private static void wall(GameTestHelper h, int z, net.minecraft.world.level.block.Block block) {
        for (int x = 5; x <= 11; x++) for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(x, y, z), block);
    }

    /** a-dinorest1: an adult Triceratops without AI, facing +z (yaw 0), tame. */
    private static Triceratops standingTri(GameTestHelper h, int x, int z) {
        Triceratops tri = h.spawn(DinosRegistry.TRICERATOPS, at(x, z));
        tri.setPersistenceRequired();
        tri.setTamed(true);
        tri.setAge(0);
        tri.setNoAi(true);
        Vec3 p = tri.position();
        tri.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
        tri.setYBodyRot(0.0F);
        tri.setYHeadRot(0.0F);
        return tri;
    }

    /**
     * A small tree: a log column at (x, 1..4, z) and a 5 x 5 crown of leaves in two layers (y 4..5); natural leaves are not persistent
     * (as grown), built ones are. Returns the log the charge hits (y 2).
     */
    private static BlockPos tree(GameTestHelper h, int x, int z, Block log, Block leaves, boolean natural) {
        for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), log);
        BlockState leaf = leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, !natural).setValue(LeavesBlock.DISTANCE, 1);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = 4; y <= 5; y++) if (!(dx == 0 && dz == 0 && y == 4)) h.setBlock(new BlockPos(x + dx, y, z + dz), leaf);
            }
        }
        return new BlockPos(x, 2, z);
    }

    private static List<ItemEntity> items(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 8, 16));
    }

    private static List<ItemEntity> itemsNear(GameTestHelper h, BlockPos rel, double r) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(r));
    }

    /** No box touches a block shape (independent of the dinosaur's own rule: the Vanilla collision check). */
    private static boolean clearOfBlocks(GameTestHelper h, net.minecraft.world.entity.Entity entity, List<AABB> boxes) {
        for (AABB box : boxes) if (!h.getLevel().noBlockCollision(entity, box.deflate(0.02))) return false;
        return true;
    }

    static {
        ALL.put("registered", h -> {
            for (String id : new String[] {"fossil_block", "trex_egg", "triceratops_egg", "stegosaurus_egg", "utahraptor_egg", "parasaurolophus_egg"}) {
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleDinos.id(id)), "block " + id);
            }
            for (String id : new String[] {"fossil_block", "trex_egg", "triceratops_egg", "stegosaurus_egg", "utahraptor_egg", "parasaurolophus_egg",
                    "large_saddle", "trex_armor_leather", "trex_armor_iron", "trex_armor_diamond", "guide_book",
                    "triceratops_armor_leather", "triceratops_armor_iron", "triceratops_armor_diamond", "triceratops_eggshell",
                    "stegosaurus_eggshell", "utahraptor_eggshell", "parasaurolophus_eggshell"}) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleDinos.id(id)), "item " + id);
            }
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("trex")), "entity trex");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("triceratops")), "entity triceratops");
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("triceratops_spawn_egg")) && !BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("trex_eggshell")),
                    "no spawn egg, no T. rex eggshell (concept DINO36 6)");
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("trex_spawn_egg")), "no spawn egg (owner)");
            h.assertTrue(DinosRegistry.FOSSIL_BE != null && DinosRegistry.EGG_BE != null, "block entity types");
            h.assertTrue(DefaultAttributes.hasSupplier(DinosRegistry.TREX) && DefaultAttributes.hasSupplier(DinosRegistry.TRICERATOPS), "attributes");
            h.assertTrue(DinosConfig.enabled && DinosConfig.BRUSH_DEFAULT == 45 && DinosConfig.incubateSeconds == 300 && DinosConfig.hatchSeconds == 600
                    && DinosConfig.growMinutes == 40 && DinosConfig.trexBiteDamage == 10.0, "config defaults = concept times");
            h.assertTrue(DinosConfig.triceratopsShake && DinosConfig.shakeShare == 0.5 && DinosConfig.shakeAppleChance == 0.35
                    && DinosConfig.shakeTreeCooldownSeconds == 600 && DinosConfig.ramCooldownSeconds == 3, "Triceratops defaults = concept 4.1");
            h.assertTrue(DinoKind.TREX.hatchable() && DinoKind.TRICERATOPS.hatchable() && !DinoKind.STEGOSAURUS.hatchable()
                    && !DinoKind.UTAHRAPTOR.hatchable() && !DinoKind.PARASAUROLOPHUS.hatchable() && DinoKind.hatchableKinds().size() == 2,
                    "DINO-REST step 1: T. rex and Triceratops hatch");
            h.assertTrue(DinoKind.TREX.eggshell() == null && DinoKind.TRICERATOPS.eggshell() == DinosRegistry.eggshell(DinoKind.TRICERATOPS)
                    && DinoKind.PARASAUROLOPHUS.eggshell() != null, "an eggshell for every kind but the T. rex");
            h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                    .get(ResourceKey.create(Registries.PLACED_FEATURE, SimpleDinos.id("fossil_block"))).isPresent(), "worldgen feature");
            h.succeed();
        });
        ALL.put("config_caps", h -> {
            try {
                DinosConfig.apply(com.google.gson.JsonParser.parseString("{\"brushSeconds\":99999,\"incubateSeconds\":0,\"hatchSeconds\":\"x\",\"growMinutes\":-5,\"trexBiteDamage\":1000,"
                        + "\"shakeShare\":7,\"shakeAppleChance\":-1,\"shakeTreeCooldownSeconds\":1,\"ramCooldownSeconds\":999,\"triceratopsShake\":\"no\"}").getAsJsonObject());
                h.assertTrue(DinosConfig.shakeShare == 1.0 && DinosConfig.shakeAppleChance == 0.0 && DinosConfig.shakeTreeCooldownSeconds == DinosConfig.TREE_REST_MIN
                        && DinosConfig.ramCooldownSeconds == DinosConfig.RAM_COOLDOWN_MAX && DinosConfig.triceratopsShake, "Triceratops keys clamped, garbage = default");
                h.assertTrue(DinosConfig.brushSeconds == DinosConfig.BRUSH_MAX && DinosConfig.incubateSeconds == DinosConfig.INCUBATE_MIN
                        && DinosConfig.hatchSeconds == DinosConfig.HATCH_DEFAULT && DinosConfig.growMinutes == DinosConfig.GROW_MIN
                        && DinosConfig.trexBiteDamage == DinosConfig.BITE_MAX, "clamps, garbage = default");
                h.assertTrue(DinosConfig.KEYS.equals(DinosConfig.toJson().keySet().stream().toList()), "KEYS lists the file's keys in order");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleDinos.MOD_ID);
            try {
                h.assertTrue(SimpleDinos.on() && !DinosRegistry.tabStacks().isEmpty(), "on: creative entries");
                com.simplebuilding.framework.api.SubMods.set(SimpleDinos.MOD_ID, false);
                h.assertTrue(!SimpleDinos.on() && DinosRegistry.tabStacks().isEmpty(), "off: creative entries hidden");
                // the mechanics keep working while the module is hidden
                FossilBlockEntity be = fossil(h, at(3, 3));
                be.advance(h.getLevel(), 100L, true);
                h.assertTrue(be.progress() == 1, "off: a fossil still takes brushing");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleDinos.MOD_ID, before);
                DinosConfig.reset();
            }
            h.succeed();
        });

        // --- progress rule (pure) ----------------------------------------------------------------------------------------------
        ALL.put("progress_rewinds_double_speed", h -> {
            Progress p = new Progress(5);
            for (int t = 1; t <= 600; t++) p.add(t, 1, 1200);
            h.assertTrue(p.value() == 600, "600 ticks of work = 600");
            p.settle(605);
            h.assertTrue(p.value() == 600, "a pause inside the grace of 5 ticks costs nothing");
            p.settle(705);
            h.assertTrue(p.value() == 600 - 2 * 100, "a pause of 100 ticks after the grace: 2 progress per tick = 400");
            p.settle(705);
            h.assertTrue(p.value() == 400, "settling twice for the same time changes nothing");
            p.settle(100_000);
            h.assertTrue(p.value() == 0, "never below zero");
            Progress capped = new Progress(5);
            capped.add(1, 5000, 1200);
            h.assertTrue(capped.value() == 1200, "capped at the maximum");
            Progress resume = new Progress(5);
            for (int t = 1; t <= 100; t++) resume.add(t, 1, 1200);
            resume.add(200, 1, 1200); // 100 ticks pause: 100 - 2 * (100 - 5) = 0 left, then +1
            h.assertTrue(resume.value() == 1, "work after a long pause starts from what is left");
            Progress chunks = new Progress(10);
            for (int i = 1; i <= 10; i++) chunks.add(i * 40L, 40, 6000);
            h.assertTrue(chunks.value() == 400, "work credited in chunks longer than the grace is not rewound (" + chunks.value() + ")");
            h.succeed();
        });

        // --- fossil block -----------------------------------------------------------------------------------------------------
        ALL.put("fossil_brushing_stages_and_rewind", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            FossilBlockEntity be = fossil(h, pos);
            int total = DinosConfig.brushTicks();
            h.assertTrue(total == 900, "45 s = 75 % of the former minute");
            int seen = 0;
            long t = 1000;
            for (int i = 0; i < total / 2; i++) be.advance(level, t++, true);
            int stage = h.getBlockState(pos).getValue(FossilBlock.STAGE);
            h.assertTrue(be.progress() == total / 2 && stage == 5, "half brushed = stage 5 (progress " + be.progress() + ", stage " + stage + ")");
            be.advance(level, t + 100, false); // a pause of 100 ticks
            int after = be.progress();
            h.assertTrue(after == total / 2 - 2 * (100 - FossilBlockEntity.GRACE + 1) || after == total / 2 - 2 * (100 - FossilBlockEntity.GRACE),
                    "1 s of pause takes 2 s of progress away: " + after);
            h.assertTrue(h.getBlockState(pos).getValue(FossilBlock.STAGE) <= stage, "the stage goes back with it");
            be.advance(level, t + 100_000, false);
            h.assertTrue(be.progress() == 0 && h.getBlockState(pos).getValue(FossilBlock.STAGE) == 0, "a long pause empties it");
            h.assertTrue(h.getBlockState(pos).is(DinosRegistry.FOSSIL), "and the block is still a fossil (no reroll, nothing lost)");
            h.succeed();
        });
        ALL.put("fossil_first_frame_within_one_second", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            FossilBlockEntity be = fossil(h, pos);
            long t = 5000;
            for (int i = 0; i < 14; i++) be.advance(level, t++, true);
            h.assertTrue(h.getBlockState(pos).getValue(FossilBlock.STAGE) == 0, "14 ticks: still stage 0");
            be.advance(level, t++, true);
            h.assertTrue(h.getBlockState(pos).getValue(FossilBlock.STAGE) == 1, "15 ticks (< 20): first frame change");
            h.assertTrue(FossilBlockEntity.stageFor(100, 100) == FossilBlock.STAGES - 1 && FossilBlockEntity.stageFor(0, 100) == 0, "short brush time still maps 0..9");
            be.advance(level, t + 100_000, false);
            h.succeed();
        });
        ALL.put("fossil_finish_drops_egg_of_its_kind", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(6, 6);
            FossilBlockEntity be = fossil(h, pos);
            DinoKind kind = be.kind();
            h.assertTrue(kind.hatchable(), "only built dinosaurs are rolled (" + kind + ")");
            be.progressState().set(DinosConfig.brushTicks() - 1, 5000L, 5000L);
            be.advance(level, 5001L, true);
            h.assertTrue(h.getBlockState(pos).is(Blocks.SANDSTONE), "the fossil turns into sandstone");
            AABB box = new AABB(h.absolutePos(pos)).inflate(2.0);
            boolean egg = level.getEntitiesOfClass(ItemEntity.class, box).stream().anyMatch(e -> e.getItem().is(DinosRegistry.eggItem(kind)));
            h.assertTrue(egg, "the egg of the rolled kind lies there");
            h.succeed();
        });
        ALL.put("fossil_kind_is_stable_per_position", h -> {
            FossilBlockEntity a = fossil(h, at(2, 2));
            DinoKind first = a.kind();
            for (int i = 0; i < 20; i++) h.assertTrue(a.kind() == first, "the kind never rerolls");
            h.succeed();
        });

        // --- egg: sitting, brooding ---------------------------------------------------------------------------------------------
        ALL.put("egg_brooding_phases_and_lock", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            DinoEggBlockEntity be = egg(h, pos);
            UUID sitter = UUID.randomUUID();
            int total = DinosConfig.incubateTicks();
            h.assertTrue(total == 6000, "five minutes");
            java.util.Set<Integer> phases = new java.util.TreeSet<>();
            long t = 1000;
            be.advance(level, t, null); // the first step only sets the clock
            while (!be.brooded() && t < 1000 + 8000) {
                t += 40;
                be.advance(level, t, sitter);
                phases.add(h.getBlockState(pos).getValue(DinoEggBlock.PHASE));
            }
            h.assertTrue(be.brooded() && be.progressState().value() == total, "five minutes of sitting broods the egg");
            for (int phase = 0; phase < DinoEggBlock.BROOD_PHASES; phase++) h.assertTrue(phases.contains(phase), "brooding step " + phase + " is shown");
            h.assertTrue(be.owner() != null && be.owner().equals(sitter) && be.sitTicks(sitter) >= total - 40, "the longest sitter is remembered");
            long hatch = be.hatchAt();
            h.assertTrue(hatch - t == DinosConfig.hatchTicks(), "hatching 10 minutes after brooding");
            be.advance(level, t + 5000, null); // nobody sits any more
            h.assertTrue(be.progressState().value() == total, "a brooded egg does not run back (R5)");
            h.succeed();
        });
        ALL.put("egg_rewinds_twice_as_fast_when_nobody_sits", h -> {
            ServerLevel level = h.getLevel();
            DinoEggBlockEntity be = egg(h, at(5, 5));
            UUID sitter = UUID.randomUUID();
            long t = 2000;
            be.advance(level, t, null);
            for (int i = 0; i < 10; i++) be.advance(level, t += 40, sitter);
            int before = be.progressState().value();
            h.assertTrue(before == 400, "10 steps of 40 ticks = 400 (" + before + ")");
            be.advance(level, t + 100, null);
            int after = be.progressState().value();
            h.assertTrue(after == before - 2 * (100 - DinoEggBlockEntity.GRACE), "standing up: back at twice the speed (" + after + ")");
            h.succeed();
        });
        ALL.put("egg_seat_and_cleanup", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            DinoEggBlockEntity be = egg(h, pos);
            ServerPlayer first = player(h, 4, 6);
            ServerPlayer second = player(h, 6, 6);
            h.assertTrue(be.sit(level, first), "the first player sits down");
            h.assertTrue(first.getVehicle() instanceof ArmorStand s && s.entityTags().contains(DinoEggBlockEntity.SEAT_TAG), "on an invisible seat");
            ArmorStand seat = (ArmorStand) first.getVehicle();
            h.assertTrue(!be.sit(level, second), "a second player cannot sit on the same egg");
            h.setBlock(pos, Blocks.AIR);
            h.assertTrue(seat.isRemoved(), "the seat goes with the egg");
            h.succeed();
        });
        ALL.put("egg_hatches_tame_hatchling", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            DinoEggBlockEntity be = egg(h, pos);
            ServerPlayer owner = player(h, 5, 5);
            long t = 3000;
            be.advance(level, t, null);
            while (!be.brooded() && t < 3000 + 8000) be.advance(level, t += 40, owner.getUUID());
            h.assertTrue(be.brooded(), "brooded");
            be.advance(level, be.hatchAt() - 1, null);
            h.assertTrue(h.getBlockState(pos).is(DinosRegistry.EGGS.get(DinoKind.TREX)), "not yet hatched one tick early");
            be.advance(level, be.hatchAt(), null);
            h.assertTrue(h.getBlockState(pos).isAir(), "the egg is gone after hatching");
            var found = level.getEntitiesOfClass(Trex.class, new AABB(h.absolutePos(pos)).inflate(3.0));
            h.assertTrue(found.size() == 1, "one hatchling");
            Trex baby = found.get(0);
            h.assertTrue(baby.isBaby() && baby.isTamed() && baby.getOwner() != null && baby.getOwner().getUUID().equals(owner.getUUID()), "a tame baby that belongs to the player who sat");
            h.assertTrue(baby.growth() == 0 && baby.getAgeScale() == Dino.MIN_SCALE, "25 % of the size at the start");
            h.assertTrue(baby.coat() >= 0 && baby.coat() < DinoVariants.COATS.size(), "the hatchling has a coat of its rearing biome");
            baby.discard();
            h.succeed();
        });
        MAX_TICKS.put("egg_hatches_tame_hatchling", 100);
        ALL.put("egg_waits_without_players", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            DinoEggBlockEntity be = egg(h, pos);
            UUID gone = UUID.randomUUID(); // an owner who is not online
            long t = 3000;
            be.advance(level, t, null);
            while (!be.brooded() && t < 3000 + 8000) be.advance(level, t += 40, gone);
            be.advance(level, be.hatchAt() + 10, null);
            Vec3 c = Vec3.atCenterOf(h.absolutePos(pos));
            if (level.getNearestPlayer(c.x, c.y, c.z, 16.0, false) == null) {
                h.assertTrue(h.getBlockState(pos).is(DinosRegistry.EGGS.get(DinoKind.TREX)), "no player near: the egg waits");
            }
            h.succeed();
        });
        // F12=B (owner 12.11.): with hatchWaitsForOwner a brooded egg hatches only for its owner (online, here, within 16 blocks)
        ALL.put("egg_waits_for_owner", h -> {
            ServerLevel level = h.getLevel();
            var trexEgg = DinosRegistry.EGGS.get(DinoKind.TREX);
            try {
                h.assertTrue(DinosConfig.hatchWaitsForOwner, "on by default");
                DinosConfig.hatchWaitsForOwner = true;
                ServerPlayer owner = player(h, 18, 1);    // 17 blocks from the egg at (1, 1): out of reach
                ServerPlayer stranger = player(h, 3, 3);  // close to every egg below
                BlockPos waitsFar = at(1, 1), waitsOffline = at(1, 8), strangerGets = at(8, 8);
                DinoEggBlockEntity far = egg(h, waitsFar);
                brood(level, far, owner.getUUID());
                DinoEggBlockEntity offline = egg(h, waitsOffline);
                brood(level, offline, UUID.randomUUID()); // an owner who is not online
                h.assertTrue(far.brooded() && offline.brooded(), "both eggs are brooded");
                long hatchAt = far.hatchAt();
                int total = DinosConfig.incubateTicks();
                far.advance(level, hatchAt + 10, null);
                offline.advance(level, offline.hatchAt() + 10, null);
                h.assertTrue(h.getBlockState(waitsFar).is(trexEgg), "the owner is online but 17 blocks away: the egg waits although a stranger is near");
                h.assertTrue(h.getBlockState(waitsOffline).is(trexEgg), "the owner is offline: the egg waits although a stranger is near");
                h.assertTrue(far.brooded() && far.hatchAt() == hatchAt && far.progressState().value() == total
                        && h.getBlockState(waitsFar).getValue(DinoEggBlock.PHASE) == 14, "still brooded, nothing lost, cracked as far as it gets");
                h.assertTrue(level.getEntitiesOfClass(Trex.class, new AABB(h.absolutePos(waitsFar)).inflate(3.0)).isEmpty(), "nobody hatched");
                // the owner comes close: now it hatches, for the owner
                Vec3 near = h.absoluteVec(new Vec3(2.5, 1.0, 2.5));
                owner.setPos(near.x, near.y, near.z);
                far.advance(level, hatchAt + 20, null);
                h.assertTrue(h.getBlockState(waitsFar).isAir(), "the owner is back within 16 blocks: it hatches");
                var found = level.getEntitiesOfClass(Trex.class, new AABB(h.absolutePos(waitsFar)).inflate(3.0));
                h.assertTrue(found.size() == 1 && found.get(0).getOwner() != null && found.get(0).getOwner().getUUID().equals(owner.getUUID()),
                        "the hatchling belongs to the owner, not to the stranger");
                found.get(0).discard();
                h.assertTrue(h.getBlockState(waitsOffline).is(trexEgg), "the egg of the offline owner is still waiting");
                // option off: the old rule, the stranger within 16 blocks gets the hatchling (the owner is away again: the stranger is the nearest)
                Vec3 away = h.absoluteVec(new Vec3(18.5, 1.0, 1.5));
                owner.setPos(away.x, away.y, away.z);
                DinosConfig.hatchWaitsForOwner = false;
                offline.advance(level, offline.hatchAt() + 30, null);
                h.assertTrue(h.getBlockState(waitsOffline).isAir(), "option off: the offline owner's egg hatches for the player nearby");
                var taken = level.getEntitiesOfClass(Trex.class, new AABB(h.absolutePos(waitsOffline)).inflate(3.0));
                h.assertTrue(taken.size() == 1 && taken.get(0).getOwner() != null && taken.get(0).getOwner().getUUID().equals(stranger.getUUID()),
                        "option off: the stranger gets the hatchling");
                taken.get(0).discard();
                // option off and the owner online but far away: the owner still gets it (as before, any distance)
                DinoEggBlockEntity anywhere = egg(h, strangerGets);
                ServerPlayer remote = player(h, 30, 8); // 22 blocks from the egg
                brood(level, anywhere, remote.getUUID());
                anywhere.advance(level, anywhere.hatchAt() + 10, null);
                var remoteBaby = level.getEntitiesOfClass(Trex.class, new AABB(h.absolutePos(strangerGets)).inflate(3.0));
                h.assertTrue(remoteBaby.size() == 1 && remoteBaby.get(0).getOwner() != null && remoteBaby.get(0).getOwner().getUUID().equals(remote.getUUID()),
                        "option off: the old rule for an online owner stays");
                remoteBaby.get(0).discard();
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("egg_other_kinds_wait_for_their_dinosaur", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            h.setBlock(pos, DinosRegistry.EGGS.get(DinoKind.STEGOSAURUS));
            DinoEggBlockEntity be = (DinoEggBlockEntity) level.getBlockEntity(h.absolutePos(pos));
            ServerPlayer owner = player(h, 5, 5);
            long t = 3000;
            be.advance(level, t, null);
            while (!be.brooded() && t < 3000 + 8000) be.advance(level, t += 40, owner.getUUID());
            be.advance(level, be.hatchAt() + 10, null);
            h.assertTrue(be.brooded() && h.getBlockState(pos).getBlock() == DinosRegistry.EGGS.get(DinoKind.STEGOSAURUS),
                    "a kind without a dinosaur yet brooded, but stays an egg (the Stegosaurus comes next)");
            h.succeed();
        });

        // --- part hitboxes (a-dino36b, owner DINO36: heads not in walls or people) ----------------------------------------------
        ALL.put("trex_part_rig_geometry", h -> {
            h.assertTrue(Trex.RIG.reach > 2.8 && Trex.RIG.reach < 2.9, "the stretched snout reaches 2.85 blocks in front (" + Trex.RIG.reach + ")");
            Trex trex = standing(h, 8, 8);
            double front = 0.0, top = 0.0;
            for (AABB b : trex.headBoxes(com.simpledinos.entity.part.PartRig.TUCK_STEPS)) {
                front = Math.max(front, b.maxZ - trex.getZ());
                top = Math.max(top, b.maxY - trex.getY());
            }
            h.assertTrue(front < trex.getBbWidth() / 2.0, "pulled in, the head stays inside the front of the core box (" + front + ")");
            h.assertTrue(top < 4.1, "pulled in, the head is no higher than the stretched one (" + top + ")");
            h.assertTrue(trex.headTuck() == 0 && trex.tailBend() == 0, "a new T. rex stands stretched and straight");
            trex.updateParts();
            h.assertTrue(trex.headTuck() == 0 && trex.tailBend() == 0, "in the open field nothing moves");
            h.succeed();
        });
        ALL.put("trex_head_pulls_in_at_wall", h -> {
            Trex trex = standing(h, 8, 4);
            wall(h, 7, Blocks.STONE);
            h.assertTrue(!clearOfBlocks(h, trex, trex.headBoxes(0)), "the stretched head would stick in the wall");
            h.assertTrue(clearOfBlocks(h, trex, List.of(trex.getBoundingBox())), "the core box itself fits in front of the wall");
            trex.updateParts();
            int tuck = trex.headTuck();
            h.assertTrue(tuck > 0, "the head pulls in before the wall (step " + tuck + ")");
            h.assertTrue(clearOfBlocks(h, trex, trex.headBoxes(tuck)), "no part of neck, head or jaw is in the wall at step " + tuck);
            h.assertTrue(!clearOfBlocks(h, trex, trex.headBoxes(tuck - 1)), "and it pulls in no further than needed");
            h.succeed();
        });
        ALL.put("trex_head_stretches_again_step_by_step", h -> {
            Trex trex = standing(h, 8, 4);
            wall(h, 7, Blocks.STONE);
            trex.updateParts();
            int tuck = trex.headTuck();
            h.assertTrue(tuck >= 2, "pulled in at the wall (step " + tuck + ")");
            wall(h, 7, Blocks.AIR);
            int last = tuck, calls = 0;
            while (trex.headTuck() > 0 && calls < 60) {
                trex.updateParts();
                calls++;
                int now = trex.headTuck();
                h.assertTrue(now == last || now == last - 1, "one step at a time (" + last + " -> " + now + ")");
                last = now;
            }
            h.assertTrue(trex.headTuck() == 0, "stretched again once the wall is gone");
            h.assertTrue(calls >= 4, "not at once: the head waits a moment before every step (" + calls + " checks)");
            h.succeed();
        });
        ALL.put("trex_head_avoids_player", h -> {
            Trex trex = standing(h, 8, 4);
            ServerPlayer p = player(h, 8, 7);
            Vec3 high = h.absoluteVec(new Vec3(8.5, 4.0, 7.0)); // standing on something at the height of the snout
            p.setPos(high.x, high.y, high.z);
            AABB body = p.getBoundingBox();
            h.assertTrue(trex.headBoxes(0).stream().anyMatch(b -> b.intersects(body)), "the stretched head would stick in the player");
            trex.updateParts();
            int tuck = trex.headTuck();
            h.assertTrue(tuck > 0, "the head pulls in before the player (step " + tuck + ")");
            h.assertTrue(trex.headBoxes(tuck).stream().noneMatch(b -> b.intersects(body)), "no part of the head is in the player");
            h.succeed();
        });
        ALL.put("trex_head_brushes_through_leaves", h -> {
            Trex trex = standing(h, 8, 4);
            wall(h, 7, Blocks.OAK_LEAVES);
            trex.updateParts();
            h.assertTrue(trex.headTuck() == 0, "leaves are no obstacle for the head (as the ravager walks into foliage; nothing breaks)");
            h.assertTrue(h.getBlockState(new BlockPos(8, 4, 7)).is(Blocks.OAK_LEAVES), "the leaves stay");
            h.succeed();
        });
        ALL.put("trex_tail_bends_away_from_pillar", h -> {
            Trex trex = standing(h, 8, 10);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(8, y, 5), Blocks.STONE);
            h.assertTrue(!clearOfBlocks(h, trex, trex.tailBoxes(0)), "the straight tail would stick in the pillar");
            trex.updateParts();
            int bend = trex.tailBend();
            h.assertTrue(bend != 0, "the tail bends away");
            h.assertTrue(clearOfBlocks(h, trex, trex.tailBoxes(bend)), "no tail segment is in the pillar (bend " + bend + ")");
            h.succeed();
        });
        ALL.put("trex_bites_from_head_reach", h -> {
            Trex trex = standing(h, 8, 4);
            net.minecraft.world.entity.monster.Monster near = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 7));
            net.minecraft.world.entity.monster.Monster far = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 9));
            near.setNoAi(true);
            far.setNoAi(true);
            double vanilla = Math.sqrt(2.04) - 0.6;
            AABB core = trex.getBoundingBox().inflate(vanilla, 0.0, vanilla);
            h.assertTrue(!core.intersects(near.getBoundingBox()), "the near zombie is out of the Vanilla reach of the core box");
            h.assertTrue(trex.isWithinMeleeAttackRange(near), "but in reach of the head: it bites from its head");
            h.assertTrue(!trex.isWithinMeleeAttackRange(far), "a zombie beyond the snout is out of reach");
            near.discard();
            far.discard();
            h.succeed();
        });
        ALL.put("trex_follow_stops_outside_head_and_hatchling_parts_scale", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Trex trex = adult(h, owner, 8, 8);
            h.assertTrue(trex.followStop(3.0) > trex.headReach() + 0.3, "it stops following before its head reaches the owner ("
                    + trex.followStop(3.0) + " > " + trex.headReach() + " + 0.3)");
            Trex baby = h.spawn(DinosRegistry.TREX, at(3, 12));
            baby.setPersistenceRequired();
            baby.hatch(owner);
            h.assertTrue(Math.abs(baby.headReach() - trex.headReach() * Dino.MIN_SCALE) < 1.0E-4, "a hatchling's head reaches a quarter as far");
            h.assertTrue(baby.followStop(3.0) == 3.0, "the hatchling keeps the old 3 blocks");
            double top = 0.0;
            for (AABB b : baby.headBoxes(0)) top = Math.max(top, b.maxY - baby.getY());
            h.assertTrue(top < 1.1 && top > 0.9, "its head boxes are a quarter of the height too (" + top + ")");
            baby.discard();
            h.succeed();
        });

        // --- growth, riding -----------------------------------------------------------------------------------------------------
        ALL.put("dino_growth_scale", h -> {
            h.assertTrue(Dino.scaleFor(0) == 0.25F && Dino.scaleFor(100) == 1.0F && Dino.scaleFor(50) == 0.625F, "25 % to 100 %");
            int total = DinosConfig.growTicks();
            h.assertTrue(total == 48000, "40 minutes");
            h.assertTrue(Dino.growthFor(-total, total) == 0 && Dino.growthFor(-total / 2, total) == 50 && Dino.growthFor(0, total) == 100, "age to percent");
            ServerPlayer owner = player(h, 3, 3);
            Trex baby = h.spawn(DinosRegistry.TREX, at(8, 8));
            baby.setPersistenceRequired();
            baby.hatch(owner);
            h.assertTrue(baby.isBaby() && baby.getAge() == -total && baby.getAgeScale() == Dino.MIN_SCALE, "just hatched");
            baby.setAge(-total / 2);
            baby.syncGrowth();
            h.assertTrue(baby.growth() == 50 && baby.getAgeScale() == Dino.scaleFor(50), "half way: 62.5 % size");
            baby.setAge(0);
            baby.syncGrowth();
            h.assertTrue(!baby.isBaby() && baby.getAgeScale() == 1.0F, "grown up: full size");
            h.succeed();
        });
        ALL.put("trex_core_hitbox_and_stats", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Trex trex = adult(h, owner, 8, 8);
            h.assertTrue(trex.getBbWidth() == 3.6F && trex.getBbHeight() == 3.5F, "core hitbox 3.6 x 3.5 (TREX32 T1)");
            h.assertTrue(trex.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 10.0, "bite 10");
            h.assertTrue(!trex.canMate(trex) && trex.getBreedOffspring(h.getLevel(), trex) == null, "dinosaurs do not breed");
            h.succeed();
        });
        // a-audsb31e2 (simpledinos-P2-1): the saved attribute must not freeze the bite at the value of the last save.
        ALL.put("trex_bite_follows_config", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Trex trex = adult(h, owner, 8, 8);
            var attack = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
            try {
                h.assertTrue(trex.getAttributeValue(attack) == DinosConfig.BITE_DEFAULT, "a new T. rex bites with the config value");
                var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
                trex.saveWithoutId(out);
                DinosConfig.trexBiteDamage = 25.0;
                Trex loaded = DinosRegistry.TREX.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                h.assertTrue(loaded != null, "the T. rex type creates");
                loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
                h.assertTrue(loaded.getAttributeValue(attack) == 25.0, "a loaded T. rex bites with the current config, not the saved value: " + loaded.getAttributeValue(attack));
                DinosConfig.trexBiteDamage = 12.0;
                trex.applyBite();
                h.assertTrue(trex.getAttributeValue(attack) == 12.0, "a live config change reaches a T. rex that stands in the world");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        // a-audsb31e2 (simpledinos-P2-2): without an online owner every idle monster was "hunting the owner" (null == null).
        // A pig stands in for the owner (any living entity can own a horse), so no player target rules (creative, peaceful) get in the way.
        ALL.put("trex_defends_only_hunted", h -> {
            net.minecraft.world.entity.LivingEntity owner = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, at(5, 5));
            Trex trex = h.spawn(DinosRegistry.TREX, at(8, 8));
            trex.setPersistenceRequired();
            trex.setTamed(true);
            trex.setOwner(owner);
            trex.setAge(0);
            net.minecraft.world.entity.monster.Monster zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(11, 11));
            zombie.setPersistenceRequired();
            zombie.setTarget(null);
            h.assertTrue(trex.getOwner() == owner, "the owner stand-in resolves");
            h.assertTrue(!trex.defends(zombie), "an idle monster is no reason to bite");
            zombie.setTarget(owner);
            h.assertTrue(zombie.getTarget() == owner && trex.defends(zombie), "a monster that hunts the owner is");
            zombie.setTarget(trex);
            h.assertTrue(zombie.getTarget() == trex && trex.defends(zombie), "a monster that hunts the T. rex is");
            zombie.setTarget(null);
            trex.setOwner(null);
            h.assertTrue(trex.getOwner() == null && !trex.defends(zombie), "owner offline: still no bite at idle monsters");
            zombie.discard();
            trex.discard();
            owner.discard();
            h.succeed();
        });
        ALL.put("trex_saddle_armor_riding", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Trex trex = adult(h, owner, 8, 8);
            var wearer = trex.typeHolder();
            h.assertTrue(DinosRegistry.LARGE_SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable big
                    && big.canBeEquippedBy(wearer) && big.slot() == EquipmentSlot.SADDLE, "the large saddle fits the T. rex");
            h.assertTrue(!(Items.SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable small
                    && small.canBeEquippedBy(wearer)), "a normal saddle does not");
            h.assertTrue(!(DinosRegistry.LARGE_SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable big2
                    && big2.canBeEquippedBy(net.minecraft.world.entity.EntityTypes.HORSE.builtInRegistryHolder())), "and the large saddle fits no horse");
            trex.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(trex.isSaddled(), "saddled");
            h.assertTrue(trex.armorTier() == 0, "no armor yet");
            for (Item armor : new Item[] {DinosRegistry.TREX_ARMOR_LEATHER, DinosRegistry.TREX_ARMOR_IRON, DinosRegistry.TREX_ARMOR_DIAMOND}) {
                h.assertTrue(trex.isEquippableInSlot(new ItemStack(armor), EquipmentSlot.BODY), "armor fits: " + BuiltInRegistries.ITEM.getKey(armor));
            }
            trex.setItemSlot(EquipmentSlot.BODY, new ItemStack(DinosRegistry.TREX_ARMOR_IRON));
            h.assertTrue(trex.armorTier() == 2, "iron armor: tier 2");
            owner.startRiding(trex, true, false);
            h.assertTrue(trex.getControllingPassenger() == owner, "the owner rides and steers");
            Trex baby = h.spawn(DinosRegistry.TREX, at(12, 12));
            baby.hatch(owner);
            h.assertTrue(!baby.canUseSlot(EquipmentSlot.SADDLE) && !baby.canUseSlot(EquipmentSlot.BODY), "a hatchling takes no saddle and no armor");
            baby.discard();
            h.succeed();
        });
        ALL.put("trex_eats_meat_and_heals", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Trex trex = adult(h, owner, 8, 8);
            trex.setHealth(10.0F);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 2));
            h.assertTrue(trex.isFood(new ItemStack(Items.BEEF)) && !trex.isFood(new ItemStack(Items.WHEAT)), "meat, not wheat");
            trex.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(trex.getHealth() == 18.0F && owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "a piece of meat heals 8");
            h.succeed();
        });
        ALL.put("trex_without_owner_cannot_be_ridden", h -> {
            ServerPlayer p = player(h, 3, 3);
            Trex wild = h.spawn(DinosRegistry.TREX, at(8, 8));
            wild.setPersistenceRequired();
            wild.setAge(0);
            wild.mobInteract(p, InteractionHand.MAIN_HAND);
            h.assertTrue(!wild.isTamed() && wild.getPassengers().isEmpty(), "an untamed T. rex is not tamed by riding");
            wild.discard();
            h.succeed();
        });
        ALL.put("dino_variant_catalog", h -> {
            h.assertTrue(DinoVariants.COATS.size() == 5 && DinoVariants.name(0).equals("olive") && DinoVariants.name(1).equals("sand")
                    && DinoVariants.name(2).equals("rust") && DinoVariants.name(3).equals("moss") && DinoVariants.name(4).equals("ash"), "five coats, stored indices keep their meaning");
            h.assertTrue(DinoVariants.clamp(-4) == 0 && DinoVariants.clamp(99) == 4 && DinoVariants.name(99).equals("ash"), "clamp");
            checkHome(h, 0, biome(h, Biomes.PLAINS), "olive in plains (standard)");
            checkHome(h, 0, biome(h, Biomes.FOREST), "olive in forest (standard)");
            checkHome(h, 1, biome(h, Biomes.DESERT), "sand in desert");
            checkHome(h, 1, biome(h, Biomes.SAVANNA), "sand in savanna");
            checkHome(h, 2, biome(h, Biomes.BADLANDS), "rust in badlands");
            checkHome(h, 3, biome(h, Biomes.JUNGLE), "moss in jungle");
            checkHome(h, 3, biome(h, Biomes.SWAMP), "moss in swamp");
            checkHome(h, 4, biome(h, Biomes.TAIGA), "ash in taiga");
            h.succeed();
        });
        ALL.put("dino_coat_state", h -> {
            Trex trex = h.spawn(DinosRegistry.TREX, at(8, 8));
            trex.setPersistenceRequired();
            h.assertTrue(trex.coat() >= 0 && trex.coat() < DinoVariants.COATS.size(), "a spawned dinosaur has a valid coat");
            trex.setCoat(99);
            h.assertTrue(trex.coat() == 4, "too large is clamped");
            trex.setCoat(-2);
            h.assertTrue(trex.coat() == 0, "negative is clamped");
            trex.setCoat(2);
            h.assertTrue(trex.coat() == 2, "a valid coat stays");
            trex.discard();
            h.succeed();
        });
        // --- Triceratops (a-dinorest1, concept KONZEPT-A-DINO36 4.1) --------------------------------------------------------------
        ALL.put("dino_kind_site_weights", h -> {
            var desert = biome(h, Biomes.DESERT);
            var badlands = biome(h, Biomes.BADLANDS);
            var swamp = biome(h, Biomes.MANGROVE_SWAMP);
            var plains = biome(h, Biomes.PLAINS);
            for (DinoKind k : DinoKind.values()) h.assertTrue(k.siteHalves(plains) == 2 && k.siteHalves(null) == 2, "plains: no shift for " + k);
            h.assertTrue(DinoKind.TRICERATOPS.siteHalves(desert) == 3 && DinoKind.TREX.siteHalves(desert) == 3, "desert: Triceratops, T. rex x1.5");
            h.assertTrue(DinoKind.UTAHRAPTOR.siteHalves(badlands) == 4 && DinoKind.STEGOSAURUS.siteHalves(badlands) == 3, "badlands: Utahraptor x2, Stegosaurus x1.5");
            h.assertTrue(DinoKind.PARASAUROLOPHUS.siteHalves(swamp) == 4 && DinoKind.TRICERATOPS.siteHalves(swamp) == 2, "mangrove swamp: Parasaurolophus x2");
            RandomSource random = RandomSource.create(42L);
            int tri = 0;
            for (int i = 0; i < 2000; i++) {
                DinoKind k = DinoKind.roll(random, badlands);
                h.assertTrue(k.hatchable(), "only hatchable kinds are rolled");
                if (k == DinoKind.TRICERATOPS) tri++;
            }
            h.assertTrue(tri > 2000 * 60 / 100 && tri < 2000 * 82 / 100, "R10 weights between the built kinds: Triceratops 25 : T. rex 10 (" + tri + ")");
            h.succeed();
        });
        ALL.put("triceratops_core_hitbox_and_stats", h -> {
            Triceratops tri = standingTri(h, 8, 8);
            h.assertTrue(tri.getBbWidth() == 2.6F && tri.getBbHeight() == 2.6F, "core hitbox 2.6 x 2.6 (barrel body and legs)");
            h.assertTrue(tri.getMaxHealth() == 100.0F && tri.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.2
                    && tri.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) == 1.0, "100 health, speed 0.20, step 1");
            h.assertTrue(!tri.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    "no attack attribute: only the T. rex fights (combat goals moved to Trex)");
            h.assertTrue(tri.kind() == DinoKind.TRICERATOPS && !tri.canMate(tri), "kind, no breeding");
            h.assertTrue(Triceratops.RIG.solidHead && !Trex.RIG.solidHead, "the Triceratops head is solid, the T. rex pulls its head in");
            h.assertTrue(Triceratops.RIG.reach > 2.7 && Triceratops.RIG.reach < 3.0, "the horns reach 2.85 blocks in front (" + Triceratops.RIG.reach + ")");
            double low0 = 9.0, low4 = 9.0;
            for (AABB b : tri.headBoxes(0)) low0 = Math.min(low0, b.minY - tri.getY());
            for (AABB b : tri.headBoxes(com.simpledinos.entity.part.PartRig.TUCK_STEPS)) low4 = Math.min(low4, b.minY - tri.getY());
            h.assertTrue(low4 > low0 + 0.4, "pulled in, the head is lifted (" + low0 + " -> " + low4 + ")");
            h.succeed();
        });
        ALL.put("triceratops_head_stops_at_wall", h -> {
            Triceratops tri = standingTri(h, 8, 2);
            wall(h, 7, Blocks.STONE);
            Vec3 step = new Vec3(0.0, 0.0, 2.0);
            Vec3 allowed = tri.clipHead(step);
            h.assertTrue(allowed.z < 1.9 && allowed.z > 1.3, "the head stops the walk in front of the wall (" + allowed.z + " of 2.0)");
            boolean clear = true;
            double floor = tri.getY() + tri.maxUpStep() + 0.05; // the part of the head above the step height (what clipHead guards)
            for (AABB b : tri.headBoxes(0)) {
                if (b.maxY <= floor) continue;
                AABB after = new AABB(b.minX, Math.max(b.minY, floor), b.minZ, b.maxX, b.maxY, b.maxZ).move(allowed.x, 0.0, allowed.z);
                if (!h.getLevel().noBlockCollision(tri, after.deflate(0.03))) clear = false;
            }
            h.assertTrue(clear, "after the allowed move no head box is in the wall");
            wall(h, 7, Blocks.OAK_LEAVES);
            h.assertTrue(tri.clipHead(step).z == 2.0, "leaves do not stop the head (it brushes through foliage)");
            wall(h, 7, Blocks.AIR);
            for (int x = 5; x <= 11; x++) h.setBlock(new BlockPos(x, 1, 7), Blocks.STONE);
            h.assertTrue(tri.clipHead(step).z == 2.0, "a step of one block does not stop the head (the body steps up)");
            h.assertTrue(tri.followStop(3.0) > tri.headReach() + 0.3, "it stops following before its head reaches the owner");
            h.succeed();
        });
        ALL.put("triceratops_hatches_tame_with_eggshells", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            h.setBlock(pos, DinosRegistry.EGGS.get(DinoKind.TRICERATOPS));
            DinoEggBlockEntity be = (DinoEggBlockEntity) level.getBlockEntity(h.absolutePos(pos));
            ServerPlayer owner = player(h, 5, 5);
            brood(level, be, owner.getUUID());
            be.advance(level, be.hatchAt(), null);
            h.assertTrue(h.getBlockState(pos).isAir(), "the Triceratops egg hatches");
            var found = level.getEntitiesOfClass(Triceratops.class, new AABB(h.absolutePos(pos)).inflate(3.0));
            h.assertTrue(found.size() == 1, "one Triceratops hatchling");
            Triceratops baby = found.get(0);
            h.assertTrue(baby.isBaby() && baby.isTamed() && baby.getOwner() != null && baby.getOwner().getUUID().equals(owner.getUUID()),
                    "tame from the first second, imprinted on the sitter");
            int shells = itemsNear(h, pos, 3.0).stream().filter(e -> e.getItem().is(DinosRegistry.eggshell(DinoKind.TRICERATOPS)))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(shells == 3, "three eggshells stay behind (" + shells + ")");
            h.assertTrue(!baby.canUseSlot(EquipmentSlot.SADDLE), "a hatchling takes no saddle");
            ItemStack apple = new ItemStack(Items.APPLE, 2);
            owner.setItemInHand(InteractionHand.MAIN_HAND, apple);
            int before = baby.getAge();
            baby.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(baby.getAge() > before && owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "feeding speeds the growth up (Vanilla)");
            baby.discard();
            h.succeed();
        });
        MAX_TICKS.put("triceratops_hatches_tame_with_eggshells", 100);
        ALL.put("triceratops_food_heals", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Triceratops tri = standingTri(h, 8, 8);
            tri.setOwner(owner);
            for (Item food : new Item[] {Items.APPLE, Items.WHEAT, Items.HAY_BLOCK, Items.CARROT}) h.assertTrue(tri.isFood(new ItemStack(food)), "food: " + food);
            h.assertTrue(!tri.isFood(new ItemStack(Items.BEEF)), "no meat");
            tri.setHealth(50.0F);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 1));
            tri.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(tri.getHealth() == 58.0F, "the apple (favourite) heals 8");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 1));
            tri.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(tri.getHealth() == 62.0F && owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "other food heals 4");
            h.succeed();
        });
        ALL.put("triceratops_saddle_armor_riding", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Triceratops tri = standingTri(h, 8, 8);
            tri.setOwner(owner);
            var wearer = tri.typeHolder();
            h.assertTrue(DinosRegistry.LARGE_SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable big
                    && big.canBeEquippedBy(wearer), "the large saddle fits the Triceratops");
            for (Item armor : new Item[] {DinosRegistry.TRICERATOPS_ARMOR_LEATHER, DinosRegistry.TRICERATOPS_ARMOR_IRON, DinosRegistry.TRICERATOPS_ARMOR_DIAMOND}) {
                h.assertTrue(tri.isEquippableInSlot(new ItemStack(armor), EquipmentSlot.BODY), "Triceratops armor fits: " + BuiltInRegistries.ITEM.getKey(armor));
                h.assertTrue(!(armor.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable e
                        && e.canBeEquippedBy(DinosRegistry.TREX.builtInRegistryHolder())), "but not the T. rex (each dinosaur its own armor)");
            }
            h.assertTrue(!tri.isEquippableInSlot(new ItemStack(DinosRegistry.TREX_ARMOR_IRON), EquipmentSlot.BODY), "T. rex armor does not fit");
            tri.setItemSlot(EquipmentSlot.BODY, new ItemStack(DinosRegistry.TRICERATOPS_ARMOR_DIAMOND));
            h.assertTrue(tri.armorTier() == 3, "diamond armor: tier 3");
            h.assertTrue(DinosRegistry.TRICERATOPS_ARMOR_IRON.components().get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS) != null,
                    "armor values like the Vanilla horse armor");
            tri.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(tri.isSaddled() && tri.canJump(), "saddled: the jump key charges");
            owner.startRiding(tri, true, false);
            h.assertTrue(tri.getControllingPassenger() == owner, "the owner rides and steers");
            h.succeed();
        });
        ALL.put("triceratops_charge_cooldown", h -> {
            Triceratops tri = standingTri(h, 8, 8);
            tri.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(tri.getJumpCooldown() == 0 && !tri.isRamming(), "ready");
            h.assertTrue(tri.startRam(), "the server starts the charge");
            h.assertTrue(tri.isRamming() && tri.abilityCooldown() == DinosConfig.ramCooldownTicks() && tri.getJumpCooldown() == 60,
                    "3 s cooldown, shown in the jump bar");
            h.assertTrue(!tri.startRam(), "no second charge during the cooldown (server authority)");
            for (int i = 0; i < Triceratops.RAM_TICKS + Triceratops.RAM_GRACE + 2; i++) tri.aiStep();
            h.assertTrue(!tri.isRamming() && tri.serverRamTicks() == 0, "the charge window ends");
            Triceratops baby = standingTri(h, 3, 12);
            baby.setAge(-1000);
            h.assertTrue(!baby.startRam() && !baby.canJump(), "a hatchling does not charge");
            Trex trex = standing(h, 12, 3);
            trex.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(trex.getJumpCooldown() == 0 && trex.canJump(), "the T. rex keeps the normal jump");
            baby.discard();
            trex.discard();
            h.succeed();
        });
        ALL.put("triceratops_shakes_natural_tree", h -> {
            ServerLevel level = h.getLevel();
            Triceratops tri = standingTri(h, 8, 2);
            BlockPos log = tree(h, 8, 11, Blocks.OAK_LOG, Blocks.OAK_LEAVES, true);
            try {
                DinosConfig.shakeAppleChance = 1.0; // the windfall roll always gives its apple
                Triceratops.Tree scanned = Triceratops.scan(level, h.absolutePos(log));
                h.assertTrue(scanned.natural().size() == 49 && scanned.placed() == 0 && scanned.windfall(), "49 natural oak leaves found (" + scanned.natural().size() + ")");
                h.assertTrue(scanned.base().equals(h.absolutePos(new BlockPos(8, 1, 11))), "the base is the lowest log");
                Triceratops.Shake first = tri.shake(level, h.absolutePos(log));
                h.assertTrue(first == Triceratops.Shake.SHAKEN, "a natural tree shakes (" + first + ")");
                var items = itemsNear(h, log, 4.0);
                h.assertTrue(items.stream().anyMatch(e -> e.getItem().is(Items.APPLE)), "the windfall apple falls");
                int count = items.size();
                for (int y = 1; y <= 4; y++) h.assertTrue(h.getBlockState(new BlockPos(8, y, 11)).is(Blocks.OAK_LOG), "the trunk stays");
                h.assertTrue(h.getBlockState(new BlockPos(8, 5, 11)).is(Blocks.OAK_LEAVES) && h.getBlockState(new BlockPos(6, 4, 9)).is(Blocks.OAK_LEAVES), "the leaves stay");
                Triceratops.Shake again = tri.shake(level, h.absolutePos(new BlockPos(8, 3, 11)));
                h.assertTrue(again == Triceratops.Shake.RESTING && itemsNear(h, log, 4.0).size() == count, "the same tree rests: a dull knock, nothing falls");
                h.assertTrue(tri.resting(scanned.base(), level.getGameTime() + DinosConfig.treeRestTicks() - 1)
                        && !tri.resting(scanned.base(), level.getGameTime() + DinosConfig.treeRestTicks() + 1), "it rests 10 minutes");
                BlockPos built = tree(h, 3, 3, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, false);
                h.assertTrue(tri.shake(level, h.absolutePos(built)) == Triceratops.Shake.NOT_NATURAL && itemsNear(h, built, 2.5).isEmpty(),
                        "placed leaves give nothing (no farm from built trees)");
                DinosConfig.triceratopsShake = false;
                BlockPos off = tree(h, 13, 4, Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, true);
                h.assertTrue(tri.shake(level, h.absolutePos(off)) == Triceratops.Shake.OFF, "switched off: only a knock");
            } finally {
                DinosConfig.reset();
            }
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        ALL.put("triceratops_shake_picks_ripe_cocoa", h -> {
            ServerLevel level = h.getLevel();
            Triceratops tri = standingTri(h, 3, 2);
            BlockPos log = tree(h, 8, 8, Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, true);
            BlockPos pod = new BlockPos(8, 2, 7), young = new BlockPos(8, 3, 7);
            h.setBlock(pod, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, net.minecraft.core.Direction.SOUTH).setValue(CocoaBlock.AGE, 2));
            h.setBlock(young, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, net.minecraft.core.Direction.SOUTH).setValue(CocoaBlock.AGE, 1));
            h.assertTrue(tri.shake(level, h.absolutePos(log)) == Triceratops.Shake.SHAKEN, "the jungle tree shakes");
            h.assertTrue(itemsNear(h, log, 4.0).stream().anyMatch(e -> e.getItem().is(Items.COCOA_BEANS)), "the ripe pod gives its beans");
            h.assertTrue(h.getBlockState(pod).is(Blocks.COCOA) && h.getBlockState(pod).getValue(CocoaBlock.AGE) == 0, "and starts over (stays on the trunk)");
            h.assertTrue(h.getBlockState(young).getValue(CocoaBlock.AGE) == 1, "an unripe pod is left alone");
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        ALL.put("triceratops_charge_hits_tree_with_head", h -> {
            ServerLevel level = h.getLevel();
            Triceratops tri = standingTri(h, 8, 2);
            tri.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(tri.hitLog() == null, "nothing in front yet");
            BlockPos log = tree(h, 8, 5, Blocks.OAK_LOG, Blocks.OAK_LEAVES, true);
            try {
                DinosConfig.shakeAppleChance = 1.0;
                h.assertTrue(tri.hitLog() != null, "the stretched head (horns) touches the trunk");
                h.assertTrue(tri.startRam(), "charge");
                tri.ramStep(level);
                h.assertTrue(itemsNear(h, log, 4.0).stream().anyMatch(e -> e.getItem().is(Items.APPLE)), "the charge shook the tree");
                int count = itemsNear(h, log, 4.0).size();
                tri.ramStep(level);
                h.assertTrue(itemsNear(h, log, 4.0).size() == count, "one shake per charge");
            } finally {
                DinosConfig.reset();
            }
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        ALL.put("triceratops_charge_pushes_mobs_not_players", h -> {
            ServerLevel level = h.getLevel();
            Triceratops tri = standingTri(h, 8, 3);
            tri.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            net.minecraft.world.entity.monster.Monster zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 6));
            zombie.setNoAi(true);
            float health = zombie.getHealth();
            ServerPlayer p = player(h, 9, 6);
            net.minecraft.world.entity.animal.wolf.Wolf wolf = h.spawn(net.minecraft.world.entity.EntityTypes.WOLF, at(7, 6));
            wolf.setNoAi(true);
            wolf.tame(p);
            h.assertTrue(!tri.rammable(p) && !tri.rammable(wolf) && tri.rammable(zombie), "players and tamed animals are never pushed");
            tri.startRam();
            tri.ramStep(level);
            h.assertTrue(zombie.getDeltaMovement().z > 0.5, "the zombie is pushed forward hard (" + zombie.getDeltaMovement() + ")");
            h.assertTrue(zombie.getHealth() == health, "without damage");
            Vec3 before = zombie.getDeltaMovement();
            tri.ramStep(level);
            h.assertTrue(zombie.getDeltaMovement().equals(before), "once per charge");
            zombie.discard();
            wolf.discard();
            h.succeed();
        });
        ALL.put("triceratops_defends_by_pushing", h -> {
            ServerLevel level = h.getLevel();
            Triceratops tri = standingTri(h, 8, 8);
            net.minecraft.world.entity.monster.Monster zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 11));
            zombie.setNoAi(true);
            float health = zombie.getHealth();
            tri.hurtServer(level, level.damageSources().mobAttack(zombie), 2.0F);
            h.assertTrue(tri.getHealth() < tri.getMaxHealth(), "the zombie hit it");
            h.assertTrue(zombie.getDeltaMovement().z > 0.3, "the Triceratops pushes the zombie away (" + zombie.getDeltaMovement() + ")");
            h.assertTrue(zombie.getHealth() == health && tri.getTarget() == null, "no damage, no hunt: it does not fight");
            zombie.discard();
            h.succeed();
        });
        ALL.put("triceratops_part_boxes_like_trex", h -> {
            Triceratops tri = standingTri(h, 8, 10);
            tri.updateParts();
            h.assertTrue(tri.headTuck() == 0 && tri.tailBend() == 0, "in the open field the head is stretched, the tail straight");
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(8, y, 7), Blocks.STONE);
            h.assertTrue(!clearOfBlocks(h, tri, tri.tailBoxes(0)), "the straight tail would stick in the pillar");
            tri.updateParts();
            h.assertTrue(tri.tailBend() != 0 && clearOfBlocks(h, tri, tri.tailBoxes(tri.tailBend())), "the tail bends away (" + tri.tailBend() + ")");
            ServerPlayer p = player(h, 8, 13);
            Vec3 at = h.absoluteVec(new Vec3(8.5, 2.0, 13.3));
            p.setPos(at.x, at.y, at.z);
            AABB body = p.getBoundingBox();
            h.assertTrue(tri.headBoxes(0).stream().anyMatch(b -> b.intersects(body)), "the stretched head would touch the player");
            tri.updateParts();
            h.assertTrue(tri.headTuck() > 0, "the head lifts before the player (step " + tri.headTuck() + "); the solid head pushes the player aside");
            h.succeed();
        });
        ALL.put("guide_book", com.simpledinos.guide.DinosGuide::gameTest);
    }

    private DinosTests() {}
}

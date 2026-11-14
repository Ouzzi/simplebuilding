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
import com.simpledinos.entity.Parasaurolophus;
import com.simpledinos.entity.Stegosaurus;
import com.simpledinos.entity.Trex;
import com.simpledinos.entity.Triceratops;
import com.simpledinos.entity.Utahraptor;
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

    /** a-dinorest2: an adult Stegosaurus without AI, facing +z (yaw 0), tame. */
    private static Stegosaurus standingStego(GameTestHelper h, int x, int z) {
        Stegosaurus stego = h.spawn(DinosRegistry.STEGOSAURUS, at(x, z));
        stego.setPersistenceRequired();
        stego.setTamed(true);
        stego.setAge(0);
        stego.setNoAi(true);
        Vec3 p = stego.position();
        stego.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
        stego.setYBodyRot(0.0F);
        stego.setYHeadRot(0.0F);
        return stego;
    }

    /** a-dinorest3: an adult Parasaurolophus without AI, facing +z (yaw 0), tame. */
    /** b-utah48: a grown, tame Utahraptor without AI, facing south (+z). */
    private static Utahraptor standingRaptor(GameTestHelper h, int x, int z) {
        Utahraptor raptor = h.spawn(DinosRegistry.UTAHRAPTOR, at(x, z));
        raptor.setPersistenceRequired();
        raptor.setTamed(true);
        raptor.setAge(0);
        raptor.setNoAi(true);
        Vec3 p = raptor.position();
        raptor.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
        raptor.setYBodyRot(0.0F);
        raptor.setYHeadRot(0.0F);
        return raptor;
    }

    /** b-utah48: a saddled raptor ridden by {@code rider}, in front of a stone wall at z = {@code wallZ} (x 4..12, y 1..7). */
    private static Utahraptor riddenAtWall(GameTestHelper h, ServerPlayer rider, int z, int wallZ, Block wall) {
        for (int x = 4; x <= 12; x++) for (int y = 1; y <= 7; y++) h.setBlock(new BlockPos(x, y, wallZ), wall);
        Utahraptor raptor = standingRaptor(h, 8, z);
        raptor.setOwner(rider);
        raptor.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
        rider.startRiding(raptor, true, false);
        raptor.forgetWall();
        return raptor;
    }

    /** b-utah48: one tick of the rider's client walking forward (Vanilla travel) and the server's stamina step. */
    private static void rideForward(GameTestHelper h, Utahraptor raptor) {
        raptor.setSpeed((float) Utahraptor.SPEED);
        raptor.travel(new Vec3(0.0, 0.0, 1.0));
        raptor.staminaStep(h.getLevel());
    }

    private static Parasaurolophus standingPara(GameTestHelper h, int x, int z) {
        Parasaurolophus para = h.spawn(DinosRegistry.PARASAUROLOPHUS, at(x, z));
        para.setPersistenceRequired();
        para.setTamed(true);
        para.setAge(0);
        para.setNoAi(true);
        Vec3 p = para.position();
        para.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
        para.setYBodyRot(0.0F);
        para.setYHeadRot(0.0F);
        return para;
    }

    /**
     * a-dinorest3: call radius in the call tests. Neighbouring test structures start 5 blocks beyond the arena (GameTest grid), so a radius of
     * 8 around the arena centre never reaches animals of other tests; every animal of these tests stands within 8 blocks of the
     * Parasaurolophus at (8, 8).
     */
    private static final int TEST_CALL_RADIUS = 8;

    /** a-dinorest3: a farm animal (or any mob) without AI standing on the arena floor at a block (on the ground: its navigation works). */
    private static <T extends net.minecraft.world.entity.Mob> T still(GameTestHelper h, net.minecraft.world.entity.EntityType<T> type, int x, int z) {
        T mob = h.spawn(type, at(x, z));
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setOnGround(true);
        return mob;
    }

    /**
     * a-dinorest2: a saddled adult Stegosaurus at (8, z) facing +z with a lowered plow and {@code rider} on it; its plow strip lies at
     * block row z - 2 (x 7, 8, 9; the stego stands at z + 0.5, the shares 1.95 behind), the ground there is y 0.
     */
    private static Stegosaurus plowing(GameTestHelper h, ServerPlayer rider, int z) {
        Stegosaurus stego = standingStego(h, 8, z);
        stego.setOwner(rider);
        stego.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
        stego.setPlow(new ItemStack(DinosRegistry.DINO_PLOW));
        stego.setPlowDown(true);
        rider.startRiding(stego, true, false);
        return stego;
    }

    /** a-dinorest2: the three ground blocks of the strip of a Stegosaurus from {@link #plowing} at row {@code z}. */
    private static BlockPos[] strip(int z) {
        return new BlockPos[] {new BlockPos(7, 0, z - 2), new BlockPos(8, 0, z - 2), new BlockPos(9, 0, z - 2)};
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
                    "stegosaurus_eggshell", "utahraptor_eggshell", "parasaurolophus_eggshell", "stegosaurus_armor_leather", "stegosaurus_armor_iron",
                    "stegosaurus_armor_diamond", "dino_plow", "parasaurolophus_armor_leather", "parasaurolophus_armor_iron",
                    "parasaurolophus_armor_diamond", "utahraptor_armor_leather", "utahraptor_armor_iron", "utahraptor_armor_diamond", "raptor_feather"}) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleDinos.id(id)), "item " + id);
            }
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("trex")), "entity trex");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("triceratops")), "entity triceratops");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("stegosaurus")), "entity stegosaurus");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("parasaurolophus")), "entity parasaurolophus");
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("utahraptor")), "entity utahraptor");
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("utahraptor_spawn_egg")), "no Utahraptor spawn egg either (owner R6)");
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("triceratops_spawn_egg")) && !BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("trex_eggshell")),
                    "no spawn egg, no T. rex eggshell (concept DINO36 6)");
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("trex_spawn_egg")), "no spawn egg (owner)");
            h.assertTrue(DinosRegistry.FOSSIL_BE != null && DinosRegistry.EGG_BE != null, "block entity types");
            h.assertTrue(DefaultAttributes.hasSupplier(DinosRegistry.TREX) && DefaultAttributes.hasSupplier(DinosRegistry.TRICERATOPS)
                    && DefaultAttributes.hasSupplier(DinosRegistry.STEGOSAURUS) && DefaultAttributes.hasSupplier(DinosRegistry.PARASAUROLOPHUS)
                    && DefaultAttributes.hasSupplier(DinosRegistry.UTAHRAPTOR), "attributes");
            h.assertTrue(DinosConfig.enabled && DinosConfig.BRUSH_DEFAULT == 45 && DinosConfig.incubateSeconds == 300 && DinosConfig.hatchSeconds == 600
                    && DinosConfig.growMinutes == 40 && DinosConfig.trexBiteDamage == 10.0, "config defaults = concept times");
            h.assertTrue(DinosConfig.triceratopsShake && DinosConfig.shakeShare == 0.5 && DinosConfig.shakeAppleChance == 0.35
                    && DinosConfig.shakeTreeCooldownSeconds == 600 && DinosConfig.ramCooldownSeconds == 3, "Triceratops defaults = concept 4.1");
            h.assertTrue(DinoKind.TREX.hatchable() && DinoKind.TRICERATOPS.hatchable() && DinoKind.STEGOSAURUS.hatchable()
                    && DinoKind.PARASAUROLOPHUS.hatchable() && DinoKind.UTAHRAPTOR.hatchable() && DinoKind.hatchableKinds().size() == 5,
                    "DINO-REST step 5: all five dinosaurs hatch");
            h.assertTrue(DinosConfig.raptorClimbSeconds == 5 && DinosConfig.raptorFeatherMinutes == 10, "Utahraptor defaults (5 s = about 12 blocks, 10 min)");
            h.assertTrue(DinosConfig.callRadius == 24 && DinosConfig.callSeconds == 120 && DinosConfig.callMaxAnimals == 24
                    && DinosConfig.callCooldownSeconds == 20 && DinosConfig.callIgnoreNamed, "Parasaurolophus defaults = concept 4.4");
            h.assertTrue(DinosConfig.stegosaurusPlowWidth == 3 && DinosConfig.stegosaurusSow, "Stegosaurus defaults = concept 4.2");
            h.assertTrue(new ItemStack(DinosRegistry.DINO_PLOW).getMaxDamage() == 750 && new ItemStack(DinosRegistry.DINO_PLOW).isEnchantable()
                    && new ItemStack(DinosRegistry.DINO_PLOW).is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE)
                    && new ItemStack(DinosRegistry.DINO_PLOW).isValidRepairItem(new ItemStack(Items.IRON_INGOT)),
                    "Dino Plow: 750 durability, enchantable (Unbreaking, Mending), repaired with iron ingots");
            h.assertTrue(DinoKind.TREX.eggshell() == null && DinoKind.TRICERATOPS.eggshell() == DinosRegistry.eggshell(DinoKind.TRICERATOPS)
                    && DinoKind.PARASAUROLOPHUS.eggshell() != null, "an eggshell for every kind but the T. rex");
            h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                    .get(ResourceKey.create(Registries.PLACED_FEATURE, SimpleDinos.id("fossil_block"))).isPresent(), "worldgen feature");
            h.succeed();
        });
        ALL.put("config_caps", h -> {
            try {
                DinosConfig.apply(com.google.gson.JsonParser.parseString("{\"brushSeconds\":99999,\"incubateSeconds\":0,\"hatchSeconds\":\"x\",\"growMinutes\":-5,\"trexBiteDamage\":1000,"
                        + "\"shakeShare\":7,\"shakeAppleChance\":-1,\"shakeTreeCooldownSeconds\":1,\"ramCooldownSeconds\":999,\"triceratopsShake\":\"no\","
                        + "\"stegosaurusPlowWidth\":9,\"stegosaurusSow\":\"x\",\"callRadius\":2,\"callSeconds\":99999,\"callMaxAnimals\":0,"
                        + "\"callCooldownSeconds\":1,\"callIgnoreNamed\":7,\"raptorClimbSeconds\":99,\"raptorFeatherMinutes\":0}").getAsJsonObject());
                h.assertTrue(DinosConfig.raptorClimbSeconds == DinosConfig.CLIMB_MAX && DinosConfig.raptorFeatherMinutes == DinosConfig.FEATHER_MIN,
                        "Utahraptor keys clamped");
                h.assertTrue(DinosConfig.shakeShare == 1.0 && DinosConfig.shakeAppleChance == 0.0 && DinosConfig.shakeTreeCooldownSeconds == DinosConfig.TREE_REST_MIN
                        && DinosConfig.ramCooldownSeconds == DinosConfig.RAM_COOLDOWN_MAX && DinosConfig.triceratopsShake, "Triceratops keys clamped, garbage = default");
                h.assertTrue(DinosConfig.stegosaurusPlowWidth == DinosConfig.PLOW_WIDTH_MAX && DinosConfig.stegosaurusSow, "Stegosaurus keys clamped, garbage = default");
                h.assertTrue(DinosConfig.callRadius == DinosConfig.CALL_RADIUS_MIN && DinosConfig.callSeconds == DinosConfig.CALL_SECONDS_MAX
                        && DinosConfig.callMaxAnimals == DinosConfig.CALL_ANIMALS_MIN && DinosConfig.callCooldownSeconds == DinosConfig.CALL_COOLDOWN_MIN
                        && DinosConfig.callIgnoreNamed, "Parasaurolophus keys clamped, garbage = default");
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
        // b-utah48: the Utahraptor came last - every egg now hatches its own dinosaur (was egg_other_kinds_wait_for_their_dinosaur)
        ALL.put("egg_every_kind_hatches", h -> {
            java.util.Map<DinoKind, net.minecraft.world.entity.EntityType<?>> expected = java.util.Map.of(DinoKind.TREX, DinosRegistry.TREX,
                    DinoKind.TRICERATOPS, DinosRegistry.TRICERATOPS, DinoKind.STEGOSAURUS, DinosRegistry.STEGOSAURUS,
                    DinoKind.UTAHRAPTOR, DinosRegistry.UTAHRAPTOR, DinoKind.PARASAUROLOPHUS, DinosRegistry.PARASAUROLOPHUS);
            for (DinoKind k : DinoKind.values()) {
                h.assertTrue(k.hatchable() && k.entity() == expected.get(k), "the " + k + " egg hatches its own dinosaur");
                h.assertTrue((k == DinoKind.TREX) == (k.eggshell() == null), "eggshell rule for " + k);
            }
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
        ALL.put("dino_part_boxes_minimal", h -> {
            // N45 (owner DINOREST3: "fewer hitboxes, not too many"): core box + ONE head box + ONE tail box per kind
            List<Dino> dinos = List.of(standing(h, 4, 4), standingTri(h, 12, 4), standingStego(h, 4, 12), standingPara(h, 12, 12));
            for (Dino d : dinos) {
                h.assertTrue(d.headBoxes(0).size() == 1 && d.headBoxes(com.simpledinos.entity.part.PartRig.TUCK_STEPS).size() == 1,
                        d.kind() + ": one head box (" + d.headBoxes(0).size() + ")");
                h.assertTrue(d.tailBoxes(0).size() == 1 && d.tailBoxes(2).size() == 1, d.kind() + ": one tail box (" + d.tailBoxes(0).size() + ")");
            }
            dinos.forEach(Dino::discard);
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
            int tri = 0, stego = 0, raptor = 0, para = 0;
            for (int i = 0; i < 2000; i++) {
                DinoKind k = DinoKind.roll(random, badlands);
                h.assertTrue(k.hatchable(), "only hatchable kinds are rolled");
                if (k == DinoKind.TRICERATOPS) tri++;
                if (k == DinoKind.STEGOSAURUS) stego++;
                if (k == DinoKind.UTAHRAPTOR) raptor++;
            }
            // b-utah48: badlands T. rex 10 x1, Triceratops 25 x1, Stegosaurus 25 x1.5, Utahraptor 20 x2, Parasaurolophus 20 x1
            // -> 7.5 % : 18.9 % : 28.3 % : 30.2 % : 15.1 %
            h.assertTrue(raptor > 2000 * 24 / 100 && raptor < 2000 * 37 / 100, "badlands favour the Utahraptor (" + raptor + " of 2000)");
            h.assertTrue(stego > 2000 * 22 / 100 && stego < 2000 * 35 / 100, "badlands favour the Stegosaurus (" + stego + " of 2000)");
            h.assertTrue(tri > 2000 * 13 / 100 && tri < 2000 * 25 / 100, "R10 weights between the built kinds: Triceratops (" + tri + " of 2000)");
            for (int i = 0; i < 2000; i++) if (DinoKind.roll(random, swamp) == DinoKind.PARASAUROLOPHUS) para++;
            // mangrove swamp: T. rex 10, Triceratops 25, Stegosaurus 25, Utahraptor 20, Parasaurolophus 20 x2 -> the Parasaurolophus 33.3 %
            h.assertTrue(para > 2000 * 27 / 100 && para < 2000 * 40 / 100, "swamps favour the Parasaurolophus (" + para + " of 2000)");
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
        // --- Stegosaurus and the Dino Plow (a-dinorest2, concept KONZEPT-A-DINO36 4.2) ---------------------------------------------
        ALL.put("stegosaurus_core_hitbox_and_stats", h -> {
            Stegosaurus stego = standingStego(h, 8, 8);
            h.assertTrue(stego.getBbWidth() == 2.6F && stego.getBbHeight() == 3.7F, "core hitbox 2.6 x 3.7 (body, legs, plates)");
            h.assertTrue(stego.getMaxHealth() == 90.0F && stego.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.17
                    && stego.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) == 1.0, "90 health, speed 0.17, step 1");
            h.assertTrue(!stego.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "no attack: only the T. rex fights");
            h.assertTrue(stego.kind() == DinoKind.STEGOSAURUS && !stego.canMate(stego), "kind, no breeding");
            h.assertTrue(Stegosaurus.RIG.solidHead && Stegosaurus.RIG.reach > 2.4 && Stegosaurus.RIG.reach < 2.7, "solid low head, reach 2.55 (" + Stegosaurus.RIG.reach + ")");
            double front0 = 0.0, front4 = 0.0, low4 = 9.0;
            for (AABB b : stego.headBoxes(0)) front0 = Math.max(front0, b.maxZ - stego.getZ());
            for (AABB b : stego.headBoxes(com.simpledinos.entity.part.PartRig.TUCK_STEPS)) {
                front4 = Math.max(front4, b.maxZ - stego.getZ());
                low4 = Math.min(low4, b.minY - stego.getY());
            }
            h.assertTrue(front4 < front0 - 0.6 && low4 < 0.3, "pulled in, the head goes down and back like grazing (" + front0 + " -> " + front4 + ")");
            double side = 0.0;
            for (AABB b : stego.tailBoxes(0)) side = Math.max(side, Math.max(b.maxX - stego.getX(), stego.getX() - b.minX));
            h.assertTrue(side > 0.6, "the tail spikes count to the tail boxes (" + side + ")");
            h.succeed();
        });
        ALL.put("stegosaurus_head_and_tail_parts", h -> {
            Stegosaurus stego = standingStego(h, 8, 2);
            for (int x = 5; x <= 11; x++) for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
            Vec3 allowed = stego.clipHead(new Vec3(0.0, 0.0, 2.0));
            h.assertTrue(allowed.z < 1.9 && allowed.z > 0.4, "the low head stops the walk in front of the wall (" + allowed.z + " of 2.0)");
            for (int x = 5; x <= 11; x++) for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, 6), Blocks.OAK_LEAVES);
            h.assertTrue(stego.clipHead(new Vec3(0.0, 0.0, 2.0)).z == 2.0, "leaves do not stop the head");
            stego.discard();
            Stegosaurus other = standingStego(h, 8, 11);
            other.updateParts();
            h.assertTrue(other.headTuck() == 0 && other.tailBend() == 0, "in the open field: head stretched, tail straight");
            for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(8, y, 7), Blocks.STONE);
            h.assertTrue(!clearOfBlocks(h, other, other.tailBoxes(0)), "the straight tail (its spikes) would stick in the pillar");
            other.updateParts();
            h.assertTrue(other.tailBend() != 0 && clearOfBlocks(h, other, other.tailBoxes(other.tailBend())), "the tail bends away, spikes and all (" + other.tailBend() + ")");
            ServerPlayer p = player(h, 8, 14);
            Vec3 at = h.absoluteVec(new Vec3(8.5, 2.0, 14.3));
            p.setPos(at.x, at.y, at.z);
            AABB body = p.getBoundingBox();
            h.assertTrue(other.headBoxes(0).stream().anyMatch(b -> b.intersects(body)), "the stretched head would touch the player");
            other.updateParts();
            h.assertTrue(other.headTuck() > 0 && other.headBoxes(other.headTuck()).stream().noneMatch(b -> b.intersects(body)),
                    "the head goes down and back before the player (step " + other.headTuck() + ")");
            h.succeed();
        });
        ALL.put("stegosaurus_hatches_tame_with_eggshells", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            h.setBlock(pos, DinosRegistry.EGGS.get(DinoKind.STEGOSAURUS));
            DinoEggBlockEntity be = (DinoEggBlockEntity) level.getBlockEntity(h.absolutePos(pos));
            ServerPlayer owner = player(h, 5, 5);
            brood(level, be, owner.getUUID());
            be.advance(level, be.hatchAt(), null);
            h.assertTrue(h.getBlockState(pos).isAir(), "the Stegosaurus egg hatches");
            var found = level.getEntitiesOfClass(Stegosaurus.class, new AABB(h.absolutePos(pos)).inflate(3.0));
            h.assertTrue(found.size() == 1, "one Stegosaurus hatchling");
            Stegosaurus baby = found.get(0);
            h.assertTrue(baby.isBaby() && baby.isTamed() && baby.getOwner() != null && baby.getOwner().getUUID().equals(owner.getUUID()),
                    "tame from the first second, imprinted on the sitter");
            int shells = itemsNear(h, pos, 3.0).stream().filter(e -> e.getItem().is(DinosRegistry.eggshell(DinoKind.STEGOSAURUS)))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(shells == 3, "three Stegosaurus eggshells stay behind (" + shells + ")");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DinosRegistry.DINO_PLOW));
            baby.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(!baby.hasPlow() && !owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "a hatchling takes no plow");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FERN, 2));
            int before = baby.getAge();
            baby.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(baby.getAge() > before && owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "feeding speeds the growth up (Vanilla)");
            baby.discard();
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        MAX_TICKS.put("stegosaurus_hatches_tame_with_eggshells", 100);
        ALL.put("stegosaurus_food_heals", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Stegosaurus stego = standingStego(h, 8, 8);
            stego.setOwner(owner);
            for (Item food : new Item[] {Items.FERN, Items.LARGE_FERN, Items.WHEAT, Items.HAY_BLOCK, Items.SWEET_BERRIES}) {
                h.assertTrue(stego.isFood(new ItemStack(food)), "food: " + food);
            }
            h.assertTrue(!stego.isFood(new ItemStack(Items.BEEF)) && !stego.isFood(new ItemStack(Items.APPLE)), "no meat, no apple (that is the Triceratops')");
            stego.setHealth(50.0F);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FERN, 1));
            stego.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(stego.getHealth() == 58.0F, "fern (favourite) heals 8");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 1));
            stego.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(stego.getHealth() == 62.0F && owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "other food heals 4");
            h.succeed();
        });
        ALL.put("stegosaurus_saddle_armor_riding", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Stegosaurus stego = standingStego(h, 8, 8);
            stego.setOwner(owner);
            h.assertTrue(DinosRegistry.LARGE_SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable big
                    && big.canBeEquippedBy(stego.typeHolder()), "the large saddle fits the Stegosaurus");
            for (Item armor : new Item[] {DinosRegistry.STEGOSAURUS_ARMOR_LEATHER, DinosRegistry.STEGOSAURUS_ARMOR_IRON, DinosRegistry.STEGOSAURUS_ARMOR_DIAMOND}) {
                h.assertTrue(stego.isEquippableInSlot(new ItemStack(armor), EquipmentSlot.BODY), "Stegosaurus armor fits: " + BuiltInRegistries.ITEM.getKey(armor));
                h.assertTrue(!(armor.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable e
                        && (e.canBeEquippedBy(DinosRegistry.TREX.builtInRegistryHolder()) || e.canBeEquippedBy(DinosRegistry.TRICERATOPS.builtInRegistryHolder()))),
                        "but not the T. rex or the Triceratops (each dinosaur its own armor)");
            }
            h.assertTrue(!stego.isEquippableInSlot(new ItemStack(DinosRegistry.TRICERATOPS_ARMOR_IRON), EquipmentSlot.BODY), "Triceratops armor does not fit");
            stego.setItemSlot(EquipmentSlot.BODY, new ItemStack(DinosRegistry.STEGOSAURUS_ARMOR_IRON));
            h.assertTrue(stego.armorTier() == 2, "iron armor: tier 2");
            stego.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(stego.isSaddled() && !stego.canJump(), "saddled without a plow: the jump key does nothing");
            owner.startRiding(stego, true, false);
            h.assertTrue(stego.getControllingPassenger() == owner, "the owner rides and steers");
            stego.positionRider(owner);
            Vec3 seat = owner.position().subtract(stego.position());
            h.assertTrue(seat.z > 0.5 && seat.y > 1.5, "the rider sits over the shoulders, in front of the plates (" + seat + ")");
            h.succeed();
        });
        ALL.put("stegosaurus_plow_on_off_and_death_drop", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer owner = player(h, 3, 3), stranger = player(h, 12, 3);
            Stegosaurus stego = standingStego(h, 8, 8);
            stego.setOwner(owner);
            stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DinosRegistry.DINO_PLOW));
            stego.mobInteract(stranger, InteractionHand.MAIN_HAND);
            h.assertTrue(!stego.hasPlow(), "only the owner puts a plow on");
            ItemStack plow = new ItemStack(DinosRegistry.DINO_PLOW);
            plow.setDamageValue(17);
            owner.setItemInHand(InteractionHand.MAIN_HAND, plow);
            h.assertTrue(stego.mobInteract(owner, InteractionHand.MAIN_HAND).consumesAction() && stego.hasPlow() && !stego.plowDown()
                    && owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the owner puts the plow on (raised)");
            h.assertTrue(stego.plowStack().getDamageValue() == 17, "the plow keeps its wear");
            stego.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            owner.interactOn(stego, InteractionHand.MAIN_HAND, owner.position());
            h.assertTrue(!stego.hasPlow() && stego.isSaddled(), "shears take the plow off first, the saddle stays");
            h.assertTrue(itemsNear(h, at(8, 8), 4.0).stream().anyMatch(e -> e.getItem().is(DinosRegistry.DINO_PLOW) && e.getItem().getDamageValue() == 17),
                    "the plow drops with its wear");
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            stego.setPlow(new ItemStack(DinosRegistry.DINO_PLOW));
            stego.hurtServer(level, level.damageSources().generic(), 1000.0F);
            h.assertTrue(!stego.isAlive(), "the Stegosaurus died");
            h.assertTrue(itemsNear(h, at(8, 8), 5.0).stream().anyMatch(e -> e.getItem().is(DinosRegistry.DINO_PLOW)), "the plow drops on death");
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        ALL.put("stegosaurus_jump_key_lowers_plow", h -> {
            Stegosaurus stego = standingStego(h, 8, 8);
            stego.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(!stego.canJump() && !stego.togglePlow(), "without a plow the key does nothing");
            stego.setPlow(new ItemStack(DinosRegistry.DINO_PLOW));
            h.assertTrue(stego.canJump() && stego.getJumpCooldown() == 0, "saddle + plow: the key is ready");
            stego.handleStartJump(100);
            h.assertTrue(stego.plowDown() && stego.getJumpCooldown() == Stegosaurus.TOGGLE_TICKS, "the jump key lowers the plow (server), short lock");
            h.assertTrue(!stego.togglePlow() && stego.plowDown(), "no flicker during the lock");
            for (int i = 0; i < Stegosaurus.TOGGLE_TICKS; i++) stego.aiStep();
            h.assertTrue(stego.togglePlow() && !stego.plowDown(), "then the key raises it again");
            double y = stego.getY();
            stego.onPlayerJump(100);
            h.assertTrue(stego.getDeltaMovement().y <= 0.0 && stego.getY() == y, "the Stegosaurus never jumps");
            Stegosaurus baby = standingStego(h, 3, 12);
            baby.setAge(-1000);
            baby.setPlow(new ItemStack(DinosRegistry.DINO_PLOW));
            h.assertTrue(!baby.togglePlow() && !baby.canJump(), "a hatchling has no plow work");
            baby.discard();
            h.succeed();
        });
        ALL.put("stegosaurus_plows_three_wide", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 3, 3);
            BlockPos[] strip = strip(10);
            h.setBlock(strip[0], Blocks.GRASS_BLOCK);
            h.setBlock(strip[1], Blocks.DIRT);
            h.setBlock(strip[2], Blocks.DIRT_PATH);
            h.setBlock(new BlockPos(6, 0, 8), Blocks.GRASS_BLOCK);
            h.setBlock(new BlockPos(10, 0, 8), Blocks.GRASS_BLOCK);
            h.setBlock(new BlockPos(8, 0, 9), Blocks.GRASS_BLOCK);
            Stegosaurus stego = plowing(h, rider, 10);
            h.assertTrue(stego.strip().stream().map(p -> p.subtract(h.absolutePos(BlockPos.ZERO))).toList().equals(java.util.List.of(strip)),
                    "the strip: three blocks across, two rows behind the hips (" + stego.strip() + ")");
            Stegosaurus.Step step = stego.plowStep(level, rider);
            h.assertTrue(step.tilled() == 3 && step.refused() == 0, "three fields at once (" + step + ")");
            for (BlockPos p : strip) h.assertTrue(h.getBlockState(p).is(Blocks.FARMLAND), "farmland at " + p);
            h.assertTrue(h.getBlockState(new BlockPos(6, 0, 8)).is(Blocks.GRASS_BLOCK) && h.getBlockState(new BlockPos(10, 0, 8)).is(Blocks.GRASS_BLOCK)
                    && h.getBlockState(new BlockPos(8, 0, 9)).is(Blocks.GRASS_BLOCK), "nothing outside the strip");
            h.assertTrue(stego.plowStack().getDamageValue() == 3, "one durability per field");
            h.assertTrue(stego.plowStep(level, rider).tilled() == 0, "farmland stays farmland (nothing more to do)");
            h.succeed();
        });
        ALL.put("stegosaurus_plow_follows_the_hoe", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 3, 3);
            BlockPos[] strip = strip(10);
            h.setBlock(strip[0], Blocks.COARSE_DIRT);
            h.setBlock(strip[1], Blocks.ROOTED_DIRT);
            h.setBlock(strip[2], Blocks.GRASS_BLOCK);
            h.setBlock(strip[2].above(), Blocks.STONE); // a block above: the hoe would not till
            Stegosaurus stego = plowing(h, rider, 10);
            stego.plowStep(level, rider);
            h.assertTrue(h.getBlockState(strip[0]).is(Blocks.DIRT), "coarse dirt -> dirt (Vanilla hoe)");
            h.assertTrue(h.getBlockState(strip[1]).is(Blocks.DIRT) && itemsNear(h, strip[1], 3.0).stream().anyMatch(e -> e.getItem().is(Items.HANGING_ROOTS)),
                    "rooted dirt -> dirt and hanging roots (Vanilla hoe)");
            h.assertTrue(h.getBlockState(strip[2]).is(Blocks.GRASS_BLOCK), "grass under a block stays grass (air above needed)");
            for (BlockPos p : strip) h.setBlock(p, Blocks.STONE);
            h.setBlock(strip[2].above(), Blocks.AIR);
            h.assertTrue(stego.plowStep(level, rider).tilled() == 0, "stone is no field");
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        ALL.put("stegosaurus_plow_needs_build_rights", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 3, 3);
            BlockPos[] strip = strip(10);
            for (BlockPos p : strip) h.setBlock(p, Blocks.GRASS_BLOCK);
            Stegosaurus stego = plowing(h, rider, 10);
            var old = com.simplelib.api.InteractionGuard.permission;
            try {
                com.simplelib.api.InteractionGuard.permission = (p, pos) -> false; // a claim mod says no
                Stegosaurus.Step refused = stego.plowStep(level, rider);
                h.assertTrue(refused.tilled() == 0 && refused.refused() > 0, "a foreign claim: nothing is tilled (" + refused + ")");
                for (BlockPos p : strip) h.assertTrue(h.getBlockState(p).is(Blocks.GRASS_BLOCK), "grass stays at " + p);
                h.assertTrue(stego.plowStack().getDamageValue() == 0, "no wear without work");
                com.simplelib.api.InteractionGuard.permission = (p, pos) -> true;
                rider.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                h.assertTrue(stego.plowStep(level, rider).tilled() == 0, "adventure mode: nothing is tilled");
                rider.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            } finally {
                com.simplelib.api.InteractionGuard.permission = old;
            }
            h.succeed();
        });
        ALL.put("stegosaurus_plow_width_and_wear", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 3, 3);
            BlockPos[] strip = strip(10);
            for (BlockPos p : strip) h.setBlock(p, Blocks.GRASS_BLOCK);
            Stegosaurus stego = plowing(h, rider, 10);
            try {
                DinosConfig.stegosaurusPlowWidth = 1;
                h.assertTrue(stego.strip().size() == 1 && stego.plowStep(level, rider).tilled() == 1, "width 1: one field");
                h.assertTrue(h.getBlockState(strip[1]).is(Blocks.FARMLAND) && h.getBlockState(strip[0]).is(Blocks.GRASS_BLOCK), "the middle one");
                DinosConfig.stegosaurusPlowWidth = 3;
                h.setBlock(strip[1], Blocks.GRASS_BLOCK);
                ItemStack worn = new ItemStack(DinosRegistry.DINO_PLOW);
                worn.setDamageValue(worn.getMaxDamage() - 2);
                stego.setPlow(worn);
                stego.setPlowDown(true);
                Stegosaurus.Step last = stego.plowStep(level, rider);
                h.assertTrue(last.tilled() == 2 && !stego.hasPlow() && !stego.plowDown(), "the worn-out plow breaks after its last field (" + last + ")");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("stegosaurus_sows_from_offhand", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 3, 3);
            BlockPos[] strip = strip(10);
            for (BlockPos p : strip) h.setBlock(p, Blocks.GRASS_BLOCK);
            h.setBlock(strip[2], Blocks.FARMLAND);
            h.setBlock(strip[2].above(), Blocks.WHEAT);
            Stegosaurus stego = plowing(h, rider, 10);
            rider.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WHEAT_SEEDS, 5));
            Stegosaurus.Step step = stego.plowStep(level, rider);
            h.assertTrue(step.tilled() == 2 && step.sown() == 2, "two new fields, two seeds; the planted field is left alone (" + step + ")");
            h.assertTrue(h.getBlockState(strip[0].above()).is(Blocks.WHEAT) && h.getBlockState(strip[1].above()).is(Blocks.WHEAT), "wheat sown");
            h.assertTrue(rider.getOffhandItem().getCount() == 3, "two seeds used");
            for (BlockPos p : strip) h.setBlock(p.above(), Blocks.AIR);
            rider.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.POTATO, 1));
            h.assertTrue(stego.plowStep(level, rider).sown() == 1 && rider.getOffhandItem().isEmpty(), "potatoes too; the last one runs out");
            for (BlockPos p : strip) h.setBlock(p.above(), Blocks.AIR);
            rider.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 4));
            h.assertTrue(stego.plowStep(level, rider).sown() == 0, "no seed in the hand: nothing is sown");
            try {
                DinosConfig.stegosaurusSow = false;
                rider.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BEETROOT_SEEDS, 4));
                h.assertTrue(stego.plowStep(level, rider).sown() == 0 && rider.getOffhandItem().getCount() == 4, "sowing switched off");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("stegosaurus_never_tramples_farmland", h -> {
            ServerLevel level = h.getLevel();
            BlockPos field = new BlockPos(8, 0, 8);
            for (int x = 6; x <= 10; x++) for (int z = 6; z <= 10; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.FARMLAND);
            Stegosaurus stego = standingStego(h, 8, 8);
            boolean griefing = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
            try {
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING, true, level.getServer());
                for (int i = 0; i < 40; i++) {
                    stego.fallDistance = 2.5; // a step down onto the field: Vanilla would trample with a chance of 2.5 - 0.5
                    stego.land(-0.1, true, h.getBlockState(field), h.absolutePos(field));
                }
                h.assertTrue(h.getBlockState(field).is(Blocks.FARMLAND), "the farmland stays (40 landings)");
                h.assertTrue(stego.fallDistance == 0.0, "the fall is counted and over");
            } finally {
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING, griefing, level.getServer());
            }
            h.succeed();
        });
        ALL.put("stegosaurus_tail_spikes_hurt_monsters_not_players", h -> {
            ServerLevel level = h.getLevel();
            Stegosaurus stego = standingStego(h, 8, 8);
            net.minecraft.world.entity.monster.Monster zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 11));
            zombie.setNoAi(true);
            float health = zombie.getHealth();
            stego.hurtServer(level, level.damageSources().mobAttack(zombie), 2.0F);
            h.assertTrue(zombie.getHealth() < health, "the zombie gets 2 damage from the tail spikes (" + health + " -> " + zombie.getHealth() + ")");
            h.assertTrue(stego.getTarget() == null, "no hunt: it does not fight");
            ServerPlayer p = player(h, 8, 4);
            float ph = p.getHealth();
            stego.setInvulnerableTime(0);
            stego.hurtServer(level, level.damageSources().playerAttack(p), 1.0F);
            h.assertTrue(p.getHealth() == ph, "a player is never hurt");
            zombie.discard();
            h.succeed();
        });
        // --- Parasaurolophus (a-dinorest3, concept KONZEPT-A-DINO36 4.4) -----------------------------------------------------------
        ALL.put("parasaurolophus_core_hitbox_and_stats", h -> {
            Parasaurolophus para = standingPara(h, 8, 8);
            h.assertTrue(para.getBbWidth() == 2.2F && para.getBbHeight() == 2.5F, "core hitbox 2.2 x 2.5 (body, legs, hip ridge)");
            h.assertTrue(para.getMaxHealth() == 60.0F && para.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.24
                    && para.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) == 1.0
                    && para.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY) == 1.0,
                    "60 health, speed 0.24, step 1, water movement 1 (swims)");
            h.assertTrue(!para.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "no attack: only the T. rex fights");
            h.assertTrue(para.kind() == DinoKind.PARASAUROLOPHUS && !para.canMate(para), "kind, no breeding");
            h.assertTrue(Parasaurolophus.RIG.solidHead && Parasaurolophus.RIG.reach > 2.45 && Parasaurolophus.RIG.reach < 2.7,
                    "solid head (the neck base stands in front of the shoulders), the bill reaches 2.57 (" + Parasaurolophus.RIG.reach + ")");
            double front0 = 0.0, front4 = 0.0, top0 = 0.0;
            for (AABB b : para.headBoxes(0)) {
                front0 = Math.max(front0, b.maxZ - para.getZ());
                top0 = Math.max(top0, b.maxY - para.getY());
            }
            for (AABB b : para.headBoxes(com.simpledinos.entity.part.PartRig.TUCK_STEPS)) front4 = Math.max(front4, b.maxZ - para.getZ());
            h.assertTrue(front4 < front0 - 0.3, "pulled in, the long neck rises and the bill comes back (" + front0 + " -> " + front4 + ")");
            h.assertTrue(top0 > 3.0 && top0 < 3.5, "the crest tip stands about 3.2 blocks high (" + top0 + ")");
            double back = 0.0, side = 0.0;
            for (AABB b : para.tailBoxes(0)) {
                back = Math.max(back, para.getZ() - b.minZ);
                side = Math.max(side, Math.max(b.maxX - para.getX(), para.getX() - b.minX));
            }
            h.assertTrue(back > 3.4 && side < 0.4, "a long, narrow tail (" + back + " back, " + side + " to the side)");
            h.succeed();
        });
        ALL.put("parasaurolophus_head_and_tail_parts", h -> {
            Parasaurolophus para = standingPara(h, 8, 2);
            for (int x = 5; x <= 11; x++) for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
            Vec3 allowed = para.clipHead(new Vec3(0.0, 0.0, 2.0));
            h.assertTrue(allowed.z < 1.9 && allowed.z > 0.4, "the bill stops the walk in front of the wall (" + allowed.z + " of 2.0)");
            for (int x = 5; x <= 11; x++) for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, 6), Blocks.OAK_LEAVES);
            h.assertTrue(para.clipHead(new Vec3(0.0, 0.0, 2.0)).z == 2.0, "leaves do not stop the head");
            para.discard();
            Parasaurolophus other = standingPara(h, 8, 11);
            other.updateParts();
            h.assertTrue(other.headTuck() == 0 && other.tailBend() == 0, "in the open field: head stretched, tail straight");
            for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(8, y, 7), Blocks.STONE);
            h.assertTrue(!clearOfBlocks(h, other, other.tailBoxes(0)), "the straight tail would stick in the pillar");
            other.updateParts();
            h.assertTrue(other.tailBend() != 0 && clearOfBlocks(h, other, other.tailBoxes(other.tailBend())), "the tail bends away (" + other.tailBend() + ")");
            ServerPlayer p = player(h, 8, 14);
            Vec3 at = h.absoluteVec(new Vec3(8.5, 2.0, 14.3));
            p.setPos(at.x, at.y, at.z);
            AABB body = p.getBoundingBox();
            h.assertTrue(other.headBoxes(0).stream().anyMatch(b -> b.intersects(body)), "the stretched bill would touch the player");
            other.updateParts();
            h.assertTrue(other.headTuck() > 0 && other.headBoxes(other.headTuck()).stream().noneMatch(b -> b.intersects(body)),
                    "the neck rises and the head comes back before the player (step " + other.headTuck() + ")");
            h.succeed();
        });
        ALL.put("parasaurolophus_hatches_tame_with_eggshells", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            h.setBlock(pos, DinosRegistry.EGGS.get(DinoKind.PARASAUROLOPHUS));
            DinoEggBlockEntity be = (DinoEggBlockEntity) level.getBlockEntity(h.absolutePos(pos));
            ServerPlayer owner = player(h, 5, 5);
            brood(level, be, owner.getUUID());
            be.advance(level, be.hatchAt(), null);
            h.assertTrue(h.getBlockState(pos).isAir(), "the Parasaurolophus egg hatches");
            var found = level.getEntitiesOfClass(Parasaurolophus.class, new AABB(h.absolutePos(pos)).inflate(3.0));
            h.assertTrue(found.size() == 1, "one Parasaurolophus hatchling");
            Parasaurolophus baby = found.get(0);
            h.assertTrue(baby.isBaby() && baby.isTamed() && baby.getOwner() != null && baby.getOwner().getUUID().equals(owner.getUUID()),
                    "tame from the first second, imprinted on the sitter");
            int shells = itemsNear(h, pos, 3.0).stream().filter(e -> e.getItem().is(DinosRegistry.eggshell(DinoKind.PARASAUROLOPHUS)))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(shells == 3, "three Parasaurolophus eggshells stay behind (" + shells + ")");
            h.assertTrue(!baby.canJump() && baby.press(owner) == Parasaurolophus.Press.NONE && baby.herd().isEmpty(), "a hatchling has no call");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 2));
            int before = baby.getAge();
            baby.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(baby.getAge() > before && owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "feeding speeds the growth up (Vanilla)");
            baby.discard();
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        MAX_TICKS.put("parasaurolophus_hatches_tame_with_eggshells", 100);
        ALL.put("parasaurolophus_food_heals", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Parasaurolophus para = standingPara(h, 8, 8);
            para.setOwner(owner);
            for (Item food : new Item[] {Items.SWEET_BERRIES, Items.WHEAT, Items.HAY_BLOCK, Items.BEETROOT}) {
                h.assertTrue(para.isFood(new ItemStack(food)), "food: " + food);
            }
            h.assertTrue(!para.isFood(new ItemStack(Items.BEEF)) && !para.isFood(new ItemStack(Items.FERN)) && !para.isFood(new ItemStack(Items.APPLE)),
                    "no meat, no fern (the Stegosaurus'), no apple (the Triceratops')");
            para.setHealth(40.0F);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 1));
            para.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(para.getHealth() == 48.0F, "sweet berries (favourite) heal 8");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEETROOT, 1));
            para.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(para.getHealth() == 52.0F && owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "other food heals 4");
            h.succeed();
        });
        ALL.put("parasaurolophus_saddle_armor_riding", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Parasaurolophus para = standingPara(h, 8, 8);
            para.setOwner(owner);
            h.assertTrue(DinosRegistry.LARGE_SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable big
                    && big.canBeEquippedBy(para.typeHolder()), "the large saddle fits the Parasaurolophus");
            for (Item armor : new Item[] {DinosRegistry.PARASAUROLOPHUS_ARMOR_LEATHER, DinosRegistry.PARASAUROLOPHUS_ARMOR_IRON, DinosRegistry.PARASAUROLOPHUS_ARMOR_DIAMOND}) {
                h.assertTrue(para.isEquippableInSlot(new ItemStack(armor), EquipmentSlot.BODY), "Parasaurolophus armor fits: " + BuiltInRegistries.ITEM.getKey(armor));
                h.assertTrue(!(armor.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable e
                        && (e.canBeEquippedBy(DinosRegistry.STEGOSAURUS.builtInRegistryHolder()) || e.canBeEquippedBy(DinosRegistry.TRICERATOPS.builtInRegistryHolder()))),
                        "but not the Stegosaurus or the Triceratops (each dinosaur its own armor)");
            }
            h.assertTrue(!para.isEquippableInSlot(new ItemStack(DinosRegistry.STEGOSAURUS_ARMOR_IRON), EquipmentSlot.BODY), "Stegosaurus armor does not fit");
            para.setItemSlot(EquipmentSlot.BODY, new ItemStack(DinosRegistry.PARASAUROLOPHUS_ARMOR_DIAMOND));
            h.assertTrue(para.armorTier() == 3, "diamond armor: tier 3");
            h.assertTrue(!para.canJump(), "without a saddle the jump key does nothing");
            para.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            h.assertTrue(para.isSaddled() && para.canJump(), "saddled: the jump key is the call");
            owner.startRiding(para, true, false);
            h.assertTrue(para.getControllingPassenger() == owner, "the owner rides and steers");
            para.positionRider(owner);
            Vec3 seat = owner.position().subtract(para.position());
            h.assertTrue(seat.y > 1.4 && Math.abs(seat.z) < 0.4, "the rider sits on the middle of the back, clear of the crest (" + seat + ")");
            double y = para.getY();
            para.onPlayerJump(100);
            h.assertTrue(para.getDeltaMovement().y <= 0.0 && para.getY() == y, "the Parasaurolophus never jumps");
            h.succeed();
        });
        ALL.put("parasaurolophus_call_picks_by_food", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 2, 13);
            Parasaurolophus para = standingPara(h, 8, 8);
            para.setOwner(rider);
            var cow = still(h, net.minecraft.world.entity.EntityTypes.COW, 3, 3);
            var cow2 = still(h, net.minecraft.world.entity.EntityTypes.COW, 5, 3);
            var pig = still(h, net.minecraft.world.entity.EntityTypes.PIG, 7, 3);
            var chicken = still(h, net.minecraft.world.entity.EntityTypes.CHICKEN, 9, 3);
            var sheep = still(h, net.minecraft.world.entity.EntityTypes.SHEEP, 11, 3);
            var wolf = still(h, net.minecraft.world.entity.EntityTypes.WOLF, 13, 3);
            java.util.function.Function<List<? extends net.minecraft.world.entity.Entity>, java.util.Set<UUID>> ids =
                    list -> list.stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            DinosConfig.callRadius = TEST_CALL_RADIUS;
            try {
                rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
                var picked = para.call(level, rider);
                h.assertTrue(ids.apply(picked).equals(java.util.Set.of(cow.getUUID(), cow2.getUUID(), sheep.getUUID())), "wheat calls the cows and the sheep (" + picked + ")");
                h.assertTrue(para.herd().equals(picked.stream().map(net.minecraft.world.entity.Entity::getUUID).toList())
                        && para.herdUntil() == level.getGameTime() + 2400L, "they follow for 120 seconds");
                rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT));
                h.assertTrue(ids.apply(para.call(level, rider)).equals(java.util.Set.of(pig.getUUID())), "a carrot calls the pig; a new call replaces the herd");
                rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS));
                h.assertTrue(ids.apply(para.call(level, rider)).equals(java.util.Set.of(chicken.getUUID())), "seeds call the chicken");
                rider.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                var all = ids.apply(para.call(level, rider));
                h.assertTrue(all.equals(java.util.Set.of(cow.getUUID(), cow2.getUUID(), pig.getUUID(), chicken.getUUID(), sheep.getUUID())),
                        "an empty hand calls every farm animal, never the wolf (not a herd animal)");
                h.assertTrue(!all.contains(wolf.getUUID()) && !all.contains(para.getUUID()), "not the wolf, not itself");
                rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
                h.assertTrue(para.call(level, rider).isEmpty() && para.herd().isEmpty(), "an item nobody eats: nobody follows");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("parasaurolophus_call_excludes", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 2, 13), stranger = player(h, 14, 13);
            Parasaurolophus para = standingPara(h, 8, 8);
            para.setOwner(rider);
            var foreign = still(h, net.minecraft.world.entity.EntityTypes.HORSE, 2, 3);
            foreign.setTamed(true);
            foreign.setOwner(stranger);
            var own = still(h, net.minecraft.world.entity.EntityTypes.HORSE, 5, 3);
            own.setTamed(true);
            own.setOwner(rider);
            var leashed = still(h, net.minecraft.world.entity.EntityTypes.COW, 8, 3);
            leashed.setLeashedTo(stranger, true);
            var named = still(h, net.minecraft.world.entity.EntityTypes.COW, 11, 3);
            named.setCustomName(net.minecraft.network.chat.Component.literal("Bessie"));
            var ridden = still(h, net.minecraft.world.entity.EntityTypes.HORSE, 2, 13);
            stranger.startRiding(ridden, true, false);
            var free = still(h, net.minecraft.world.entity.EntityTypes.COW, 13, 13);
            var old = com.simplelib.api.InteractionGuard.permission;
            DinosConfig.callRadius = TEST_CALL_RADIUS;
            try {
                var picked = para.call(level, rider).stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
                h.assertTrue(picked.equals(java.util.Set.of(own.getUUID(), free.getUUID())),
                        "only the own horse and the free cow: not tamed by someone else, not leashed, not named, not ridden (" + picked + ")");
                DinosConfig.callIgnoreNamed = false;
                h.assertTrue(para.call(level, rider).stream().anyMatch(a -> a == named), "callIgnoreNamed off: the named cow follows too");
                DinosConfig.callIgnoreNamed = true;
                int[] probes = {0};
                com.simplelib.api.InteractionGuard.permission = (p, pos) -> {
                    probes[0]++;
                    return false; // a claim mod says no everywhere
                };
                h.assertTrue(para.call(level, rider).isEmpty(), "animals in a claim the rider may not use stay");
                h.assertTrue(probes[0] >= 1 && probes[0] <= 4, "the claim probe runs once per chunk (" + probes[0] + ")");
            } finally {
                com.simplelib.api.InteractionGuard.permission = old;
                DinosConfig.reset();
                stranger.stopRiding();
            }
            h.succeed();
        });
        ALL.put("parasaurolophus_call_max_animals", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 1, 14);
            Parasaurolophus para = standingPara(h, 8, 8);
            List<net.minecraft.world.entity.animal.chicken.Chicken> chickens = new java.util.ArrayList<>();
            for (int z : new int[] {3, 4, 13}) for (int x = 2; x <= 14; x++) chickens.add(still(h, net.minecraft.world.entity.EntityTypes.CHICKEN, x, z));
            DinosConfig.callRadius = TEST_CALL_RADIUS;
            try {
                h.assertTrue(chickens.stream().allMatch(c -> para.distanceToSqr(c) <= 64.0), "39 chickens within 8 blocks");
                var picked = para.call(level, rider);
                h.assertTrue(picked.size() == 24, "at most 24 animals (" + picked.size() + " of 39)");
                double farIn = 0.0, nearOut = Double.MAX_VALUE;
                for (var c : chickens) {
                    if (picked.contains(c)) farIn = Math.max(farIn, para.distanceToSqr(c));
                    else nearOut = Math.min(nearOut, para.distanceToSqr(c));
                }
                h.assertTrue(farIn <= nearOut + 1.0E-6, "the nearest ones first (" + farIn + " <= " + nearOut + ")");
                DinosConfig.callMaxAnimals = 5;
                h.assertTrue(para.call(level, rider).size() == 5, "callMaxAnimals 5: five follow");
                DinosConfig.callMaxAnimals = DinosConfig.CALL_ANIMALS_DEFAULT;
                DinosConfig.callRadius = 6;
                long within = chickens.stream().filter(c -> para.distanceToSqr(c) <= 36.0).count();
                var close = para.call(level, rider);
                h.assertTrue(within > 0 && within < 24 && close.size() == within && close.stream().allMatch(c -> para.distanceToSqr(c) <= 36.0),
                        "callRadius 6: only the animals within 6 blocks (" + close.size() + " of " + within + ")");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("parasaurolophus_call_cooldown_and_dismiss", h -> {
            ServerPlayer rider = player(h, 3, 3);
            Parasaurolophus para = standingPara(h, 8, 8);
            para.setOwner(rider);
            still(h, net.minecraft.world.entity.EntityTypes.COW, 3, 13);
            para.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            rider.startRiding(para, true, false);
            DinosConfig.callRadius = TEST_CALL_RADIUS;
            try {
                h.assertTrue(para.canJump() && para.getJumpCooldown() == 0, "saddled and ridden: the call is ready");
                para.handleStartJump(60);
                h.assertTrue(para.herd().size() == 1 && para.abilityCooldown() == 400 && para.dismissTicks() == Parasaurolophus.DISMISS_TICKS,
                        "the jump key calls (server): one cow follows, 20 s cooldown, 5 s to dismiss");
                h.assertTrue(para.getJumpCooldown() == 0, "the jump bar stays free while a second press dismisses");
                h.assertTrue(para.press(rider) == Parasaurolophus.Press.DISMISSED && para.herd().isEmpty() && para.getJumpCooldown() > 0,
                        "a second press right after the call dismisses the herd; then the cooldown shows");
                h.assertTrue(para.press(rider) == Parasaurolophus.Press.COOLING && para.herd().isEmpty(), "no new call while it cools down");
                para.setAbilityCooldown(0);
                h.assertTrue(para.press(rider) == Parasaurolophus.Press.CALLED && para.herd().size() == 1, "after the cooldown it calls again");
                for (int i = 0; i < Parasaurolophus.DISMISS_TICKS; i++) para.aiStep();
                h.assertTrue(para.dismissTicks() == 0 && para.getJumpCooldown() > 0 && para.getJumpCooldown() == para.abilityCooldown(),
                        "after 5 s the jump bar shows the rest of the cooldown (" + para.getJumpCooldown() + ")");
                h.assertTrue(para.press(rider) == Parasaurolophus.Press.COOLING && para.herd().size() == 1, "and the herd keeps following");
            } finally {
                DinosConfig.reset();
                rider.stopRiding();
                para.release();
            }
            h.succeed();
        });
        ALL.put("parasaurolophus_herd_follows_and_ends", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer rider = player(h, 1, 14);
            Parasaurolophus para = standingPara(h, 8, 8);
            var cow = still(h, net.minecraft.world.entity.EntityTypes.COW, 8, 1);
            var chicken = still(h, net.minecraft.world.entity.EntityTypes.CHICKEN, 8, 11);
            DinosConfig.callRadius = TEST_CALL_RADIUS;
            try {
                h.assertTrue(para.call(level, rider).size() == 2, "the cow and the chicken follow");
                para.lead(level, cow, true);
                h.assertTrue(cow.getNavigation().isInProgress(), "the far cow walks toward the Parasaurolophus (its own navigation)");
                chicken.getNavigation().moveTo(chicken.getX() + 2.0, chicken.getY(), chicken.getZ(), 1.0);
                para.lead(level, chicken, false);
                h.assertTrue(!chicken.getNavigation().isInProgress(), "the chicken close by stops");
                long now = level.getGameTime();
                for (int k = 0; k < Parasaurolophus.FOLLOW_STEP; k++) para.herdStep(level, now + k);
                h.assertTrue(para.herd().size() == 2, "every follower had its turn and stays");
                cow.setLeashedTo(rider, true);
                for (int k = 0; k < Parasaurolophus.FOLLOW_STEP; k++) para.herdStep(level, now + k);
                h.assertTrue(para.herd().equals(List.of(chicken.getUUID())), "a leashed follower drops out");
                cow.removeLeash();
                para.herdStep(level, para.herdUntil());
                h.assertTrue(para.herd().isEmpty() && para.herdUntil() == 0L, "after 120 s the herd goes its own way");
                h.assertTrue(para.call(level, rider).size() == 2, "called again");
                para.lead(level, cow, false);
                para.discard();
                h.assertTrue(para.herd().isEmpty() && !cow.getNavigation().isInProgress(), "the herd ends with the Parasaurolophus (never saved)");
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("parasaurolophus_swims_with_rider", h -> {
            for (int i = 1; i <= 14; i++) {
                for (int y = 1; y <= 5; y++) {
                    h.setBlock(new BlockPos(i, y, 1), Blocks.STONE);
                    h.setBlock(new BlockPos(i, y, 14), Blocks.STONE);
                    h.setBlock(new BlockPos(1, y, i), Blocks.STONE);
                    h.setBlock(new BlockPos(14, y, i), Blocks.STONE);
                }
            }
            for (int x = 2; x <= 13; x++) for (int z = 2; z <= 13; z++) for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
            ServerPlayer rider = player(h, 3, 3);
            Parasaurolophus para = standingPara(h, 8, 8);
            para.setOwner(rider);
            para.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            rider.startRiding(para, true, false);
            h.assertTrue(para.is(net.minecraft.tags.EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN) && !para.dismountsUnderwater(),
                    "it floats while ridden like a horse, but never throws its rider off in water");
            double floor = h.absolutePos(new BlockPos(0, 1, 0)).getY(), surface = h.absolutePos(new BlockPos(0, 4, 0)).getY() + 8.0 / 9.0;
            para.setDeltaMovement(Vec3.ZERO);
            // the rider's client moves a ridden animal: its travel, tick by tick (Vanilla floats a can_float_while_ridden vehicle up to getFluidJumpThreshold)
            for (int i = 0; i < 120; i++) {
                para.baseTick();
                para.travel(Vec3.ZERO);
            }
            double depth = surface - para.getY();
            h.assertTrue(para.isInWater() && para.getY() > floor + 1.5 && depth > 0.5 && depth < 1.8,
                    "it rose from the pool floor and swims with its back out of the water (depth " + depth + ")");
            para.positionRider(rider);
            h.assertTrue(rider.getVehicle() == para && rider.getEyeY() > surface + 1.0, "the rider stays on, high and dry (eyes " + (rider.getEyeY() - surface) + " above)");
            h.assertTrue(Math.abs(para.getRiddenSpeed(rider) - 0.24F * Parasaurolophus.WATER_SPEED) < 1.0E-4F, "slower in the water (0.8 x, swimming about 0.6 x)");
            rider.stopRiding();
            h.succeed();
        });
        ALL.put("parasaurolophus_warns_and_flees", h -> {
            ServerLevel level = h.getLevel();
            Parasaurolophus para = standingPara(h, 8, 8);
            net.minecraft.world.entity.monster.Monster zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 12));
            zombie.setNoAi(true);
            float health = zombie.getHealth();
            h.assertTrue(para.hurtServer(level, level.damageSources().mobAttack(zombie), 2.0F), "the zombie hits it");
            h.assertTrue(!para.warn(level), "the warning call came with the hit (and waits 3 s before the next)");
            h.assertTrue(zombie.getHealth() == health && para.getTarget() == null, "it never fights back");
            zombie.discard();
            ServerPlayer rider = player(h, 3, 3);
            Parasaurolophus other = standingPara(h, 3, 12);
            other.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            rider.startRiding(other, true, false);
            other.hurtServer(level, level.damageSources().playerAttack(rider), 1.0F);
            h.assertTrue(other.warn(level), "a slap by its own rider is no danger: no warning call");
            rider.stopRiding();
            h.succeed();
        });
        // --- Utahraptor (b-utah48, concept KONZEPT-A-DINO36 4.3) --------------------------------------------------------------------
        ALL.put("utahraptor_core_hitbox_and_stats", h -> {
            Utahraptor raptor = standingRaptor(h, 8, 8);
            h.assertTrue(raptor.getBbWidth() == 1.3F && raptor.getBbHeight() == 1.6F, "core hitbox 1.3 x 1.6 (body, legs, saddle; fits 2-wide corridors)");
            h.assertTrue(raptor.getMaxHealth() == 40.0F && raptor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.3
                    && raptor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH) == 0.7
                    && raptor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) == 1.5,
                    "40 health, speed 0.30 (below the best horses' 0.3375), jump 0.7 (about 3 blocks), step 1.5");
            h.assertTrue(raptor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER) == 0.5
                    && raptor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE) == 6.0,
                    "half fall damage, 3 more safe blocks");
            h.assertTrue(!raptor.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "no attack: only the T. rex fights");
            h.assertTrue(raptor.kind() == DinoKind.UTAHRAPTOR && !raptor.canMate(raptor), "kind, no breeding");
            h.assertTrue(!Utahraptor.RIG.solidHead && Utahraptor.RIG.reach > 1.15 && Utahraptor.RIG.reach < 1.4,
                    "head not solid, the snout reaches about 1.27 (" + Utahraptor.RIG.reach + ")");
            double front4 = 0.0, top0 = 0.0;
            for (AABB b : raptor.headBoxes(0)) top0 = Math.max(top0, b.maxY - raptor.getY());
            for (AABB b : raptor.headBoxes(com.simpledinos.entity.part.PartRig.TUCK_STEPS)) front4 = Math.max(front4, b.maxZ - raptor.getZ());
            h.assertTrue(front4 < raptor.getBbWidth() / 2.0, "pulled in, the head stays inside the front of the core box (" + front4 + ")");
            h.assertTrue(top0 > 1.8 && top0 < 2.2, "the head stands about 2 blocks high (" + top0 + ")");
            double back = 0.0, side = 0.0;
            for (AABB b : raptor.tailBoxes(0)) {
                back = Math.max(back, raptor.getZ() - b.minZ);
                side = Math.max(side, Math.max(b.maxX - raptor.getX(), raptor.getX() - b.minX));
            }
            h.assertTrue(raptor.headBoxes(0).size() == 1 && raptor.tailBoxes(0).size() == 1, "one head box, one tail box (N45)");
            h.assertTrue(back > 2.2 && back < 2.6 && side < 0.3, "a long, stiff, narrow tail (" + back + " back, " + side + " to the side)");
            h.succeed();
        });
        ALL.put("utahraptor_head_and_tail_parts", h -> {
            Utahraptor raptor = standingRaptor(h, 8, 11);
            raptor.updateParts();
            h.assertTrue(raptor.headTuck() == 0 && raptor.tailBend() == 0, "in the open field: head stretched, tail straight");
            // a thin fence post behind it: the one tail box (hull of the bent segments) is free only bent far to a side
            for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(8, y, 9), Blocks.OAK_FENCE);
            h.assertTrue(!clearOfBlocks(h, raptor, raptor.tailBoxes(0)), "the straight tail would stick in the fence post");
            raptor.updateParts();
            h.assertTrue(raptor.tailBend() != 0 && clearOfBlocks(h, raptor, raptor.tailBoxes(raptor.tailBend())), "the tail bends away (" + raptor.tailBend() + ")");
            Utahraptor other = standingRaptor(h, 8, 3);
            ServerPlayer p = player(h, 8, 5);
            Vec3 at = h.absoluteVec(new Vec3(8.5, 1.0, 4.46));
            p.setPos(at.x, at.y, at.z);
            AABB body = p.getBoundingBox();
            h.assertTrue(other.headBoxes(0).stream().anyMatch(b -> b.intersects(body)), "the stretched head would touch the player");
            other.updateParts();
            h.assertTrue(other.headTuck() > 0 && other.headBoxes(other.headTuck()).stream().noneMatch(b -> b.intersects(body)),
                    "the S-neck stands up and the head comes back before the player (step " + other.headTuck() + ")");
            h.succeed();
        });
        ALL.put("utahraptor_hatches_tame_with_eggshells", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            h.setBlock(pos, DinosRegistry.EGGS.get(DinoKind.UTAHRAPTOR));
            DinoEggBlockEntity be = (DinoEggBlockEntity) level.getBlockEntity(h.absolutePos(pos));
            ServerPlayer owner = player(h, 5, 5);
            brood(level, be, owner.getUUID());
            be.advance(level, be.hatchAt(), null);
            h.assertTrue(h.getBlockState(pos).isAir(), "the Utahraptor egg hatches");
            var found = level.getEntitiesOfClass(Utahraptor.class, new AABB(h.absolutePos(pos)).inflate(3.0));
            h.assertTrue(found.size() == 1, "one Utahraptor hatchling");
            Utahraptor baby = found.get(0);
            h.assertTrue(baby.isBaby() && baby.isTamed() && baby.getOwner() != null && baby.getOwner().getUUID().equals(owner.getUUID()),
                    "tame from the first second, imprinted on the sitter");
            int shells = itemsNear(h, pos, 3.0).stream().filter(e -> e.getItem().is(DinosRegistry.eggshell(DinoKind.UTAHRAPTOR)))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(shells == 3, "three Utahraptor eggshells stay behind (" + shells + ")");
            h.assertTrue(!baby.climbAllowed() && !baby.brushFeather(level, level.getGameTime()), "a hatchling neither climbs nor gives feathers");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHICKEN, 2));
            int before = baby.getAge();
            baby.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(baby.getAge() > before && owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "feeding speeds the growth up (Vanilla)");
            baby.discard();
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        MAX_TICKS.put("utahraptor_hatches_tame_with_eggshells", 100);
        ALL.put("utahraptor_food_heals", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Utahraptor raptor = standingRaptor(h, 8, 8);
            raptor.setOwner(owner);
            for (Item food : new Item[] {Items.CHICKEN, Items.RABBIT, Items.BEEF, Items.COOKED_PORKCHOP}) h.assertTrue(raptor.isFood(new ItemStack(food)), "food: " + food);
            h.assertTrue(!raptor.isFood(new ItemStack(Items.WHEAT)) && !raptor.isFood(new ItemStack(Items.APPLE)), "no plants");
            raptor.setHealth(20.0F);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RABBIT, 1));
            raptor.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(raptor.getHealth() == 28.0F, "raw rabbit (favourite) heals 8");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 1));
            raptor.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(raptor.getHealth() == 32.0F && owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "other meat heals 4");
            h.succeed();
        });
        ALL.put("utahraptor_saddle_armor_riding", h -> {
            ServerPlayer owner = player(h, 3, 3);
            Utahraptor raptor = standingRaptor(h, 8, 8);
            raptor.setOwner(owner);
            h.assertTrue(DinosRegistry.LARGE_SADDLE.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable big
                    && big.canBeEquippedBy(raptor.typeHolder()), "the large saddle fits the Utahraptor");
            for (Item armor : new Item[] {DinosRegistry.UTAHRAPTOR_ARMOR_LEATHER, DinosRegistry.UTAHRAPTOR_ARMOR_IRON, DinosRegistry.UTAHRAPTOR_ARMOR_DIAMOND}) {
                h.assertTrue(raptor.isEquippableInSlot(new ItemStack(armor), EquipmentSlot.BODY), "Utahraptor armor fits: " + BuiltInRegistries.ITEM.getKey(armor));
                h.assertTrue(!(armor.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE) instanceof Equippable e
                        && (e.canBeEquippedBy(DinosRegistry.PARASAUROLOPHUS.builtInRegistryHolder()) || e.canBeEquippedBy(DinosRegistry.TREX.builtInRegistryHolder()))),
                        "but not the Parasaurolophus or the T. rex (each dinosaur its own armor)");
            }
            h.assertTrue(!raptor.isEquippableInSlot(new ItemStack(DinosRegistry.PARASAUROLOPHUS_ARMOR_IRON), EquipmentSlot.BODY), "Parasaurolophus armor does not fit");
            raptor.setItemSlot(EquipmentSlot.BODY, new ItemStack(DinosRegistry.UTAHRAPTOR_ARMOR_IRON));
            h.assertTrue(raptor.armorTier() == 2, "iron armor: tier 2");
            raptor.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(DinosRegistry.LARGE_SADDLE));
            owner.startRiding(raptor, true, false);
            h.assertTrue(raptor.getControllingPassenger() == owner && raptor.climbAllowed(), "the owner rides and steers; saddled and ridden it may climb");
            raptor.positionRider(owner);
            Vec3 seat = owner.position().subtract(raptor.position());
            h.assertTrue(seat.y > 0.8 && seat.y < 1.2 && Math.abs(seat.z) < 0.3, "the rider sits on the saddle in the middle of the back (" + seat + ")");
            h.assertTrue(raptor.getJumpCooldown() == 0, "rested: the jump bar is free, it jumps like a horse");
            h.succeed();
        });
        ALL.put("utahraptor_climbs_wall", h -> {
            ServerPlayer rider = player(h, 3, 3);
            Utahraptor raptor = riddenAtWall(h, rider, 7, 9, Blocks.STONE);
            double y0 = raptor.getY();
            boolean climbed = false;
            for (int i = 0; i < 40; i++) {
                rideForward(h, raptor);
                climbed |= raptor.onClimbable();
            }
            double gained = raptor.getY() - y0;
            h.assertTrue(climbed && raptor.touchesClimbableWall(), "at the stone wall it climbs (Vanilla climbing rule)");
            h.assertTrue(gained > 3.0 && gained < 6.0, "about 2.35 blocks a second up the wall (" + gained + " blocks in 2 s)");
            h.assertTrue(raptor.stamina() < Utahraptor.STAMINA_FULL && raptor.stamina() > 0 && !raptor.exhausted(), "climbing uses stamina (" + raptor.stamina() + ")");
            h.assertTrue(raptor.fallDistance == 0.0, "no fall height while climbing");
            rider.stopRiding();
            raptor.forgetWall();
            h.assertTrue(!raptor.onClimbable(), "without a rider it never climbs (no AI climbing)");
            h.succeed();
        });
        ALL.put("utahraptor_stamina_runs_out_and_slides", h -> {
            try {
                DinosConfig.raptorClimbSeconds = 1;
                ServerPlayer rider = player(h, 3, 3);
                Utahraptor raptor = riddenAtWall(h, rider, 7, 9, Blocks.STONE);
                float health = raptor.getHealth(), riderHealth = rider.getHealth();
                int i = 0;
                while (!raptor.exhausted() && i++ < 80) rideForward(h, raptor);
                h.assertTrue(raptor.exhausted() && raptor.stamina() == 0, "after 1 s of climbing the stamina is used up (" + i + " ticks)");
                double top = raptor.getY() - h.absoluteVec(new Vec3(0, 1, 0)).y;
                h.assertTrue(top > 1.5, "it got up the wall first (" + top + ")");
                h.assertTrue(!raptor.onClimbable() && raptor.getJumpCooldown() > 0, "exhausted: no more climbing, the jump bar shows the recharge");
                double minDy = 0.0;
                int slide = 0;
                while (!raptor.onGround() && slide++ < 200) {
                    double y = raptor.getY();
                    rideForward(h, raptor);
                    minDy = Math.min(minDy, raptor.getY() - y);
                    h.assertTrue(raptor.fallDistance == 0.0, "sliding builds up no fall height");
                }
                h.assertTrue(raptor.onGround() && minDy >= -Utahraptor.SLIDE_SPEED - 1.0E-6, "it slid down slowly (fastest " + minDy + " per tick)");
                h.assertTrue(raptor.getHealth() == health && rider.getHealth() == riderHealth, "no fall damage for raptor and rider");
                for (int t = 0; t < Utahraptor.RECHARGE_TICKS && raptor.exhausted(); t++) raptor.staminaStep(h.getLevel());
                h.assertTrue(!raptor.exhausted() && raptor.stamina() == Utahraptor.STAMINA_FULL && raptor.getJumpCooldown() == 0,
                        "on the ground it is rested again within 5 s");
                rider.stopRiding();
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("utahraptor_unclimbable_and_config", h -> {
            try {
                ServerPlayer rider = player(h, 3, 3);
                Utahraptor raptor = riddenAtWall(h, rider, 7, 9, Blocks.STONE);
                for (int i = 0; i < 4; i++) rideForward(h, raptor);
                h.assertTrue(raptor.touchesClimbableWall() && raptor.onClimbable(), "stone: a hold for the claws");
                for (Block b : new Block[] {Blocks.GLASS, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
                        BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_stained_glass")), Blocks.TINTED_GLASS}) {
                    for (int x = 4; x <= 12; x++) for (int y = 1; y <= 7; y++) h.setBlock(new BlockPos(x, y, 9), b);
                    raptor.forgetWall();
                    h.assertTrue(!raptor.touchesClimbableWall() && !raptor.onClimbable(), "no hold on " + BuiltInRegistries.BLOCK.getKey(b) + " (tag raptor_unclimbable)");
                }
                for (int x = 4; x <= 12; x++) for (int y = 1; y <= 7; y++) h.setBlock(new BlockPos(x, y, 9), Blocks.OAK_PLANKS);
                raptor.forgetWall();
                h.assertTrue(raptor.onClimbable(), "planks: climbable");
                DinosConfig.raptorClimbSeconds = 0;
                raptor.forgetWall();
                h.assertTrue(!raptor.climbAllowed() && !raptor.onClimbable(), "raptorClimbSeconds 0 switches climbing off");
                DinosConfig.reset();
                raptor.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
                h.assertTrue(!raptor.climbAllowed(), "without a saddle nobody steers it: no climbing");
                rider.stopRiding();
            } finally {
                DinosConfig.reset();
            }
            h.succeed();
        });
        ALL.put("utahraptor_fall_damage_halved", h -> {
            ServerLevel level = h.getLevel();
            // a mock player cannot be hurt (no loaded client), so a zombie rides: the fall is passed on the same way to every passenger
            Utahraptor raptor = standingRaptor(h, 8, 8);
            net.minecraft.world.entity.monster.Monster rider = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 8));
            rider.setNoAi(true);
            rider.startRiding(raptor, true, false);
            h.assertTrue(rider.getVehicle() == raptor, "the zombie sits on the raptor");
            float health = raptor.getHealth(), riderHealth = rider.getHealth();
            raptor.causeFallDamage(5.0, 1.0F, level.damageSources().fall());
            h.assertTrue(raptor.getHealth() == health && rider.getHealth() == riderHealth, "5 blocks are safe for both (3 more than Vanilla)");
            raptor.causeFallDamage(10.0, 1.0F, level.damageSources().fall());
            h.assertTrue(raptor.getHealth() == health - 2.0F, "10 blocks: (10 - 6) x 0.5 = 2 damage for the raptor (" + (health - raptor.getHealth()) + ")");
            h.assertTrue(rider.getHealth() == riderHealth - 2.0F, "and 2 for the rider instead of 7 (" + (riderHealth - rider.getHealth()) + ")");
            rider.stopRiding();
            h.succeed();
        });
        ALL.put("utahraptor_feather_brush", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer p = player(h, 8, 5);
            Utahraptor raptor = standingRaptor(h, 8, 8);
            ItemStack brush = new ItemStack(Items.BRUSH);
            p.setItemInHand(InteractionHand.MAIN_HAND, brush);
            h.assertTrue(raptor.mobInteract(p, InteractionHand.MAIN_HAND).consumesAction(), "brushing a grown raptor works");
            int feathers = itemsNear(h, at(8, 8), 3.0).stream().filter(e -> e.getItem().is(DinosRegistry.RAPTOR_FEATHER)).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(feathers == 1, "one raptor feather (" + feathers + ")");
            h.assertTrue(p.getMainHandItem().getDamageValue() == 16, "the brush wears like on an armadillo (16)");
            h.assertTrue(!raptor.mobInteract(p, InteractionHand.MAIN_HAND).consumesAction() && p.getMainHandItem().getDamageValue() == 16,
                    "right after: nothing (the pause), the brush is not worn");
            long ready = raptor.featherReadyAt();
            h.assertTrue(ready - level.getGameTime() == DinosConfig.raptorFeatherTicks() && DinosConfig.raptorFeatherTicks() == 12000, "10 minutes pause");
            h.assertTrue(!raptor.brushFeather(level, ready - 1) && raptor.brushFeather(level, ready), "after the pause the next feather");
            items(h).forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
        ALL.put("utahraptor_flees_never_fights", h -> {
            ServerLevel level = h.getLevel();
            Utahraptor raptor = standingRaptor(h, 8, 8);
            net.minecraft.world.entity.monster.Monster zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, at(8, 12));
            zombie.setNoAi(true);
            float health = zombie.getHealth();
            h.assertTrue(raptor.hurtServer(level, level.damageSources().mobAttack(zombie), 2.0F), "the zombie hits it");
            h.assertTrue(zombie.getHealth() == health && raptor.getTarget() == null, "it never fights back");
            zombie.discard();
            h.succeed();
        });
        ALL.put("guide_book", com.simpledinos.guide.DinosGuide::gameTest);
    }

    private DinosTests() {}
}

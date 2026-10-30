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
import java.util.LinkedHashMap;
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
import net.minecraft.world.level.block.Blocks;
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

    static {
        ALL.put("registered", h -> {
            for (String id : new String[] {"fossil_block", "trex_egg", "triceratops_egg", "stegosaurus_egg", "utahraptor_egg", "parasaurolophus_egg"}) {
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleDinos.id(id)), "block " + id);
            }
            for (String id : new String[] {"fossil_block", "trex_egg", "triceratops_egg", "stegosaurus_egg", "utahraptor_egg", "parasaurolophus_egg",
                    "large_saddle", "trex_armor_leather", "trex_armor_iron", "trex_armor_diamond", "guide_book"}) {
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleDinos.id(id)), "item " + id);
            }
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleDinos.id("trex")), "entity trex");
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(SimpleDinos.id("trex_spawn_egg")), "no spawn egg (owner)");
            h.assertTrue(DinosRegistry.FOSSIL_BE != null && DinosRegistry.EGG_BE != null, "block entity types");
            h.assertTrue(DefaultAttributes.hasSupplier(DinosRegistry.TREX), "attributes");
            h.assertTrue(DinosConfig.enabled && DinosConfig.brushSeconds == 60 && DinosConfig.incubateSeconds == 300 && DinosConfig.hatchSeconds == 600
                    && DinosConfig.growMinutes == 40 && DinosConfig.trexBiteDamage == 10.0, "config defaults = concept times");
            h.assertTrue(DinoKind.TREX.hatchable() && !DinoKind.TRICERATOPS.hatchable() && DinoKind.hatchableKinds().size() == 1,
                    "stage 1: only the T. rex hatches");
            h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                    .get(ResourceKey.create(Registries.PLACED_FEATURE, SimpleDinos.id("fossil_block"))).isPresent(), "worldgen feature");
            h.succeed();
        });
        ALL.put("config_caps", h -> {
            try {
                DinosConfig.apply(com.google.gson.JsonParser.parseString("{\"brushSeconds\":99999,\"incubateSeconds\":0,\"hatchSeconds\":\"x\",\"growMinutes\":-5,\"trexBiteDamage\":1000}").getAsJsonObject());
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
            h.assertTrue(total == 1200, "about one minute");
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
        ALL.put("fossil_finish_drops_trex_egg", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(6, 6);
            FossilBlockEntity be = fossil(h, pos);
            h.assertTrue(be.kind() == DinoKind.TREX, "only built dinosaurs are rolled");
            be.progressState().set(DinosConfig.brushTicks() - 1, 5000L, 5000L);
            be.advance(level, 5001L, true);
            h.assertTrue(h.getBlockState(pos).is(Blocks.SANDSTONE), "the fossil turns into sandstone");
            AABB box = new AABB(h.absolutePos(pos)).inflate(2.0);
            boolean egg = level.getEntitiesOfClass(ItemEntity.class, box).stream().anyMatch(e -> e.getItem().is(DinosRegistry.eggItem(DinoKind.TREX)));
            h.assertTrue(egg, "a T. rex egg lies there");
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
        ALL.put("egg_other_kinds_wait_for_their_dinosaur", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = at(4, 4);
            h.setBlock(pos, DinosRegistry.EGGS.get(DinoKind.TRICERATOPS));
            DinoEggBlockEntity be = (DinoEggBlockEntity) level.getBlockEntity(h.absolutePos(pos));
            ServerPlayer owner = player(h, 5, 5);
            long t = 3000;
            be.advance(level, t, null);
            while (!be.brooded() && t < 3000 + 8000) be.advance(level, t += 40, owner.getUUID());
            be.advance(level, be.hatchAt() + 10, null);
            h.assertTrue(be.brooded() && h.getBlockState(pos).getBlock() == DinosRegistry.EGGS.get(DinoKind.TRICERATOPS),
                    "a kind without a dinosaur yet brooded, but stays an egg (stage 1)");
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
            h.assertTrue(trex.getBbWidth() == 3.0F && trex.getBbHeight() == 4.0F, "core hitbox 3 x 4");
            h.assertTrue(trex.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 10.0, "bite 10");
            h.assertTrue(!trex.canMate(trex) && trex.getBreedOffspring(h.getLevel(), trex) == null, "dinosaurs do not breed");
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
        ALL.put("guide_book", com.simpledinos.guide.DinosGuide::gameTest);
    }

    private DinosTests() {}
}

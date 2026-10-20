package com.simplemobheads.test;

import com.google.gson.Gson;
import com.simplelib.api.IdMoves;
import com.simplemobheads.MobHeadType;
import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.ability.HeadAbilities;
import com.simplemobheads.config.MobHeadsConfig;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import com.simplemobheads.drop.HeadDropCondition;
import com.simplemobheads.drop.HeadDrops;
import com.simplemobheads.drop.Variants;
import com.simplemobheads.ritual.HerobrineHaunt;
import com.simplemobheads.ritual.HerobrineRitual;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests of Simple Mobheads (ids {@code simplemobheads:module_game_test_<name>}). They run with only Vanilla + SimpleLib
 * + framework (standalone target) and with SimpleBuilding (integration); parts that need another mod's mob skip themselves.
 * Every test restores the config it changed.
 */
public final class MobHeadTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        put("registered", MobHeadTests::registered);
        put("mobs_have_heads", MobHeadTests::everyMobHasAHead);
        put("sounds_and_names", MobHeadTests::soundsAndNames);
        put("creeper_drops_every_head", MobHeadTests::creeperDropsEveryHead);
        put("variants_drop_with_the_head", MobHeadTests::variantsDropWithTheHead);
        put("babies_drop_the_normal_head", MobHeadTests::babiesDropTheNormalHead);
        put("other_deaths_drop_nothing", MobHeadTests::otherDeathsDropNothing);
        put("boss_heads", MobHeadTests::bossHeads);
        put("player_kill_chance", MobHeadTests::playerKillChance);
        put("pvp_player_head", MobHeadTests::pvpPlayerHead);
        put("placed_head_keeps_its_variant", MobHeadTests::placedHeadKeepsItsVariant);
        put("old_ids_move", MobHeadTests::oldIdsMove);
        put("fun_config_is_adopted", MobHeadTests::funConfigIsAdopted);
        put("moved_abilities", MobHeadTests::movedAbilities);
        put("animal_abilities", MobHeadTests::animalAbilities);
        put("new_abilities", MobHeadTests::newAbilities);
        put("extra_head_slot", MobHeadTests::extraHeadSlot);
        put("extra_head_greeting", MobHeadTests::extraHeadGreeting);
        put("extra_head_variant_look", MobHeadTests::extraHeadVariantLook);
        put("ability_switches", MobHeadTests::abilitySwitches);
        put("greeting", MobHeadTests::greeting);
        put("commands", MobHeadTests::commands);
        put("creative_tabs", MobHeadTests::creativeTabs);
        put("sub_mod_switch", MobHeadTests::subModSwitch);
        put("guide_book", com.simplemobheads.guide.MobHeadsGuide::gameTest);
        put("discover_recipe_unlocks", com.simplemobheads.discover.MobHeadsDiscover::recipeUnlocks);
        put("discover_behaviour_hints", com.simplemobheads.discover.MobHeadsDiscover::behaviourHints);
        put("herobrine_ritual", MobHeadTests::herobrineRitual);
        put("herobrine_ritual_wrong_shrines", MobHeadTests::herobrineRitualWrongShrines);
        put("herobrine_haunt", MobHeadTests::herobrineHaunt);
        put("herobrine_gaze", MobHeadTests::herobrineGaze);
        put("herobrine_torch_flicker", MobHeadTests::herobrineTorchFlicker);
        put("herobrine_hint_book", MobHeadTests::herobrineHintBook);
    }

    private MobHeadTests() {
    }

    private static void put(String name, Consumer<GameTestHelper> body) {
        ALL.put(name, helper -> {
            Gson gson = new Gson();
            MobHeadsConfig saved = gson.fromJson(gson.toJson(SimpleMobHeads.config()), MobHeadsConfig.class);
            try {
                body.accept(helper);
            } finally {
                SimpleMobHeads.setConfig(saved);
            }
        });
    }

    // --- registration and data ---------------------------------------------------------------------------------

    /** Every head: item, standing + wall block sharing one loot table, skull types, wearable, in the tags. */
    public static void registered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        TagKey<Item> trial = TagKey.create(Registries.ITEM, SimpleMobHeads.id("trial_chamber_heads"));
        for (HeadDef head : HeadTable.HEADS) {
            Item item = BuiltInRegistries.ITEM.getValue(SimpleMobHeads.id(head.item()));
            Block standing = BuiltInRegistries.BLOCK.getValue(SimpleMobHeads.id(head.item()));
            Block wall = BuiltInRegistries.BLOCK.getValue(SimpleMobHeads.id(head.wall()));
            if (item != MobHeads.item(head.id()) || standing != MobHeads.standing(head.id()) || wall != MobHeads.wall(head.id())) {
                problems.add(head.id() + ": not registered");
                continue;
            }
            if (wall.getLootTable() != standing.getLootTable()) {
                problems.add(head.id() + ": wall block has its own loot");
            }
            ItemStack stack = new ItemStack(item);
            if (stack.get(DataComponents.EQUIPPABLE) == null || stack.get(DataComponents.EQUIPPABLE).slot() != EquipmentSlot.HEAD) {
                problems.add(head.id() + ": not worn on the head");
            }
            if (!stack.is(ItemTags.SKULLS)) {
                problems.add(head.id() + ": not in #minecraft:skulls");
            }
            if (stack.is(trial) != head.trial()) {
                problems.add(head.id() + ": trial chamber tag wrong");
            }
            for (HeadDef.Look look : head.looks()) {
                if (!(net.minecraft.world.level.block.SkullBlock.Type.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                        new com.google.gson.JsonPrimitive(look.kind())).result().orElse(null) instanceof MobHeadType)) {
                    problems.add(look.kind() + ": skull type not found by name");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "heads: " + problems);
        helper.succeed();
    }

    /**
     * Concept §3.1: the registry is the list - every living mob type that is a {@code Mob} has a head or an exclusion with a
     * reason, so a new Mojang (or Simple-mod) mob shows up at the next port run.
     */
    public static void everyMobHasAHead(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (HeadTable.EXCLUDED.containsKey(id) || HeadDrops.headOf(type) != null) {
                continue;
            }
            Entity entity;
            try {
                entity = type.create(helper.getLevel(), EntitySpawnReason.LOAD);
            } catch (RuntimeException e) {
                continue;
            }
            if (entity instanceof Mob) {
                missing.add(id);
            }
            if (entity != null) {
                entity.discard();
            }
        }
        helper.assertTrue(missing.isEmpty(), "mobs without a head or exclusion: " + missing);
        helper.succeed();
    }

    /** Every head plays a registered sound on a note block, and every head and variant has an English and German name. */
    public static void soundsAndNames(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Map<String, com.google.gson.JsonObject> lang = new LinkedHashMap<>();
        for (String locale : List.of("en_us", "de_de")) {
            try (var stream = MobHeadTests.class.getResourceAsStream("/assets/simplemobheads/lang/" + locale + ".json")) {
                lang.put(locale, com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                        java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        for (HeadDef head : HeadTable.HEADS) {
            Identifier sound = new ItemStack(MobHeads.item(head.id())).get(DataComponents.NOTE_BLOCK_SOUND);
            if (sound == null || !BuiltInRegistries.SOUND_EVENT.containsKey(sound)) {
                problems.add(head.id() + ": note block sound " + sound);
            }
            for (var entry : lang.entrySet()) {
                if (!entry.getValue().has("block.simplemobheads." + head.item())) {
                    problems.add(entry.getKey() + ": no name for " + head.id());
                }
                for (String variant : head.variantIds()) {
                    if (!entry.getValue().has("variant.simplemobheads." + head.id() + "." + variant)) {
                        problems.add(entry.getKey() + ": no name for " + head.id() + "/" + variant);
                    }
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "sounds and names: " + problems);
        helper.succeed();
    }

    // --- drops -------------------------------------------------------------------------------------------------

    /** Each head (default drop rule, mob registered) drops exactly once when a charged creeper's explosion kills its mob. */
    public static void creeperDropsEveryHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        int tested = 0;
        for (HeadDef head : HeadTable.HEADS) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(head.mob())).orElse(null);
            if (type == null || head.drop().equals("wither")) {
                continue;
            }
            Creeper creeper = chargedCreeper(helper, new BlockPos(1, 2, 1));
            Entity spawned = helper.spawn(type, new BlockPos(4, 2, 4));
            if (!(spawned instanceof LivingEntity victim)) {
                problems.add(head.id() + ": not a living mob");
                creeper.discard();
                continue;
            }
            var blast = level.damageSources().explosion(creeper, creeper);
            victim.hurtServer(level, blast, 100000.0F);
            if (victim.isAlive()) {
                // Explosion-proof mobs (a closed shellker): the creeper still counts as the killer.
                victim.setHealth(0.0F);
                victim.die(blast);
            }
            int heads = count(helper, MobHeads.item(head.id()));
            if (heads != 1) {
                problems.add(head.id() + ": " + heads + " heads" + (victim.isDeadOrDying() ? "" : " (survived)"));
            }
            tested++;
            clearAll(helper);
            creeper.discard();
        }
        helper.assertTrue(tested >= 80, "only " + tested + " heads tested");
        helper.assertTrue(problems.isEmpty(), "creeper drops: " + problems);
        helper.succeed();
    }

    /** The head keeps the mob's variant: a red sheep, a Siamese cat, a blue axolotl, a brown panda, an oxidized golem. */
    public static void variantsDropWithTheHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(4, 2, 4));
        sheep.setColor(DyeColor.RED);
        expectVariant(helper, sheep, "sheep", "red");
        var cat = helper.spawn(EntityTypes.CAT, new BlockPos(4, 2, 4));
        Holder<net.minecraft.world.entity.animal.feline.CatVariant> siamese = level.registryAccess()
                .lookupOrThrow(Registries.CAT_VARIANT).getOrThrow(net.minecraft.world.entity.animal.feline.CatVariants.SIAMESE);
        cat.setComponent(DataComponents.CAT_VARIANT, siamese);
        expectVariant(helper, cat, "cat", "siamese");
        // A royal-voiced cat drops a head that plays the royal meow on a note block; the classic voice keeps the base sound.
        for (var voice : java.util.List.of(net.minecraft.world.entity.animal.feline.CatSoundVariants.ROYAL, net.minecraft.world.entity.animal.feline.CatSoundVariants.CLASSIC)) {
            var voiced = helper.spawn(EntityTypes.CAT, new BlockPos(4, 2, 4));
            voiced.setComponent(DataComponents.CAT_SOUND_VARIANT, level.registryAccess().lookupOrThrow(Registries.CAT_SOUND_VARIANT).getOrThrow(voice));
            String expected = voice == net.minecraft.world.entity.animal.feline.CatSoundVariants.ROYAL ? "minecraft:entity.cat_royal.ambient" : "minecraft:entity.cat.ambient";
            Object played = drop(helper, voiced, "cat").get(DataComponents.NOTE_BLOCK_SOUND);
            helper.assertTrue(expected.equals(String.valueOf(played)), "a cat with the " + voice.identifier() + " voice plays " + played);
        }
        var axolotl = helper.spawn(EntityTypes.AXOLOTL, new BlockPos(4, 2, 4));
        axolotl.setComponent(DataComponents.AXOLOTL_VARIANT, net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.BLUE);
        expectVariant(helper, axolotl, "axolotl", "blue");
        var panda = helper.spawn(EntityTypes.PANDA, new BlockPos(4, 2, 4));
        panda.setMainGene(net.minecraft.world.entity.animal.panda.Panda.Gene.BROWN);
        panda.setHiddenGene(net.minecraft.world.entity.animal.panda.Panda.Gene.BROWN);
        expectVariant(helper, panda, "panda", "brown");
        var golem = helper.spawn(EntityTypes.COPPER_GOLEM, new BlockPos(4, 2, 4));
        golem.setWeatherState(net.minecraft.world.level.block.WeatheringCopper.WeatherState.OXIDIZED);
        expectVariant(helper, golem, "copper_golem", "oxidized");
        // The base look carries no component: a tabby cat drops a plain cat head.
        var tabby = helper.spawn(EntityTypes.CAT, new BlockPos(4, 2, 4));
        tabby.setComponent(DataComponents.CAT_VARIANT, level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT)
                .getOrThrow(net.minecraft.world.entity.animal.feline.CatVariants.TABBY));
        ItemStack plain = drop(helper, tabby, "cat");
        helper.assertTrue(plain.get(MobHeads.VARIANT) == null, "the base look got a variant: " + plain.get(MobHeads.VARIANT));
        helper.assertTrue(MobHeads.stack(MobHeads.head("cat"), "siamese").getHoverName().getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents named && named.getKey().equals("simplemobheads.head_with_variant"),
                "a variant head does not name its variant");
        helper.succeed();
    }

    private static void expectVariant(GameTestHelper helper, LivingEntity mob, String headId, String variant) {
        HeadDef head = MobHeads.head(headId);
        helper.assertTrue(Variants.of(head, mob).equals(variant), headId + " reads as " + Variants.of(head, mob) + ", not " + variant);
        ItemStack dropped = drop(helper, mob, headId);
        helper.assertTrue(variant.equals(dropped.get(MobHeads.VARIANT)), headId + " head dropped with " + dropped.get(MobHeads.VARIANT));
    }

    private static ItemStack drop(GameTestHelper helper, LivingEntity mob, String headId) {
        ServerLevel level = helper.getLevel();
        clearItems(helper);
        Creeper creeper = chargedCreeper(helper, new BlockPos(1, 2, 1));
        mob.hurtServer(level, level.damageSources().explosion(creeper, creeper), 100000.0F);
        creeper.discard();
        Item item = MobHeads.item(headId);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(item));
        helper.assertTrue(drops.size() == 1, headId + ": " + drops.size() + " heads dropped");
        ItemStack stack = drops.get(0).getItem().copy();
        clearAll(helper);
        return stack;
    }

    /** Owner question 5: a baby killed by a charged creeper drops the normal head. */
    public static void babiesDropTheNormalHead(GameTestHelper helper) {
        var calf = helper.spawn(EntityTypes.COW, new BlockPos(4, 2, 4));
        calf.setAge(-24000);
        helper.assertTrue(calf.isBaby(), "the calf is no baby");
        drop(helper, calf, "cow");
        helper.succeed();
    }

    /** A player's kill (default switch off), an uncharged creeper and a plain death drop no head. */
    public static void otherDeathsDropNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        clearAll(helper);
        Creeper plain = helper.spawn(EntityTypes.CREEPER, new BlockPos(1, 2, 1));
        LivingEntity byCreeper = helper.spawn(EntityTypes.CAT, new BlockPos(4, 2, 4));
        byCreeper.hurtServer(level, level.damageSources().explosion(plain, plain), 100000.0F);
        ServerPlayer player = player(helper);
        LivingEntity byPlayer = helper.spawn(EntityTypes.CAT, new BlockPos(5, 2, 5));
        byPlayer.hurtServer(level, level.damageSources().playerAttack(player), 100000.0F);
        HeadDrops.onDeath(byPlayer, level.damageSources().playerAttack(player));
        LivingEntity fallen = helper.spawn(EntityTypes.CAT, new BlockPos(5, 2, 4));
        fallen.hurtServer(level, level.damageSources().generic(), 100000.0F);
        helper.assertTrue(byCreeper.isDeadOrDying() && byPlayer.isDeadOrDying() && fallen.isDeadOrDying(), "the cats did not die");
        helper.assertValueEqual(count(helper, MobHeads.item("cat")), 0, "cat heads from other deaths");
        plain.discard();
        clearAll(helper);
        helper.succeed();
    }

    /** Owner 13.10.: the wither's head is off by default (switchable), the warden's on; the pools exist for both. */
    public static void bossHeads(GameTestHelper helper) {
        HeadDef wither = MobHeads.head("wither");
        HeadDef warden = MobHeads.head("warden");
        helper.assertTrue(!HeadDropCondition.allowed(wither), "the wither head drops by default");
        helper.assertTrue(HeadDropCondition.allowed(warden), "the warden head does not drop by default");
        SimpleMobHeads.config().witherHead = true;
        SimpleMobHeads.config().wardenHead = false;
        helper.assertTrue(HeadDropCondition.allowed(wither) && !HeadDropCondition.allowed(warden), "the boss switches do nothing");
        List<LootPool.Builder> pools = new ArrayList<>();
        HeadDrops.apply(BuiltInLootTables.CHARGED_CREEPER, pools::add, helper.getLevel().registryAccess());
        long registered = HeadTable.HEADS.stream()
                .filter(h -> BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(h.mob()))).count();
        helper.assertValueEqual((long) pools.size(), registered, "charged creeper pools");
        List<LootPool.Builder> other = new ArrayList<>();
        HeadDrops.apply(BuiltInLootTables.SIMPLE_DUNGEON, other::add, helper.getLevel().registryAccess());
        helper.assertTrue(other.isEmpty(), "pools outside charged_creeper");
        helper.succeed();
    }

    /** Owner question 4: player kills drop nothing by default; switched on: base chance + bonus per looting level, bounded. */
    public static void playerKillChance(GameTestHelper helper) {
        MobHeadsConfig defaults = new MobHeadsConfig();
        helper.assertTrue(!defaults.playerKillDrops && defaults.playerKillChance == 0.0F, "player kills drop heads by default");
        helper.assertTrue(Math.abs(HeadDrops.playerKillChance(defaults, 3) - 0.03F) < 1.0E-6, "looting III is not 3 %");
        MobHeadsConfig greedy = new MobHeadsConfig();
        greedy.playerKillChance = 5.0F;
        greedy.playerKillLootingBonus = 1.0F;
        greedy.normalize(SimpleMobHeads.abilityIds());
        helper.assertTrue(greedy.playerKillChance <= MobHeadsConfig.MAX_PLAYER_KILL_CHANCE
                && greedy.playerKillLootingBonus <= MobHeadsConfig.MAX_LOOTING_BONUS, "chances are not bounded");
        helper.succeed();
    }

    /** PvP (moved in from Simple Fun): the victim drops their head with their skin; the switch turns it off. */
    public static void pvpPlayerHead(GameTestHelper helper) {
        ServerPlayer victim = player(helper);
        ServerPlayer killer = player(helper);
        clearAll(helper);
        HeadDrops.onDeath(victim, helper.getLevel().damageSources().playerAttack(killer));
        List<ItemEntity> heads = helper.getLevel().getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(16.0),
                e -> e.getItem().is(Items.PLAYER_HEAD));
        helper.assertTrue(heads.size() == 1 && heads.get(0).getItem().has(DataComponents.PROFILE), "no PvP head with skin");
        heads.forEach(Entity::discard);
        SimpleMobHeads.config().playerHeadDrops = false;
        HeadDrops.onDeath(victim, helper.getLevel().damageSources().playerAttack(killer));
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(16.0),
                e -> e.getItem().is(Items.PLAYER_HEAD)).isEmpty(), "the PvP switch does nothing");
        helper.succeed();
    }

    /** A placed head keeps its variant: in the block entity, in the update packet for clients and in its drop. */
    public static void placedHeadKeepsItsVariant(GameTestHelper helper) {
        HeadDef cat = MobHeads.head("cat");
        BlockPos pos = new BlockPos(2, 2, 2);
        for (Block block : List.of(MobHeads.standing("cat"), MobHeads.wall("cat"))) {
            helper.setBlock(pos, block);
            BlockPos abs = helper.absolutePos(pos);
            helper.assertTrue(helper.getLevel().getBlockEntity(abs) instanceof SkullBlockEntity, block + " has no skull block entity");
            SkullBlockEntity skull = (SkullBlockEntity) helper.getLevel().getBlockEntity(abs);
            skull.applyComponentsFromItemStack(MobHeads.stack(cat, "siamese"));
            helper.assertTrue("siamese".equals(skull.components().get(MobHeads.VARIANT)), "the block entity lost the variant");
            CompoundTag update = skull.getUpdateTag(helper.getLevel().registryAccess());
            helper.assertTrue(update.contains("components"), "clients would not see the variant");
            List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(abs), helper.getLevel(), abs, skull);
            helper.assertTrue(drops.size() == 1 && "siamese".equals(drops.get(0).get(MobHeads.VARIANT)), block + " drops " + drops);
        }
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }

    // --- migration ---------------------------------------------------------------------------------------------

    /**
     * Concept §4: the old ids of Simple Fun's and SimpleBuilding's heads resolve to the Mobheads objects - items (inventories,
     * containers), standing and wall blocks (chunk palettes of placed heads) and Simple Fun's advancements.
     */
    public static void oldIdsMove(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        int moved = 0;
        for (HeadDef head : HeadTable.HEADS) {
            if (head.from().isEmpty()) {
                continue;
            }
            moved++;
            Identifier oldItem = Identifier.fromNamespaceAndPath(head.from(), head.item());
            Identifier oldWall = Identifier.fromNamespaceAndPath(head.from(), head.wall());
            if (BuiltInRegistries.ITEM.getValue(oldItem) != MobHeads.item(head.id())) {
                problems.add(oldItem + " (item)");
            }
            if (BuiltInRegistries.BLOCK.getValue(oldItem) != MobHeads.standing(head.id())) {
                problems.add(oldItem + " (block)");
            }
            if (BuiltInRegistries.BLOCK.getValue(oldWall) != MobHeads.wall(head.id())) {
                problems.add(oldWall + " (wall block)");
            }
            try {
                CompoundTag stackTag = TagParser.parseCompoundFully("{id:\"" + oldItem + "\",count:2}");
                ItemStack stack = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), stackTag)
                        .result().orElse(ItemStack.EMPTY);
                if (!stack.is(MobHeads.item(head.id())) || stack.getCount() != 2) {
                    problems.add(oldItem + " (saved stack -> " + stack + ")");
                }
                // 26.3 palette entry: {id, properties}.
                var parsed = BlockState.CODEC.parse(NbtOps.INSTANCE,
                        TagParser.parseCompoundFully("{id:\"" + oldWall + "\",properties:{facing:\"east\"}}"));
                BlockState state = parsed.result().orElse(null);
                if (state == null || !state.is(MobHeads.wall(head.id()))) {
                    problems.add(oldWall + " (chunk palette -> " + state + " " + parsed.error().map(e -> e.message()).orElse("") + ")");
                }
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalStateException(e);
            }
            if (head.from().equals("simplefun")) {
                Identifier advancement = IdMoves.advancement(Identifier.fromNamespaceAndPath("simplefun", "content/" + head.item()));
                if (!SimpleMobHeads.id("heads/" + head.id()).equals(advancement)
                        || helper.getLevel().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("simplefun", "content/" + head.item())) == null) {
                    problems.add("advancement simplefun:content/" + head.item() + " -> " + advancement);
                }
            }
        }
        helper.assertValueEqual(moved, 16, "heads that moved in");
        helper.assertTrue(problems.isEmpty(), "old ids: " + problems);
        helper.succeed();
    }

    /** Simple Fun 1.3's head switches are taken over once into config/simplemobheads.json (the old file stays as it is). */
    public static void funConfigIsAdopted(GameTestHelper helper) {
        try {
            Path dir = Files.createTempDirectory("simplemobheads-adopt");
            String fun = "{\"fun\":{\"playerHeadDrops\":false,\"enableAnimalHeads\":true,\"cowHeadMilking\":false,"
                    + "\"pigHeadGreeting\":false,\"cowHeadGreeting\":false,\"chickenHeadGreeting\":false,\"sheepHeadGreeting\":false}}";
            Files.writeString(dir.resolve("simplefun.json"), fun);
            helper.assertTrue(SimpleMobHeads.adoptFunConfig(dir), "nothing adopted");
            MobHeadsConfig adopted = new Gson().fromJson(Files.readString(dir.resolve("simplemobheads.json")), MobHeadsConfig.class);
            helper.assertTrue(!adopted.playerHeadDrops && !adopted.greeting && !adopted.ability("cow") && adopted.ability("sheep"),
                    "adopted values wrong: " + Files.readString(dir.resolve("simplemobheads.json")));
            helper.assertTrue(Files.readString(dir.resolve("simplefun.json")).equals(fun), "the old file changed");
            helper.assertTrue(!SimpleMobHeads.adoptFunConfig(dir), "adopted twice");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    // --- abilities ---------------------------------------------------------------------------------------------

    /** The twelve abilities that moved in from SimpleBuilding: with the head they work, without it they do not. */
    public static void movedAbilities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        check(problems, player, "blaze", () -> HeadAbilities.ignoresDamage(player, level.damageSources().hotFloor()));
        check(problems, player, "enderman", () -> HeadAbilities.ignoresDamage(player, level.damageSources().enderPearl()));
        check(problems, player, "husk", () -> HeadAbilities.blocksFoodEffect(player, new ItemStack(Items.ROTTEN_FLESH),
                new MobEffectInstance(MobEffects.HUNGER, 600)));
        check(problems, player, "bogged", () -> HeadAbilities.blocksFoodEffect(player, new ItemStack(Items.POISONOUS_POTATO),
                new MobEffectInstance(MobEffects.POISON, 100)));
        check(problems, player, "spider", () -> HeadAbilities.ignoresCobweb(player, Blocks.COBWEB.defaultBlockState()));
        check(problems, player, "cave_spider", () -> HeadAbilities.cobwebBreakSpeed(player, Blocks.COBWEB.defaultBlockState(), 1.0F)
                == HeadAbilities.COBWEB_SPEED);
        check(problems, player, "breeze", () -> !HeadAbilities.tramplesFarmland(player));
        check(problems, player, "shulker", () -> HeadAbilities.opensBlockedShulkerBoxes(player));
        check(problems, player, "drowned", () -> HeadAbilities.resistsDownwardBubbles(player, true));
        helper.assertTrue(new ItemStack(MobHeads.item("stray")).is(ItemTags.FREEZE_IMMUNE_WEARABLES), "the stray skull is not freeze immune");
        wear(player, "silverfish");
        player.doTick();
        helper.assertTrue(player.getScale() < 0.6F, "the silverfish head does not shrink: " + player.getScale());
        wear(player, "slime");
        player.doTick();
        helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE) == 4.0,
                "the slime head does not add a block of safe fall");
        wear(player, null);
        // Real food: the husk head eats rotten flesh without hunger.
        wear(player, "husk");
        eat(level, player, Items.ROTTEN_FLESH);
        helper.assertTrue(!player.hasEffect(MobEffects.HUNGER), "rotten flesh still gives hunger with the husk head");
        helper.assertTrue(problems.isEmpty(), "moved abilities: " + problems);
        helper.succeed();
    }

    /** Simple Fun's four animal heads: milk the cow head wearer, shear the sheep head wearer, eggs, truffles. */
    public static void animalAbilities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer wearer = player(helper);
        ServerPlayer user = player(helper);
        wear(wearer, "cow");
        user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        HeadAbilities.interact(wearer, user, InteractionHand.MAIN_HAND);
        helper.assertTrue(has(user, Items.MILK_BUCKET), "the cow head wearer gave no milk");
        wear(wearer, "mooshroom");
        user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
        HeadAbilities.interact(wearer, user, InteractionHand.MAIN_HAND);
        helper.assertTrue(has(user, Items.MUSHROOM_STEW), "the mooshroom head wearer gave no stew");
        wear(wearer, "sheep");
        user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        clearItems(helper);
        HeadAbilities.interact(wearer, user, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, wearer.getBoundingBox().inflate(4.0), e -> e.getItem().is(Items.WOOL.pick(DyeColor.WHITE)))
                .size() == 1, "the sheep head wearer gave no wool");
        helper.assertTrue(HeadAbilities.interact(wearer, user, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS,
                "the wool grew back at once");
        wear(wearer, null);
        user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertTrue(HeadAbilities.interact(wearer, user, InteractionHand.MAIN_HAND) == null, "a bare head gives milk");
        clearItems(helper);
        HeadAbilities.layEgg(level, wearer);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, wearer.getBoundingBox().inflate(4.0), e -> e.getItem().is(Items.EGG)).size() == 1,
                "no egg");
        helper.assertTrue(HeadAbilities.truffleGround(Blocks.GRASS_BLOCK.defaultBlockState()) && HeadAbilities.sniffGround(Blocks.MOSS_BLOCK.defaultBlockState()),
                "truffle/sniff ground");
        clearAll(helper);
        helper.succeed();
    }

    /** The twelve new abilities (owner 13.10., concept §3.5). */
    public static void newAbilities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        List<String> problems = new ArrayList<>();
        check(problems, player, "fox", () -> HeadAbilities.ignoresDamage(player, level.damageSources().sweetBerryBush()));
        var goat = helper.spawn(EntityTypes.GOAT, new BlockPos(4, 2, 4));
        check(problems, player, "goat", () -> HeadAbilities.ignoresDamage(player, level.damageSources().noAggroMobAttack(goat))
                && HeadAbilities.ramKnockback(player) < 1.0);
        goat.discard();
        check(problems, player, "bee", () -> HeadAbilities.calmsBees(player));
        check(problems, player, "turtle", () -> HeadAbilities.sparesTurtleEggs(player));
        check(problems, player, "polar_bear", () -> HeadAbilities.friction(player, 0.98F) == HeadAbilities.POLAR_BEAR_FRICTION
                && HeadAbilities.friction(player, 0.6F) == 0.6F);
        check(problems, player, "wolf", () -> HeadAbilities.wolvesSpare(player));
        check(problems, player, "cat", () -> HeadAbilities.scaresCreepers(player));
        check(problems, player, "warden", () -> {
            int muffled = 0;
            for (int i = 0; i < 200; i++) {
                muffled += HeadAbilities.muffledStep(player) ? 1 : 0;
            }
            return muffled > 50 && muffled < 150;
        });
        player.snapTo(helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.LILY_PAD);
        check(problems, player, "frog", () -> HeadAbilities.jumpPower(player, 0.42F) > 0.42F);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        // Real behaviour: a wild wolf does not take the wolf head wearer as target, a creeper registers the cat avoid goal.
        wear(player, "wolf");
        var wolf = helper.spawn(EntityTypes.WOLF, new BlockPos(4, 2, 4));
        wolf.setTarget(player);
        helper.assertTrue(wolf.getTarget() == null, "a wild wolf targets the wolf head wearer");
        wolf.discard();
        clearItems(helper);
        HeadAbilities.dig(level, player, HeadAbilities.seed(level.getRandom()));
        helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4.0)).isEmpty(), "the sniffer found nothing");
        clearAll(helper);
        helper.assertTrue(problems.isEmpty(), "new abilities: " + problems);
        helper.succeed();
    }

    /** A head in an extra head slot (framework WornHeads, SimpleBuilding's slot) works like the helmet slot. */
    public static void extraHeadSlot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack extra = new ItemStack(MobHeads.item("blaze"));
        com.simplebuilding.framework.api.WornHeads.register("simplemobheads_test", entity -> entity == player ? List.of(extra) : List.of());
        try {
            helper.assertTrue(HeadAbilities.wears(player, "blaze") && HeadAbilities.ignoresDamage(player, helper.getLevel().damageSources().hotFloor()),
                    "the extra head slot does not count");
        } finally {
            com.simplebuilding.framework.api.WornHeads.unregister("simplemobheads_test");
        }
        helper.assertTrue(!HeadAbilities.wears(player, "blaze"), "the provider is still asked");
        helper.succeed();
    }

    /** N55: sneaking with a head in an extra head slot greets like one in the helmet slot, and the extra head is heard first. */
    public static void extraHeadGreeting(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack extra = new ItemStack(MobHeads.item("cat"));
        com.simplebuilding.framework.api.WornHeads.register("simplemobheads_test", entity -> entity == player ? List.of(extra) : List.of());
        try {
            helper.assertTrue(HeadAbilities.wornHead(player) != null && HeadAbilities.wornHead(player).id().equals("cat"), "the extra head is not the worn head");
            HeadAbilities.resetForTests(player);
            helper.assertTrue(HeadAbilities.greet(helper.getLevel(), player), "a head in the extra slot does not greet");
            wear(player, "cow");
            helper.assertTrue(HeadAbilities.wornHead(player).id().equals("cat"), "the helmet head is heard before the extra head");
        } finally {
            com.simplebuilding.framework.api.WornHeads.unregister("simplemobheads_test");
        }
        helper.assertTrue(HeadAbilities.wornHead(player).id().equals("cow"), "the helmet head is not worn after the provider is gone");
        wear(player, null);
        helper.assertTrue(HeadAbilities.wornHead(player) == null, "a bare head wears a head");
        helper.succeed();
    }

    /** N55: the look the extra head slot asks for shows the stack's variant and knows nothing about foreign skull types. */
    public static void extraHeadVariantLook(GameTestHelper helper) {
        com.simplemobheads.client.MobHeadLook look = new com.simplemobheads.client.MobHeadLook();
        HeadDef cat = MobHeads.head("cat");
        helper.assertTrue(cat != null && cat.variantIds().size() > 1, "the cat head has no variants");
        String other = cat.variantIds().get(1);
        ItemStack siamese = MobHeads.stack(cat, other);
        com.simplemobheads.MobHeadType base = MobHeads.type(cat, null);
        helper.assertTrue(base != null && MobHeads.type(cat, other) != base, "the variant has no skull type of its own");
        helper.assertTrue(look.type(siamese, base) == MobHeads.type(cat, other), "the stack's variant is not shown");
        helper.assertTrue(look.type(new ItemStack(MobHeads.item("cat")), base) == base, "the base head changed its look");
        helper.assertTrue(look.type(siamese, net.minecraft.world.level.block.SkullBlock.Types.ZOMBIE) == net.minecraft.world.level.block.SkullBlock.Types.ZOMBIE,
                "a Vanilla skull type was swapped");
        helper.assertTrue(look.edge(net.minecraft.world.level.block.SkullBlock.Types.ZOMBIE) == 0.0F && look.factor(net.minecraft.world.level.block.SkullBlock.Types.ZOMBIE) == 1.0F,
                "a Vanilla skull got a mob head size");
        helper.succeed();
    }

    /** Every ability has its own switch, and the master switch turns all of them off. */
    public static void abilitySwitches(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        wear(player, "blaze");
        helper.assertTrue(HeadAbilities.active(player, "blaze"), "the blaze head is not active");
        SimpleMobHeads.config().abilities.put("blaze", false);
        helper.assertTrue(!HeadAbilities.active(player, "blaze"), "the ability switch does nothing");
        SimpleMobHeads.config().abilities.put("blaze", true);
        SimpleMobHeads.config().enabled = false;
        helper.assertTrue(!HeadAbilities.active(player, "blaze"), "the master switch does nothing");
        helper.assertTrue(SimpleMobHeads.abilityIds().size() == 29, "abilities: " + SimpleMobHeads.abilityIds());
        helper.succeed();
    }

    /** Sneaking with a head: one greeting, then a cooldown; the switch turns it off. */
    public static void greeting(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        wear(player, "cat");
        HeadAbilities.resetForTests(player);
        helper.assertTrue(HeadAbilities.greet(helper.getLevel(), player), "no greeting");
        helper.assertTrue(!HeadAbilities.greet(helper.getLevel(), player), "greeting spam");
        wear(player, null);
        HeadAbilities.resetForTests(player);
        helper.assertTrue(!HeadAbilities.greet(helper.getLevel(), player), "a bare head greets");
        helper.succeed();
    }

    /** {@code /simplemobheads} sets the switches; {@code /simplefun pvp headDrops} stays as an alias. */
    public static void commands(GameTestHelper helper) {
        ServerPlayer admin = player(helper);
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, "simplemobheads pvp headDrops false");
        helper.assertTrue(!SimpleMobHeads.config().playerHeadDrops, "/simplemobheads pvp headDrops false");
        server.getCommands().performPrefixedCommand(source, "simplefun pvp headDrops true");
        helper.assertTrue(SimpleMobHeads.config().playerHeadDrops, "/simplefun pvp headDrops (alias)");
        server.getCommands().performPrefixedCommand(source, "simplemobheads ability bee false");
        helper.assertTrue(!SimpleMobHeads.config().ability("bee"), "/simplemobheads ability bee false");
        server.getCommands().performPrefixedCommand(source, "simplemobheads drops wither true");
        helper.assertTrue(SimpleMobHeads.config().witherHead, "/simplemobheads drops wither true");
        admin.discard();
        helper.succeed();
    }

    /** Own tab: every head with its variants; Functional Blocks and the search tab: the heads right behind the dragon head. */
    public static void creativeTabs(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        CreativeModeTab own = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(MobHeads.TAB);
        own.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        helper.assertValueEqual(own.getDisplayItems().size(), MobHeads.tabStacks().size(), "own tab entries");
        List<Item> functional = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.FUNCTIONAL_BLOCKS).getDisplayItems()
                .stream().map(ItemStack::getItem).toList();
        List<Item> heads = MobHeads.baseStacks().stream().map(ItemStack::getItem).toList();
        int at = functional.indexOf(MobHeads.HEAD_ANCHOR);
        helper.assertTrue(at >= 0 && at + heads.size() < functional.size() && functional.subList(at + 1, at + 1 + heads.size()).equals(heads),
                "the heads do not follow the dragon head");
        helper.succeed();
    }

    /** The super mod's switch (framework SubMods) turns drops and abilities off, the heads stay registered. */
    public static void subModSwitch(GameTestHelper helper) {
        boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleMobHeads.MOD_ID);
        try {
            com.simplebuilding.framework.api.SubMods.set(SimpleMobHeads.MOD_ID, false);
            helper.assertTrue(!SimpleMobHeads.on() && !HeadDropCondition.allowed(MobHeads.head("cat")), "the switch does not reach Mobheads");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleMobHeads.id("cat_head")), "the heads left the registry");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleMobHeads.MOD_ID, before);
        }
        helper.succeed();
    }

    // --- Herobrine (KONZEPT-HEROBRINE-KOPF-2026-10-19, owner KONZ20) ---------------------------------------------

    private static final BlockPos SHRINE_FIRE = new BlockPos(2, 5, 2);

    /** The totem shrine: 3x3 gold at y 2, mossy cobblestone at (2,3,2), four redstone torches around it, netherrack on top. */
    private static void buildShrine(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.GOLD_BLOCK);
            }
        }
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.MOSSY_COBBLESTONE);
        for (BlockPos torch : List.of(new BlockPos(1, 3, 2), new BlockPos(3, 3, 2), new BlockPos(2, 3, 1), new BlockPos(2, 3, 3))) {
            helper.setBlock(torch, Blocks.REDSTONE_TORCH);
        }
        helper.setBlock(new BlockPos(2, 4, 2), Blocks.NETHERRACK);
        helper.setBlock(SHRINE_FIRE, Blocks.FIRE);
    }

    private static HerobrineRitual.Result ignite(GameTestHelper helper, ServerPlayer player, Boolean night) {
        return HerobrineRitual.ignite(helper.getLevel(), helper.absolutePos(SHRINE_FIRE), player, night);
    }

    private static void clearShrine(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(4, 6, 4))) {
            helper.setBlock(pos, Blocks.AIR);
        }
        clearAll(helper);
    }

    /**
     * H1/H4: the right shrine lit at night gives exactly one head; the torches go out, the moss turns to cobblestone, gold and
     * netherrack stay. By day nothing is used up. A second ritual within the cooldown (same player or same shrine) gives
     * nothing; fire without a player and the switch off do nothing. The real flint and steel reaches the ritual too.
     */
    public static void herobrineRitual(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        HerobrineRitual.resetForTests();
        HerobrineHaunt.resetForTests();
        Item head = MobHeads.item("herobrine");
        clearShrine(helper);
        buildShrine(helper);
        helper.assertTrue(HerobrineRitual.isShrine(helper.getLevel(), helper.absolutePos(new BlockPos(2, 4, 2))), "the shrine is not seen");
        var day = ignite(helper, player, false);
        helper.assertTrue(day == HerobrineRitual.Result.DAY, "by day: " + day);
        helper.assertTrue(count(helper, head) == 0 && helper.getBlockState(new BlockPos(1, 3, 2)).is(Blocks.REDSTONE_TORCH)
                && helper.getBlockState(new BlockPos(2, 3, 2)).is(Blocks.MOSSY_COBBLESTONE), "the day used the shrine up");
        helper.assertTrue(ignite(helper, null, true) == HerobrineRitual.Result.NO_PLAYER, "fire without a player counts");
        SimpleMobHeads.config().herobrineRitual = false;
        helper.assertTrue(ignite(helper, player, true) == HerobrineRitual.Result.DISABLED, "the switch does nothing");
        SimpleMobHeads.config().herobrineRitual = true;
        var result = ignite(helper, player, true);
        helper.assertTrue(result == HerobrineRitual.Result.SUMMONED, "at night: " + result);
        helper.assertValueEqual(count(helper, head), 1, "Herobrine heads");
        for (BlockPos torch : List.of(new BlockPos(1, 3, 2), new BlockPos(3, 3, 2), new BlockPos(2, 3, 1), new BlockPos(2, 3, 3))) {
            helper.assertTrue(helper.getBlockState(torch).isAir(), "a torch stayed at " + torch);
        }
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 3, 2)).is(Blocks.COBBLESTONE), "the moss stayed");
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 4, 2)).is(Blocks.NETHERRACK) && helper.getBlockState(SHRINE_FIRE).isAir(),
                "netherrack or fire wrong");
        for (BlockPos gold : BlockPos.betweenClosed(new BlockPos(1, 2, 1), new BlockPos(3, 2, 3))) {
            helper.assertTrue(helper.getBlockState(gold).is(Blocks.GOLD_BLOCK), "gold was used up");
        }
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(LightningBolt.class, helper.getBounds().inflate(6.0)).isEmpty(), "no lightning");
        helper.assertTrue(HerobrineHaunt.haunted(player), "the haunting did not start");
        clearItems(helper);
        // Cooldown: the same player at the rebuilt shrine, and another player at the same shrine.
        buildShrine(helper);
        helper.assertTrue(ignite(helper, player, true) == HerobrineRitual.Result.COOLDOWN, "no cooldown for the player");
        ServerPlayer other = player(helper);
        helper.assertTrue(ignite(helper, other, true) == HerobrineRitual.Result.COOLDOWN, "no cooldown for the shrine");
        helper.assertValueEqual(count(helper, head), 0, "heads during the cooldown");
        // The real item: flint and steel on the netherrack (the mixin asks the world's time).
        HerobrineRitual.resetForTests();
        helper.setBlock(SHRINE_FIRE, Blocks.AIR);
        boolean night = helper.getLevel().isDarkOutside();
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        BlockPos netherrack = helper.absolutePos(new BlockPos(2, 4, 2));
        Items.FLINT_AND_STEEL.useOn(new net.minecraft.world.item.context.UseOnContext(other, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(netherrack).add(0.0, 0.5, 0.0), net.minecraft.core.Direction.UP, netherrack, false)));
        helper.assertValueEqual(count(helper, head), night ? 1 : 0, "heads from a real flint and steel (night " + night + ")");
        // The config keeps the cooldown within its bounds.
        MobHeadsConfig bounded = new MobHeadsConfig();
        bounded.ritualCooldownTicks = 5;
        bounded.normalize(SimpleMobHeads.abilityIds());
        helper.assertValueEqual(bounded.ritualCooldownTicks, MobHeadsConfig.MIN_RITUAL_COOLDOWN, "cooldown bound");
        HerobrineHaunt.resetForTests();
        HerobrineRitual.resetForTests();
        clearShrine(helper);
        helper.succeed();
    }

    /** H1: a missing torch, no moss (cobblestone), an incomplete gold square or a torch of the wrong kind - no ritual. */
    public static void herobrineRitualWrongShrines(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        HerobrineRitual.resetForTests();
        Map<String, Consumer<GameTestHelper>> wrong = new LinkedHashMap<>();
        wrong.put("a torch missing", h -> h.setBlock(new BlockPos(3, 3, 2), Blocks.AIR));
        wrong.put("a plain torch", h -> h.setBlock(new BlockPos(2, 3, 1), Blocks.TORCH));
        wrong.put("no moss", h -> h.setBlock(new BlockPos(2, 3, 2), Blocks.COBBLESTONE));
        wrong.put("a corner not gold", h -> h.setBlock(new BlockPos(1, 2, 1), Blocks.IRON_BLOCK));
        wrong.put("an edge not gold", h -> h.setBlock(new BlockPos(3, 2, 2), Blocks.STONE));
        List<String> problems = new ArrayList<>();
        for (var entry : wrong.entrySet()) {
            clearShrine(helper);
            buildShrine(helper);
            entry.getValue().accept(helper);
            helper.setBlock(SHRINE_FIRE, Blocks.FIRE);
            var result = ignite(helper, player, true);
            if (result != HerobrineRitual.Result.NO_SHRINE || count(helper, MobHeads.item("herobrine")) != 0) {
                problems.add(entry.getKey() + ": " + result);
            }
        }
        clearShrine(helper);
        helper.assertTrue(problems.isEmpty(), "wrong shrines: " + problems);
        HerobrineRitual.resetForTests();
        HerobrineHaunt.resetForTests();
        helper.succeed();
    }

    /** H2: the haunting is only the trigger's; the figure vanishes up close and when looked at; the switch ends it. */
    public static void herobrineHaunt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ServerPlayer bystander = player(helper);
        HerobrineHaunt.start(player);
        helper.assertTrue(HerobrineHaunt.haunted(player) && !HerobrineHaunt.haunted(bystander),
                "the haunting reaches someone else");
        Vec3 eye = new Vec3(0.0, 65.0, 0.0);
        Vec3 north = new Vec3(0.0, 0.0, -1.0);
        helper.assertTrue(HerobrineHaunt.vanishes(eye, north, new Vec3(0.0, 63.4, -50.0)), "looked at, it stays");
        helper.assertTrue(!HerobrineHaunt.vanishes(eye, north, new Vec3(0.0, 63.4, 50.0)), "behind the player it vanishes");
        helper.assertTrue(HerobrineHaunt.vanishes(eye, north, new Vec3(10.0, 63.4, 10.0)), "up close it stays");
        Vec3 spot = HerobrineHaunt.placeFigure(helper.getLevel(), player, helper.getLevel().getRandom());
        helper.assertTrue(spot == null || player.position().multiply(1, 0, 1).distanceTo(spot.multiply(1, 0, 1)) >= HerobrineHaunt.FIGURE_MIN - 1.0, "the figure stands too close: " + spot);
        HerobrineHaunt.tick(player);
        helper.assertTrue(HerobrineHaunt.haunted(player), "one tick ended the haunting");
        SimpleMobHeads.config().hauntPhase = false;
        HerobrineHaunt.tick(player);
        helper.assertTrue(!HerobrineHaunt.haunted(player), "the switch does not end the haunting");
        HerobrineHaunt.resetForTests();
        helper.succeed();
    }

    /** The Herobrine head's extra ability: in the dark a hostile mob that targets the wearer hesitates, once per cooldown. */
    public static void herobrineGaze(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        // Mobs never target a creative player (the mock player is one).
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == player, "the zombie does not target the player");
        helper.assertValueEqual(HeadAbilities.gaze(level, player, true), 0, "hesitation without the head");
        wear(player, "herobrine");
        helper.assertValueEqual(HeadAbilities.gaze(level, player, false), 0, "hesitation in the light");
        helper.assertValueEqual(HeadAbilities.gaze(level, player, true), 1, "hesitation in the dark");
        helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "the zombie does not hesitate");
        helper.assertValueEqual(HeadAbilities.gaze(level, player, true), 0, "hesitation again within the cooldown");
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 4));
        HeadAbilities.resetForTests(player);
        helper.assertValueEqual(HeadAbilities.gaze(level, player, true), 1, "after the cooldown (the cow never counts)");
        helper.assertTrue(!cow.hasEffect(MobEffects.SLOWNESS), "a peaceful mob hesitates");
        HeadAbilities.resetForTests(player);
        SimpleMobHeads.config().abilities.put("herobrine", false);
        helper.assertValueEqual(HeadAbilities.gaze(level, player, true), 0, "the ability switch does nothing");
        wear(player, null);
        clearAll(helper);
        helper.succeed();
    }

    /** H5 f: torches (every TorchBlock) near the wearer flicker; redstone torches do not; without the head nothing flickers. */
    public static void herobrineTorchFlicker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.TORCH);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.SOUL_TORCH);
        helper.setBlock(new BlockPos(5, 3, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 2), Blocks.WALL_TORCH.defaultBlockState()
                .setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, net.minecraft.core.Direction.SOUTH));
        helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 4), Blocks.REDSTONE_TORCH);
        List<BlockPos> torches = List.of(helper.absolutePos(new BlockPos(1, 3, 1)), helper.absolutePos(new BlockPos(4, 3, 4)),
                helper.absolutePos(new BlockPos(5, 3, 2)));
        helper.assertTrue(HeadAbilities.flickerTorches(level, player).isEmpty(), "torches flicker without the head");
        wear(player, "herobrine");
        List<BlockPos> flickered = HeadAbilities.flickerTorches(level, player);
        helper.assertTrue(flickered.containsAll(torches), "not every torch flickers: " + flickered);
        helper.assertTrue(!flickered.contains(helper.absolutePos(new BlockPos(1, 3, 4))), "a redstone torch flickers");
        helper.assertTrue(flickered.size() <= HeadAbilities.FLICKER_MAX, "too many torches: " + flickered.size());
        wear(player, null);
        for (BlockPos pos : List.of(new BlockPos(1, 3, 1), new BlockPos(4, 3, 4), new BlockPos(5, 3, 2), new BlockPos(5, 3, 1), new BlockPos(1, 3, 4),
                new BlockPos(1, 2, 1), new BlockPos(4, 2, 4), new BlockPos(1, 2, 4))) {
            helper.setBlock(pos, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Hint (rule A6): mineshaft chests get one rare pool with the three-page Herobrine Notes; the switch removes it. */
    public static void herobrineHintBook(GameTestHelper helper) {
        List<LootPool.Builder> pools = new ArrayList<>();
        HeadDrops.apply(BuiltInLootTables.ABANDONED_MINESHAFT, pools::add, helper.getLevel().registryAccess());
        helper.assertValueEqual(pools.size(), 1, "mineshaft pools");
        ItemStack book = HerobrineRitual.hintBook();
        var content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.pages().size() == 3, "the notes have no three pages");
        SimpleMobHeads.config().hintBook = false;
        List<LootPool.Builder> none = new ArrayList<>();
        HeadDrops.apply(BuiltInLootTables.ABANDONED_MINESHAFT, none::add, helper.getLevel().registryAccess());
        helper.assertTrue(none.isEmpty(), "the hint book switch does nothing");
        helper.assertTrue(MobHeads.head("herobrine") != null && MobHeads.head("herobrine").drop().equals("ritual"), "the head is no ritual head");
        helper.succeed();
    }

    // --- helpers -----------------------------------------------------------------------------------------------

    private interface Check {
        boolean holds();
    }

    /** {@code works} must hold with the head and fail without it. */
    private static void check(List<String> problems, ServerPlayer player, String head, Check works) {
        wear(player, head);
        if (!works.holds()) {
            problems.add(head + " does nothing");
        }
        wear(player, null);
        if (works.holds()) {
            problems.add(head + " works without the head");
        }
    }

    /** In hand or anywhere in the inventory (a creative mock player keeps the bucket and gets the filled one too). */
    private static boolean has(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void wear(ServerPlayer player, String head) {
        player.setItemSlot(EquipmentSlot.HEAD, head == null ? ItemStack.EMPTY : new ItemStack(MobHeads.item(head)));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // The mock player starts outside the test structure (maybe in unloaded chunks): drops would vanish there.
        player.snapTo(helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)));
        HeadAbilities.resetForTests(player);
        return player;
    }

    private static void eat(ServerLevel level, ServerPlayer player, Item food) {
        ItemStack stack = new ItemStack(food);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        for (int i = 0; i < 20; i++) {
            for (ConsumeEffect effect : consumable.onConsumeEffects()) {
                effect.apply(level, stack, player);
            }
        }
    }

    private static Creeper chargedCreeper(GameTestHelper helper, BlockPos pos) {
        Creeper creeper = helper.spawn(EntityTypes.CREEPER, pos);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        creeper.thunderHit(helper.getLevel(), bolt);
        creeper.clearFire();
        creeper.setHealth(creeper.getMaxHealth());
        return creeper;
    }

    private static int count(GameTestHelper helper, Item item) {
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(8.0), e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    private static void clearItems(GameTestHelper helper) {
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(8.0))) {
            entity.discard();
        }
    }

    /** Items and every mob left behind (split slimes, fallen riders ...), players stay. */
    private static void clearAll(GameTestHelper helper) {
        for (Entity entity : helper.getLevel().getEntitiesOfClass(Entity.class, helper.getBounds().inflate(8.0),
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            entity.discard();
        }
    }

    /** Resolves a test id (for adapters). */
    public static ResourceKey<?> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, SimpleMobHeads.id("module_game_test_" + name));
    }
}

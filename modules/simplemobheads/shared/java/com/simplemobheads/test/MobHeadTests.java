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
import com.simplemobheads.easter.Giants;
import com.simplemobheads.easter.KillerBunny;
import com.simplemobheads.easter.Technoblade;
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
        put("config_command", MobHeadTests::configCommand);
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
        put("easter_heads_data", MobHeadTests::easterHeadsData);
        put("technoblade_crown", MobHeadTests::technobladeCrown);
        put("technoblade_lightning", MobHeadTests::technobladeLightning);
        put("crowned_pig_ability", MobHeadTests::crownedPigAbility);
        put("killer_bunny_spawn", MobHeadTests::killerBunnySpawn);
        put("killer_bunny_head", MobHeadTests::killerBunnyHead);
        put("giant_zombie_lightning", MobHeadTests::giantZombieLightning);
        put("giant_head_drop", MobHeadTests::giantHeadDrop);
        put("giant_head_wearing", MobHeadTests::giantHeadWearing);
        put("giant_cow_hay", MobHeadTests::giantCowHay);
        put("giant_cow_head", MobHeadTests::giantCowHead);
        put("worn_head_death", MobHeadTests::wornHeadDeath);
    }

    private MobHeadTests() {
    }

    /** CMD24: the object config (Gson) through /simplemobheads config: set, clamp to the cap, one switch per ability, restart hint. */
    private static void configCommand(GameTestHelper h) {
        com.simplelib.test.ConfigCommandTests.roundTrip(h, SimpleMobHeads.MOD_ID, "playerKillChance", "0.05", "0.05", "x");
        var spec = com.simplelib.api.config.ConfigCommands.spec(SimpleMobHeads.MOD_ID, false);
        var r = spec.set("playerKillChance", "0.5");
        h.assertTrue(r.status() == com.simplelib.api.config.ConfigCommands.Status.CLAMPED && SimpleMobHeads.config().playerKillChance == MobHeadsConfig.MAX_PLAYER_KILL_CHANCE,
                "clamp " + r);
        String ability = SimpleMobHeads.abilityIds().iterator().next();
        h.assertTrue(spec.option("abilities." + ability) != null, "no switch for ability " + ability);
        h.assertTrue(spec.set("abilities." + ability, "false").ok() && !SimpleMobHeads.config().ability(ability), "ability switch");
        h.assertTrue(spec.restart("hintBook") && !spec.restart("greeting"), "restart hint");
        h.succeed();
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
        // REV2-B: a server stop forgets the game-time cooldowns (the next world in single player has its own clock)
        HeadAbilities.forgetAll();
        clearItems(helper);
        HeadAbilities.interact(wearer, user, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, wearer.getBoundingBox().inflate(4.0), e -> e.getItem().is(Items.WOOL.pick(DyeColor.WHITE)))
                .size() == 1, "after a server stop the sheep head wearer can be shorn again");
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
        helper.assertTrue(SimpleMobHeads.abilityIds().size() == 32, "abilities: " + SimpleMobHeads.abilityIds());
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
        HerobrineRitual.resetForTests(helper.getLevel().getServer());
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
        // The cooldowns live in the world save: a save/load round trip (a restart) keeps them.
        var saved = com.simplemobheads.ritual.RitualCooldowns.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,
                com.simplemobheads.ritual.RitualCooldowns.get(helper.getLevel().getServer())).getOrThrow();
        var loaded = com.simplemobheads.ritual.RitualCooldowns.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, saved).getOrThrow();
        String shrineId = helper.getLevel().dimension().identifier() + "@" + helper.absolutePos(new BlockPos(2, 4, 2)).asLong();
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(loaded.waiting(shrineId, java.util.UUID.randomUUID(), now) && loaded.waiting("none", player.getUUID(), now),
                "cooldowns lost by save/load");
        helper.assertTrue(!loaded.waiting(shrineId, player.getUUID(), now + SimpleMobHeads.config().ritualCooldownTicks + 1), "cooldown never ends");
        // The real item: flint and steel on the netherrack (the mixin asks the world's time).
        HerobrineRitual.resetForTests(helper.getLevel().getServer());
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
        HerobrineRitual.resetForTests(helper.getLevel().getServer());
        clearShrine(helper);
        helper.succeed();
    }

    /** H1: a missing torch, no moss (cobblestone), an incomplete gold square or a torch of the wrong kind - no ritual. */
    public static void herobrineRitualWrongShrines(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        HerobrineRitual.resetForTests(helper.getLevel().getServer());
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
        HerobrineRitual.resetForTests(helper.getLevel().getServer());
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
        // Mobs never target a creative player: the in-level mock player always is one, so a survival mock player stands here.
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.setPos(at.x, at.y, at.z);
        HeadAbilities.resetForTests(player);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == player, "the zombie does not target the player (difficulty " + level.getDifficulty() + ")");
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

    // --- easter eggs EH21 (docs/ai/PLAN-B-EH21-2026-10-22.md) ------------------------------------------------------

    private static final List<String> ZOMBIE_KINDS = List.of("zombie", "husk", "drowned", "zombie_villager", "zombified_piglin");

    /** The three secret heads: group, switches on, variants, giant blocks a full block, camouflage, counts-as, lore, hints. */
    public static void easterHeadsData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        for (String id : List.of("crowned_pig", "giant", "giant_cow")) {
            HeadDef head = MobHeads.head(id);
            if (head == null || MobHeads.item(id) == null) {
                problems.add(id + ": missing");
                continue;
            }
            if (!head.group().equals("secret") || !HeadDropCondition.allowed(head)) {
                problems.add(id + ": not a secret head that drops by default");
            }
        }
        HeadDef giant = MobHeads.head("giant");
        HeadDef giantCow = MobHeads.head("giant_cow");
        helper.assertTrue(problems.isEmpty(), "secret heads: " + problems);
        helper.assertTrue(giant.variantIds().equals(ZOMBIE_KINDS), "giant variants: " + giant.variantIds());
        helper.assertTrue(giantCow.variantIds().equals(List.of("temperate", "warm", "cold")), "giant cow variants: " + giantCow.variantIds());
        helper.assertTrue(giant.big() && giantCow.big() && !MobHeads.head("crowned_pig").big() && !MobHeads.head("cat").big(), "which heads are big");
        for (Block block : List.of(MobHeads.standing("giant"), MobHeads.wall("giant"), MobHeads.standing("giant_cow"), MobHeads.wall("giant_cow"))) {
            var box = block.defaultBlockState().getShape(level, BlockPos.ZERO).bounds();
            helper.assertTrue(box.getXsize() == 1.0 && box.getYsize() == 1.0 && box.getZsize() == 1.0, block + " is no full block: " + box);
        }
        helper.assertTrue(MobHeads.standing("cat").defaultBlockState().getShape(level, BlockPos.ZERO).bounds().getYsize() == 0.5,
                "an ordinary head lost its Vanilla shape");
        var visibility = new ItemStack(MobHeads.item("giant")).get(DataComponents.MOB_VISIBILITY);
        for (String kind : ZOMBIE_KINDS) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(kind));
            helper.assertTrue(visibility != null && visibility.targetingEntityTypes().stream().anyMatch(h -> h.value() == type)
                    && visibility.visibility() == 0.5F, "a " + kind + " sees the giant head's wearer fully");
        }
        helper.assertTrue(HeadAbilities.is(MobHeads.stack(giant, "husk"), "husk") && !HeadAbilities.is(MobHeads.stack(giant, "husk"), "drowned")
                && HeadAbilities.is(MobHeads.stack(giant, "drowned"), "drowned") && HeadAbilities.is(new ItemStack(MobHeads.item("giant_cow")), "cow")
                && !HeadAbilities.is(new ItemStack(MobHeads.item("cow")), "giant_cow"), "counts-as");
        var lore = new ItemStack(MobHeads.item("crowned_pig")).get(DataComponents.LORE);
        helper.assertTrue(lore != null && lore.lines().size() == 1 && lore.lines().get(0).getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("lore.simplemobheads.crowned_pig_head"),
                "the crowned pig head has no 'Technoblade never dies' lore");
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        helper.assertTrue(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.FLOWER_FOREST).is(com.simplemobheads.easter.KillerBunny.BIOMES)
                && biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.MEADOW).is(com.simplemobheads.easter.KillerBunny.BIOMES)
                && !biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).is(com.simplemobheads.easter.KillerBunny.BIOMES), "killer bunny biomes");
        for (String advancement : List.of("heads/crowned_pig", "heads/giant", "heads/giant_cow", "heads/killer_bunny")) {
            helper.assertTrue(level.getServer().getAdvancements().get(SimpleMobHeads.id(advancement)) != null, "no advancement " + advancement);
        }
        helper.succeed();
    }

    /** TO-1: the crown comes with the name, the pig never dies, a player's deadly hit makes the crown fall once; cooldowns. */
    public static void technobladeCrown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        clearAll(helper);
        Item crown = MobHeads.item("crowned_pig");
        var pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 2, 3));
        helper.assertTrue(!Technoblade.update(level, pig), "a nameless pig got a crown");
        pig.setCustomName(net.minecraft.network.chat.Component.literal("technoblade"));
        helper.assertTrue(!Technoblade.update(level, pig), "the name is not exact");
        pig.setCustomName(net.minecraft.network.chat.Component.literal(Technoblade.NAME));
        helper.assertTrue(Technoblade.update(level, pig) && Technoblade.crowned(pig), "Technoblade has no crown");
        // Without a player the crown stays: it never dies.
        pig.hurtServer(level, level.damageSources().generic(), 1000.0F);
        helper.assertTrue(pig.isAlive() && pig.getHealth() == 1.0F && Technoblade.crowned(pig), "a plain deadly hit killed it or took the crown");
        helper.assertValueEqual(count(helper, crown), 0, "crowns without a player");
        // A player's deadly hit: it lives on, the crown falls as the head, once. (Within the damage cooldown only a hit above
        // the last one counts, so every further hit here is bigger.)
        ServerPlayer player = player(helper);
        pig.hurtServer(level, level.damageSources().playerAttack(player), 2000.0F);
        helper.assertTrue(pig.isAlive() && !Technoblade.crowned(pig), "the crown did not fall (alive " + pig.isAlive() + ")");
        helper.assertValueEqual(count(helper, crown), 1, "crowned heads from a player's hit");
        helper.assertTrue(!Technoblade.update(level, pig), "the crown grew back at once");
        // Without the crown it is mortal.
        pig.hurtServer(level, level.damageSources().playerAttack(player), 4000.0F);
        helper.assertTrue(!pig.isAlive(), "a pig without a crown never dies");
        // The same player waits a day: a second Technoblade keeps its crown and lives.
        var other = helper.spawn(EntityTypes.PIG, new BlockPos(4, 2, 4));
        other.setCustomName(net.minecraft.network.chat.Component.literal(Technoblade.NAME));
        Technoblade.update(level, other);
        other.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(other.isAlive() && Technoblade.crowned(other), "a waiting player still took a crown");
        helper.assertValueEqual(count(helper, crown), 1, "crowned heads while the player waits");
        // The void and /kill still work.
        other.hurtServer(level, level.damageSources().fellOutOfWorld(), 2000.0F);
        helper.assertTrue(!other.isAlive(), "the void cannot kill a crowned pig");
        // The cooldown runs out: the crown grows back.
        var third = helper.spawn(EntityTypes.PIG, new BlockPos(2, 2, 4));
        third.setCustomName(net.minecraft.network.chat.Component.literal(Technoblade.NAME));
        com.simplemobheads.easter.EntityTimes.set(third, Technoblade.CROWN_READY, level.getGameTime() + 100);
        helper.assertTrue(!Technoblade.update(level, third), "a crown before its time");
        com.simplemobheads.easter.EntityTimes.set(third, Technoblade.CROWN_READY, level.getGameTime() - 1);
        helper.assertTrue(Technoblade.update(level, third), "the crown does not grow back");
        // Renamed or switched off: the crown goes, nothing drops; piglets get none.
        SimpleMobHeads.config().technoblade = false;
        helper.assertTrue(!Technoblade.update(level, third) && third.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the switch does nothing");
        SimpleMobHeads.config().technoblade = true;
        var piglet = helper.spawn(EntityTypes.PIG, new BlockPos(1, 2, 4));
        piglet.setAge(-24000);
        piglet.setCustomName(net.minecraft.network.chat.Component.literal(Technoblade.NAME));
        helper.assertTrue(!Technoblade.update(level, piglet), "a piglet got a crown");
        helper.assertValueEqual(count(helper, crown), 1, "crowns dropped by renaming or the switch");
        clearAll(helper);
        helper.succeed();
    }

    /** TO-1: lightning leaves Technoblade a pig (no conversion, no fire), with or without its crown. */
    public static void technobladeLightning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 2, 3));
        pig.setCustomName(net.minecraft.network.chat.Component.literal(Technoblade.NAME));
        Technoblade.update(level, pig);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        pig.thunderHit(level, bolt);
        helper.assertTrue(!pig.isRemoved() && pig.isAlive() && Technoblade.crowned(pig) && pig.getRemainingFireTicks() <= 0,
                "lightning changed the crowned pig");
        // Waiting for its crown it is still Technoblade.
        var waiting = helper.spawn(EntityTypes.PIG, new BlockPos(4, 2, 4));
        waiting.setCustomName(net.minecraft.network.chat.Component.literal(Technoblade.NAME));
        com.simplemobheads.easter.EntityTimes.set(waiting, Technoblade.CROWN_READY, level.getGameTime() + 1000);
        waiting.thunderHit(level, bolt);
        helper.assertTrue(!waiting.isRemoved() && !Technoblade.crowned(waiting), "lightning turned Technoblade into a zombified piglin");
        clearAll(helper);
        helper.succeed();
    }

    /** F-A: pigs nearby look at the wearer of the crowned head; the ability switch stops it. */
    public static void crownedPigAbility(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        var pig = helper.spawn(EntityTypes.PIG, new BlockPos(5, 2, 5));
        helper.assertTrue(Technoblade.charm(level, player, 1).isEmpty(), "pigs look without the head");
        wear(player, "crowned_pig");
        helper.assertTrue(Technoblade.charm(level, player, 1).contains(pig) && pig.getLookControl().isLookingAtTarget()
                && Math.abs(pig.getLookControl().getWantedX() - player.getX()) < 0.01, "the pig does not look at the wearer");
        SimpleMobHeads.config().abilities.put("crowned_pig", false);
        helper.assertTrue(Technoblade.charm(level, player, 1).isEmpty(), "the ability switch does nothing");
        wear(player, null);
        clearAll(helper);
        helper.succeed();
    }

    /** KO-2: one rabbit in killerBunnyChance, natural spawns in flower forests and meadows only; light bite, fast. */
    public static void killerBunnySpawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<net.minecraft.world.level.biome.Biome> flowers = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.FLOWER_FOREST);
        Holder<net.minecraft.world.level.biome.Biome> meadow = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.MEADOW);
        Holder<net.minecraft.world.level.biome.Biome> plains = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        helper.assertTrue(KillerBunny.eligible(EntitySpawnReason.NATURAL, flowers) && KillerBunny.eligible(EntitySpawnReason.CHUNK_GENERATION, meadow),
                "natural spawns in the biomes are not eligible");
        helper.assertTrue(!KillerBunny.eligible(EntitySpawnReason.NATURAL, plains) && !KillerBunny.eligible(EntitySpawnReason.BREEDING, flowers)
                && !KillerBunny.eligible(EntitySpawnReason.SPAWN_ITEM_USE, flowers) && !KillerBunny.eligible(EntitySpawnReason.COMMAND, flowers),
                "other biomes or spawn reasons are eligible");
        var rabbit = helper.spawn(EntityTypes.RABBIT, new BlockPos(3, 2, 3));
        double baseSpeed = rabbit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(!KillerBunny.onSpawn(rabbit, EntitySpawnReason.NATURAL, flowers, 0.5F), "a roll above the chance");
        helper.assertTrue(KillerBunny.onSpawn(rabbit, EntitySpawnReason.NATURAL, flowers, 0.0F), "a roll below the chance");
        helper.assertTrue(rabbit.getVariant() == net.minecraft.world.entity.animal.rabbit.Rabbit.Variant.EVIL
                && rabbit.getSoundSource() == net.minecraft.sounds.SoundSource.HOSTILE, "not a hostile killer bunny");
        helper.assertTrue(rabbit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 2.0,
                "the bite is not one heart: " + rabbit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(rabbit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - baseSpeed * 1.5) < 1.0E-6,
                "the killer bunny is not fast");
        var vanilla = helper.spawn(EntityTypes.RABBIT, new BlockPos(4, 2, 3));
        SimpleMobHeads.config().killerBunnies = false;
        helper.assertTrue(!KillerBunny.onSpawn(vanilla, EntitySpawnReason.NATURAL, flowers, 0.0F), "the switch does nothing");
        SimpleMobHeads.config().killerBunnies = true;
        // The spawn hook runs inside the real finalizeSpawn (chance 0: it must run and change nothing).
        SimpleMobHeads.config().killerBunnyChance = 0.0F;
        vanilla.finalizeSpawn(level, level.getCurrentDifficultyAt(vanilla.blockPosition()), EntitySpawnReason.NATURAL, null);
        helper.assertTrue(vanilla.getVariant() != net.minecraft.world.entity.animal.rabbit.Rabbit.Variant.EVIL, "chance 0 made a killer bunny");
        MobHeadsConfig greedy = new MobHeadsConfig();
        greedy.killerBunnyChance = 1.0F;
        greedy.giantCowChance = 1.0F;
        greedy.giantCowHayLimit = 100000;
        greedy.technobladeCooldownTicks = 1;
        greedy.normalize(SimpleMobHeads.abilityIds());
        helper.assertTrue(greedy.killerBunnyChance == MobHeadsConfig.MAX_KILLER_BUNNY_CHANCE && greedy.giantCowChance == MobHeadsConfig.MAX_GIANT_COW_CHANCE
                && greedy.giantCowHayLimit == MobHeadsConfig.MAX_HAY_LIMIT && greedy.technobladeCooldownTicks == MobHeadsConfig.MIN_RITUAL_COOLDOWN,
                "the easter-egg settings are not bounded");
        MobHeadsConfig defaults = new MobHeadsConfig();
        helper.assertTrue(defaults.killerBunnyChance == 0.001F && defaults.giantCowChance == 0.0002F && defaults.giantCowHayLimit == 64
                && defaults.technobladeCooldownTicks == 24000, "owner values (1:1000, 1:5000, 64, one day)");
        clearAll(helper);
        helper.succeed();
    }

    /** KO-2: a charged creeper's explosion drops the killer bunny's head - the rabbit head's killer bunny variant. */
    public static void killerBunnyHead(GameTestHelper helper) {
        var rabbit = helper.spawn(EntityTypes.RABBIT, new BlockPos(4, 2, 4));
        KillerBunny.turn(rabbit);
        expectVariant(helper, rabbit, "rabbit", "evil");
        helper.succeed();
    }

    /** ZO-1: a natural bolt on a full-moon night makes every zombie kind a giant - not by day, not from a player, not a baby. */
    public static void giantZombieLightning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        for (String kind : ZOMBIE_KINDS) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(kind));
            var zombie = (net.minecraft.world.entity.monster.zombie.Zombie) helper.spawn(type, new BlockPos(3, 2, 3));
            zombie.setBaby(false);
            float before = zombie.getMaxHealth();
            helper.assertTrue(Giants.onLightning(level, zombie, bolt, true) == Giants.Lightning.GIANT, kind + " did not become a giant");
            helper.assertTrue(Giants.isGiant(zombie) && Math.abs(zombie.getScale() - 3.0F) < 1.0E-4
                    && zombie.getMaxHealth() == before + (float) Giants.ZOMBIE_HEALTH && zombie.getHealth() == zombie.getMaxHealth(),
                    kind + " giant: scale " + zombie.getScale() + ", health " + zombie.getMaxHealth());
            helper.assertTrue(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) >= 1.5, kind + " giant cannot step up");
            helper.assertTrue(Giants.onLightning(level, zombie, bolt, true) == Giants.Lightning.ALREADY, kind + " grew twice");
            zombie.discard();
        }
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 3));
        zombie.setBaby(false);
        helper.assertTrue(Giants.onLightning(level, zombie, bolt, false) == Giants.Lightning.NOT_FULL_MOON_NIGHT && !Giants.isGiant(zombie), "by day or without full moon");
        LightningBolt channeled = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        channeled.setCause(player(helper));
        helper.assertTrue(Giants.onLightning(level, zombie, channeled, true) == Giants.Lightning.NOT_NATURAL, "a player's bolt (Channeling)");
        var baby = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
        baby.setBaby(true);
        helper.assertTrue(Giants.onLightning(level, baby, bolt, true) == Giants.Lightning.BABY, "a baby giant");
        // Under a roof a giant would suffocate: the bolt does nothing there.
        helper.setBlock(new BlockPos(3, 5, 3), Blocks.STONE);
        helper.assertTrue(Giants.onLightning(level, zombie, bolt, true) == Giants.Lightning.NO_ROOM && !Giants.isGiant(zombie), "a giant under a roof");
        helper.setBlock(new BlockPos(3, 5, 3), Blocks.AIR);
        SimpleMobHeads.config().giantZombies = false;
        helper.assertTrue(Giants.onLightning(level, zombie, bolt, true) == Giants.Lightning.DISABLED, "the switch does nothing");
        SimpleMobHeads.config().giantZombies = true;
        // The real hook (Entity#thunderHit) with the world's real time and moon.
        boolean fullMoon = Giants.fullMoonNight(level, zombie.blockPosition());
        zombie.thunderHit(level, bolt);
        helper.assertTrue(Giants.isGiant(zombie) == fullMoon, "thunderHit: giant " + Giants.isGiant(zombie) + ", full-moon night " + fullMoon);
        clearAll(helper);
        helper.succeed();
    }

    /** ZO-1 + owner: only a charged creeper drops the giant's head - its kind, instead of the normal head; nothing else does. */
    public static void giantHeadDrop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        Item giantHead = MobHeads.item("giant");
        for (String kind : List.of("husk", "zombie")) {
            clearAll(helper);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(kind));
            var zombie = (net.minecraft.world.entity.monster.zombie.Zombie) helper.spawn(type, new BlockPos(4, 2, 4));
            zombie.setBaby(false);
            Giants.onLightning(level, zombie, bolt, true);
            Creeper creeper = chargedCreeper(helper, new BlockPos(1, 2, 1));
            zombie.hurtServer(level, level.damageSources().explosion(creeper, creeper), 100000.0F);
            Item normal = kind.equals("zombie") ? Items.ZOMBIE_HEAD : MobHeads.item(kind);
            helper.assertValueEqual(count(helper, giantHead), 1, "giant heads from a giant " + kind);
            helper.assertValueEqual(count(helper, normal), 0, "normal heads from a giant " + kind);
            ItemStack head = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(8.0), e -> e.getItem().is(giantHead)).get(0).getItem();
            String expected = kind.equals("zombie") ? null : kind;
            helper.assertTrue(java.util.Objects.equals(head.get(MobHeads.VARIANT), expected), "the giant head shows " + head.get(MobHeads.VARIANT));
        }
        // A player's kill (even with the option on) gives a giant no head.
        clearAll(helper);
        SimpleMobHeads.config().playerKillDrops = true;
        SimpleMobHeads.config().playerKillChance = MobHeadsConfig.MAX_PLAYER_KILL_CHANCE;
        SimpleMobHeads.config().playerKillLootingBonus = 1.0F;
        SimpleMobHeads.config().normalize(SimpleMobHeads.abilityIds());
        ServerPlayer player = player(helper);
        for (int i = 0; i < 30; i++) {
            var husk = helper.spawn(EntityTypes.HUSK, new BlockPos(4, 2, 4));
            Giants.onLightning(level, husk, bolt, true);
            HeadDrops.onDeath(husk, level.damageSources().playerAttack(player));
            husk.discard();
        }
        helper.assertValueEqual(count(helper, MobHeads.item("husk")) + count(helper, giantHead), 0, "heads from a player's kill of a giant");
        // Giant heads switched off: the charged creeper drops the normal head as before.
        SimpleMobHeads.config().giantZombies = false;
        var husk = helper.spawn(EntityTypes.HUSK, new BlockPos(4, 2, 4));
        husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).addOrReplacePermanentModifier(
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(Giants.MARK, Giants.SCALE,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        drop(helper, husk, "husk");
        helper.succeed();
    }

    /** Owner: worn, 1.5x size, more step height, a little faster, and the zombie kind's own head; off with the head or the switch. */
    public static void giantHeadWearing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double step = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        player.setItemSlot(EquipmentSlot.HEAD, MobHeads.stack(MobHeads.head("giant"), "husk"));
        Giants.wearerTick(player);
        helper.assertTrue(Math.abs(player.getScale() - 1.5F) < 1.0E-4, "the wearer is not 1.5x: " + player.getScale());
        helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) - (step + Giants.WEAR_STEP)) < 1.0E-6,
                "no extra step height");
        helper.assertTrue(speed.hasModifier(Giants.WEAR_SPEED_ID), "not faster");
        helper.assertTrue(HeadAbilities.active(player, "husk") && !HeadAbilities.active(player, "drowned")
                && HeadAbilities.blocksFoodEffect(player, new ItemStack(Items.ROTTEN_FLESH), new MobEffectInstance(MobEffects.HUNGER, 600)),
                "the husk giant head does not work like the husk head");
        player.setItemSlot(EquipmentSlot.HEAD, MobHeads.stack(MobHeads.head("giant"), "drowned"));
        helper.assertTrue(HeadAbilities.resistsDownwardBubbles(player, true) && !HeadAbilities.active(player, "husk"), "the drowned giant head");
        SimpleMobHeads.config().abilities.put("giant", false);
        Giants.wearerTick(player);
        helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 1.0E-4 && !speed.hasModifier(Giants.WEAR_SPEED_ID), "the ability switch does nothing");
        SimpleMobHeads.config().abilities.put("giant", true);
        // The giant cow head: size and step height like the giant's, no speed; another player milks the wearer.
        wear(player, "giant_cow");
        Giants.wearerTick(player);
        helper.assertTrue(Math.abs(player.getScale() - 1.5F) < 1.0E-4 && !speed.hasModifier(Giants.WEAR_SPEED_ID), "the giant cow head's size");
        ServerPlayer milker = player(helper);
        milker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertTrue(HeadAbilities.interact(player, milker, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.SUCCESS
                && has(milker, Items.MILK_BUCKET), "the giant cow head's wearer cannot be milked");
        wear(player, null);
        Giants.wearerTick(player);
        helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 1.0E-4
                && Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) - step) < 1.0E-6, "taken off, still big");
        milker.discard();
        helper.succeed();
    }

    /** CO-2 + owner: each hay bale is one try at 1:5000, at most 64 per cow; calves, mooshrooms and other food never. */
    public static void giantCowHay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // A survival mock player (bales are used up); it has no connection, so it is moved with setPos, never snapTo.
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.setPos(at.x, at.y, at.z);
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(3, 2, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK, 64));
        for (int i = 0; i < 64; i++) {
            helper.assertTrue(Giants.feedHay(cow, player, InteractionHand.MAIN_HAND, 0.5F) == net.minecraft.world.InteractionResult.SUCCESS, "bale " + i + " refused");
        }
        helper.assertTrue(player.getMainHandItem().isEmpty() && !Giants.isGiant(cow), "64 bales not eaten or a giant without luck");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK, 16));
        helper.assertTrue(Giants.feedHay(cow, player, InteractionHand.MAIN_HAND, 0.0F) == net.minecraft.world.InteractionResult.PASS
                && player.getMainHandItem().getCount() == 16 && !Giants.isGiant(cow), "a full cow ate the 65th bale");
        // The lucky bale.
        var lucky = helper.spawn(EntityTypes.COW, new BlockPos(4, 2, 4));
        helper.assertTrue(Giants.feedHay(lucky, player, InteractionHand.MAIN_HAND, 0.0F) == net.minecraft.world.InteractionResult.SUCCESS
                && Giants.isGiant(lucky) && Math.abs(lucky.getScale() - 3.0F) < 1.0E-4 && lucky.getMaxHealth() == 10.0F + (float) Giants.COW_HEALTH
                && lucky.hasCustomName(), "the lucky bale made no giant cow");
        helper.assertTrue(Giants.feedHay(lucky, player, InteractionHand.MAIN_HAND, 0.0F) == null, "a giant cow eats on");
        // The owner's chance: a roll just above 1:5000 does nothing, the chance 0 never.
        var unlucky = helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));
        Giants.feedHay(unlucky, player, InteractionHand.MAIN_HAND, 0.0002F);
        helper.assertTrue(!Giants.isGiant(unlucky), "a roll of 1:5000 exactly made a giant");
        SimpleMobHeads.config().giantCowChance = 0.0F;
        Giants.feedHay(unlucky, player, InteractionHand.MAIN_HAND, 0.0F);
        helper.assertTrue(!Giants.isGiant(unlucky), "chance 0 made a giant");
        SimpleMobHeads.config().giantCowChance = 0.0002F;
        // A barn too low: the luck is kept, the cow grows once it has room.
        var barn = helper.spawn(EntityTypes.COW, new BlockPos(5, 2, 5));
        helper.setBlock(new BlockPos(5, 4, 5), Blocks.STONE);
        Giants.feedHay(barn, player, InteractionHand.MAIN_HAND, 0.0F);
        helper.assertTrue(!Giants.isGiant(barn) && barn.entityTags().contains(Giants.PENDING), "the giant cow grew into the roof or lost its luck");
        helper.assertTrue(Giants.feedHay(barn, player, InteractionHand.MAIN_HAND, 0.0F) == null, "a pending giant cow eats on");
        barn.tickCount = 20;
        Giants.tick(level, barn);
        helper.assertTrue(!Giants.isGiant(barn), "it grew under the roof");
        helper.setBlock(new BlockPos(5, 4, 5), Blocks.AIR);
        Giants.tick(level, barn);
        helper.assertTrue(Giants.isGiant(barn) && !barn.entityTags().contains(Giants.PENDING), "the pending giant cow does not grow in the open");
        var calf = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 4));
        calf.setAge(-24000);
        helper.assertTrue(Giants.feedHay(calf, player, InteractionHand.MAIN_HAND, 0.0F) == null && !Giants.isGiant(calf), "a calf ate hay");
        // The real hook (AbstractCow#mobInteract): one bale is eaten and counted; a mooshroom does not eat it.
        var counted = helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
        int before = player.getMainHandItem().getCount();
        counted.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == before - 1
                && com.simplemobheads.easter.EntityTimes.get(counted, Giants.HAY, 0L) + (Giants.isGiant(counted) ? 1 : 0) == 1, "mobInteract did not feed");
        var mooshroom = helper.spawn(EntityTypes.MOOSHROOM, new BlockPos(5, 2, 1));
        int left = player.getMainHandItem().getCount();
        mooshroom.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == left && !Giants.isGiant(mooshroom), "a mooshroom ate hay");
        SimpleMobHeads.config().giantCows = false;
        helper.assertTrue(Giants.feedHay(unlucky, player, InteractionHand.MAIN_HAND, 0.0F) == null, "the switch does nothing");
        player.discard();
        clearAll(helper);
        helper.succeed();
    }

    /** CO-2: only a charged creeper drops the giant cow's head, with its variant, instead of the cow head. */
    public static void giantCowHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        clearAll(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK, 1));
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(4, 2, 4));
        cow.setComponent(DataComponents.COW_VARIANT, level.registryAccess().lookupOrThrow(Registries.COW_VARIANT)
                .getOrThrow(net.minecraft.world.entity.animal.cow.CowVariants.WARM));
        Giants.feedHay(cow, player, InteractionHand.MAIN_HAND, 0.0F);
        helper.assertTrue(Giants.isGiant(cow), "no giant cow");
        Creeper creeper = chargedCreeper(helper, new BlockPos(1, 2, 1));
        cow.hurtServer(level, level.damageSources().explosion(creeper, creeper), 100000.0F);
        helper.assertValueEqual(count(helper, MobHeads.item("giant_cow")), 1, "giant cow heads");
        helper.assertValueEqual(count(helper, MobHeads.item("cow")), 0, "cow heads from a giant cow");
        ItemStack head = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(8.0), e -> e.getItem().is(MobHeads.item("giant_cow"))).get(0).getItem();
        helper.assertTrue("warm".equals(head.get(MobHeads.VARIANT)), "the giant cow head shows " + head.get(MobHeads.VARIANT));
        player.discard();
        clearAll(helper);
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

    /**
     * BS25-19: a survival player who dies wearing a head (soulfire blaze head with several deaths, then every head) is dead
     * afterwards and the head either dropped once or stayed in the head slot (Simple Tweaks' partial keep inventory keeps
     * armor); the soulfire blaze wearer respawns (the head comes along when it was kept) and the server goes on ticking.
     */
    public static void wornHeadDeath(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        ids.add("soulfire_blaze");
        for (HeadDef head : MobHeads.heads()) {
            if (MobHeads.item(head.id()) != null && !ids.contains(head.id())) ids.add(head.id());
        }
        List<net.minecraft.world.damagesource.DamageSource> sources = new ArrayList<>(List.of(level.damageSources().generic(),
                level.damageSources().inFire(), level.damageSources().onFire(), level.damageSources().lava(),
                level.damageSources().genericKill()));
        // With Simple Mobs: killed by a soulfire blaze, in melee and by its soul fireball (what SoulFireball#onHitEntity does).
        Entity blaze = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse("simplemobs:soulfire_blaze"))
                .map(type -> (Entity) type.create(level, EntitySpawnReason.MOB_SUMMONED)).orElse(null);
        Entity ball = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse("simplemobs:soul_fireball"))
                .map(type -> (Entity) type.create(level, EntitySpawnReason.MOB_SUMMONED)).orElse(null);
        if (blaze instanceof LivingEntity living) {
            blaze.snapTo(helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)));
            level.addFreshEntity(blaze);
            sources.add(level.damageSources().mobAttack(living));
            if (ball != null) sources.add(level.damageSources().fireball((net.minecraft.world.entity.projectile.hurtingprojectile.Fireball) ball, blaze));
        }
        var soulBurn = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("simplecrucibles:soul_burn"));
        for (String id : ids) {
            boolean soul = id.equals("soulfire_blaze");
            for (var source : soul ? sources : List.of(level.damageSources().generic())) {
                ServerPlayer player = player(helper);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getAbilities().invulnerable = false;
                // A mock player is invulnerable until its "client" says it loaded (ServerPlayer#isInvulnerableTo).
                player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                wear(player, id);
                // The soul sand valley case: dying in the Nether, respawning in the Overworld.
                ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
                if (soul && source.is(net.minecraft.world.damagesource.DamageTypes.LAVA) && nether != null) {
                    nether.getChunk(0, 0);
                    player.teleportTo(nether, 0.5, 70.0, 0.5, java.util.Set.of(), 0.0F, 0.0F, false);
                    player.hasChangedDimension(); // what the client's arrival confirms; until then the player is invulnerable
                }
                ServerLevel at = player.level();
                if (soul) {
                    player.igniteForSeconds(5.0F);
                    soulBurn.ifPresent(burn -> player.addEffect(new MobEffectInstance(burn, 2400, 0)));
                }
                player.hurtServer(at, source, 100000.0F);
                List<ItemEntity> drops = at.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0),
                        e -> e.getItem().is(MobHeads.item(id)));
                boolean kept = HeadAbilities.is(player.getItemBySlot(EquipmentSlot.HEAD), id);
                String where = id + "/" + source.getMsgId() + (at != level ? "/nether" : "");
                if (!player.isDeadOrDying() || (kept ? drops.size() != 0 : drops.size() != 1)) {
                    problems.add(where + ": dead=" + player.isDeadOrDying() + " kept=" + kept + " drops=" + drops.size());
                }
                drops.forEach(Entity::discard);
                if (soul) {
                    SimpleMobHeads.LOG.info("BS25-19 death test {}: kept={} drops={}", where, kept, drops.size());
                    // The dead player goes on ticking until the respawn (death screen): mod tick hooks see it dead.
                    for (int i = 0; i < 40; i++) player.doTick();
                    ServerPlayer fresh = level.getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
                    for (int i = 0; i < 3; i++) fresh.doTick();
                    if (!fresh.isAlive() || kept != HeadAbilities.is(fresh.getItemBySlot(EquipmentSlot.HEAD), id)) {
                        problems.add(where + " respawn: alive=" + fresh.isAlive() + " head=" + fresh.getItemBySlot(EquipmentSlot.HEAD));
                    }
                    level.getServer().getPlayerList().remove(fresh);
                } else {
                    player.discard();
                }
            }
        }
        if (blaze != null) blaze.discard();
        helper.assertTrue(problems.isEmpty(), "death with a worn head: " + problems);
        long start = level.getGameTime();
        helper.succeedWhen(() -> helper.assertTrue(level.getGameTime() >= start + 5, "the server stopped ticking"));
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

    // JEI24: recipe viewer data against its sources.
    static {
        put("jei_charged_creeper_heads", HeadJeiTests::chargedCreeperHeads);
    }
}

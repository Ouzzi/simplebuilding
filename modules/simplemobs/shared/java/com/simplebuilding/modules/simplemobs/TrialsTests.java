package com.simplebuilding.modules.simplemobs;

import com.google.gson.JsonObject;
import com.simplebuilding.modules.simplemobs.trials.EvilBrewing;
import com.simplebuilding.modules.simplemobs.trials.EvilOmen;
import com.simplebuilding.modules.simplemobs.trials.EvilRaid;
import com.simplebuilding.modules.simplemobs.trials.SoulBrewing;
import com.simplebuilding.modules.simplemobs.trials.TrialKind;
import com.simplebuilding.modules.simplemobs.trials.TrialVaultBlock;
import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import com.simplebuilding.modules.simplemobs.trials.Trials;
import com.simplebuilding.modules.simplemobs.trials.TrialsViewer;
import com.simplebuilding.modules.simplemobs.trials.WeatheredTrialSpawnerBlock;
import com.simplelib.api.InWorldUpgrades;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Weathered Trials, Evil Omen and the Void chain (a-trials31): registries, rooms, loot, brewing, conversion, keys, the turning of a trial, the mob buffs, the Void switch. */
public final class TrialsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    private static final String[] ROOMS = {"normal_a", "normal_b", "normal_c", "large_a", "large_b", "large_c", "giant_a", "giant_b", "giant_c"};

    private TrialsTests() {}

    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    private static void add(String name, Consumer<GameTestHelper> body) { ALL.put(name, body); }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simplemobs", path); }

    private static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager(GameTestHelper h) {
        try {   // 26.3 renamed ServerLevel#getStructureManager() to getStructureTemplateManager()
            return (net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager) h.getLevel().getClass().getMethod("getStructureTemplateManager").invoke(h.getLevel());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no structure template manager", e);
        }
    }

    private static void place(GameTestHelper h, String template, BlockPos pos) {
        var server = h.getLevel().getServer();
        try {
            int r = server.getCommands().getDispatcher().execute("place template " + id("weathered/" + template) + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                    server.createCommandSourceStack().withLevel(h.getLevel()).withSuppressedOutput());
            check(r > 0, "/place failed for " + template);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new AssertionError("/place error: " + e.getMessage());
        }
    }

    private static void clear(GameTestHelper h, BlockPos base, int w, int hgt, int d) {
        for (BlockPos p : BlockPos.betweenClosed(base, base.offset(w, hgt, d))) {
            h.getLevel().removeBlockEntity(p);
            h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static int count(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate t, Block block) {
        return t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    private static LootParams vaultParams(GameTestHelper h, ItemStack key) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        return new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.TOOL, key).create(LootContextParamSets.VAULT);
    }

    private static boolean forbidden(ItemStack s) {
        return s.is(WitherMobs.HEART) || s.is(WitherWings.ENDER_HEART) || s.is(Items.NETHER_STAR);
    }

    private static ItemStack bottle(int amplifier) {
        ItemStack bottle = new ItemStack(Items.OMINOUS_BOTTLE);
        bottle.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(amplifier));
        return bottle;
    }

    private static void brew(ServerLevel level, BlockPos pos, BrewingStandBlockEntity be) {
        for (int i = 0; i < EvilBrewing.BREW_TICKS + 5; i++) BrewingStandBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
    }

    static {
        add("trials_registered", h -> {
            var regs = h.getLevel().registryAccess();
            for (String s : new String[] {"weathered_trial"}) {
                check(regs.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id(s))).isPresent(), "structure " + s);
                check(regs.lookupOrThrow(Registries.STRUCTURE_SET).get(ResourceKey.create(Registries.STRUCTURE_SET, id(s))).isPresent(), "structure set " + s);
                check(regs.lookupOrThrow(Registries.TEMPLATE_POOL).get(ResourceKey.create(Registries.TEMPLATE_POOL, id(s))).isPresent(), "template pool " + s);
            }
            check(regs.lookupOrThrow(Registries.PROCESSOR_LIST).get(ResourceKey.create(Registries.PROCESSOR_LIST, id("weathered_decay"))).isPresent(), "decay processor list");
            var biomes = regs.lookupOrThrow(Registries.BIOME).get(TagKey.<Biome>create(Registries.BIOME, id("has_structure/weathered_trials")));
            check(biomes.isPresent() && biomes.get().size() > 20, "land biome tag");
            for (String theme : new String[] {"undead", "spiders", "blasters", "winds"}) {
                for (String kind : new String[] {"normal", "ominous"}) {
                    check(regs.lookupOrThrow(Registries.TRIAL_SPAWNER_CONFIG).get(ResourceKey.create(Registries.TRIAL_SPAWNER_CONFIG, id("weathered/" + theme + "_" + kind))).isPresent(), "spawner config " + theme + "_" + kind);
                }
            }
            for (int n = 1; n <= 5; n++) {
                check(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(Trials.keyKey(TrialKind.EVIL, n).identifier()), "evil key " + n);
                check(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(Trials.potionKey(TrialKind.VOID, n).identifier()), "void potion " + n);
            }
            check(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.containsKey(Trials.EVIL_OMEN_KEY.identifier()), "evil omen effect");
            check(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(Trials.SPAWNER_BLOCK_KEY.identifier()), "spawner block");
            check(Blocks.TRIAL_SPAWNER != Trials.WEATHERED_TRIAL_SPAWNER && Trials.WEATHERED_TRIAL_SPAWNER instanceof net.minecraft.world.level.block.TrialSpawnerBlock, "own spawner block of the Vanilla kind");
            h.succeed();
        });
        add("trials_templates", h -> {
            var manager = manager(h);
            int[] spawners = {1, 1, 1, 2, 3, 4, 6, 7, 8}, vaults = {1, 1, 1, 4, 4, 4, 8, 8, 8}, ominous = {0, 0, 0, 1, 1, 1, 2, 2, 2}, side = {11, 11, 11, 15, 15, 15, 21, 21, 21};
            for (int i = 0; i < ROOMS.length; i++) {
                var template = manager.get(id("weathered/" + ROOMS[i]));
                check(template.isPresent(), "template " + ROOMS[i]);
                var t = template.get();
                check(t.getSize().getX() == side[i] && t.getSize().getZ() == side[i], ROOMS[i] + " footprint " + t.getSize());
                check(count(t, Trials.WEATHERED_TRIAL_SPAWNER) == spawners[i], ROOMS[i] + " spawners " + count(t, Trials.WEATHERED_TRIAL_SPAWNER));
                check(count(t, Trials.WEATHERED_VAULT) == vaults[i], ROOMS[i] + " vaults " + count(t, Trials.WEATHERED_VAULT));
                long om = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Trials.WEATHERED_VAULT).stream().filter(b -> b.state().getValue(VaultBlock.OMINOUS)).count();
                check(om == ominous[i], ROOMS[i] + " ominous vaults " + om);
            }
            h.succeed();
        });
        add("trials_place_rooms", h -> {
            BlockPos base = h.absolutePos(new BlockPos(0, 48, 0));
            place(h, "normal_a", base);
            ServerLevel level = h.getLevel();
            BlockPos spawner = base.offset(5, 2, 5), vault = base.offset(5, 1, 9);
            check(level.getBlockState(spawner).getBlock() instanceof WeatheredTrialSpawnerBlock, "spawner placed");
            check(level.getBlockEntity(spawner) instanceof TrialSpawnerBlockEntity, "spawner block entity");
            check(level.getBlockEntity(vault) instanceof VaultBlockEntity v && v.getConfig().keyItem().is(Items.TRIAL_KEY)
                    && v.getConfig().lootTable().equals(Trials.vaultLoot(TrialKind.WEATHERED, 1, false)), "normal vault: trial key, weathered loot");
            clear(h, base, 11, 7, 11);
            place(h, "large_a", base);
            int ominousVaults = 0;
            for (BlockPos p : BlockPos.betweenClosed(base, base.offset(15, 8, 15))) {
                if (level.getBlockState(p).is(Trials.WEATHERED_VAULT) && level.getBlockEntity(p) instanceof VaultBlockEntity v && level.getBlockState(p).getValue(VaultBlock.OMINOUS)) {
                    ominousVaults++;
                    check(v.getConfig().keyItem().is(Items.OMINOUS_TRIAL_KEY) && v.getConfig().lootTable().equals(Trials.vaultLoot(TrialKind.WEATHERED, 1, true)), "ominous vault: ominous key, own loot");
                }
            }
            check(ominousVaults == 1, "large room has 1 ominous vault, found " + ominousVaults);
            clear(h, base, 15, 8, 15);
            h.succeed();
        });
        add("trials_loot", h -> {
            var tables = h.getLevel().getServer().reloadableRegistries();
            java.util.List<ResourceKey<LootTable>> keys = new java.util.ArrayList<>(java.util.List.of(Trials.vaultLoot(TrialKind.WEATHERED, 1, false), Trials.vaultLoot(TrialKind.WEATHERED, 1, true)));
            for (int n = 1; n <= 5; n++) {
                keys.add(Trials.vaultLoot(TrialKind.EVIL, n, false));
                keys.add(Trials.vaultLoot(TrialKind.VOID, n, false));
            }
            for (ResourceKey<LootTable> key : keys) {
                LootTable table = tables.getLootTable(key);
                check(table != LootTable.EMPTY, "loot table loads: " + key.identifier());
                LootParams params = vaultParams(h, new ItemStack(Items.TRIAL_KEY));
                for (int seed = 0; seed < 150; seed++) {
                    var stacks = table.getRandomItems(params, 9000L + seed);
                    check(!stacks.isEmpty(), key.identifier() + " rolls items, seed " + seed);
                    for (ItemStack s : stacks) check(!forbidden(s), key.identifier() + " gives no hearts or nether stars: " + s);
                }
            }
            for (TrialKind kind : new TrialKind[] {TrialKind.EVIL, TrialKind.VOID}) {
                for (int n = 1; n <= 5; n++) {
                    LootTable raid = tables.getLootTable(Trials.raidLoot(kind, n));
                    check(raid != LootTable.EMPTY, "raid chest table loads: " + kind.id() + " " + n);
                    LootParams raidParams = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO))).create(LootContextParamSets.CHEST);
                    for (int seed = 0; seed < 40; seed++) {
                        var drawn = raid.getRandomItems(raidParams, 700L + seed);
                        check(!drawn.isEmpty() && drawn.stream().noneMatch(TrialsTests::forbidden), "raid chest " + kind.id() + " " + n + " gives items, no hearts or nether stars, seed " + seed);
                    }
                    LootTable table = tables.getLootTable(Trials.spawnerLoot(kind, n));
                    LootParams chest = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO))).create(LootContextParamSets.CHEST);
                    Item key = (kind == TrialKind.EVIL ? Trials.EVIL_KEYS : Trials.VOID_KEYS)[n - 1];
                    for (int seed = 0; seed < 60; seed++) {
                        check(table.getRandomItems(chest, 100L + seed).stream().anyMatch(s -> s.is(key)), kind.id() + " spawner stage " + n + " always drops its key, seed " + seed);
                    }
                }
            }
            h.succeed();
        });
        add("trials_brew_soul", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, Blocks.BREWING_STAND.defaultBlockState(), 3);
            var be = (BrewingStandBlockEntity) level.getBlockEntity(pos);
            boolean voidOld = MobsConfig.voidChain;
            try {
                be.setItem(0, bottle(2));
                be.setItem(1, bottle(0));
                be.setItem(3, new ItemStack(WitherMobs.HEART));
                brew(level, pos, be);
                check(be.getItem(0).is(Items.OMINOUS_BOTTLE) && be.getItem(3).getCount() == 1, "no soul charge: nothing is brewed");
                be.setItem(4, new ItemStack(EndNetherMobs.POWDER));
                brew(level, pos, be);
                check(be.getItem(0).is(Trials.EVIL_POTIONS[2]) && be.getItem(1).is(Trials.EVIL_POTIONS[0]), "each bottle keeps its stage: " + be.getItem(0) + ", " + be.getItem(1));
                check(be.getItem(3).isEmpty(), "one heart for both bottles");
                check(((SoulBrewing) (Object) be).simplemobs$soul() == EvilBrewing.CHARGES - 1, "one soul charge used");
                be.setItem(3, new ItemStack(WitherWings.ENDER_HEART));
                be.setItem(2, bottle(4));
                MobsConfig.voidChain = false;
                brew(level, pos, be);
                check(be.getItem(2).is(Items.OMINOUS_BOTTLE), "Void chain off: the Ender Heart brews nothing");
                MobsConfig.voidChain = true;
                brew(level, pos, be);
                check(be.getItem(2).is(Trials.VOID_POTIONS[4]), "Void chain on: Void Potion V");
            } finally {
                MobsConfig.voidChain = voidOld;
            }
            h.succeed();
        });
        add("trials_vault_convert", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, Trials.WEATHERED_VAULT.defaultBlockState(), 3);
            check(level.getBlockEntity(pos) instanceof VaultBlockEntity v && v.getConfig().keyItem().is(Items.TRIAL_KEY), "fresh weathered vault wants the trial key");
            var entry = InWorldUpgrades.entryFor(Trials.WEATHERED_VAULT);
            check(entry != null && entry.to() == Trials.EVIL_VAULT && entry.material() == Items.DIAMOND_BLOCK && entry.strikesPerMaterial() == 5, "diamond block, 5 strikes: " + entry);
            var voidEntry = InWorldUpgrades.entryFor(Trials.EVIL_VAULT);
            check(voidEntry != null && voidEntry.to() == Trials.VOID_VAULT && voidEntry.material() == Items.NETHERITE_BLOCK && voidEntry.strikesPerMaterial() == 5, "netherite block, 5 strikes: " + voidEntry);
            check(InWorldUpgrades.entryFor(Blocks.VAULT) == null, "Vanilla vaults are never converted");
            check(InWorldUpgrades.replace(level, pos, Trials.EVIL_VAULT) == 1 && level.getBlockState(pos).is(Trials.EVIL_VAULT), "converted to the Evil Vault");
            check(level.getBlockEntity(pos) instanceof VaultBlockEntity e && e.getConfig().keyItem().is(Trials.EVIL_KEYS[0]), "the Evil Vault wants the Evil Trial Key");
            boolean old = MobsConfig.voidChain;
            try {
                MobsConfig.voidChain = false;
                check(InWorldUpgrades.replace(level, pos, Trials.VOID_VAULT) == 0 && level.getBlockState(pos).is(Trials.EVIL_VAULT), "Void chain off: no Void Vault");
                MobsConfig.voidChain = true;
                check(InWorldUpgrades.replace(level, pos, Trials.VOID_VAULT) == 1 && level.getBlockState(pos).is(Trials.VOID_VAULT), "Void chain on: Void Vault");
                check(level.getBlockEntity(pos) instanceof VaultBlockEntity v2 && v2.getConfig().keyItem().is(Trials.VOID_KEYS[0]), "the Void Vault wants the Void Trial Key");
            } finally {
                MobsConfig.voidChain = old;
            }
            h.succeed();
        });
        add("trials_vault_keys", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, Trials.EVIL_VAULT.defaultBlockState().setValue(VaultBlock.STATE, VaultState.ACTIVE), 3);
            var vault = (VaultBlockEntity) level.getBlockEntity(pos);
            ItemStack wrong = new ItemStack(Items.TRIAL_KEY);
            VaultBlockEntity.Server.tryInsertKey(level, pos, level.getBlockState(pos), vault.getConfig(), vault.getServerData(), vault.getSharedData(), player, wrong);
            check(wrong.getCount() == 1 && level.getBlockState(pos).getValue(VaultBlock.STATE) == VaultState.ACTIVE, "a Trial Key does not open the Evil Vault");
            ItemStack voidKey = new ItemStack(Trials.VOID_KEYS[0]);
            VaultBlockEntity.Server.tryInsertKey(level, pos, level.getBlockState(pos), vault.getConfig(), vault.getServerData(), vault.getSharedData(), player, voidKey);
            check(voidKey.getCount() == 1, "a Void Key does not open the Evil Vault");
            ItemStack key = new ItemStack(Trials.EVIL_KEYS[2]);
            VaultBlockEntity.Server.tryInsertKey(level, pos, level.getBlockState(pos), vault.getConfig(), vault.getServerData(), vault.getSharedData(), player, key);
            check(level.getBlockState(pos).getValue(VaultBlock.STATE) == VaultState.UNLOCKING, "an Evil Trial Key of stage 3 opens it: " + key + " " + level.getBlockState(pos));   // a creative mock player keeps the key (Vanilla consume rule)
            check(Trials.keyStage(new ItemStack(Trials.EVIL_KEYS[2])) == 3 && Trials.keyKind(new ItemStack(Trials.VOID_KEYS[4])) == TrialKind.VOID && Trials.keyKind(new ItemStack(Items.TRIAL_KEY)) == null, "key helpers");
            h.succeed();
        });
        add("trials_evil_turn", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, Trials.WEATHERED_TRIAL_SPAWNER.defaultBlockState(), 3);
            var be = (TrialSpawnerBlockEntity) level.getBlockEntity(pos);
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            check(!EvilOmen.turn(level, pos, be.getTrialSpawner()), "no omen: the trial stays as it is");
            Trials.applyOmen(player, TrialKind.EVIL, 3);
            check(Trials.omenStage(player, TrialKind.EVIL) == 3, "drinking gives Evil Omen III");
            check(EvilOmen.turn(level, pos, be.getTrialSpawner()), "the omen turns the trial");
            var state = level.getBlockState(pos);
            check(WeatheredTrialSpawnerBlock.kind(state) == TrialKind.EVIL && WeatheredTrialSpawnerBlock.level(state) == 3, "Evil Trial, stage 3: " + state);
            check(Trials.omenStage(player, TrialKind.EVIL) == 0, "the omen is used up");
            check(Trials.spawnerLoot(TrialKind.EVIL, 3).equals(EvilOmen.rewardTable(level, pos)), "the reward is the Evil Trial Key table of stage 3");
            EvilOmen.reset(level, pos);
            check(WeatheredTrialSpawnerBlock.kind(level.getBlockState(pos)) == TrialKind.WEATHERED && EvilOmen.rewardTable(level, pos) == null, "back to a plain Weathered Trial");
            h.succeed();
        });
        add("trials_buffs", h -> {
            for (int n = 1; n <= 5; n++) check(Math.abs(EvilOmen.lifeBonus(TrialKind.EVIL, n) - 0.1 * n) < 1e-9, "Evil life bonus is 10 % per stage");
            check(EvilOmen.lifeBonus(TrialKind.VOID, 1) > EvilOmen.lifeBonus(TrialKind.EVIL, 5) - 1e-9, "Void is stronger than Evil");
            Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
            float base = zombie.getMaxHealth();
            EvilOmen.buff(zombie, TrialKind.EVIL, 3, 0.9f);
            check(Math.abs(zombie.getMaxHealth() - base * 1.3f) < 0.01f && zombie.getHealth() == zombie.getMaxHealth(), "stage 3: +30 % life, healed");
            var strength = zombie.getEffect(net.minecraft.world.effect.MobEffects.STRENGTH);
            check(strength != null && strength.getAmplifier() == 1 && zombie.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE), "stage 3: Strength II, Resistance");
            Zombie boss = h.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 1));
            EvilOmen.buff(boss, TrialKind.EVIL, 5, 0.1f);
            check(boss.hasEffect(CombatEffects.deflection()) || boss.hasEffect(CombatEffects.mirror()), "stage 5: Deflection or Mirror (MOB27-1)");
            Zombie voidMob = h.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
            EvilOmen.buff(voidMob, TrialKind.VOID, 1, 0.9f);
            check(voidMob.getEffect(net.minecraft.world.effect.MobEffects.STRENGTH).getAmplifier() == 1, "Void adds one effect step");
            h.succeed();
        });
        add("trials_config_void_off", h -> {
            boolean evil = MobsConfig.evilOmen, voidChain = MobsConfig.voidChain;
            try {
                MobsConfig.apply(new JsonObject());
                check(MobsConfig.evilOmen && !MobsConfig.voidChain, "defaults: Evil on, Void chain off");
                check(Trials.evilOn() && !Trials.voidOn() && !Trials.chainOn(TrialKind.VOID), "Void chain is off by default");
                check(TrialsViewer.special().stream().noneMatch(e -> e.id().contains("void")), "no Void brewing page while the chain is off");
                MobsConfig.voidChain = true;
                check(Trials.voidOn() && TrialsViewer.special().stream().anyMatch(e -> e.id().contains("void")), "Void chain on");
                MobsConfig.evilOmen = false;
                check(!Trials.evilOn() && !Trials.voidOn(), "the Evil switch is the master switch");
                check(TrialsViewer.infoPages().size() >= 4, "info pages for the flow, the Void chain, the conversion and the Weathered Trials");
            } finally {
                MobsConfig.evilOmen = evil;
                MobsConfig.voidChain = voidChain;
            }
            h.succeed();
        });

        // ---- TR31-R
        add("trials_spawner_cap", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, Trials.WEATHERED_TRIAL_SPAWNER.defaultBlockState(), 3);
            var spawner = ((TrialSpawnerBlockEntity) level.getBlockEntity(pos)).getTrialSpawner();
            int old = MobsConfig.trialSpawnerCap;
            try {
                MobsConfig.trialSpawnerCap = 3;
                TrialsExtras.clearActive();
                long now = level.getGameTime();
                TrialsExtras.markActive(level.dimension(), pos.offset(5, 0, 0), now);
                TrialsExtras.markActive(level.dimension(), pos.offset(0, 0, 6), now);
                check(!TrialsExtras.capReached(level, pos, spawner), "two active neighbours: below the cap of 3");
                TrialsExtras.markActive(level.dimension(), pos.offset(-4, 2, 4), now);
                check(TrialsExtras.capReached(level, pos, spawner), "three active neighbours: a fourth may not wake up");
                MobsConfig.trialSpawnerCap = 0;
                check(!TrialsExtras.capReached(level, pos, spawner), "cap 0 = no limit");
                MobsConfig.trialSpawnerCap = 3;
                TrialsExtras.clearActive();
                for (int i = 0; i < 3; i++) TrialsExtras.markActive(level.dimension(), pos.offset(40 + i, 0, 0), now);
                check(!TrialsExtras.capReached(level, pos, spawner), "active spawners of another structure (40 blocks away) do not count");
                TrialsExtras.clearActive();
                for (int i = 0; i < 3; i++) TrialsExtras.markActive(level.dimension(), pos.offset(3 + i, 0, 0), now - 200);
                check(!TrialsExtras.capReached(level, pos, spawner), "stale marks (spawner stopped ticking) do not count");
                level.setBlock(pos, Blocks.TRIAL_SPAWNER.defaultBlockState(), 3);
                TrialsExtras.clearActive();
                for (int i = 0; i < 3; i++) TrialsExtras.markActive(level.dimension(), pos.offset(3 + i, 0, 0), now);
                check(!TrialsExtras.capReached(level, pos, ((TrialSpawnerBlockEntity) level.getBlockEntity(pos)).getTrialSpawner()), "Vanilla Trial Spawners are never capped");
            } finally {
                MobsConfig.trialSpawnerCap = old;
                TrialsExtras.clearActive();
            }
            h.succeed();
        });
        add("trials_loot_distinct", h -> {
            java.util.Iterator<java.util.List<ItemStack>> rolls = java.util.List.of(
                    java.util.List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD)),
                    java.util.List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND)),
                    java.util.List.of(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND))).iterator();
            var drawn = TrialsExtras.drawDistinct(rolls::next);
            check(drawn.size() == 3 && drawn.stream().map(ItemStack::getItem).distinct().count() == 3, "a repeated draw is rolled again until nothing repeats: " + drawn);
            var always = TrialsExtras.drawDistinct(() -> java.util.List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND)));
            check(always.size() == 1 && always.get(0).is(Items.DIAMOND), "a pool that cannot avoid repeats keeps each item once: " + always);
            var tables = h.getLevel().getServer().reloadableRegistries();
            for (int n = 1; n <= 5; n++) {
                LootTable table = tables.getLootTable(Trials.vaultLoot(TrialKind.EVIL, n, false));
                LootParams params = vaultParams(h, new ItemStack(Trials.EVIL_KEYS[n - 1]));
                for (int i = 0; i < 60; i++) {
                    var stacks = TrialsExtras.drawDistinct(() -> table.getRandomItems(params));
                    check(stacks.size() >= 2 && stacks.stream().map(ItemStack::getItem).distinct().count() == stacks.size(), "Evil vault stage " + n + ": no item twice, at least two items: " + stacks);
                }
            }
            h.succeed();
        });
        add("trials_hit_effects", h -> {
            check(TrialsExtras.hitEffects(TrialKind.EVIL, 1, true, 0.0f).isEmpty(), "stage 1: no hit effects");
            check(TrialsExtras.hitEffects(TrialKind.EVIL, 2, false, 0.05f).stream().anyMatch(e -> e.effect().is(net.minecraft.world.effect.MobEffects.WITHER)), "stage 2: 10 % Wither");
            check(TrialsExtras.hitEffects(TrialKind.EVIL, 2, false, 0.5f).isEmpty(), "stage 2: usually nothing");
            check(TrialsExtras.hitEffects(TrialKind.EVIL, 3, true, 0.9f).stream().anyMatch(e -> e.effect().is(net.minecraft.world.effect.MobEffects.SLOWNESS) && e.amplifier() == 1), "stage 3: an arrow slows (II)");
            check(TrialsExtras.hitEffects(TrialKind.EVIL, 3, false, 0.9f).isEmpty(), "stage 3: a melee hit does not");
            check(TrialsExtras.hitEffects(TrialKind.EVIL, 5, false, 0.9f).stream().anyMatch(e -> e.effect().is(net.minecraft.world.effect.MobEffects.DARKNESS)), "stage 5: Darkness");
            check(TrialsExtras.hitEffects(TrialKind.VOID, 1, false, 0.05f).stream().anyMatch(e -> e.effect().is(net.minecraft.world.effect.MobEffects.WITHER)), "Void counts one stage higher");
            Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 1));
            EvilOmen.buff(zombie, TrialKind.EVIL, 5, 0.9f);
            int[] tag = TrialsExtras.evilOf(zombie);
            check(tag != null && tag[0] == TrialKind.EVIL.ordinal() && tag[1] == 5, "the buffed mob carries its kind and stage");
            check(TrialsExtras.evilOf(h.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1))) == null, "a plain mob has no tag");
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            boolean old = MobsConfig.evilHitEffects;
            try {
                MobsConfig.evilHitEffects = false;
                TrialsExtras.onHit(player, h.getLevel(), h.getLevel().damageSources().mobAttack(zombie));
                check(!player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS), "config off: no hit effects");
                MobsConfig.evilHitEffects = true;
                TrialsExtras.onHit(player, h.getLevel(), h.getLevel().damageSources().mobAttack(zombie));
                check(player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS), "an Evil V mob's hit darkens the player");
            } finally {
                MobsConfig.evilHitEffects = old;
            }
            h.succeed();
        });
        add("trials_void_tricks", h -> {
            ServerLevel level = h.getLevel();
            boolean voidOld = MobsConfig.voidChain, tricks = MobsConfig.voidMobTricks;
            try {
                for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                var skeleton = h.spawn(EntityTypes.SKELETON, new BlockPos(10, 1, 10));
                Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(12, 1, 10));
                check(TrialsExtras.isArcher(skeleton) && !TrialsExtras.isArcher(zombie), "skeletons shoot, zombies do not");
                EvilOmen.buff(skeleton, TrialKind.VOID, 1, 0.9f);
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                player.setPos(skeleton.getX() + 3, skeleton.getY(), skeleton.getZ());
                MobsConfig.voidMobTricks = true;
                MobsConfig.voidChain = false;
                TrialsExtras.auraLevel(level);
                check(player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS), "a Void mob within 6 blocks darkens the player");
                player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
                player.setPos(skeleton.getX() + 12, skeleton.getY(), skeleton.getZ());
                TrialsExtras.auraLevel(level);
                check(!player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS), "far away: no darkness");
                double x0 = skeleton.getX(), z0 = skeleton.getZ();
                check(TrialsExtras.teleportAway(skeleton, level) && (skeleton.getX() != x0 || skeleton.getZ() != z0), "the Void archer blinks away");
            } finally {
                MobsConfig.voidChain = voidOld;
                MobsConfig.voidMobTricks = tricks;
            }
            h.succeed();
        });
        add("trials_vanilla_spawner", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(pos, Blocks.TRIAL_SPAWNER.defaultBlockState(), 3);
            var spawner = ((TrialSpawnerBlockEntity) level.getBlockEntity(pos)).getTrialSpawner();
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            Trials.applyOmen(player, TrialKind.EVIL, 2);
            boolean old = MobsConfig.evilOnVanillaSpawners;
            try {
                check(!MobsConfig.evilOnVanillaSpawners, "default: Vanilla spawners are left alone");
                check(!EvilOmen.turn(level, pos, spawner) && EvilOmen.kindAt(level, pos) == TrialKind.WEATHERED, "config off: the omen does not turn a Vanilla spawner");
                MobsConfig.evilOnVanillaSpawners = true;
                check(EvilOmen.turn(level, pos, spawner), "config on: it does");
                check(EvilOmen.kindAt(level, pos) == TrialKind.EVIL && EvilOmen.stageAt(level, pos) == 2, "kind and stage are remembered");
                check(Trials.spawnerLoot(TrialKind.EVIL, 2).equals(EvilOmen.rewardTable(level, pos)), "the reward is the Evil key table of stage 2");
                EvilOmen.reset(level, pos);
                check(EvilOmen.kindAt(level, pos) == TrialKind.WEATHERED && EvilOmen.rewardTable(level, pos) == null, "reset: a plain Vanilla spawner again");
            } finally {
                MobsConfig.evilOnVanillaSpawners = old;
                EvilOmen.clearRaids();
            }
            h.succeed();
        });
        add("trials_raid_extras", h -> {
            ServerLevel level = h.getLevel();
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            boolean old = MobsConfig.evilRaidExtras;
            try {
                EvilOmen.clearRaids();
                net.minecraft.world.entity.raid.Raid plain = new net.minecraft.world.entity.raid.Raid(h.absolutePos(BlockPos.ZERO), level.getDifficulty());
                int waves = ((EvilRaid) plain).simplemobs$waves();
                EvilOmen.onRaidCreated(player, plain);
                check(((EvilRaid) plain).simplemobs$waves() == waves, "no running Evil Raid: no extra wave");
                EvilOmen.markRaid(player.getUUID(), TrialKind.EVIL, 3, level.getGameTime() + 1000);
                net.minecraft.world.entity.raid.Raid evil = new net.minecraft.world.entity.raid.Raid(h.absolutePos(BlockPos.ZERO), level.getDifficulty());
                int before = ((EvilRaid) evil).simplemobs$waves();
                EvilOmen.onRaidCreated(player, evil);
                check(((EvilRaid) evil).simplemobs$waves() == before + 1, "an Evil Raid has one more wave");
                EvilOmen.onRaidCreated(player, evil);
                check(((EvilRaid) evil).simplemobs$waves() == before + 1, "...only once");
                MobsConfig.evilRaidExtras = false;
                net.minecraft.world.entity.raid.Raid off = new net.minecraft.world.entity.raid.Raid(h.absolutePos(BlockPos.ZERO), level.getDifficulty());
                EvilOmen.onRaidCreated(player, off);
                check(((EvilRaid) off).simplemobs$waves() == before, "config off: no extra wave");
                for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                BlockPos chest = EvilOmen.placeRaidChest(level, h.absolutePos(new BlockPos(6, 1, 6)), TrialKind.EVIL, 3);
                check(level.getBlockState(chest).is(Blocks.CHEST), "the Looter Chest is a chest at " + chest);
                check(level.getBlockEntity(chest) instanceof net.minecraft.world.RandomizableContainer c && Trials.raidLoot(TrialKind.EVIL, 3).equals(c.getLootTable()), "with the loot table of the stage");
            } finally {
                MobsConfig.evilRaidExtras = old;
                EvilOmen.clearRaids();
            }
            h.succeed();
        });
        add("trials_structure_switch", h -> {
            var regs = h.getLevel().registryAccess();
            var weathered = regs.lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id("weathered_trial"))).orElseThrow();
            var vanilla = regs.lookupOrThrow(Registries.STRUCTURE).get(net.minecraft.world.level.levelgen.structure.BuiltinStructures.TRIAL_CHAMBERS).orElseThrow();
            boolean on = MobsConfig.weatheredStructures;
            int decay = MobsConfig.weatheredDecayPercent;
            try {
                MobsConfig.weatheredStructures = true;
                check(!TrialsExtras.weatheredBlocked(weathered), "default: the Weathered Trial generates");
                MobsConfig.weatheredStructures = false;
                check(TrialsExtras.weatheredBlocked(weathered) && !TrialsExtras.weatheredBlocked(vanilla), "off: only the Weathered Trial is blocked");
                var tag = TagKey.<Block>create(Registries.BLOCK, id("weathered_rottable"));
                check(TrialsExtras.isWeatheredRottable(tag) && !TrialsExtras.isWeatheredRottable(TagKey.<Block>create(Registries.BLOCK, Identifier.withDefaultNamespace("logs"))), "the decay tag is ours");
                var rot = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor(regs.lookupOrThrow(Registries.BLOCK).getOrThrow(tag), 0.89f);
                var other = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor(regs.lookupOrThrow(Registries.BLOCK).getOrThrow(TagKey.<Block>create(Registries.BLOCK, Identifier.withDefaultNamespace("logs"))), 1.0f);
                BlockPos at = h.absolutePos(BlockPos.ZERO);
                var copper = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(at, Blocks.OXIDIZED_CUT_COPPER.defaultBlockState(), null);
                var log = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(at, Blocks.OAK_LOG.defaultBlockState(), null);
                var settings = new StructurePlaceSettings();
                MobsConfig.weatheredDecayPercent = 100;
                check(rot.processBlock(h.getLevel(), at, at, at, copper, settings) == null, "decay 100 %: every copper block rots away");
                check(other.processBlock(h.getLevel(), at, at, at, log, settings) == log, "a Vanilla block_rot with another tag stays Vanilla");
                MobsConfig.weatheredDecayPercent = 0;
                check(rot.processBlock(h.getLevel(), at, at, at, copper, settings) == copper, "decay 0 %: nothing rots");
                check(TrialsExtras.integrity() == 1.0f, "integrity follows the percent");
                MobsConfig.weatheredDecayPercent = 11;
                check(Math.abs(TrialsExtras.integrity() - 0.89f) < 1e-6f, "default decay 11 % = the data pack's 0.89");
            } finally {
                MobsConfig.weatheredStructures = on;
                MobsConfig.weatheredDecayPercent = decay;
            }
            h.succeed();
        });
        add("trials_loot_pages", h -> {
            boolean voidOld = MobsConfig.voidChain;
            try {
                MobsConfig.voidChain = false;
                int off = TrialsViewer.lootPages().size();
                check(off == 7, "weathered, ominous and Evil I-V while the Void chain is off: " + off);
                MobsConfig.voidChain = true;
                check(TrialsViewer.lootPages().size() == 12, "plus Void I-V when it is on");
                var page = TrialsViewer.lootPages().get(2);
                check(page.lines().size() >= 8 && !page.items().isEmpty(), "a page lists the rolls and every entry");
            } finally {
                MobsConfig.voidChain = voidOld;
            }
            h.succeed();
        });
    }
}

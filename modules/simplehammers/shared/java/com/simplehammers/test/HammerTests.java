package com.simplehammers.test;

import com.google.gson.JsonObject;
import com.simplehammers.HammerTiers;
import com.simplehammers.SimpleHammers;
import com.simplehammers.config.HammerConfig;
import com.simplehammers.engine.HammerCorners;
import com.simplehammers.engine.SledgehammerCrafting;
import com.simplehammers.engine.SledgehammerUpgrades;
import com.simplehammers.engine.SledgehammerUtils;
import com.simplehammers.item.SledgehammerItem;
import com.simplehammers.registry.HammerRegistry;
import com.simplelib.api.HammerActions;
import com.simplelib.api.IdMoves;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests (split stage 7); the Fabric/NeoForge/Forge adapters register them as
 * module_game_test_&lt;name&gt; on {@code simplehammers:empty}. Every test runs standalone and with SimpleBuilding; checks
 * that only hold for one of them say so in the log.
 */
public final class HammerTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    static final BlockPos SPOT = new BlockPos(2, 1, 2);

    static {
        ALL.put("registered", h -> {
            List<String> problems = new ArrayList<>();
            for (HammerTiers tier : HammerTiers.values()) {
                var item = BuiltInRegistries.ITEM.getValue(SimpleHammers.id(tier.id()));
                if (!(item instanceof SledgehammerItem hammer) || hammer.tier() != tier) {
                    problems.add("item " + tier.id());
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                if (stack.getMaxDamage() != tier.baseDurability * SledgehammerItem.BASE_DURABILITY_MULTIPLIER) problems.add("durability " + tier.id());
                if (stack.has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT) != tier.fireResistant()) problems.add("fire resistance " + tier.id());
                if (!SimpleLibApi.isHammer(stack)) problems.add("not in #simplelib:hammers: " + tier.id());
                if (!stack.is(ItemTags.MINING_ENCHANTABLE)) problems.add("not in #minecraft:enchantable/mining: " + tier.id());
            }
            var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (String path : SimpleHammers.ENCHANTMENTS) {
                if (enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleHammers.id(path))).isEmpty()) problems.add("enchantment " + path);
            }
            if (!BuiltInRegistries.ITEM.containsKey(SimpleHammers.id("guide_book"))) problems.add("guide book");
            h.assertTrue(problems.isEmpty(), "registration: " + problems);
            h.succeed();
        });
        ALL.put("migration_from_simplebuilding", h -> {
            List<String> problems = new ArrayList<>();
            for (String tier : SimpleHammers.TIERS) {
                String path = tier + "_sledgehammer";
                if (BuiltInRegistries.ITEM.getValue(SimpleHammers.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleHammers.id(path))) problems.add("item " + path);
            }
            var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (String path : SimpleHammers.ENCHANTMENTS) {
                var moved = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleHammers.old(path)));
                var now = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleHammers.id(path)));
                if (moved.isEmpty() || now.isEmpty() || moved.get().value() != now.get().value()) problems.add("enchantment " + path);
            }
            SimpleHammers.RECIPES.forEach((path, folder) -> {
                if (!SimpleHammers.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleHammers.old(path)))) problems.add("recipe " + path);
                Identifier advancement = SimpleHammers.id("recipes/" + folder + "/" + path);
                if (!advancement.equals(IdMoves.advancement(SimpleHammers.old("recipes/" + folder + "/" + path)))) problems.add("advancement " + path);
            });
            // A stack of an old world: simplebuilding:diamond_sledgehammer with Radius and Override (old ids) and damage.
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("id", "simplebuilding:diamond_sledgehammer");
            tag.putInt("count", 1);
            net.minecraft.nbt.CompoundTag components = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag levels = new net.minecraft.nbt.CompoundTag();
            levels.putInt("simplebuilding:radius", 1);
            levels.putInt("simplebuilding:override", 2);
            components.put("minecraft:enchantments", levels);
            components.putInt("minecraft:damage", 7);
            tag.put("components", components);
            var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            ItemStack loaded = ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
            if (!(loaded.getItem() instanceof SledgehammerItem hammer) || hammer.tier() != HammerTiers.DIAMOND) problems.add("old stack: " + loaded);
            else {
                if (loaded.getDamageValue() != 7) problems.add("old stack damage " + loaded.getDamageValue());
                if (com.simplehammers.engine.HammerEnchantments.level(loaded, h.getLevel(), com.simplehammers.engine.HammerEnchantments.RADIUS) != 1
                        || com.simplehammers.engine.HammerEnchantments.level(loaded, h.getLevel(), com.simplehammers.engine.HammerEnchantments.OVERRIDE) != 2) {
                    problems.add("old enchantments lost: " + loaded.getEnchantments());
                }
            }
            h.assertTrue(problems.isEmpty(), "migration: " + problems);
            h.succeed();
        });
        ALL.put("config_defaults", h -> {
            JsonObject before = HammerConfig.toJson();
            HammerConfig.reset();
            h.assertTrue(HammerConfig.enabled && HammerConfig.upgradeSeconds == 5 && HammerConfig.reinforcedDamagePerHit == 2
                    && HammerConfig.netheriteDamagePerHit == 4 && HammerConfig.enderiteDamagePerHit == 10 && HammerConfig.diamondBlockPebbles == 81,
                    "defaults 5 s, 2/4/10, 81 pebbles");
            JsonObject json = new JsonObject();
            json.addProperty("upgradeSeconds", 999);
            json.addProperty("netheriteDamagePerHit", -3);
            json.addProperty("diamondBlockPebbles", "many");
            HammerConfig.apply(json);
            h.assertTrue(HammerConfig.upgradeSeconds == HammerConfig.MAX_UPGRADE_SECONDS && HammerConfig.netheriteDamagePerHit == 0
                    && HammerConfig.diamondBlockPebbles == 81, "clamped / junk falls back");
            h.assertTrue(HammerConfig.KEYS.size() == HammerConfig.toJson().size(), "KEYS lists every key of the file");
            HammerConfig.apply(before);
            h.succeed();
        });
        ALL.put("config_adopts_simplebuilding_tools", h -> {
            JsonObject before = HammerConfig.toJson();
            try {
                Path dir = Files.createTempDirectory("simplehammers-config");
                Files.writeString(dir.resolve(HammerConfig.LEGACY_FILE),
                        "{\"server\":{\"tools\":{\"sledgehammerUpgradeSeconds\":7,\"enderiteUpgradeDamagePerHit\":12,\"diamondBlockPebbles\":40,\"stoneChiselCooldownTicks\":3}}}");
                HammerConfig.load(dir);
                h.assertTrue(HammerConfig.upgradeSeconds == 7 && HammerConfig.enderiteDamagePerHit == 12 && HammerConfig.diamondBlockPebbles == 40
                        && HammerConfig.netheriteDamagePerHit == 4, "values taken over from simplebuilding.json");
                h.assertTrue(Files.isRegularFile(dir.resolve(HammerConfig.FILE)), "own file written");
                Files.writeString(dir.resolve(HammerConfig.LEGACY_FILE), "{\"server\":{\"tools\":{\"sledgehammerUpgradeSeconds\":9}}}");
                HammerConfig.load(dir);
                h.assertTrue(HammerConfig.upgradeSeconds == 7, "the takeover happens once");
            } catch (java.io.IOException e) {
                h.fail("temp config: " + e);
            } finally {
                HammerConfig.apply(before);
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            com.simplebuilding.framework.api.SubMods.set(SimpleHammers.MOD_ID, false);
            try {
                h.assertFalse(SimpleHammers.on(), "switched off");
                h.assertTrue(HammerRegistry.tabStacks().isEmpty(), "no tab stacks while off");
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleHammers.id("iron_sledgehammer")), "registrations stay");
                ServerPlayer player = lookingDown(h, new ItemStack(HammerRegistry.hammer(HammerTiers.IRON)));
                stoneLayer(h);
                h.assertTrue(SledgehammerItem.getBlocksToBeDestroyed(1, h.absolutePos(SPOT), player).isEmpty(), "no area while off");
                h.assertTrue(SledgehammerUtils.miningSpeedDivisor(player, h.absolutePos(SPOT)) == 1.0F, "mines like a pickaxe while off");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleHammers.MOD_ID, true);
            }
            h.succeed();
        });
        ALL.put("area_break", h -> {
            stoneLayer(h);
            ServerPlayer player = lookingDown(h, new ItemStack(HammerRegistry.hammer(HammerTiers.IRON)));
            BlockPos center = h.absolutePos(SPOT);
            h.assertTrue(SledgehammerUtils.countBlocksBroken(player, center) == 9, "3x3 of stone counted");
            h.assertTrue(Math.abs(SledgehammerUtils.miningSpeedDivisor(player, center) - SledgehammerUtils.swingTimeFactor(9, false)) < 1.0E-4F,
                    "area time follows the piecewise factor");
            h.assertTrue(player.gameMode.destroyBlock(center), "origin breaks");
            int broken = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (h.getLevel().getBlockState(center.offset(dx, 0, dz)).isAir()) broken++;
            h.assertTrue(broken == 9, "the hammer broke its area (" + broken + " of 9)");
            h.assertFalse(HammerActions.areaBreaking(), "area flag cleared after the swing");
            player.setShiftKeyDown(true);
            stoneLayer(h);
            h.assertTrue(SledgehammerItem.getBlocksToBeDestroyed(1, center, player).size() == 1, "sneaking breaks one block");
            h.succeed();
        });
        ALL.put("carving_makes_stairs", h -> {
            BlockState carved = HammerCorners.subtract(Blocks.STONE.defaultBlockState(), true, Direction.UP, new Vec3(0.25, 1.0, 0.25));
            h.assertTrue(carved != null && carved.getBlock() == Blocks.STONE_STAIRS, "one octet off a stone block is a stone stair, got " + carved);
            h.assertTrue(carved.hasProperty(HammerCorners.CARVED) && carved.getValue(HammerCorners.CARVED), "the stair keeps its carved shape");
            BlockState slab = HammerCorners.subtract(Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM),
                    false, Direction.UP, new Vec3(0.25, 1.0, 0.25));
            h.assertTrue(slab != null, "a stair carves on");
            h.assertTrue(SledgehammerItem.reshapeTarget(Blocks.OAK_PLANKS, false, true).orElse(null) == Blocks.OAK_STAIRS
                    && SledgehammerItem.reshapeTarget(Blocks.OAK_STAIRS, false, false).orElse(null) == Blocks.OAK_SLAB
                    && SledgehammerItem.reshapeTarget(Blocks.OAK_SLAB, true, false).orElse(null) == Blocks.OAK_STAIRS, "reshape name rule");
            h.succeed();
        });
        ALL.put("diamond_block_crush", h -> {
            BlockPos pos = h.absolutePos(SPOT);
            h.getLevel().setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
            ServerPlayer player = lookingDown(h, new ItemStack(HammerRegistry.hammer(HammerTiers.IRON)));
            ItemStack hammer = player.getMainHandItem();
            h.assertFalse(SledgehammerItem.strikesDiamondBlock(Blocks.DIAMOND_BLOCK.defaultBlockState(), HammerRegistry.hammer(HammerTiers.STONE)),
                    "a stone hammer only clanks");
            long now = h.getLevel().getGameTime();
            int done = 0;
            for (int i = 0; i < SledgehammerItem.DIAMOND_BLOCK_STRIKES; i++) {
                done = SledgehammerItem.strikeDiamondBlock(h.getLevel(), pos, player, hammer, now + i * 20L);
            }
            h.assertTrue(done == SledgehammerItem.DIAMOND_BLOCK_STRIKES && h.getLevel().getBlockState(pos).isAir(), "nine strikes crush the block");
            int pebbles = 0;
            for (ItemEntity item : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
                if (item.getItem().is(SimpleLibApi.materialItem("diamond_pebble"))) pebbles += item.getItem().getCount();
            }
            h.assertTrue(pebbles == HammerConfig.diamondBlockPebbles, "pebbles " + pebbles + " of " + HammerConfig.diamondBlockPebbles);
            h.succeed();
        });
        ALL.put("upgrade_table", h -> {
            var entry = new InWorldUpgrades.Entry(Blocks.CAULDRON, Blocks.ANVIL, Items.IRON_NUGGET, 2, 3, InWorldUpgrades.UPGRADE_TOOLS,
                    "netherite", 4, "enderite");
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgrade(entry);
            h.assertTrue(upgrade != null && upgrade.minHammerRank() == SledgehammerUpgrades.RANK_NETHERITE && upgrade.toEnderite()
                    && !upgrade.toReinforced() && upgrade.durationFactor() == 3 && upgrade.materialCost() == 2, "entry read: " + upgrade);
            h.assertTrue(SledgehammerUpgrades.upgrade(new InWorldUpgrades.Entry(Blocks.CAULDRON, Blocks.ANVIL, Items.IRON_NUGGET, 1, 1,
                    InWorldUpgrades.UPGRADE_TOOLS, "wooden", 1, "netherite")) == null, "unknown grade skipped");
            h.assertTrue(SledgehammerUpgrades.hammerRank(HammerRegistry.hammer(HammerTiers.DIAMOND)) == SledgehammerUpgrades.RANK_DIAMOND
                    && SledgehammerUpgrades.hammerRank(HammerRegistry.hammer(HammerTiers.ENDERITE)) == SledgehammerUpgrades.RANK_ENDERITE
                    && SledgehammerUpgrades.hammerRank(HammerRegistry.hammer(HammerTiers.GOLD)) == 0, "hammer ranks");
            h.assertTrue(SledgehammerUpgrades.blows() == HammerConfig.upgradeSeconds, "blows from the config");
            h.assertFalse(SimpleLibApi.axeWaysEnabled(), "the hammer mod switches the Vanilla axe ways off");
            // The shipped upgrades of the loaded mods all read (crucibles, ovens, SimpleBuilding's machines).
            int unread = 0;
            for (InWorldUpgrades.Entry each : InWorldUpgrades.table().values()) if (SledgehammerUpgrades.upgrade(each) == null) unread++;
            h.assertTrue(unread == 0, unread + " shipped upgrade entries unreadable");
            h.succeed();
        });
        ALL.put("upgrade_tooltip_hints", h -> {
            // Queue 738: the hint under upgradable block items is the module's own (SimpleBuilding 26.3 adds none).
            var en = lang("en_us");
            var de = lang("de_de");
            List<String> problems = new ArrayList<>();
            Map<String, SledgehammerUpgrades.Upgrade> ways = new LinkedHashMap<>();
            ways.put("reinforced", new SledgehammerUpgrades.Upgrade(Blocks.CAULDRON, Blocks.ANVIL, Items.DIAMOND, 0, 2, false, true, 1, 8));
            ways.put("netherite", new SledgehammerUpgrades.Upgrade(Blocks.CAULDRON, Blocks.ANVIL, Items.NETHERITE_SCRAP, 1, 4, false, false));
            ways.put("enderite", new SledgehammerUpgrades.Upgrade(Blocks.CAULDRON, Blocks.ANVIL, Items.ENDER_PEARL, 2, 10, true, false));
            ways.put("shulker_box", new SledgehammerUpgrades.Upgrade(Blocks.BARREL, Blocks.SHULKER_BOX, Items.DIAMOND, 0, 2, false, true, 2, 2));
            ways.forEach((way, upgrade) -> {
                List<net.minecraft.network.chat.Component> lines = com.simplehammers.engine.UpgradeHints.lines(upgrade);
                String first = lines.isEmpty() ? "" : lines.get(0).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc ? tc.getKey() : "";
                Object[] args = lines.size() < 2 ? new Object[0] : lines.get(1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc ? tc.getArgs() : new Object[0];
                if (lines.size() != 2 || !first.equals(com.simplehammers.engine.UpgradeHints.PREFIX + way)) problems.add(way + ": " + lines);
                else if (args.length != 2 || !Integer.valueOf(upgrade.materialCost()).equals(args[0])) problems.add(way + " material args " + java.util.Arrays.toString(args));
            });
            for (String key : List.of("reinforced", "netherite", "enderite", "shulker_box", "material")) {
                String full = com.simplehammers.engine.UpgradeHints.PREFIX + key;
                if (!en.containsKey(full) || !de.containsKey(full)) problems.add("no EN/DE text " + full);
            }
            // Every upgradable block item of the loaded mods gets the two lines; anything else none.
            int hinted = 0;
            for (SledgehammerUpgrades.Upgrade upgrade : SledgehammerUpgrades.table().values()) {
                ItemStack stack = new ItemStack(upgrade.from().asItem());
                if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item) || item.getBlock() != upgrade.from()) continue;
                if (com.simplehammers.engine.UpgradeHints.lines(stack).size() != 2) problems.add("no hint on " + stack);
                hinted++;
            }
            if (!com.simplehammers.engine.UpgradeHints.lines(new ItemStack(Items.STONE)).isEmpty()) problems.add("hint on stone");
            h.assertTrue(problems.isEmpty(), "upgrade hints: " + problems);
            com.simplehammers.SimpleHammers.LOG.info("upgrade_tooltip_hints: {} upgradable block items hinted", hinted);
            h.succeed();
        });
        ALL.put("hammer_stays_in_the_grid", h -> {
            ItemStack hammer = new ItemStack(HammerRegistry.hammer(HammerTiers.IRON));
            ItemStack after = SledgehammerCrafting.damaged(hammer);
            h.assertTrue(after.is(hammer.getItem()) && after.getDamageValue() == SledgehammerCrafting.CRAFT_DAMAGE, "hammer comes back damaged");
            hammer.setDamageValue(hammer.getMaxDamage() - 1);
            h.assertTrue(SledgehammerCrafting.damaged(hammer).isEmpty(), "a hammer that breaks is gone");
            h.succeed();
        });
        ALL.put("guide_book", com.simplehammers.guide.HammersGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplehammers.discover.HammersDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplehammers.discover.HammersDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplehammers.discover.HammersDiscover::firstPickupTips);
    }

    private HammerTests() {}

    /** The module's own language file (HammersDiscover is generated by tools/discover/discover.py and keeps its reader private). */
    static Map<String, String> lang(String locale) {
        Map<String, String> out = new LinkedHashMap<>();
        try (var in = HammerTests.class.getResourceAsStream("/assets/" + SimpleHammers.MOD_ID + "/lang/" + locale + ".json")) {
            if (in == null) return out;
            for (var entry : com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().entrySet()) {
                out.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception e) {
            return out;
        }
        return out;
    }

    /** A 3x3 layer of stone around {@link #SPOT} (relative y 1). */
    static void stoneLayer(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) h.setBlock(SPOT.offset(dx, 0, dz), Blocks.STONE);
    }

    /** A survival player above {@link #SPOT}, looking straight down, {@code tool} in the main hand. */
    static ServerPlayer lookingDown(GameTestHelper h, ItemStack tool) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos center = h.absolutePos(SPOT);
        player.snapTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 0.0F, 90.0F);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }
}

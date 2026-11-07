package com.simplechisels.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplechisels.ChiselTier;
import com.simplechisels.SimpleChisels;
import com.simplechisels.config.ChiselConfig;
import com.simplechisels.data.BuiltInChains;
import com.simplechisels.data.ChiselTables;
import com.simplechisels.item.ChiselItem;
import com.simplechisels.registry.ChiselRegistry;
import com.simplelib.api.IdMoves;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Module tests of Simple Chisels (WE25-7); the Fabric adapter registers them as module_game_test_&lt;name&gt; on
 * {@code simplechisels:empty}. Every test runs standalone and with SimpleBuilding; checks that only hold for one of them say so.
 */
public final class ChiselTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static final BlockPos SPOT = new BlockPos(1, 1, 1);

    private ChiselTests() {}

    static {
        ALL.put("registered", h -> {
            List<String> problems = new ArrayList<>();
            for (ChiselTier tier : ChiselTier.values()) {
                var item = BuiltInRegistries.ITEM.getValue(SimpleChisels.id(tier.id()));
                if (!(item instanceof ChiselItem chisel) || chisel.tier() != tier) {
                    problems.add("item " + tier.id());
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                if (stack.getMaxDamage() != tier.durability) problems.add("durability " + tier.id());
                if (stack.has(DataComponents.DAMAGE_RESISTANT) != tier.fireResistant()) problems.add("fire resistance " + tier.id());
                if (!stack.is(net.minecraft.tags.TagKey.create(Registries.ITEM, SimpleChisels.id("chisels")))) problems.add("tag " + tier.id());
                if (!stack.is(ItemTags.DURABILITY_ENCHANTABLE)) problems.add("not durability enchantable: " + tier.id());
            }
            var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var fast = enchantments.get(ChiselItem.FAST_CHISELING);
            if (fast.isEmpty()) problems.add("enchantment fast_chiseling");
            else if (!fast.get().value().canEnchant(new ItemStack(ChiselRegistry.chisel(ChiselTier.STONE)))) problems.add("fast_chiseling on a chisel");
            if (fast.isPresent() && fast.get().value().canEnchant(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE))) problems.add("fast_chiseling on a pickaxe");
            if (!BuiltInRegistries.ITEM.containsKey(SimpleChisels.id("guide_book"))) problems.add("guide book");
            if (ChiselRegistry.TAB == null || ChiselRegistry.tabStacks().size() != ChiselTier.values().length + 1) problems.add("tab: " + ChiselRegistry.tabStacks());
            if (com.simplelib.api.ChiselActions.tools().size() != ChiselTier.values().length) problems.add("published tools");
            h.assertTrue(problems.isEmpty(), "registration: " + problems);
            h.succeed();
        });
        ALL.put("migration_from_simplebuilding", h -> {
            List<String> problems = new ArrayList<>();
            for (ChiselTier tier : ChiselTier.values()) {
                if (BuiltInRegistries.ITEM.getValue(SimpleChisels.old(tier.id())) != ChiselRegistry.chisel(tier)) problems.add("item " + tier.id());
            }
            for (String tier : SimpleChisels.SPATULA_TIERS) {
                if (!SimpleChisels.id(tier + "_chisel").equals(IdMoves.target(Registries.ITEM, SimpleChisels.old(tier + "_spatula")))) problems.add("spatula " + tier);
            }
            var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var moved = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleChisels.old(SimpleChisels.FAST_CHISELING)));
            var now = enchantments.get(ChiselItem.FAST_CHISELING);
            if (moved.isEmpty() || now.isEmpty() || moved.get().value() != now.get().value()) problems.add("enchantment fast_chiseling");
            SimpleChisels.RECIPES.forEach((path, folder) -> {
                if (!SimpleChisels.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleChisels.old(path)))) problems.add("recipe " + path);
                if (!SimpleChisels.id("recipes/" + folder + "/" + path).equals(IdMoves.advancement(SimpleChisels.old("recipes/" + folder + "/" + path)))) {
                    problems.add("advancement " + path);
                }
            });
            // Stacks of an old world: a damaged netherite spatula with the old Fast Chiseling, and a named diamond chisel.
            var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            ItemStack spatula = ItemStack.CODEC.parse(ops, oldStack("simplebuilding:netherite_spatula", 9, 2)).result().orElse(ItemStack.EMPTY);
            if (!(spatula.getItem() instanceof ChiselItem chisel) || chisel.tier() != ChiselTier.NETHERITE) problems.add("old spatula: " + spatula);
            else {
                if (spatula.getDamageValue() != 9) problems.add("old spatula damage " + spatula.getDamageValue());
                if (ChiselItem.level(spatula, h.getLevel(), ChiselItem.FAST_CHISELING) != 2) problems.add("old Fast Chiseling lost: " + spatula.getEnchantments());
            }
            ItemStack chisel = ItemStack.CODEC.parse(ops, oldStack("simplebuilding:diamond_chisel", 4, 1)).result().orElse(ItemStack.EMPTY);
            if (!chisel.is(ChiselRegistry.chisel(ChiselTier.DIAMOND)) || chisel.getDamageValue() != 4) problems.add("old chisel: " + chisel);
            h.assertTrue(problems.isEmpty(), "migration: " + problems);
            h.succeed();
        });
        ALL.put("forward_and_back", h -> {
            h.setBlock(SPOT, Blocks.STONE);
            ServerPlayer player = player(h, new ItemStack(ChiselRegistry.chisel(ChiselTier.STONE)));
            h.assertTrue(click(h, player, false).consumesAction(), "the stone chisel acts on stone");
            h.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, SPOT);
            ItemStack stack = player.getMainHandItem();
            h.assertTrue(stack.getDamageValue() == ChiselItem.TRANSFORM_DAMAGE, "one step costs 1, got " + stack.getDamageValue());
            h.assertTrue(player.getCooldowns().isOnCooldown(stack), "cooldown after a step");
            h.assertFalse(click(h, player, false).consumesAction(), "no second step while cooling down");
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
            h.assertTrue(click(h, player, true).consumesAction(), "sneaking walks back");
            h.assertBlockPresent(Blocks.STONE, SPOT);
            h.assertTrue(stack.getDamageValue() == ChiselItem.TRANSFORM_DAMAGE + ChiselItem.REVERSE_TRANSFORM_DAMAGE, "a step back costs 2");
            h.assertTrue(h.absolutePos(SPOT).equals(stack.get(ChiselRegistry.LAST_TARGET)), "last target stored");
            // A block outside the stone table: nothing happens.
            h.setBlock(SPOT, Blocks.POLISHED_BLACKSTONE);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
            h.assertFalse(click(h, player, false).consumesAction(), "the stone chisel leaves blackstone alone");
            h.assertBlockPresent(Blocks.POLISHED_BLACKSTONE, SPOT);
            h.succeed();
        });
        ALL.put("tiers_inherit", h -> {
            List<String> problems = new ArrayList<>();
            Map<Block, Block> stone = ChiselTables.tier("stone").forward();
            for (String tier : ChiselTables.TIERS) {
                if (!ChiselTables.tier(tier).forward().entrySet().containsAll(stone.entrySet()) && !stone.isEmpty()) problems.add(tier + " lacks stone");
            }
            if (ChiselTables.tier("diamond").forward().get(Blocks.POLISHED_BLACKSTONE) != Blocks.BLACKSTONE) problems.add("diamond: polished blackstone");
            if (ChiselTables.tier("iron").forward().containsKey(Blocks.POLISHED_BLACKSTONE)) problems.add("iron reaches blackstone");
            if (ChiselRegistry.chisel(ChiselTier.COPPER).getForwardMap() != ChiselRegistry.chisel(ChiselTier.IRON).getForwardMap()) problems.add("copper = iron table");
            if (ChiselTables.tier("stone").touchForward().get(Blocks.OAK_LOG) != Blocks.STRIPPED_OAK_LOG) problems.add("touch: oak log");
            if (ChiselTables.tier("stone").forward().containsKey(Blocks.OAK_LOG)) problems.add("plain stone chisel strips logs");
            h.assertTrue(problems.isEmpty(), "tier tables: " + problems);
            h.succeed();
        });
        ALL.put("datapack_tables", h -> {
            List<String> problems = new ArrayList<>();
            if (!ChiselTables.isLoaded()) problems.add("no chisel files were loaded");
            // The shipped Vanilla files equal the built-in chains (the defaults never drift from the files).
            Map<Block, Block> loaded = ChiselTables.tier("netherite").touchForward();
            Map<Block, Block> builtIn = ChiselTables.builtInTiers().get("netherite").touchForward();
            for (var link : builtIn.entrySet()) {
                if (loaded.get(link.getKey()) != link.getValue()) problems.add("built-in " + link + " not in the loaded table");
            }
            if (BuiltInChains.all().isEmpty()) problems.add("built-in chains empty");
            try {
                Map<Identifier, com.google.gson.JsonElement> files = new HashMap<>();
                files.put(SimpleChisels.id("stone_chisel"), JsonParser.parseString(
                        "{\"tier\":\"stone\",\"table\":\"chisel\",\"chains\":[{\"blocks\":[\"minecraft:stone\",\"minecraft:chiseled_stone_bricks\"]}]}"));
                files.put(Identifier.fromNamespaceAndPath("othermod", "extra"), JsonParser.parseString(
                        "{\"tier\":\"stone\",\"chains\":[{\"blocks\":[\"minecraft:dirt\",\"minecraft:coarse_dirt\",\"othermod:missing\"]}],"
                                + "\"remove\":[\"minecraft:stone\"]}"));
                ChiselTables.apply(files);
                Map<Block, Block> stone = ChiselTables.tier("stone").forward();
                if (stone.get(Blocks.DIRT) != Blocks.COARSE_DIRT) problems.add("other file adds dirt -> coarse dirt");
                if (stone.containsKey(Blocks.COARSE_DIRT)) problems.add("link to an unknown block not skipped");
                if (stone.containsKey(Blocks.STONE)) problems.add("remove after the defaults");
                if (ChiselTables.tier("iron").forward().get(Blocks.DIRT) != Blocks.COARSE_DIRT) problems.add("iron inherits the addition");
            } finally {
                ChiselTables.reapplyLastLoad();
            }
            if (ChiselTables.tier("stone").forward().get(Blocks.STONE) != Blocks.CHISELED_STONE_BRICKS) problems.add("tables restored");
            h.assertTrue(problems.isEmpty(), "datapack tables: " + problems);
            h.succeed();
        });
        ALL.put("fast_chiseling_cooldown", h -> {
            var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ChiselItem.FAST_CHISELING);
            ItemStack stack = new ItemStack(ChiselRegistry.chisel(ChiselTier.STONE));
            EnchantmentHelper.updateEnchantments(stack, e -> e.set(holder, 2));
            h.setBlock(SPOT, Blocks.STONE);
            ServerPlayer player = player(h, stack);
            h.assertTrue(click(h, player, false).consumesAction(), "the enchanted chisel acts");
            h.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "cooldown running");
            int expected = Math.max(1, (int) (ChiselConfig.cooldownTicks(ChiselTier.STONE) * (1.0f - 2 * 0.3f)));
            // 30 * (1 - 0.6f) in float is 11.999..., floored: 11 ticks (the formula SimpleBuilding's chisel always had).
            h.assertTrue(expected == 11, "Fast Chiseling II: 40 % of 30 ticks, floored = 11, got " + expected);
            float plainSpeed = new ItemStack(ChiselRegistry.chisel(ChiselTier.STONE)).getDestroySpeed(Blocks.STONE.defaultBlockState());
            h.assertTrue(stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > plainSpeed, "Fast Chiseling mines faster");
            h.succeed();
        });
        ALL.put("config_defaults_and_adoption", h -> {
            JsonObject before = ChiselConfig.toJson();
            try {
                ChiselConfig.reset();
                h.assertTrue(ChiselConfig.enabled && ChiselConfig.cooldownTicks(ChiselTier.STONE) == 30 && ChiselConfig.cooldownTicks(ChiselTier.COPPER) == 25
                        && ChiselConfig.cooldownTicks(ChiselTier.GOLD) == 20 && ChiselConfig.cooldownTicks(ChiselTier.ENDERITE) == 5, "defaults");
                JsonObject junk = JsonParser.parseString("{\"cooldownTicks\":{\"iron\":999,\"gold\":-1,\"diamond\":\"x\"}}").getAsJsonObject();
                ChiselConfig.apply(junk);
                h.assertTrue(ChiselConfig.cooldownTicks(ChiselTier.IRON) == ChiselConfig.MAX_COOLDOWN && ChiselConfig.cooldownTicks(ChiselTier.GOLD) == ChiselConfig.MIN_COOLDOWN
                        && ChiselConfig.cooldownTicks(ChiselTier.DIAMOND) == 10, "clamped / junk falls back");
                h.assertTrue(ChiselConfig.KEYS.size() == 1 + ChiselTier.values().length, "KEYS lists every key");
                Path dir = Files.createTempDirectory("simplechisels-config");
                Files.writeString(dir.resolve(ChiselConfig.LEGACY_FILE),
                        "{\"server\":{\"tools\":{\"stoneChiselCooldownTicks\":12,\"netheriteChiselCooldownTicks\":3,\"sledgehammerUpgradeSeconds\":7}}}");
                ChiselConfig.load(dir);
                h.assertTrue(ChiselConfig.cooldownTicks(ChiselTier.STONE) == 12 && ChiselConfig.cooldownTicks(ChiselTier.NETHERITE) == 3
                        && ChiselConfig.cooldownTicks(ChiselTier.IRON) == 25, "cooldowns taken over from simplebuilding.json");
                h.assertTrue(Files.isRegularFile(dir.resolve(ChiselConfig.FILE)), "own file written");
                Files.writeString(dir.resolve(ChiselConfig.LEGACY_FILE), "{\"server\":{\"tools\":{\"stoneChiselCooldownTicks\":40}}}");
                ChiselConfig.load(dir);
                h.assertTrue(ChiselConfig.cooldownTicks(ChiselTier.STONE) == 12, "the takeover happens once");
            } catch (java.io.IOException e) {
                h.fail("temp config: " + e);
            } finally {
                ChiselConfig.apply(before);
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            com.simplebuilding.framework.api.SubMods.set(SimpleChisels.MOD_ID, false);
            try {
                h.assertFalse(SimpleChisels.on(), "switched off");
                h.assertTrue(ChiselRegistry.tabStacks().isEmpty(), "no tab stacks while off");
                h.setBlock(SPOT, Blocks.STONE);
                ServerPlayer player = player(h, new ItemStack(ChiselRegistry.chisel(ChiselTier.STONE)));
                h.assertTrue(click(h, player, false) == InteractionResult.PASS, "no reshaping while off");
                h.assertBlockPresent(Blocks.STONE, SPOT);
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleChisels.id("stone_chisel")), "registrations stay");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleChisels.MOD_ID, true);
            }
            h.succeed();
        });
        ALL.put("enderite_needs_simple_ores", h -> {
            var recipes = h.getLevel().getServer().getRecipeManager();
            boolean present = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleChisels.id("enderite_chisel_smithing"))).isPresent();
            boolean ores = SimpleChisels.isLoaded("simpleores");
            h.assertTrue(present == ores, "the Enderite smithing recipe loads exactly with Simple Ores (" + ores + ")");
            boolean upgrade = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleChisels.id("upgrade_copper_chisel_to_iron_chisel"))).isPresent();
            h.assertTrue(upgrade == SimpleChisels.isLoaded("simplebuilding"), "the Basic Upgrade recipes load exactly with SimpleBuilding");
            h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleChisels.id("stone_chisel"))).isPresent(), "plain recipes always");
            h.succeed();
        });
        ALL.put("with_simplebuilding", h -> {
            if (!SimpleChisels.isLoaded("simplebuilding")) {
                com.mojang.logging.LogUtils.getLogger().info("[standalone] SimpleBuilding missing - its chains, Constructor's Touch and statistic are not checked");
                h.succeed();
                return;
            }
            List<String> problems = new ArrayList<>();
            // SimpleBuilding's own data file adds its End stone palette to the Enderite tier (read like any datapack file).
            Block polished = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "polished_astralit"));
            Block pillar = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "astralit_pillar"));
            if (ChiselTables.tier("enderite").forward().get(polished) != pillar) problems.add("enderite: polished astralit -> astralit pillar (SB file)");
            if (ChiselTables.tier("netherite").forward().containsKey(polished)) problems.add("netherite reaches the End palette");
            // Constructor's Touch (SimpleBuilding's enchantment, read by id): cobblestone -> mossy with the stone chisel.
            var touch = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ChiselItem.CONSTRUCTORS_TOUCH);
            if (touch.isEmpty()) problems.add("Constructor's Touch missing");
            else {
                ItemStack stack = new ItemStack(ChiselRegistry.chisel(ChiselTier.STONE));
                EnchantmentHelper.updateEnchantments(stack, e -> e.set(touch.get(), 1));
                h.setBlock(SPOT, Blocks.COBBLESTONE);
                ServerPlayer player = player(h, stack);
                // The registered instance: StatType#get looks its key up by identity.
                Identifier usesId = BuiltInRegistries.CUSTOM_STAT.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "chisel_uses"));
                net.minecraft.stats.Stat<Identifier> uses = net.minecraft.stats.Stats.CUSTOM.get(usesId);
                int before = player.getStats().getValue(uses);
                if (!click(h, player, false).consumesAction() || h.getBlockState(SPOT).getBlock() != Blocks.MOSSY_COBBLESTONE) {
                    problems.add("touch: cobblestone -> mossy cobblestone, got " + h.getBlockState(SPOT));
                }
                if (player.getStats().getValue(uses) != before + 1) problems.add("SimpleBuilding's chisel_uses statistic did not count");
            }
            h.assertTrue(problems.isEmpty(), "with SimpleBuilding: " + problems);
            h.succeed();
        });
    }

    private static net.minecraft.nbt.CompoundTag oldStack(String id, int damage, int fastChiseling) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", id);
        tag.putInt("count", 1);
        net.minecraft.nbt.CompoundTag components = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.CompoundTag levels = new net.minecraft.nbt.CompoundTag();
        levels.putInt("simplebuilding:fast_chiseling", fastChiseling);
        components.put("minecraft:enchantments", levels);
        components.putInt("minecraft:damage", damage);
        tag.put("components", components);
        return tag;
    }

    /** A survival player above {@link #SPOT}, {@code tool} in the main hand. */
    static ServerPlayer player(GameTestHelper h, ItemStack tool) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(SPOT);
        player.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    /** A right click on the middle of the top face of {@link #SPOT}. */
    static InteractionResult click(GameTestHelper h, ServerPlayer player, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        BlockPos at = h.absolutePos(SPOT);
        BlockHitResult hit = new BlockHitResult(new Vec3(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5), Direction.UP, at, false);
        InteractionResult result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        player.setShiftKeyDown(false);
        return result;
    }
}

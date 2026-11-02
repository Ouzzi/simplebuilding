package com.simpleovens.test;

import com.google.gson.JsonObject;
import com.simplelib.api.IdMoves;
import com.simplelib.api.InWorldStrikes;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import com.simpleovens.LongCookContainerData;
import com.simpleovens.OvenPerks;
import com.simpleovens.OvenTier;
import com.simpleovens.OvensRegistry;
import com.simpleovens.SimpleOvens;
import com.simpleovens.block.OvenUse;
import com.simpleovens.cart.TieredFurnaceMinecart;
import com.simpleovens.config.OvensConfig;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests (split plan stage 4); the Fabric/NeoForge/Forge adapters register them as
 * module_game_test_<name> on {@code simpleovens:empty}. Every test runs standalone (axe mode) and with SimpleBuilding
 * (hammer mode, read from SimpleLib and the registry, never a class); partner-only checks say so in the log.
 */
public final class OvensTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    static final BlockPos SPOT = new BlockPos(2, 1, 2);

    static {
        ALL.put("config_defaults", h -> {
            OvensConfig.reset();
            h.assertTrue(OvensConfig.enabled && OvensConfig.reinforcedSpeed == 2 && OvensConfig.netheriteSpeed == 4
                    && OvensConfig.enderiteSpeed == 8, "defaults 2/4/8");
            JsonObject json = new JsonObject();
            json.addProperty("reinforcedSpeed", 99);
            json.addProperty("netheriteSpeed", 0);
            json.addProperty("enderiteSpeed", "fast");
            OvensConfig.apply(json);
            h.assertTrue(OvensConfig.reinforcedSpeed == OvensConfig.MAX_SPEED && OvensConfig.netheriteSpeed == 1
                    && OvensConfig.enderiteSpeed == 8, "clamped / junk falls back");
            h.assertTrue(OvensConfig.KEYS.size() == OvensConfig.toJson().size(), "KEYS lists every key of the file");
            OvensConfig.reset();
            h.succeed();
        });
        ALL.put("config_adopts_simplebuilding_speeds", h -> {
            JsonObject before = OvensConfig.toJson();
            try {
                Path dir = Files.createTempDirectory("simpleovens-config");
                Files.writeString(dir.resolve(OvensConfig.LEGACY_FILE),
                        "{\"server\":{\"machines\":{\"reinforcedFurnaceSpeed\":3,\"enderiteFurnaceSpeed\":6,\"reinforcedHopperSpeed\":5}}}");
                OvensConfig.load(dir);
                h.assertTrue(OvensConfig.reinforcedSpeed == 3 && OvensConfig.netheriteSpeed == 4 && OvensConfig.enderiteSpeed == 6,
                        "speeds taken over from simplebuilding.json: " + OvensConfig.toJson());
                h.assertTrue(Files.readString(dir.resolve(OvensConfig.LEGACY_FILE)).contains("reinforcedHopperSpeed"), "old file unchanged");
                Files.writeString(dir.resolve(OvensConfig.LEGACY_FILE), "{\"server\":{\"machines\":{\"reinforcedFurnaceSpeed\":7}}}");
                OvensConfig.load(dir);
                h.assertTrue(OvensConfig.reinforcedSpeed == 3, "taken over once: an existing simpleovens file wins");
            } catch (java.io.IOException e) {
                throw h.assertionException("temp config: " + e);
            } finally {
                OvensConfig.apply(before);
            }
            h.succeed();
        });
        ALL.put("registered", h -> {
            List<String> problems = new ArrayList<>();
            for (String path : SimpleOvens.MOVED_BLOCKS) {
                if (!BuiltInRegistries.BLOCK.containsKey(SimpleOvens.id(path))) problems.add("block " + path);
                if (!BuiltInRegistries.ITEM.containsKey(SimpleOvens.id(path))) problems.add("item " + path);
            }
            for (String path : SimpleOvens.MOVED_CARTS) {
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleOvens.id(path))) problems.add("entity " + path);
                if (!BuiltInRegistries.ITEM.containsKey(SimpleOvens.id(path))) problems.add("item " + path);
            }
            for (String path : SimpleOvens.MOVED_BLOCK_ENTITIES.values()) {
                if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleOvens.id(path))) problems.add("block entity " + path);
            }
            for (OvenTier tier : OvenTier.values()) {
                if (!OvensRegistry.FURNACE_BE.isValid(OvensRegistry.FURNACES.get(tier).defaultBlockState())) problems.add("furnace BE " + tier);
                if (!OvensRegistry.SMOKER_BE.isValid(OvensRegistry.SMOKERS.get(tier).defaultBlockState())) problems.add("smoker BE " + tier);
                if (!OvensRegistry.BLAST_FURNACE_BE.isValid(OvensRegistry.BLAST_FURNACES.get(tier).defaultBlockState())) problems.add("blast BE " + tier);
                boolean fireproof = new ItemStack(OvensRegistry.FURNACES.get(tier)).has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT);
                if (fireproof != tier.fireResistant()) problems.add("fire resistance of " + tier);
            }
            if (OvensRegistry.FURNACE_BE.isValid(OvensRegistry.SMOKERS.get(OvenTier.ENDERITE).defaultBlockState())) problems.add("furnace BE takes a smoker");
            h.assertTrue(problems.isEmpty(), "registration: " + problems);
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            com.simplebuilding.framework.api.SubMods.set(SimpleOvens.MOD_ID, false);
            try {
                h.assertFalse(SimpleOvens.on(), "switched off");
                h.assertTrue(OvensRegistry.tabStacks().isEmpty() && OvensRegistry.machineStacks().isEmpty(), "no tab stacks while off");
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleOvens.id("enderite_furnace")), "registrations stay");
                Player player = h.makeMockServerPlayer(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(SimpleLibApi.materialItem("netherite_nugget")));
                h.setBlock(SPOT, OvensRegistry.FURNACES.get(OvenTier.REINFORCED));
                h.assertTrue(OvenUse.useItemOn(h.getBlockState(SPOT), h.getLevel(), h.absolutePos(SPOT), player, InteractionHand.MAIN_HAND) == null,
                        "no upgrade click while off");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleOvens.MOD_ID, true);
            }
            h.succeed();
        });
        ALL.put("migration_from_simplebuilding", h -> {
            List<String> problems = new ArrayList<>();
            for (String path : SimpleOvens.MOVED_BLOCKS) {
                if (BuiltInRegistries.BLOCK.getValue(SimpleOvens.old(path)) != BuiltInRegistries.BLOCK.getValue(SimpleOvens.id(path))) problems.add("block " + path);
                if (BuiltInRegistries.ITEM.getValue(SimpleOvens.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleOvens.id(path))) problems.add("item " + path);
            }
            for (String path : SimpleOvens.MOVED_CARTS) {
                if (BuiltInRegistries.ENTITY_TYPE.getValue(SimpleOvens.old(path)) != BuiltInRegistries.ENTITY_TYPE.getValue(SimpleOvens.id(path))) problems.add("entity " + path);
            }
            SimpleOvens.MOVED_BLOCK_ENTITIES.forEach((from, to) -> {
                if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleOvens.old(from)) != BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleOvens.id(to))) problems.add("block entity " + from);
            });
            SimpleOvens.MOVED_RECIPES.forEach((path, folder) -> {
                if (!SimpleOvens.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleOvens.old(path)))) problems.add("recipe " + path);
                Identifier advancement = SimpleOvens.id("recipes/" + folder + "/" + path);
                if (!advancement.equals(IdMoves.advancement(SimpleOvens.old("recipes/" + folder + "/" + path)))) problems.add("advancement " + path);
            });
            // A furnace of an old world: block entity id simplebuilding:mod_blast_furnace with its contents.
            BlockState state = OvensRegistry.BLAST_FURNACES.get(OvenTier.NETHERITE).defaultBlockState();
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("id", "simplebuilding:mod_blast_furnace");
            net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
            net.minecraft.nbt.CompoundTag slot = new net.minecraft.nbt.CompoundTag();
            slot.putByte("Slot", (byte) 0);
            slot.putString("id", "minecraft:raw_iron");
            slot.putInt("count", 5);
            items.add(slot);
            tag.put("Items", items);
            tag.putInt("cooking_time_spent", 40);
            BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(SPOT), state, tag, h.getLevel().registryAccess());
            if (!(loaded instanceof AbstractFurnaceBlockEntity furnace) || loaded.getType() != OvensRegistry.BLAST_FURNACE_BE
                    || !furnace.getItem(0).is(Items.RAW_IRON) || furnace.getItem(0).getCount() != 5) {
                problems.add("old blast furnace entity loads as " + loaded);
            } else if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(loaded.getType()).equals(SimpleOvens.id("blast_furnace"))) {
                problems.add("saved as " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(loaded.getType()));
            }
            h.assertTrue(problems.isEmpty(), "migration: " + problems);
            h.succeed();
        });
        MAX_TICKS.put("tiers_cook_faster", 400);
        ALL.put("tiers_cook_faster", h -> {
            OvensConfig.reset();
            List<String> problems = new ArrayList<>();
            List<List<Block>> families = List.of(
                    List.of(Blocks.FURNACE, OvensRegistry.FURNACES.get(OvenTier.REINFORCED), OvensRegistry.FURNACES.get(OvenTier.NETHERITE), OvensRegistry.FURNACES.get(OvenTier.ENDERITE)),
                    List.of(Blocks.SMOKER, OvensRegistry.SMOKERS.get(OvenTier.REINFORCED), OvensRegistry.SMOKERS.get(OvenTier.NETHERITE), OvensRegistry.SMOKERS.get(OvenTier.ENDERITE)),
                    List.of(Blocks.BLAST_FURNACE, OvensRegistry.BLAST_FURNACES.get(OvenTier.REINFORCED), OvensRegistry.BLAST_FURNACES.get(OvenTier.NETHERITE), OvensRegistry.BLAST_FURNACES.get(OvenTier.ENDERITE)));
            List<Item> inputs = List.of(Items.RAW_IRON, Items.BEEF, Items.RAW_IRON);
            for (int f = 0; f < families.size(); f++) {
                for (int i = 0; i < 4; i++) {
                    BlockPos pos = new BlockPos(1 + i, 1, 1 + 2 * f);
                    h.setBlock(pos, families.get(f).get(i));
                    AbstractFurnaceBlockEntity furnace = h.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
                    furnace.setItem(0, new ItemStack(inputs.get(f), 4));
                    furnace.setItem(1, new ItemStack(Items.COAL, 4));
                }
            }
            h.runAfterDelay(20, () -> {
                for (int f = 0; f < families.size(); f++) {
                    int vanilla = progress(h, new BlockPos(1, 1, 1 + 2 * f));
                    int[] speeds = {2, 4, 8};
                    for (int i = 1; i < 4; i++) {
                        int got = progress(h, new BlockPos(1 + i, 1, 1 + 2 * f));
                        int expected = vanilla * speeds[i - 1];
                        // A boost never reaches the full cook time; a finished item resets the timer, so allow either.
                        if (got < expected - speeds[i - 1] * 2 && output(h, new BlockPos(1 + i, 1, 1 + 2 * f)) == 0) {
                            problems.add(families.get(f).get(i) + ": " + got + " cook ticks after 20 ticks, Vanilla " + vanilla + " (x" + speeds[i - 1] + ")");
                        }
                    }
                    if (vanilla <= 0) problems.add("Vanilla " + families.get(f).get(0) + " did not cook");
                }
                h.assertTrue(problems.isEmpty(), "speeds: " + problems);
                h.succeed();
            });
        });
        ALL.put("upper_tiers_pay_double_experience", h -> {
            ServerLevel level = h.getLevel();
            RecipeHolder<?> iron = level.getServer().getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")))
                    .orElseThrow(() -> h.assertionException("no Vanilla iron smelting recipe"));
            List<String> problems = new ArrayList<>();
            int[] expected = {1, 2, 2};
            for (OvenTier tier : OvenTier.values()) {
                h.setBlock(SPOT, OvensRegistry.FURNACES.get(tier));
                AbstractFurnaceBlockEntity furnace = h.getBlockEntity(SPOT, AbstractFurnaceBlockEntity.class);
                furnace.setRecipeUsed(iron);
                int used = recipesUsed(h, furnace, iron.id().identifier().toString());
                if (used != expected[tier.ordinal()]) problems.add(tier + " counts the smelt " + used + " times");
                h.setBlock(SPOT, Blocks.AIR);
            }
            // Cracked diamonds: no bonus (their lossless crafting cycle would farm experience).
            var cracked = level.getServer().getRecipeManager().getRecipes().stream()
                    .filter(r -> r.value() instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe cooking
                            && cooking.input().items().anyMatch(item -> item.is(OvenPerks.FURNACE_BONUS_EXCLUDED)))
                    .findFirst();
            if (cracked.isPresent()) {
                h.setBlock(SPOT, OvensRegistry.BLAST_FURNACES.get(OvenTier.ENDERITE));
                AbstractFurnaceBlockEntity furnace = h.getBlockEntity(SPOT, AbstractFurnaceBlockEntity.class);
                furnace.setRecipeUsed(cracked.get());
                int used = recipesUsed(h, furnace, cracked.get().id().identifier().toString());
                if (used != 1) problems.add("an excluded input counts " + used + " times");
            } else {
                problems.add("no cooking recipe with an input of #simpleovens:furnace_bonus_excluded (cracked diamond)");
            }
            h.assertTrue(problems.isEmpty(), "experience: " + problems);
            h.succeed();
        });
        ALL.put("every_tier_opens_its_vanilla_menu", h -> {
            ServerPlayer player = inLevel(h);
            List<String> problems = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            for (OvenTier tier : OvenTier.values()) {
                for (Map.Entry<Block, MenuType<?>> entry : Map.<Block, MenuType<?>>of(OvensRegistry.FURNACES.get(tier), MenuType.FURNACE,
                        OvensRegistry.SMOKERS.get(tier), MenuType.SMOKER, OvensRegistry.BLAST_FURNACES.get(tier), MenuType.BLAST_FURNACE).entrySet()) {
                    h.setBlock(SPOT, entry.getKey());
                    player.containerMenu = player.inventoryMenu;
                    h.useBlock(SPOT, player);
                    AbstractContainerMenu menu = player.containerMenu;
                    if (menu == player.inventoryMenu || menu.getType() != entry.getValue()) problems.add(entry.getKey() + " opened " + menu.getType());
                    BlockEntity be = h.getLevel().getBlockEntity(h.absolutePos(SPOT));
                    if (be instanceof net.minecraft.world.Nameable nameable && nameable.getDisplayName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key) {
                        titles.add(key.getKey());
                    }
                }
            }
            player.containerMenu = player.inventoryMenu;
            if (titles.stream().distinct().count() != 9) problems.add("container titles not distinct: " + titles);
            if (!titles.stream().allMatch(t -> t.startsWith("container.simpleovens."))) problems.add("titles " + titles);
            h.assertTrue(problems.isEmpty(), "menus: " + problems);
            h.succeed();
        });
        ALL.put("long_cook_times_reach_the_client", h -> {
            SimpleContainerData data = new SimpleContainerData(4);
            data.set(0, 50000);
            data.set(1, 72000);
            data.set(2, 36000);
            data.set(3, 72000);
            LongCookContainerData wrapped = new LongCookContainerData(data);
            h.assertTrue(wrapped.get(3) <= Short.MAX_VALUE && wrapped.get(1) <= Short.MAX_VALUE, "totals fit a short");
            h.assertTrue(Math.abs(wrapped.get(2) * 2 - wrapped.get(3)) <= 2, "progress ratio kept: " + wrapped.get(2) + "/" + wrapped.get(3));
            // The mixin: what a furnace menu sends goes through the same division.
            List<Integer> sent = new ArrayList<>();
            FurnaceMenu menu = new FurnaceMenu(1, h.makeMockServerPlayer(GameType.SURVIVAL).getInventory(), new net.minecraft.world.SimpleContainer(3), data);
            menu.addSlotListener(new ContainerListener() {
                @Override
                public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack stack) {
                }

                @Override
                public void dataChanged(AbstractContainerMenu container, int id, int value) {
                    sent.add(value);
                }
            });
            menu.broadcastChanges();
            h.assertTrue(!sent.isEmpty() && sent.stream().allMatch(v -> v <= Short.MAX_VALUE), "menu data sent as " + sent);
            h.succeed();
        });
        ALL.put("recipes", h -> {
            List<String> problems = new ArrayList<>();
            var manager = h.getLevel().getServer().getRecipeManager();
            SimpleOvens.MOVED_RECIPES.keySet().forEach(path -> {
                var holder = manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleOvens.id(path)));
                // IG31-13: Enderite recipes load only with Simple Ores (owner of the material).
                boolean expected = !path.contains("enderite") || SimpleLibApi.materialPresent("enderite"); // MACH32-ENDSYS: every Enderite recipe is conditional
                if (holder.isEmpty() == expected) problems.add((expected ? "missing " : "present without Simple Ores: ") + path);
            });
            long old = manager.getRecipes().stream().filter(r -> SimpleOvens.MOVED_RECIPES.containsKey(r.id().identifier().getPath())
                    && r.id().identifier().getNamespace().equals(SimpleOvens.OLD_NAMESPACE)).count();
            if (old > 0) problems.add(old + " furnace recipes still under simplebuilding:");
            h.assertTrue(problems.isEmpty(), "recipes: " + problems);
            h.succeed();
        });
        ALL.put("upgrade_table", h -> {
            List<String> problems = new ArrayList<>();
            for (Map<OvenTier, Block> family : List.of(OvensRegistry.FURNACES, OvensRegistry.SMOKERS, OvensRegistry.BLAST_FURNACES)) {
                InWorldUpgrades.Entry toNetherite = InWorldUpgrades.entryFor(family.get(OvenTier.REINFORCED));
                InWorldUpgrades.Entry toEnderite = InWorldUpgrades.entryFor(family.get(OvenTier.NETHERITE));
                if (toNetherite == null || toNetherite.to() != family.get(OvenTier.NETHERITE) || toNetherite.material() != SimpleLibApi.materialItem("netherite_nugget")) {
                    problems.add("reinforced -> netherite: " + toNetherite);
                }
                // IG31-13: the Enderite nugget is Simple Ores' (by id); without it there is no Enderite step.
                var nugget = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleores", "enderite_nugget"));
                if (!SimpleLibApi.materialPresent("enderite")) {
                    if (toEnderite != null) problems.add("netherite -> enderite without Simple Ores: " + toEnderite);
                } else if (toEnderite == null || toEnderite.to() != family.get(OvenTier.ENDERITE) || toEnderite.material() != nugget) {
                    problems.add("netherite -> enderite: " + toEnderite);
                }
                if (InWorldUpgrades.entryFor(family.get(OvenTier.ENDERITE)) != null) problems.add("enderite is the last tier");
            }
            h.assertTrue(problems.isEmpty(), "upgrade table: " + problems);
            h.succeed();
        });
        ALL.put("axe_upgrade_alone", h -> {
            if (!SimpleLibApi.axeWaysEnabled()) {
                // A hammer mod is loaded (SimpleBuilding): the axe does nothing on a furnace.
                Player player = h.makeMockServerPlayer(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(SimpleLibApi.materialItem("netherite_nugget")));
                h.setBlock(SPOT, OvensRegistry.FURNACES.get(OvenTier.REINFORCED));
                h.assertFalse(InWorldUpgrades.axeWants(h.getBlockState(SPOT), player, InteractionHand.MAIN_HAND), "axe way while a hammer mod is loaded");
                SimpleOvens.LOG.info("[with hammer mod] axe way off, checked");
                h.succeed();
                return;
            }
            Player player = h.makeMockServerPlayer(GameType.SURVIVAL);
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(SimpleLibApi.materialItem("netherite_nugget"), 3));
            h.setBlock(SPOT, OvensRegistry.SMOKERS.get(OvenTier.REINFORCED).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.EAST));
            AbstractFurnaceBlockEntity before = h.getBlockEntity(SPOT, AbstractFurnaceBlockEntity.class);
            before.setItem(0, new ItemStack(Items.BEEF, 7));
            BlockPos abs = h.absolutePos(SPOT);
            int strikes = InWorldUpgrades.AXE_STRIKES;
            for (int i = 1; i <= strikes; i++) {
                InWorldStrikes.allowNextStrike(h.getLevel(), abs);
                InteractionResult result = OvenUse.useItemOn(h.getLevel().getBlockState(abs), h.getLevel(), abs, player, InteractionHand.MAIN_HAND);
                h.assertTrue(result == InteractionResult.SUCCESS, "strike " + i + " answered " + result);
                if (i < strikes) h.assertTrue(h.getBlockState(SPOT).is(OvensRegistry.SMOKERS.get(OvenTier.REINFORCED)), "upgraded before the last strike");
            }
            BlockState after = h.getBlockState(SPOT);
            h.assertTrue(after.is(OvensRegistry.SMOKERS.get(OvenTier.NETHERITE)), "after " + strikes + " strikes: " + after);
            h.assertTrue(after.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING) == Direction.EAST, "facing kept");
            AbstractFurnaceBlockEntity entity = h.getBlockEntity(SPOT, AbstractFurnaceBlockEntity.class);
            h.assertTrue(entity.getItem(0).is(Items.BEEF) && entity.getItem(0).getCount() == 7, "contents kept: " + entity.getItem(0));
            h.assertTrue(player.getOffhandItem().getCount() == 2, "one nugget used, left " + player.getOffhandItem().getCount());
            h.assertTrue(axe.getDamageValue() > 0, "the axe wears");
            // An axe without the material opens the menu (no upgrade click).
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            h.assertTrue(OvenUse.useItemOn(after, h.getLevel(), abs, player, InteractionHand.MAIN_HAND) == null, "no material, no upgrade click");
            h.succeed();
        });
        ALL.put("hammer_click_passes_on", h -> {
            Identifier hammerId = Identifier.fromNamespaceAndPath("simplehammers", "diamond_sledgehammer");
            if (!BuiltInRegistries.ITEM.containsKey(hammerId)) {
                SimpleOvens.LOG.info("[standalone] no hammer mod - hammer click not checked");
                h.succeed();
                return;
            }
            Player player = h.makeMockServerPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(hammerId)));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(SimpleLibApi.materialItem("netherite_nugget")));
            h.setBlock(SPOT, OvensRegistry.BLAST_FURNACES.get(OvenTier.REINFORCED));
            InteractionResult result = OvenUse.useItemOn(h.getBlockState(SPOT), h.getLevel(), h.absolutePos(SPOT), player, InteractionHand.MAIN_HAND);
            h.assertTrue(result == InteractionResult.PASS, "the hammer stance passes on to the hammer, got " + result);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            h.assertTrue(OvenUse.useItemOn(h.getBlockState(SPOT), h.getLevel(), h.absolutePos(SPOT), player, InteractionHand.MAIN_HAND) == null,
                    "a hammer without the material opens the menu");
            h.succeed();
        });
        ALL.put("enderite_loot_alone", h -> {
            // IG31-13: Enderite and its End city loot belong to Simple Ores; SimpleLib adds none any more, and the Enderite
            // tier of Simple Ovens is reachable exactly when a mod provides Enderite.
            List<LootPool.Builder> pools = new ArrayList<>();
            com.simplelib.material.MaterialLoot.apply(BuiltInLootTables.END_CITY_TREASURE, pools::add, h.getLevel().registryAccess());
            h.assertTrue(SimpleLibApi.materialRequested("netherite_nugget"), "Simple Ovens requests its materials");
            h.assertTrue(pools.isEmpty(), "SimpleLib adds no Enderite loot: " + pools.size() + " pools");
            boolean ores = SimpleLibApi.materialPresent("enderite");
            var recipe = h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, SimpleOvens.id("enderite_furnace_minecart_smithing")));
            h.assertTrue(recipe.isPresent() == ores, "the Enderite cart recipe exists exactly with Simple Ores (" + ores + ")");
            h.succeed();
        });
        MAX_TICKS.put("furnace_carts_run_on_fuel", 200);
        ALL.put("furnace_carts_run_on_fuel", h -> {
            OvensConfig.reset();
            List<String> problems = new ArrayList<>();
            for (OvenTier tier : OvenTier.values()) {
                TieredFurnaceMinecart cart = OvensRegistry.CARTS.get(tier).create(h.getLevel(), EntitySpawnReason.TRIGGERED);
                if (cart == null) {
                    problems.add("no cart " + tier);
                    continue;
                }
                cart.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5)));
                if (!cart.addFuel(cart.position().add(1, 0, 0), new ItemStack(Items.COAL))) problems.add(tier + " refused coal");
                if (cart.fuel() != TieredFurnaceMinecart.VANILLA_FUEL_PER_ITEM * tier.speed()) problems.add(tier + " fuel " + cart.fuel());
                int added = 1;
                while (cart.addFuel(cart.position(), new ItemStack(Items.COAL))) added++;
                if (cart.fuel() > TieredFurnaceMinecart.VANILLA_MAX_FUEL * tier.speed()) problems.add(tier + " over-full: " + cart.fuel());
                if (cart.getPickResult().getItem() != OvensRegistry.CART_ITEMS.get(tier)) problems.add(tier + " pick result");
                if (!cart.getDefaultDisplayBlockState().is(OvensRegistry.FURNACES.get(tier))) problems.add(tier + " shows " + cart.getDefaultDisplayBlockState());
            }
            if (!(TieredFurnaceMinecart.topSpeedShare(OvenTier.REINFORCED) < TieredFurnaceMinecart.topSpeedShare(OvenTier.NETHERITE)
                    && TieredFurnaceMinecart.topSpeedShare(OvenTier.ENDERITE) == 1.0)) problems.add("top speeds");
            h.assertTrue(problems.isEmpty(), "carts: " + problems);
            h.succeed();
        });
    }

    /** BOAT40: the furnace boats. */
    static {
        ALL.put("furnace_boats_registered_and_crafted", h -> {
            List<String> problems = new ArrayList<>();
            var manager = h.getLevel().getServer().getRecipeManager();
            for (String wood : OvensRegistry.BOAT_WOODS) {
                String name = OvensRegistry.boatName(wood);
                Item item = OvensRegistry.BOAT_ITEMS.get(wood);
                if (item == null || BuiltInRegistries.ITEM.getValue(SimpleOvens.id(name)) != item) problems.add("item " + name);
                if (BuiltInRegistries.ENTITY_TYPE.getValue(SimpleOvens.id(name)) != OvensRegistry.boatType(wood)) problems.add("entity " + name);
                var holder = manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleOvens.id(name)));
                Item vanilla = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(wood + (wood.equals("bamboo") ? "_raft" : "_boat")));
                if (holder.isEmpty() || !(holder.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)) {
                    problems.add("recipe " + name);
                    continue;
                }
                var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.FURNACE), new ItemStack(vanilla)));
                if (!recipe.matches(input, h.getLevel()) || recipe.assemble(input).getItem() != item) problems.add("boat + furnace gives " + name + "? " + wood);
                var wrong = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.CHEST), new ItemStack(vanilla)));
                if (recipe.matches(wrong, h.getLevel())) problems.add("a chest also crafts " + name);
            }
            h.assertTrue(problems.isEmpty(), "furnace boats: " + problems);
            h.succeed();
        });
        ALL.put("furnace_boat_fuel", h -> {
            List<String> problems = new ArrayList<>();
            var boat = OvensRegistry.boatType("oak").create(h.getLevel(), EntitySpawnReason.TRIGGERED);
            if (boat == null) throw h.assertionException("no furnace boat");
            if (boat.isLit() || boat.fuel() != 0 || boat.thrust() != 0.0) problems.add("starts with fuel");
            if (boat.addFuel(new ItemStack(Items.STICK))) problems.add("a stick is no fuel");
            if (!boat.addFuel(new ItemStack(Items.COAL)) || boat.fuel() != com.simpleovens.boat.OvenBoat.FUEL_PER_ITEM || !boat.isLit()) problems.add("coal: " + boat.fuel());
            boat.burn(false);
            if (boat.fuel() != com.simpleovens.boat.OvenBoat.FUEL_PER_ITEM) problems.add("burns while empty of passengers");
            boat.burn(true);
            if (boat.fuel() != com.simpleovens.boat.OvenBoat.FUEL_PER_ITEM - 1) problems.add("does not burn with a passenger");
            if (!boat.addFuel(new ItemStack(Items.CHARCOAL)) || boat.fuel() != 2 * com.simpleovens.boat.OvenBoat.FUEL_PER_ITEM - 1) problems.add("charcoal adds on top: " + boat.fuel());
            int added = 2;
            while (boat.addFuel(new ItemStack(Items.COAL))) added++;
            if (boat.fuel() > com.simpleovens.boat.OvenBoat.MAX_FUEL || added != 8) problems.add("max fuel: " + boat.fuel() + " after " + added);
            // saved and read back
            var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
            boat.saveWithoutId(out);
            var copy = OvensRegistry.boatType("oak").create(h.getLevel(), EntitySpawnReason.TRIGGERED);
            copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
            if (copy.fuel() != boat.fuel() || !copy.isLit()) problems.add("fuel not saved: " + copy.fuel());
            while (boat.fuel() > 0) boat.burn(true);
            if (boat.isLit() || boat.thrust() != 0.0) problems.add("lit without fuel");
            h.assertTrue(problems.isEmpty(), "furnace boat fuel: " + problems);
            h.succeed();
        });
        MAX_TICKS.put("furnace_boat_pushes_only_with_fuel", 100);
        ALL.put("furnace_boat_pushes_only_with_fuel", h -> {
            for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
            var boat = OvensRegistry.boatType("oak").create(h.getLevel(), EntitySpawnReason.TRIGGERED);
            if (boat == null) throw h.assertionException("no furnace boat");
            boat.setInitialPos(h.absoluteVec(new Vec3(2.5, 1.2, 2.5)).x, h.absoluteVec(new Vec3(2.5, 1.2, 2.5)).y, h.absoluteVec(new Vec3(2.5, 1.2, 2.5)).z);
            boat.setYRot(0.0F);
            h.getLevel().addFreshEntity(boat);
            h.runAfterDelay(10, () -> {
                h.assertTrue(boat.isInWater(), "the boat floats in the water");
                Vec3 before = boat.getDeltaMovement();
                h.assertFalse(boat.applyThrust() || !boat.getDeltaMovement().equals(before), "no push without fuel");
                boat.addFuel(new ItemStack(Items.COAL));
                before = boat.getDeltaMovement();
                h.assertTrue(boat.applyThrust(), "a push with fuel");
                // heading 0 = +z (like Vanilla: sin(-yaw), cos(yaw))
                h.assertTrue(boat.getDeltaMovement().z - before.z > 0.01 && Math.abs(boat.getDeltaMovement().x - before.x) < 1.0E-6, "pushes forward: " + boat.getDeltaMovement());
                boat.setDeltaMovement(new Vec3(0.0, 0.0, com.simpleovens.boat.OvenBoat.THRUST_SPEED_CAP + 0.01));
                h.assertFalse(boat.applyThrust(), "no push over the speed cap");
                while (boat.fuel() > 0) boat.burn(true);
                boat.setDeltaMovement(Vec3.ZERO);
                h.assertFalse(boat.applyThrust(), "no push once the fuel is gone");
                boat.discard();
                h.succeed();
            });
        });
        ALL.put("furnace_boat_drops_its_item", h -> {
            var boat = OvensRegistry.boatType("bamboo").create(h.getLevel(), EntitySpawnReason.TRIGGERED);
            if (boat == null) throw h.assertionException("no furnace raft");
            Vec3 at = h.absoluteVec(new Vec3(2.5, 1, 2.5));
            boat.setInitialPos(at.x, at.y, at.z);
            h.getLevel().addFreshEntity(boat);
            h.assertTrue(boat.getPickResult().getItem() == OvensRegistry.BOAT_ITEMS.get("bamboo"), "pick result");
            boat.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 5.0F);
            h.assertTrue(boat.isRemoved(), "destroyed by damage");
            var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, net.minecraft.world.phys.AABB.ofSize(at, 4, 4, 4));
            h.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(OvensRegistry.BOAT_ITEMS.get("bamboo"))), "drops the furnace raft item: " + drops);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            h.succeed();
        });
    }

    static {
        ALL.put("discover_recipe_unlocks", com.simpleovens.discover.OvensDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleovens.discover.OvensDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simpleovens.discover.OvensDiscover::firstPickupTips);
    }

    private OvensTests() {}

    static int progress(GameTestHelper h, BlockPos pos) {
        BlockEntity be = h.getLevel().getBlockEntity(h.absolutePos(pos));
        return be == null ? -1 : be.saveWithoutMetadata(h.getLevel().registryAccess()).getIntOr("cooking_time_spent", -1);
    }

    static int output(GameTestHelper h, BlockPos pos) {
        return h.getBlockEntity(pos, AbstractFurnaceBlockEntity.class).getItem(2).getCount();
    }

    static int recipesUsed(GameTestHelper h, AbstractFurnaceBlockEntity furnace, String recipe) {
        return furnace.saveWithoutMetadata(h.getLevel().registryAccess()).getCompoundOrEmpty("RecipesUsed").getIntOr(recipe, 0);
    }

    /** A creative player in the level (menus open); handed back when the test ends. */
    static ServerPlayer inLevel(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Vec3 pos = h.absoluteVec(new Vec3(3.5, 1.0, 4.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        h.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            h.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }
}

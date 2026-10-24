package com.simpletrims.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simpletrims.SimpleTrims;
import com.simpletrims.TemplateTools;
import com.simpletrims.TrimGlow;
import com.simpletrims.TrimUpgrades;
import com.simpletrims.TrimsRegistry;
import com.simpletrims.config.TrimsConfig;
import com.simpletrims.light.OwnedLightHolder;
import com.simpletrims.light.TrimLight;
import com.simpletrims.placed.PlacedTemplates;
import com.simpletrims.placed.PlacedTrimTemplateBlock;
import com.simpletrims.placed.PlacedTrimTemplateBlockEntity;
import com.simpletrims.recipe.ToolShapelessRecipe;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests; the Fabric/NeoForge/Forge adapters register them as module_game_test_<name>. Every
 * test also runs standalone (axe mode); with SimpleBuilding loaded (Fabric integration run) the same tests check the
 * hammer mode, read from {@link TemplateTools#hammerMode()} - never from a class of SimpleBuilding.
 */
public final class TrimsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();

    static {
        ALL.put("config_defaults", h -> {
            TrimsConfig.apply(new JsonObject());
            h.assertTrue(TrimsConfig.enabled, "enabled by default");
            JsonObject off = new JsonObject();
            off.addProperty("enabled", false);
            TrimsConfig.apply(off);
            h.assertTrue(!TrimsConfig.enabled, "enabled=false is read");
            JsonObject junk = new JsonObject();
            junk.addProperty("enabled", "maybe");
            TrimsConfig.apply(junk);
            h.assertTrue(TrimsConfig.enabled, "junk falls back to the default");
            TrimsConfig.reset();
            h.succeed();
        });
        ALL.put("template_tool_without_simplebuilding", h -> {
            h.assertTrue(TemplateTools.isTemplateTool(new ItemStack(Items.IRON_AXE), false), "axe mode: the axe works");
            h.assertTrue(!TemplateTools.isTemplateTool(new ItemStack(Items.IRON_PICKAXE), false), "axe mode: a pickaxe does not");
            h.succeed();
        });
        ALL.put("template_tool_with_simplebuilding", h -> {
            h.assertTrue(!TemplateTools.isTemplateTool(new ItemStack(Items.IRON_AXE), true), "hammer mode: the axe steps back");
            for (Holder<Item> hammer : BuiltInRegistries.ITEM.getTagOrEmpty(TemplateTools.HAMMERS)) {
                h.assertTrue(TemplateTools.isTemplateTool(new ItemStack(hammer), true), "hammer mode: " + hammer + " works");
                h.assertTrue(!TemplateTools.isTemplateTool(new ItemStack(hammer), false), "axe mode: " + hammer + " does not");
            }
            h.succeed();
        });
        // Same principle as the crucibles: the axe way lasts exactly as long as no hammer partner switched it off.
        ALL.put("tool_mode_follows_the_partner", h -> {
            boolean sledgehammers = BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplehammers", "iron_sledgehammer"));
            h.assertTrue(TemplateTools.hammerMode() == sledgehammers, "hammer mode " + TemplateTools.hammerMode() + " but SimpleBuilding hammers " + sledgehammers);
            h.assertTrue(TemplateTools.isTemplateTool(tool()) && !TemplateTools.isTemplateTool(wrongTool().orElse(new ItemStack(Items.STICK))),
                    "the current tool works, the other one does not");
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean old = com.simplebuilding.framework.api.SubMods.enabled("simpletrims");
            try {
                com.simplebuilding.framework.api.SubMods.set("simpletrims", false);
                h.assertTrue(!SimpleTrims.on(), "Switch off reaches Simple Trims");
                h.assertTrue(!TemplateTools.isTemplateTool(new ItemStack(Items.IRON_AXE), false), "No template tool while off");
                h.assertTrue(TrimUpgrades.result(new ItemStack(TrimsRegistry.GLOWING_TRIM_TEMPLATE), new ItemStack(Items.IRON_CHESTPLATE),
                        new ItemStack(Items.GLOW_INK_SAC)) == null, "No upgrade while off");
                h.assertTrue(!PlacedTemplates.isPlaceable(new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)), "Nothing placed while off");
                h.assertTrue(TrimsRegistry.tabStacks().isEmpty(), "No tab stacks while off");
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleTrims.id("glowing_trim_template")), "Registrations stay");
            } finally {
                com.simplebuilding.framework.api.SubMods.set("simpletrims", old);
            }
            h.assertTrue(SimpleTrims.on() == old, "Switch restored");
            h.succeed();
        });
        ALL.put("ids", h -> {
            h.assertTrue("simpletrims:x".equals(SimpleTrims.id("x").toString()), "namespace");
            h.succeed();
        });
        ALL.put("registered", h -> {
            h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleTrims.id("trims")), "own creative tab trims is registered");
            for (String path : SimpleTrims.MOVED_ITEMS) {
                Item item = BuiltInRegistries.ITEM.getValue(SimpleTrims.id(path));
                h.assertTrue(item != Items.AIR && item instanceof net.minecraft.world.item.SmithingTemplateItem, path + " is a registered template");
                h.assertTrue(new ItemStack(item).is(TemplateTools.TRIM_TEMPLATES), path + " is in #minecraft:trim_templates");
            }
            for (String path : SimpleTrims.MOVED_COMPONENTS) {
                h.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleTrims.id(path)), "component " + path);
            }
            h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleTrims.id("placed_template")) == TrimsRegistry.PLACED_TEMPLATE, "placed block");
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleTrims.id("placed_template")) == TrimsRegistry.PLACED_TEMPLATE_BE, "block entity");
            h.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(SimpleTrims.id("trim_upgrade"))
                    && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(SimpleTrims.id("tool_shapeless")), "recipe serializers");
            var advancements = h.getLevel().getServer().getAdvancements();
            for (String name : List.of("glow_up", "bright_idea", "full_radiance")) {
                h.assertTrue(advancements.get(SimpleTrims.id(name)) != null, "advancement " + name);
            }
            h.succeed();
        });
        // N18 F1: old SimpleBuilding ids load as Simple Trims ids (SimpleLib IdMoves) - items, components, recipes, advancements.
        ALL.put("migration_from_simplebuilding", h -> {
            var ops = h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            for (String path : SimpleTrims.MOVED_ITEMS) {
                h.assertTrue(BuiltInRegistries.ITEM.getValue(SimpleTrims.old(path)) == BuiltInRegistries.ITEM.getValue(SimpleTrims.id(path)),
                        "simplebuilding:" + path + " resolves to simpletrims:" + path);
                ItemStack loaded = ItemStack.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"simplebuilding:" + path + "\",\"count\":3}"))
                        .getOrThrow(message -> h.assertionException("old stack of " + path + " does not load: " + message));
                h.assertTrue(BuiltInRegistries.ITEM.getKey(loaded.getItem()).equals(SimpleTrims.id(path)) && loaded.getCount() == 3,
                        "old stack loads as " + loaded);
            }
            ItemStack armor = ItemStack.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"minecraft:iron_chestplate\",\"count\":1,\"components\":{"
                            + "\"simplebuilding:glow_level\":2,\"simplebuilding:pulsating\":true,\"simplebuilding:light_source\":true,"
                            + "\"minecraft:custom_data\":{\"SimpleBuildingEmissionLevel\":4}}}"))
                    .getOrThrow(message -> h.assertionException("old upgraded armor does not load: " + message));
            h.assertTrue(TrimGlow.getGlowLevel(armor) == 1, "old Glowing II reads as Glowing: " + TrimGlow.getGlowLevel(armor));
            h.assertTrue(TrimGlow.isPulsating(armor) && Boolean.TRUE.equals(armor.get(TrimsRegistry.LIGHT_SOURCE)), "pulsating and legacy marker kept");
            h.assertTrue(TrimGlow.getEmissionLevel(armor) == 4, "radiance kept: " + TrimGlow.getEmissionLevel(armor));
            JsonObject saved = ItemStack.CODEC.encodeStart(ops, armor).getOrThrow().getAsJsonObject();
            h.assertTrue(saved.toString().contains("simpletrims:glow_level") && !saved.toString().contains("simplebuilding:glow_level"),
                    "the next save writes the new component id: " + saved);
            for (String path : SimpleTrims.MOVED_RECIPES) {
                h.assertTrue(SimpleTrims.id(path).equals(com.simplelib.api.IdMoves.target(Registries.RECIPE, SimpleTrims.old(path))), "recipe move " + path);
                h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleTrims.id(path))).isPresent(),
                        "recipe " + path + " exists");
            }
            for (String[] advancement : SimpleTrims.MOVED_ADVANCEMENTS) {
                h.assertTrue(SimpleTrims.id(advancement[1]).equals(com.simplelib.api.IdMoves.advancement(SimpleTrims.old(advancement[0]))),
                        "advancement move " + advancement[0]);
            }
            h.succeed();
        });
        // The smithing table (no mixin, recipe type simpletrims:trim_upgrade): Glowing once, Radiance up to 5, Pulsating once.
        ALL.put("smithing_upgrades", h -> {
            SmithingMenu table = new SmithingMenu(1, mockPlayer(h).getInventory());
            ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
            ItemStack glowing = smith(table, TrimsRegistry.GLOWING_TRIM_TEMPLATE, armor, Items.GLOW_INK_SAC);
            h.assertTrue(TrimGlow.getGlowLevel(glowing) == 1 && glowing.is(Items.IRON_CHESTPLATE), "glow ink gives Glowing: " + glowing);
            h.assertTrue(smith(table, TrimsRegistry.GLOWING_TRIM_TEMPLATE, glowing, Items.GLOW_INK_SAC).isEmpty(), "Glowing only once");
            ItemStack emitting = armor;
            for (int level = 1; level <= TrimGlow.MAX_EMISSION_LEVEL; level++) {
                emitting = smith(table, TrimsRegistry.EMITTING_TRIM_TEMPLATE, emitting, Items.GLOWSTONE_DUST);
                h.assertTrue(TrimGlow.getEmissionLevel(emitting) == level, "radiance step " + level + ": " + emitting);
            }
            h.assertTrue(smith(table, TrimsRegistry.EMITTING_TRIM_TEMPLATE, emitting, Items.GLOWSTONE_DUST).isEmpty(), "Radiance stops at 5");
            ItemStack pulsing = smith(table, TrimsRegistry.PULSATING_TRIM_TEMPLATE, glowing, Items.ECHO_SHARD);
            h.assertTrue(TrimGlow.isPulsating(pulsing) && TrimGlow.getGlowLevel(pulsing) == 1, "Pulsating combines with Glowing: " + pulsing);
            h.assertTrue(smith(table, TrimsRegistry.PULSATING_TRIM_TEMPLATE, pulsing, Items.ECHO_SHARD).isEmpty(), "Pulsating only once");
            ItemStack inverted = smith(table, TrimsRegistry.INVERTING_TRIM_TEMPLATE, pulsing, Items.INK_SAC);
            h.assertTrue(TrimGlow.getInvertLevel(inverted) == 1 && TrimGlow.isPulsating(inverted) && TrimGlow.getGlowLevel(inverted) == 1, "Inverting combines: " + inverted);
            ItemStack stage2 = smith(table, TrimsRegistry.INVERTING_TRIM_TEMPLATE, inverted, Items.INK_SAC);
            h.assertTrue(TrimGlow.getInvertLevel(stage2) == 2 && TrimGlow.isPulsating(stage2), "Inverting stage 2: " + stage2);
            ItemStack stage3 = smith(table, TrimsRegistry.INVERTING_TRIM_TEMPLATE, stage2, Items.INK_SAC);
            h.assertTrue(TrimGlow.getInvertLevel(stage3) == 3, "Inverting stage 3: " + stage3);
            h.assertTrue(smith(table, TrimsRegistry.INVERTING_TRIM_TEMPLATE, stage3, Items.INK_SAC).isEmpty(), "Inverting stage 3 stays");
            h.assertTrue(smith(table, TrimsRegistry.INVERTING_TRIM_TEMPLATE, armor, Items.GLOW_INK_SAC).isEmpty(), "glow ink does not invert");
            h.assertTrue(smith(table, TrimsRegistry.GLOWING_TRIM_TEMPLATE, armor, Items.GLOWSTONE_DUST).isEmpty(), "wrong material: nothing");
            h.assertTrue(smith(table, TrimsRegistry.GLOWING_TRIM_TEMPLATE, new ItemStack(Items.STICK), Items.GLOW_INK_SAC).isEmpty(), "no armor: nothing");
            h.assertTrue(table.getSlot(SmithingMenu.TEMPLATE_SLOT).mayPlace(new ItemStack(TrimsRegistry.PULSATING_TRIM_TEMPLATE))
                    && table.getSlot(SmithingMenu.ADDITIONAL_SLOT).mayPlace(new ItemStack(Items.ECHO_SHARD)), "the table takes template and material");
            // Vanilla trims stay Vanilla.
            ItemStack trimmed = smith(table, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, armor, Items.GOLD_INGOT);
            h.assertTrue(trimmed.has(net.minecraft.core.component.DataComponents.TRIM), "Vanilla trim still works: " + trimmed);
            h.succeed();
        });
        // Pulsating template at the crafting table: the tool of the current mode stays and loses 1 durability.
        ALL.put("pulsating_recipe_keeps_the_tool", h -> {
            ItemStack tool = tool();
            tool.setDamageValue(3);
            CraftingInput input = pulsatingGrid(tool);
            Optional<RecipeHolder<CraftingRecipe>> match = craft(h, input);
            h.assertTrue(match.isPresent(), "tool + 2 echo shards + 2 sculk + 4 diamonds craft nothing with " + tool);
            ItemStack out = match.get().value().assemble(input);
            h.assertTrue(out.is(TrimsRegistry.PULSATING_TRIM_TEMPLATE) && out.getCount() == 1, "crafts " + out);
            ItemStack left = match.get().value().getRemainingItems(input).get(0);
            h.assertTrue(left.is(tool.getItem()) && left.getDamageValue() == 3 + ToolShapelessRecipe.TOOL_DAMAGE, "the tool came back as " + left);
            ItemStack worn = tool();
            worn.setDamageValue(worn.getMaxDamage() - 1);
            h.assertTrue(match.get().value().getRemainingItems(pulsatingGrid(worn)).get(0).isEmpty(), "a worn-out tool breaks");
            wrongTool().ifPresent(wrong -> h.assertTrue(craft(h, pulsatingGrid(wrong)).isEmpty(), "the other tool does not craft: " + wrong));
            h.succeed();
        });
        // Inverting upgrade as pure maths: pattern cut out, rest filled; stage 2 adds a 1 px rim, stage 3 a 2 px rim + 1 px transition.
        ALL.put("trim_inversion", h -> {
            int w = 8, hgt = 3;
            int[] pattern = new int[w * hgt];
            pattern[1 * w + 1] = 0xFFFFFFFF;
            pattern[1 * w + 2] = 0xFFFFFFFF;
            int fill = 0xFF112233, rim = 0xFF000011;
            int[] hard = com.simpletrims.TrimInversion.invert(pattern, w, hgt, fill, rim, com.simpletrims.TrimInversion.Mode.HARD);
            h.assertTrue(hard[1 * w + 1] == 0 && hard[1 * w + 2] == 0 && hard[0] == fill && hard[1 * w] == fill, "hard: cut-out + flat fill");
            int[] withRim = com.simpletrims.TrimInversion.invert(pattern, w, hgt, fill, rim, com.simpletrims.TrimInversion.Mode.RIM);
            h.assertTrue(withRim[1 * w] == rim && withRim[1 * w + 3] == rim && withRim[2 * w + 1] == rim && withRim[0] == fill && withRim[1 * w + 4] == fill, "rim: 1 px border around the cut-out only");
            int[] soft = com.simpletrims.TrimInversion.invert(pattern, w, hgt, fill, rim, com.simpletrims.TrimInversion.Mode.SOFT);
            int mid = soft[1 * w + 5];
            h.assertTrue(soft[1 * w + 3] == rim && soft[1 * w + 4] == rim && mid != rim && mid != fill && soft[1 * w + 6] == fill && soft[1 * w + 1] == 0, "soft: 2 px rim, 1 px transition, then fill");
            h.assertTrue(com.simpletrims.TrimInversion.mode(1) == com.simpletrims.TrimInversion.Mode.HARD && com.simpletrims.TrimInversion.mode(2) == com.simpletrims.TrimInversion.Mode.RIM
                    && com.simpletrims.TrimInversion.mode(3) == com.simpletrims.TrimInversion.Mode.SOFT, "stage -> mode");
            int[] twice = com.simpletrims.TrimInversion.invert(hard, w, hgt, fill, rim, com.simpletrims.TrimInversion.Mode.HARD);
            h.assertTrue(twice[1 * w + 1] == fill && twice[0] == 0, "inverting twice swaps back the mask");
            int[] colours = com.simpletrims.TrimInversion.colours(new int[] {0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF404040, 0xFF202020}, pattern);
            h.assertTrue(colours[0] == 0xFF808080 && colours[1] == 0xFF202020, "fill = third-brightest, rim = darkest");
            h.succeed();
        });
        ALL.put("duplication_recipes", h -> {
            for (Item[] pair : new Item[][] {{TrimsRegistry.GLOWING_TRIM_TEMPLATE, Items.GLOWSTONE},
                    {TrimsRegistry.EMITTING_TRIM_TEMPLATE, Items.MAGMA_BLOCK}, {TrimsRegistry.PULSATING_TRIM_TEMPLATE, Items.SCULK},
                    {TrimsRegistry.INVERTING_TRIM_TEMPLATE, Items.OBSIDIAN}}) {
                List<ItemStack> grid = new ArrayList<>();
                for (int i = 0; i < 9; i++) grid.add(new ItemStack(Items.DIAMOND));
                grid.set(1, new ItemStack(pair[0]));
                grid.set(4, new ItemStack(pair[1]));
                CraftingInput input = CraftingInput.of(3, 3, grid);
                Optional<RecipeHolder<CraftingRecipe>> match = craft(h, input);
                h.assertTrue(match.isPresent(), "no copy recipe for " + pair[0]);
                ItemStack out = match.get().value().assemble(input);
                h.assertTrue(out.is(pair[0]) && out.getCount() == 2, "copy of " + pair[0] + " gives " + out);
            }
            h.succeed();
        });
        // Sneak + use lays a trim template flat (floor, wall, ceiling); breaking gives exactly the stack back.
        ALL.put("placing_trim_templates", h -> {
            ServerPlayer player = mockPlayer(h);
            player.setShiftKeyDown(true);
            h.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
            ItemStack named = new ItemStack(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
            named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Note"));
            InteractionResult floor = use(h, player, named.copy(), new BlockPos(1, 1, 1), Direction.UP);
            h.assertTrue(floor.consumesAction(), "sneak + use did not place: " + floor);
            BlockState state = h.getBlockState(new BlockPos(1, 2, 1));
            h.assertTrue(state.is(TrimsRegistry.PLACED_TEMPLATE) && state.getValue(PlacedTrimTemplateBlock.FACE) == AttachFace.FLOOR, "floor: " + state);
            h.assertTrue(ItemStack.isSameItemSameComponents(PlacedTemplates.templateAt(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1))), named),
                    "the placed template keeps its stack");
            h.setBlock(new BlockPos(4, 2, 2), Blocks.STONE);
            use(h, player, new ItemStack(TrimsRegistry.GLOWING_TRIM_TEMPLATE), new BlockPos(4, 2, 2), Direction.NORTH);
            BlockState wall = h.getBlockState(new BlockPos(4, 2, 1));
            h.assertTrue(wall.is(TrimsRegistry.PLACED_TEMPLATE) && wall.getValue(PlacedTrimTemplateBlock.FACE) == AttachFace.WALL, "wall: " + wall);
            h.setBlock(new BlockPos(6, 4, 1), Blocks.STONE);
            use(h, player, new ItemStack(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(6, 4, 1), Direction.DOWN);
            h.assertTrue(h.getBlockState(new BlockPos(6, 3, 1)).getValue(PlacedTrimTemplateBlock.FACE) == AttachFace.CEILING, "ceiling");
            // Not ours: the netherite upgrade is no trim template (SimpleBuilding places it, if loaded).
            h.setBlock(new BlockPos(1, 1, 5), Blocks.STONE);
            use(h, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(1, 1, 5), Direction.UP);
            h.assertTrue(!h.getBlockState(new BlockPos(1, 2, 5)).is(TrimsRegistry.PLACED_TEMPLATE), "netherite upgrade is not placed by Simple Trims");
            // Without sneaking nothing is placed.
            player.setShiftKeyDown(false);
            h.setBlock(new BlockPos(3, 1, 5), Blocks.STONE);
            use(h, player, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(3, 1, 5), Direction.UP);
            h.assertTrue(h.getBlockState(new BlockPos(3, 2, 5)).isAir(), "without sneaking nothing lies down");
            // Breaking returns the stack with its name.
            List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)),
                    h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1, 2, 1))));
            h.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.get(0), named), "drops " + drops);
            h.succeed();
        });
        // Upgrade in the world: tool of the current mode + glow ink (glowstone dust), five strikes, materials paid at the end.
        ALL.put("in_world_upgrade", h -> {
            for (Item catalyst : List.of(Items.GLOW_INK_SAC, Items.GLOWSTONE_DUST, Items.INK_SAC)) {
                BlockPos rel = catalyst == Items.GLOW_INK_SAC ? new BlockPos(2, 2, 2) : catalyst == Items.GLOWSTONE_DUST ? new BlockPos(5, 2, 2) : new BlockPos(5, 2, 5);
                BlockPos pos = placed(h, rel, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                ServerPlayer player = mockPlayer(h);
                ItemStack tool = tool();
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(catalyst, 3));
                h.assertTrue(!PlacedTemplates.hit(h.getLevel(), pos, player), "a strike counted without diamonds");
                player.getInventory().add(new ItemStack(Items.DIAMOND, 4));
                player.getInventory().add(new ItemStack(PlacedTemplates.extraMaterials(catalyst).keySet().stream().skip(1).findFirst().orElseThrow(), 2));
                for (int i = 1; i <= PlacedTemplates.PLACED_HITS; i++) {
                    h.assertTrue(PlacedTemplates.hit(h.getLevel(), pos, player), "strike " + i + " did not count");
                }
                ItemStack result = PlacedTemplates.templateAt(h.getLevel(), pos);
                Item expected = PlacedTemplates.upgrades().get(catalyst);
                h.assertTrue(PlacedTemplates.upgrades().size() == 3 && expected != null, "upgrade table: " + PlacedTemplates.upgrades());
                h.assertTrue(result.is(expected), "five strikes with " + catalyst + " made " + result);
                h.assertTrue(player.getOffhandItem().getCount() == 3 - PlacedTemplates.CATALYST_COST, "catalyst left: " + player.getOffhandItem());
                h.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 0, "diamonds were not paid");
                h.assertTrue(tool.getDamageValue() == PlacedTemplates.PLACED_HITS * PlacedTemplates.TOOL_DAMAGE, "tool damage " + tool.getDamageValue());
                h.assertTrue(!PlacedTemplates.isUpgradable(h.getLevel(), pos), "an upgraded template cannot be upgraded again");
            }
            // The wrong tool does nothing.
            BlockPos pos = placed(h, new BlockPos(2, 2, 5), Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
            ServerPlayer player = mockPlayer(h);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC, 2));
            h.assertTrue(!PlacedTemplates.isToolTarget(h.getLevel(), pos, player) && !PlacedTemplates.hit(h.getLevel(), pos, player), "a pickaxe strikes");
            // Right click through the game mode: the block takes the click, one strike per click interval.
            player.setItemInHand(InteractionHand.MAIN_HAND, tool());
            player.getInventory().add(new ItemStack(Items.DIAMOND, 4));
            player.getInventory().add(new ItemStack(Items.GLOWSTONE, 2));
            BlockHitResult target = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, target);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, target);
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof PlacedTrimTemplateBlockEntity be && be.hits() == 1,
                    "a right click with the tool is one strike, its repeat in the same tick none");
            h.succeed();
        });
        // Hand tilt / target highlight (N58): the tool stance aimed at an upgradable plate is a framework hand hint.
        ALL.put("plate_hint", h -> {
            h.assertTrue(com.simplebuilding.framework.api.TransformHints.ids().contains(com.simpletrims.placed.PlateHint.ID), "plate hint not registered");
            BlockPos pos = placed(h, new BlockPos(2, 2, 2), Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
            ServerPlayer player = mockPlayer(h);
            BlockHitResult target = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool());
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC, 2));
            player.getInventory().add(new ItemStack(Items.DIAMOND, 4));
            player.getInventory().add(new ItemStack(Items.GLOWSTONE, 2));
            var query = new com.simplebuilding.framework.api.TransformHints.Query(h.getLevel(), player, target, true);
            h.assertTrue(com.simpletrims.placed.PlateHint.aimedAtStrikePlate(player, target) && com.simplebuilding.framework.api.TransformHints.any(query),
                    "tool + glow ink at an upgradable plate gives no hint");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            h.assertTrue(!com.simpletrims.placed.PlateHint.aimedAtStrikePlate(player, target), "hint without a material");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(!com.simpletrims.placed.PlateHint.aimedAtStrikePlate(player, target), "hint for a pickaxe");
            h.succeed();
        });
        // Item display 3D (owner D1/D7): trim templates lie as 2 px models, the hammer switches one to the flat plate and back.
        ALL.put("placed_templates_in3d", h -> {
            for (String id : com.simpletrims.placed.PlacedSolids.IDS) {
                String path = id.substring(id.indexOf(':') + 1);
                for (String res : List.of("/assets/simpletrims/items/placed_" + path + ".json", "/assets/simpletrims/models/block/placed_" + path + ".json")) {
                    h.assertTrue(TrimsTests.class.getResource(res) != null, "missing " + res);
                }
            }
            h.assertTrue(com.simpletrims.placed.PlacedSolids.has(new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE))
                    && com.simpletrims.placed.PlacedSolids.has(new ItemStack(TrimsRegistry.GLOWING_TRIM_TEMPLATE)), "trim templates have no 3D model");
            h.assertTrue(!com.simpletrims.placed.PlacedSolids.has(new ItemStack(Items.PAPER)), "paper has a template model");
            BlockPos pos = placed(h, new BlockPos(2, 2, 2), Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
            var item = Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
            double solid = com.simpletrims.placed.PlacedPlate.shape(item, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR, Direction.NORTH, false).max(Direction.Axis.Y);
            double flat = com.simpletrims.placed.PlacedPlate.shape(item, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR, Direction.NORTH, true).max(Direction.Axis.Y);
            double gap = com.simpletrims.placed.PlacedPlate.GAP;
            h.assertTrue(Math.abs(solid - gap - 2.0 / 16.0) < 1.0E-3, "the 3D template is " + (solid - gap) * 16 + " px thick instead of 2");
            h.assertTrue(flat < solid && Math.abs(flat - gap - 1.4 / 16.0) < 2.0E-3, "the flat plate is " + (flat - gap) * 16 + " px thick");
            h.assertTrue(!h.getBlockState(new BlockPos(2, 2, 2)).getValue(PlacedTrimTemplateBlock.FLAT), "a new template lies flat");
            Item hammer = null;
            for (Item candidate : BuiltInRegistries.ITEM) {
                if (new ItemStack(candidate).is(TemplateTools.HAMMERS)) {
                    hammer = candidate;
                    break;
                }
            }
            if (hammer != null) {
                ServerPlayer player = mockPlayer(h);
                ItemStack tool = new ItemStack(hammer);
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                h.assertTrue(PlacedTemplates.toggleFlat(h.getLevel(), pos, player, tool, InteractionHand.MAIN_HAND), "the hammer did not switch");
                h.assertTrue(h.getLevel().getBlockState(pos).getValue(PlacedTrimTemplateBlock.FLAT), "3D -> flat did not switch");
                h.assertTrue(tool.getDamageValue() == 1, "hammer damage " + tool.getDamageValue());
                h.assertTrue(PlacedTemplates.toggleFlat(h.getLevel(), pos, player, tool, InteractionHand.MAIN_HAND)
                        && !h.getLevel().getBlockState(pos).getValue(PlacedTrimTemplateBlock.FLAT), "flat -> 3D did not switch back");
                h.assertTrue(!PlacedTemplates.toggleFlat(h.getLevel(), pos, player, new ItemStack(Items.IRON_PICKAXE), InteractionHand.MAIN_HAND), "a pickaxe switched");
            }
            h.succeed();
        });
        // Radiance: worn emitting armor lights the block over the head; armor stands store and clean up their light.
        ALL.put("radiance_light", h -> {
            ServerPlayer player = mockPlayer(h);
            Vec3 at = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
            for (int i = 0; i < 3; i++) TrimGlow.incrementEmissionLevel(chest);
            ItemStack helmet = new ItemStack(Items.IRON_HELMET);
            for (int i = 0; i < 2; i++) TrimGlow.incrementEmissionLevel(helmet);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            BlockPos head = player.blockPosition().above();
            try {
                TrimLight.tickPlayer(player);
                BlockState light = h.getLevel().getBlockState(head);
                h.assertTrue(light.is(Blocks.LIGHT) && light.getValue(LightBlock.LEVEL) == 15, "radiance 3 + 2 gives light 15, got " + light);
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).getValue(LightBlock.LEVEL) == 9, "radiance 3 gives light 9");
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).isAir(), "the light goes out with the armor");
            } finally {
                TrimLight.forget(player);
            }
            ArmorStand stand = h.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(5, 2, 5));
            ItemStack boots = new ItemStack(Items.IRON_BOOTS);
            TrimGlow.incrementEmissionLevel(boots);
            stand.setItemSlot(EquipmentSlot.FEET, boots);
            BlockPos standHead = stand.blockPosition().above();
            TrimLight.tickHolder(h.getLevel(), (OwnedLightHolder) stand, standHead, TrimLight.wornEmission(stand));
            h.assertTrue(h.getLevel().getBlockState(standHead).is(Blocks.LIGHT) && standHead.equals(((OwnedLightHolder) stand).simpletrims$getOwnedLight()),
                    "the armor stand lights and remembers its block");
            stand.kill(h.getLevel());
            h.assertTrue(h.getLevel().getBlockState(standHead).isAir(), "a destroyed stand takes its light along");
            h.assertTrue(TrimLight.lightLevelFor(5) == 15 && TrimLight.lightLevelFor(1) == 3 && TrimLight.lightLevelFor(9) == 15, "3 light per level, max 15");
            h.succeed();
        });
        // Review F-04: a foreign minecraft:light block (map maker, adventure) is neither taken over nor removed.
        ALL.put("foreign_light_untouched", h -> {
            ServerPlayer player = mockPlayer(h);
            Vec3 at = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
            for (int i = 0; i < 3; i++) TrimGlow.incrementEmissionLevel(chest);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            BlockPos head = player.blockPosition().above();
            BlockState foreign = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 4);
            h.getLevel().setBlock(head, foreign, 3);
            try {
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).equals(foreign), "a foreign light block was overwritten: " + h.getLevel().getBlockState(head));
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                TrimLight.tickPlayer(player);
                TrimLight.forget(player);
                h.assertTrue(h.getLevel().getBlockState(head).equals(foreign), "a foreign light block was removed: " + h.getLevel().getBlockState(head));
            } finally {
                TrimLight.forget(player);
                h.getLevel().setBlock(head, Blocks.AIR.defaultBlockState(), 3);
            }
            h.succeed();
        });
        // REV24-M-5a: after a crash the carrier table is gone; the marked light block is swept with its chunk, a foreign Vanilla light stays.
        ALL.put("orphan_light_swept", h -> {
            ServerPlayer player = mockPlayer(h);
            Vec3 at = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
            for (int i = 0; i < 3; i++) TrimGlow.incrementEmissionLevel(chest);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            BlockPos head = player.blockPosition().above();
            BlockPos foreign = head.offset(2, 0, 0);
            h.getLevel().setBlock(foreign, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 7), 3);
            try {
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).is(Blocks.LIGHT) && com.simplelib.api.OwnedLights.isMarked(h.getLevel(), head), "the light stands and is marked");
                h.assertTrue(!com.simplelib.api.OwnedLights.isMarked(h.getLevel(), foreign), "a foreign light is not marked");
                h.assertTrue(com.simplelib.api.OwnedLights.sweep(h.getLevel(), net.minecraft.world.level.ChunkPos.containing(head)) == 0 && h.getLevel().getBlockState(head).is(Blocks.LIGHT),
                        "a live carrier keeps its light through a sweep");
                TrimLight.forgetAll(); // crash: the table is gone, the block stays
                h.assertTrue(com.simplelib.api.OwnedLights.sweep(h.getLevel(), net.minecraft.world.level.ChunkPos.containing(head)) == 1, "the orphan is swept");
                h.assertTrue(h.getLevel().getBlockState(head).isAir() && !com.simplelib.api.OwnedLights.isMarked(h.getLevel(), head), "orphan light gone, mark gone");
                h.assertTrue(h.getLevel().getBlockState(foreign).is(Blocks.LIGHT) && h.getLevel().getBlockState(foreign).getValue(LightBlock.LEVEL) == 7, "a foreign minecraft:light stays");
            } finally {
                TrimLight.forget(player);
                h.getLevel().setBlock(foreign, Blocks.AIR.defaultBlockState(), 3);
            }
            h.succeed();
        });
        // N52: torches, lanterns and lava buckets in either hand shine like the block; radiance and held light never add.
        ALL.put("held_light", h -> {
            ServerPlayer player = mockPlayer(h);
            Vec3 at = h.absoluteVec(new Vec3(2.5, 2.0, 2.5));
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            BlockPos head = player.blockPosition().above();
            Object[][] cases = {{Items.TORCH, 14}, {Items.SOUL_TORCH, 10}, {Items.REDSTONE_TORCH, 7}, {Items.LANTERN, 15},
                    {Items.SOUL_LANTERN, 10}, {Items.LAVA_BUCKET, 15}, {Items.COPPER_TORCH, 14}, {Items.STICK, 0}, {Items.GLOWSTONE, 0}};
            try {
                for (Object[] c : cases) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack((net.minecraft.world.item.Item) c[0]));
                    TrimLight.tickPlayer(player);
                    BlockState st = h.getLevel().getBlockState(head);
                    int got = st.is(Blocks.LIGHT) ? st.getValue(LightBlock.LEVEL) : 0;
                    h.assertTrue(got == (int) c[1], c[0] + " in the main hand gives light " + c[1] + ", got " + got);
                }
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).getValue(LightBlock.LEVEL) == 14, "the off hand shines too");
                ItemStack boots = new ItemStack(Items.IRON_BOOTS);
                for (int i = 0; i < 2; i++) TrimGlow.incrementEmissionLevel(boots);
                player.setItemSlot(EquipmentSlot.FEET, boots);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.REDSTONE_TORCH));
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).getValue(LightBlock.LEVEL) == 7, "the brighter of radiance 6 and torch 7 wins, not the sum");
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                TrimLight.tickPlayer(player);
                h.assertTrue(h.getLevel().getBlockState(head).isAir(), "the light goes out with the torch");
            } finally {
                TrimLight.forget(player);
            }
            h.succeed();
        });
        ALL.put("guide_book", com.simpletrims.guide.TrimsGuide::gameTest);
        ALL.put("glow_rules", h -> {
            int environment = TrimGlow.packLight(4, 9);
            h.assertTrue(TrimGlow.trimLight(environment, 0, true, 0) == environment, "without Glowing the light stays");
            h.assertTrue(TrimGlow.trimLight(environment, 1, false, 0) == TrimGlow.packLight(15, 15), "Glowing: full light");
            h.assertTrue(TrimGlow.pulseLight(0) == 15 && TrimGlow.pulseLight(TrimGlow.PULSE_PERIOD_MS / 2) == 1, "pulse 15 -> 1");
            h.assertTrue(TrimGlow.desaturationStep(0) == 0 && TrimGlow.desaturationStep(TrimGlow.PULSE_PERIOD_MS / 2) == TrimGlow.DESATURATION_STEPS - 1,
                    "Pulsating alone: full colour -> gray");
            int gray = TrimGlow.desaturate(0xFFFF0000, 1.0F);
            h.assertTrue(((gray >> 16) & 255) == ((gray >> 8) & 255) && ((gray >> 8) & 255) == (gray & 255), "fully desaturated is gray");
            h.assertTrue(TrimGlow.normalizeGlowLevel(2) == 1, "Glowing II reads as Glowing");
            h.succeed();
        });
    }

    private TrimsTests() {}

    /** The tool of the current mode: an iron axe alone, the first template hammer with a hammer partner. */
    static ItemStack tool() {
        if (TemplateTools.hammerMode()) {
            for (Holder<Item> hammer : BuiltInRegistries.ITEM.getTagOrEmpty(TemplateTools.HAMMERS)) return new ItemStack(hammer);
        }
        return new ItemStack(Items.IRON_AXE);
    }

    /** The tool of the other mode, if it exists here. */
    static Optional<ItemStack> wrongTool() {
        if (TemplateTools.hammerMode()) return Optional.of(new ItemStack(Items.IRON_AXE));
        for (Holder<Item> hammer : BuiltInRegistries.ITEM.getTagOrEmpty(TemplateTools.HAMMERS)) return Optional.of(new ItemStack(hammer));
        return Optional.empty();
    }

    private static ServerPlayer mockPlayer(GameTestHelper h) {
        Player raw = h.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) throw h.assertionException("makeMockServerPlayer returned " + raw.getClass().getName());
        Vec3 pos = h.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static InteractionResult use(GameTestHelper h, Player player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    private static BlockPos placed(GameTestHelper h, BlockPos rel, Item template) {
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, TrimsRegistry.PLACED_TEMPLATE.defaultBlockState());
        BlockPos pos = h.absolutePos(rel);
        if (!(h.getLevel().getBlockEntity(pos) instanceof PlacedTrimTemplateBlockEntity be)) throw h.assertionException("no placed template entity");
        be.setTemplate(new ItemStack(template));
        return pos;
    }

    private static ItemStack smith(SmithingMenu table, Item template, ItemStack base, Item material) {
        var inputs = table.getSlot(SmithingMenu.TEMPLATE_SLOT).container;
        table.getSlot(SmithingMenu.TEMPLATE_SLOT).set(new ItemStack(template));
        table.getSlot(SmithingMenu.BASE_SLOT).set(base.copy());
        table.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(material));
        table.slotsChanged(inputs);
        return table.getSlot(table.getResultSlot()).getItem().copy();
    }

    private static CraftingInput pulsatingGrid(ItemStack tool) {
        List<ItemStack> grid = new ArrayList<>(List.of(tool, new ItemStack(Items.ECHO_SHARD), new ItemStack(Items.ECHO_SHARD),
                new ItemStack(Items.SCULK), new ItemStack(Items.SCULK)));
        for (int i = 0; i < 4; i++) grid.add(new ItemStack(Items.DIAMOND));
        return CraftingInput.of(3, 3, grid);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> craft(GameTestHelper h, CraftingInput input) {
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
    }
}

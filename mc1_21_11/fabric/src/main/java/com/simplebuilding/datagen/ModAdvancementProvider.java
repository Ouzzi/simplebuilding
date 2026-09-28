package com.simplebuilding.datagen;

import com.simplebuilding.advancement.ModTriggers;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The SimpleBuilding advancement tree: a tab of its own ({@code simplebuilding:root}) with one branch
 * per theme, plus two teasers hung under vanilla tabs ("Getting an Upgrade" and "Nether"), which point
 * at the tab. Every description says what to do and how - the visible children of a finished
 * advancement are the player's clue for the next step, exactly like vanilla's Story tab.
 *
 * <p>Criteria: vanilla {@code inventory_changed} for "have this item", and the mod's own
 * {@code simplebuilding:feature_used} ({@link ModTriggers}) for actions no vanilla trigger sees.
 * Titles and descriptions: {@code advancements.simplebuilding.<path with dots>.title|description}
 * in both lang files. Nothing is hidden - the only secrets are the easter advancements
 * ({@code tweaks.datagen.EasterEggData}), which stay separate.
 *
 * <p>1.21.11 copy of the 26.x provider in src/main/java: same tree, same ids and lang keys; only
 * the imports and the icon type (ItemStack instead of ItemStackTemplate) differ.
 */
public class ModAdvancementProvider extends FabricAdvancementProvider {

    public static final String NS = "simplebuilding";

    public ModAdvancementProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> out) {
        new Tree(registries.lookupOrThrow(Registries.ITEM), out).build();
    }

    /** Builds the tree; one method per branch, in the order the tab shows them. */
    private static final class Tree {
        private final HolderGetter<Item> items;
        private final Consumer<AdvancementHolder> out;

        Tree(HolderGetter<Item> items, Consumer<AdvancementHolder> out) {
            this.items = items;
            this.out = out;
        }

        void build() {
            AdvancementHolder root = save(Advancement.Builder.advancement()
                    .display(new DisplayInfo(new ItemStack(ModItems.IRON_BUILDING_WAND),
                            title("root"), description("root"),
                            Optional.of(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone"))),
                            AdvancementType.TASK, false, false, false))
                    .addCriterion("crafting_table", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CRAFTING_TABLE)), "root");

            // Teasers in the vanilla tabs.
            node("story/hammer_time", Identifier.withDefaultNamespace("story/upgrade_tools"), ModItems.STONE_SLEDGEHAMMER,
                    AdvancementType.TASK, "sledgehammer", any(ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER,
                            ModItems.IRON_SLEDGEHAMMER, ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER,
                            ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_SLEDGEHAMMER));
            node("nether/nugget_of_wisdom", Identifier.withDefaultNamespace("nether/root"), ModItems.NETHERITE_NUGGET,
                    AdvancementType.TASK, "netherite_nugget", any(ModItems.NETHERITE_NUGGET));

            hammerAndMachines(root);
            chisel(root);
            node("tiers/step_by_step", root, ModItems.BASIC_UPGRADE_TEMPLATE, AdvancementType.TASK,
                    "basic_upgrade_template", any(ModItems.BASIC_UPGRADE_TEMPLATE));
            AdvancementHolder cores = wandsAndBlueprints(root);
            gadgets(cores);
            storage(root);
            endAndEnderite(root);
            pads(root);
        }

        private void hammerAndMachines(AdvancementHolder root) {
            AdvancementHolder stairs = feature("hammer/stair_master", root, ModItems.STONE_SLEDGEHAMMER, AdvancementType.TASK, ModTriggers.HAMMER_RESHAPE);
            AdvancementHolder pebbles = feature("hammer/pebble_dash", stairs, ModItems.DIAMOND_PEBBLE, AdvancementType.TASK, ModTriggers.DIAMOND_CRUSH);
            AdvancementHolder cracked = node("hammer/cracked_up", pebbles, ModItems.CRACKED_DIAMOND, AdvancementType.TASK,
                    "cracked_diamond", any(ModItems.CRACKED_DIAMOND));
            feature("hammer/glow_up", stairs, ModItems.GLOWING_TRIM_TEMPLATE, AdvancementType.TASK, ModTriggers.TRIM_TEMPLATE_FORGED);
            node("hammer/heavy_metal", stairs, ModItems.NETHERITE_SLEDGEHAMMER, AdvancementType.GOAL,
                    "netherite_sledgehammer", any(ModItems.NETHERITE_SLEDGEHAMMER));

            AdvancementHolder reinforced = node("machines/reinforcements", cracked, ModItems.REINFORCED_FURNACE, AdvancementType.TASK,
                    "reinforced_machine", any(ModItems.REINFORCED_FURNACE, ModItems.REINFORCED_SMOKER, ModItems.REINFORCED_BLAST_FURNACE,
                            ModItems.REINFORCED_HOPPER, ModItems.REINFORCED_PISTON, ModItems.REINFORCED_STICKY_PISTON));
            AdvancementHolder netherite = feature("machines/forged_in_place", reinforced, ModItems.NETHERITE_FURNACE, AdvancementType.GOAL,
                    ModTriggers.HAMMER_UPGRADE_NETHERITE);
            feature("machines/end_of_the_line", netherite, ModItems.ENDERITE_FURNACE, AdvancementType.CHALLENGE,
                    ModTriggers.HAMMER_UPGRADE_ENDERITE);
        }

        private void chisel(AdvancementHolder root) {
            AdvancementHolder chip = feature("chisel/chip_off_the_old_block", root, ModItems.STONE_CHISEL, AdvancementType.TASK, ModTriggers.CHISEL);
            node("chisel/fine_detail", chip, ModItems.DIAMOND_CHISEL, AdvancementType.TASK, "better_chisel",
                    any(ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL));
        }

        /** Returns "Core Values", the parent of the gadget branch. */
        private AdvancementHolder wandsAndBlueprints(AdvancementHolder root) {
            AdvancementHolder cores = node("wand/core_values", root, ModItems.COPPER_CORE, AdvancementType.TASK, "core",
                    any(ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE, ModItems.DIAMOND_CORE,
                            ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE));
            AdvancementHolder oneClick = feature("wand/one_click_wonder", cores, ModItems.COPPER_BUILDING_WAND, AdvancementType.TASK, ModTriggers.WAND_BUILD);
            node("wand/wand_erful", oneClick, ModItems.NETHERITE_BUILDING_WAND, AdvancementType.GOAL, "netherite_wand",
                    any(ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND));

            AdvancementHolder measure = feature("octant/measure_twice", root, ModItems.OCTANT, AdvancementType.TASK, ModTriggers.OCTANT_MARK);
            AdvancementHolder scan = feature("blueprint/copy_that", measure, ModItems.BLUEPRINT, AdvancementType.TASK, ModTriggers.BLUEPRINT_SCAN);
            feature("blueprint/carbon_copy", scan, Items.CARTOGRAPHY_TABLE, AdvancementType.TASK, ModTriggers.BLUEPRINT_COPY);
            feature("blueprint/instant_architect", scan, ModItems.DIAMOND_BUILDING_WAND, AdvancementType.GOAL, ModTriggers.BLUEPRINT_BUILD);
            return cores;
        }

        private void gadgets(AdvancementHolder cores) {
            node("gadgets/attractive_personality", cores, ModItems.MAGNET, AdvancementType.TASK, "magnet", any(ModItems.MAGNET));
            feature("gadgets/spin_doctor", cores, ModItems.ROTATOR, AdvancementType.TASK, ModTriggers.ROTATE);
            feature("gadgets/burning_focus", cores, TweaksItems.LASER_POINTER, AdvancementType.TASK, ModTriggers.LENS_BEAM);
            feature("gadgets/ping", cores, ModItems.ORE_DETECTOR, AdvancementType.TASK, ModTriggers.ORE_DETECTED);
            node("gadgets/speed_reader", cores, ModItems.VELOCITY_GAUGE, AdvancementType.TASK, "velocity_gauge", any(ModItems.VELOCITY_GAUGE));
        }

        private void storage(AdvancementHolder root) {
            AdvancementHolder sheet = node("storage/sheet_happens", root, ModItems.LEATHER_SHEET, AdvancementType.TASK, "leather_sheet",
                    any(ModItems.LEATHER_SHEET));
            AdvancementHolder backpack = node("storage/pack_mule", sheet, ModItems.BACKPACK, AdvancementType.TASK, "backpack",
                    any(ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK));
            node("storage/heavy_luggage", backpack, ModItems.NETHERITE_BACKPACK, AdvancementType.GOAL, "netherite_backpack",
                    any(ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK));
            node("storage/bundle_of_joy", sheet, ModItems.REINFORCED_BUNDLE, AdvancementType.TASK, "reinforced_bundle",
                    any(ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE));
            node("storage/quiver_in_fear", sheet, ModItems.QUIVER, AdvancementType.TASK, "quiver",
                    any(ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_QUIVER));
        }

        private void endAndEnderite(AdvancementHolder root) {
            AdvancementHolder dust = node("end/stardust", root, ModItems.ASTRALIT_DUST, AdvancementType.TASK, "end_mineral",
                    any(ModItems.ASTRALIT_DUST, ModItems.NIHILITH_SHARD));
            node("end/quartz_fusion", dust, ModItems.ENDER_QUARTZ, AdvancementType.TASK, "ender_quartz", any(ModItems.ENDER_QUARTZ));
            node("end/palette_cleanser", dust, ModItems.ASTRALIT_BRICKS, AdvancementType.TASK, "end_bricks",
                    any(ModItems.ASTRALIT_BRICKS, ModItems.NIHILITH_BRICKS, ModItems.ENDER_QUARTZ_BRICKS));
            node("end/defying_gravity", dust, ModItems.LEVITATING_SAND, AdvancementType.TASK, "gravity_block",
                    any(ModItems.SUSPENDED_SAND, ModItems.SUSPENDED_GRAVEL, ModItems.LEVITATING_SAND, ModItems.LEVITATING_GRAVEL));

            AdvancementHolder raw = node("enderite/raw_deal", dust, ModItems.RAW_ENDERITE, AdvancementType.TASK, "raw_enderite",
                    any(ModItems.RAW_ENDERITE));
            AdvancementHolder scrap = node("enderite/patience_is_a_virtue", raw, ModItems.ENDERITE_SCRAP, AdvancementType.TASK,
                    "enderite_scrap", any(ModItems.ENDERITE_SCRAP));
            AdvancementHolder ingot = node("enderite/beyond_netherite", scrap, ModItems.ENDERITE_INGOT, AdvancementType.GOAL,
                    "enderite_ingot", any(ModItems.ENDERITE_INGOT));
            AdvancementHolder nugget = node("enderite/pocket_change", ingot, ModItems.ENDERITE_NUGGET, AdvancementType.TASK,
                    "enderite_nugget", any(ModItems.ENDERITE_NUGGET));
            feature("enderite/echolocation", nugget, TweaksItems.ECHO_COMPASS, AdvancementType.GOAL, ModTriggers.ECHO_TELEPORT);

            AdvancementHolder template = node("enderite/template_of_the_end", ingot, ModItems.ENDERITE_UPGRADE_TEMPLATE,
                    AdvancementType.TASK, "enderite_upgrade_template", any(ModItems.ENDERITE_UPGRADE_TEMPLATE));
            AdvancementHolder armour = node("enderite/cover_me_in_enderite", template, ModItems.ENDERITE_CHESTPLATE, AdvancementType.GOAL,
                    "enderite_armor", any(ModItems.ENDERITE_HELMET, ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_LEGGINGS,
                            ModItems.ENDERITE_BOOTS));
            Map<String, Criterion<?>> fullSet = new LinkedHashMap<>();
            fullSet.put("enderite_helmet", any(ModItems.ENDERITE_HELMET));
            fullSet.put("enderite_chestplate", any(ModItems.ENDERITE_CHESTPLATE));
            fullSet.put("enderite_leggings", any(ModItems.ENDERITE_LEGGINGS));
            fullSet.put("enderite_boots", any(ModItems.ENDERITE_BOOTS));
            node("enderite/void_walker", armour, ModItems.ENDERITE_BOOTS, AdvancementType.CHALLENGE, fullSet, AdvancementRequirements.Strategy.AND);
            node("enderite/cutting_edge", template, ModItems.ENDERITE_PICKAXE, AdvancementType.GOAL, "enderite_tool",
                    any(ModItems.ENDERITE_SWORD, ModItems.ENDERITE_SPEAR, ModItems.ENDERITE_PICKAXE, ModItems.ENDERITE_AXE,
                            ModItems.ENDERITE_SHOVEL, ModItems.ENDERITE_HOE));
            node("enderite/hammer_of_the_end", template, ModItems.ENDERITE_SLEDGEHAMMER, AdvancementType.CHALLENGE,
                    "enderite_sledgehammer", any(ModItems.ENDERITE_SLEDGEHAMMER));
            node("enderite/endless_pockets", template, ModItems.ENDERITE_BUNDLE, AdvancementType.GOAL, "enderite_storage",
                    any(ModItems.ENDERITE_BUNDLE, ModItems.ENDERITE_QUIVER, ModItems.ENDERITE_BACKPACK));
            node("enderite/heart_of_the_end", template, ModItems.ENDERITE_CORE, AdvancementType.GOAL, "enderite_core",
                    any(ModItems.ENDERITE_CORE));
        }

        private void pads(AdvancementHolder root) {
            AdvancementHolder plate = node("pads/under_pressure", root, TweaksBlocks.DIAMOND_PRESSURE_PLATE, AdvancementType.TASK,
                    "diamond_pressure_plate", any(TweaksBlocks.DIAMOND_PRESSURE_PLATE));
            AdvancementHolder liftoff = feature("pads/liftoff", plate, TweaksBlocks.LAUNCHPAD, AdvancementType.TASK, ModTriggers.LAUNCHPAD);
            AdvancementHolder wings = feature("pads/wings_on_loan", liftoff, TweaksBlocks.ELYTRA_PAD, AdvancementType.TASK, ModTriggers.ELYTRA_PAD);
            feature("pads/fly_me_to_the_moon", wings, TweaksBlocks.FLYPAD, AdvancementType.GOAL, ModTriggers.FLYPAD);
            feature("pads/home_sweet_spawn", plate, TweaksBlocks.SPAWN_TELEPORTER, AdvancementType.TASK, ModTriggers.SPAWN_TELEPORT);
            node("pads/always_loaded", plate, TweaksBlocks.CHUNK_LOADER, AdvancementType.TASK, "chunk_loader",
                    any(TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER));
            AdvancementHolder head = node("pads/hot_head", plate, TweaksItems.BLAZE_HEAD, AdvancementType.TASK, "blaze_head",
                    any(TweaksItems.BLAZE_HEAD));
            feature("pads/bottoms_up", head, TweaksBlocks.POTION_PAD, AdvancementType.GOAL, ModTriggers.POTION_PAD);
        }

        // ---------------------------------------------------------------------------------

        private Criterion<InventoryChangeTrigger.TriggerInstance> any(ItemLike... anyOf) {
            return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, anyOf));
        }

        private AdvancementHolder feature(String path, AdvancementHolder parent, ItemLike icon, AdvancementType type, String feature) {
            return node(path, parent, icon, type, feature, ModTriggers.FEATURE_USED.used(feature));
        }

        private AdvancementHolder node(String path, AdvancementHolder parent, ItemLike icon, AdvancementType type,
                                       String criterion, Criterion<?> trigger) {
            return save(builder(path, icon, type).parent(parent).addCriterion(criterion, trigger), path);
        }

        private AdvancementHolder node(String path, Identifier parent, ItemLike icon, AdvancementType type,
                                       String criterion, Criterion<?> trigger) {
            return save(builder(path, icon, type).parent(parent).addCriterion(criterion, trigger), path);
        }

        private AdvancementHolder node(String path, AdvancementHolder parent, ItemLike icon, AdvancementType type,
                                       Map<String, Criterion<?>> criteria, AdvancementRequirements.Strategy strategy) {
            Advancement.Builder builder = builder(path, icon, type).parent(parent).requirements(strategy);
            criteria.forEach(builder::addCriterion);
            return save(builder, path);
        }

        private static Advancement.Builder builder(String path, ItemLike icon, AdvancementType type) {
            return Advancement.Builder.advancement().display(new DisplayInfo(new ItemStack(icon.asItem()),
                    title(path), description(path), Optional.empty(), type, true, true, false));
        }

        private AdvancementHolder save(Advancement.Builder builder, String path) {
            AdvancementHolder holder = builder.build(Identifier.fromNamespaceAndPath(NS, path));
            out.accept(holder);
            return holder;
        }

        private static Component title(String path) {
            return Component.translatable(key(path) + ".title");
        }

        private static Component description(String path) {
            return Component.translatable(key(path) + ".description");
        }
    }

    /** {@code advancements.simplebuilding.<path with dots>} - the lang key stem of an advancement. */
    public static String key(String path) {
        return "advancements." + NS + "." + path.replace('/', '.');
    }
}

package com.simplelib.registry;

import com.simplelib.SimpleLib;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Shared materials (split plan stage 3d, §2.5): one owner for the base materials several Simple mods use. SimpleLib
 * always registers them, so a world stays loadable whichever mod is missing; their ways into the world (recipes with the
 * condition {@code simplelib:material_requested}, the anvil crushing) only run when a loaded mod requested the group
 * ({@code SimpleLibApi.requestMaterial}). SimpleLib names none of its users.
 *
 * <p>Registration is idempotent: the loader entry and a partner's first {@code SimpleLibApi.materialItem} call both
 * reach {@link #ensureBlocks}/{@link #ensureItems}, so the order of the mods' registration events does not matter
 * (blocks in the block event, items in the item event).
 */
public final class LibMaterials {
    /** Group -> ids (items; the blocks have block items of the same id). */
    public static final Map<String, List<String>> GROUPS = Map.of(
            "cracked_diamond", List.of("diamond_pebble", "cracked_diamond", "cracked_diamond_block"),
            "netherite_nugget", List.of("netherite_nugget"),
            "enderite", List.of("raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_ingot", "enderite_nugget", "enderite_block"),
            "upgrade_templates", List.of("basic_upgrade_template", "enderite_upgrade_template"),
            // Module family stage 3 (rule 16): the building block several family mods share (Blocks, End, Enderite, Octets).
            "polished_end_stone", List.of("polished_end_stone"));

    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    private LibMaterials() {}

    public static void request(String group) {
        if (!GROUPS.containsKey(group)) throw new IllegalArgumentException("unknown SimpleLib material group " + group + ", known: " + GROUPS.keySet());
        if (REQUESTED.add(group)) SimpleLib.LOG.info("SimpleLib material '{}' requested", group);
    }

    public static boolean requested(String group) {
        return REQUESTED.contains(group);
    }

    public static synchronized void ensureBlocks() {
        if (!BLOCKS.isEmpty()) return;
        block("cracked_diamond_block", BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).strength(7.0F, 14.0F));
        block("enderite_block", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                .strength(50.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK));
        block("polished_end_stone", BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops()
                .strength(3.0F, 9.0F).sound(SoundType.STONE));
    }

    public static synchronized void ensureItems() {
        if (!ITEMS.isEmpty()) return;
        ensureBlocks();
        item("basic_upgrade_template", p -> upgradeTemplate("basic_upgrade_template", p));
        item("enderite_upgrade_template", p -> upgradeTemplate("enderite_upgrade_template", p));
        item("diamond_pebble", Item::new);
        item("cracked_diamond", Item::new);
        item("cracked_diamond_block", p -> new BlockItem(BLOCKS.get("cracked_diamond_block"), p.useBlockDescriptionPrefix()));
        item("netherite_nugget", p -> new Item(p.fireResistant()));
        item("enderite_nugget", p -> new Item(p.fireResistant()));
        item("raw_enderite", Item::new);
        item("layered_raw_enderite", Item::new);
        item("enderite_scrap", p -> new Item(p.fireResistant()));
        // The armor trim material (SimpleBuilding's) is added by that mod when the item's components are built.
        item("enderite_ingot", p -> new Item(p.fireResistant()));
        item("enderite_block", p -> new BlockItem(BLOCKS.get("enderite_block"), p.fireResistant().useBlockDescriptionPrefix()));
        item("polished_end_stone", p -> new BlockItem(BLOCKS.get("polished_end_stone"), p.useBlockDescriptionPrefix()));
    }

    public static Item item(String id) {
        ensureItems();
        Item item = ITEMS.get(id);
        if (item == null) throw new IllegalArgumentException("not a SimpleLib material item: " + id);
        return item;
    }

    public static Block block(String id) {
        ensureBlocks();
        Block block = BLOCKS.get(id);
        if (block == null) throw new IllegalArgumentException("not a SimpleLib material block: " + id);
        return block;
    }

    /** Every material item id (tests, tab, info). */
    public static List<String> itemIds() {
        ensureItems();
        return List.copyOf(ITEMS.keySet());
    }

    private static void block(String name, BlockBehaviour.Properties properties) {
        Identifier id = SimpleLib.id(name);
        Block block = new Block(properties.setId(ResourceKey.create(Registries.BLOCK, id)));
        BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, id, block));
    }

    private static void item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLib.id(name));
        ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key))));
    }

    private static SmithingTemplateItem upgradeTemplate(String id, Item.Properties properties) {
        String key = "item." + SimpleLib.MOD_ID + "." + id;
        return new SmithingTemplateItem(
                Component.translatable(key + ".applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".base_slot_description"),
                Component.translatable(key + ".additions_slot_description"),
                List.of(Identifier.withDefaultNamespace("container/slot/pickaxe"), Identifier.withDefaultNamespace("container/slot/chestplate")),
                List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
                properties.stacksTo(64).rarity(Rarity.UNCOMMON));
    }
}

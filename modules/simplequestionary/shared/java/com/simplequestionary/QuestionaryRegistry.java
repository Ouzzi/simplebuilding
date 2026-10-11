package com.simplequestionary;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/** Block, block item and menu; each loader calls the parts inside its matching registration window. */
public final class QuestionaryRegistry {
    public static final Identifier TABLE_ID = SimpleQuestionary.id("questionary_table");
    /** SimpleBuilding's functional tab: the table also goes there when that mod is installed (no class import). */
    public static final ResourceKey<CreativeModeTab> SB_FUNCTIONAL_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("simplebuilding", "functional"));

    public static QuestionaryTableBlock TABLE;
    public static Item TABLE_ITEM;
    public static MenuType<QuestionaryMenu> MENU;

    public static void registerBlocks() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, TABLE_ID);
        TABLE = Registry.register(BuiltInRegistries.BLOCK, key, new QuestionaryTableBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD)
                .ignitedByLava().noOcclusion().setId(key)));
    }

    public static void registerItems() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TABLE_ID);
        TABLE_ITEM = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(TABLE, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    public static void registerMenus() {
        MENU = Registry.register(BuiltInRegistries.MENU, TABLE_ID, new MenuType<>(QuestionaryMenu::new, FeatureFlags.VANILLA_SET));
    }

    /** What the creative tabs show: nothing while the host switched the module off. */
    public static List<ItemStack> tabStacks() {
        return QuestionaryConfig.enabled ? List.of(new ItemStack(TABLE_ITEM)) : List.of();
    }

    private QuestionaryRegistry() {}
}

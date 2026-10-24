package com.simplelights;

import com.simplelights.block.CopperLightPaneBlock;
import com.simplelights.block.CopperRedstoneLightPaneBlock;
import com.simplelights.block.LightPaneBlock;
import com.simplelights.block.LightPaneItem;
import com.simplelights.block.RedstoneLightPaneBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything Simple Lights registers (Fabric: directly in the initializer), in this order: blocks, items, creative tab.
 * Ids, properties (copied from glass, popped by pistons) and order as in SimpleBuilding before the move (N57 Light Panes,
 * N65 copper pane, N68 copper colors).
 */
public final class LightsRegistry {
    public static Block LIGHT_PANE;
    public static Block TRANSPARENT_LIGHT_PANE;
    /** The sixteen single-color panes in dye order (white ... black). */
    public static List<Block> COLORED_LIGHT_PANES = List.of();
    public static Block REDSTONE_LIGHT_PANE;
    /** N65: one fixed color, flips on every rising redstone edge like the Vanilla Copper Bulb; no oxidation stages. */
    public static Block COPPER_LIGHT_PANE;
    /** N68: the sixteen colors of the plain panes with the copper switch (same dye order), named {@code <dye>_copper_light_pane}. */
    public static List<Block> COPPER_COLORED_LIGHT_PANES = List.of();
    /** N68: the multicolor pane with the copper switch. */
    public static Block COPPER_REDSTONE_LIGHT_PANE;
    /** The block items of {@link #lightPanes()}, same order. */
    public static List<Item> ITEMS = List.of();
    public static CreativeModeTab TAB;

    private LightsRegistry() {}

    public static void registerBlocks() {
        LIGHT_PANE = plain("light_pane", MapColor.COLOR_YELLOW);
        TRANSPARENT_LIGHT_PANE = plain("transparent_light_pane", MapColor.NONE);
        List<Block> colored = new ArrayList<>();
        for (DyeColor dye : DyeColor.values()) colored.add(plain(dye.getSerializedName() + "_light_pane", dye.getMapColor()));
        COLORED_LIGHT_PANES = List.copyOf(colored);
        REDSTONE_LIGHT_PANE = block("redstone_light_pane", MapColor.COLOR_RED, RedstoneLightPaneBlock::light, RedstoneLightPaneBlock::new);
        COPPER_LIGHT_PANE = block("copper_light_pane", MapColor.COLOR_ORANGE, CopperLightPaneBlock::light, CopperLightPaneBlock::new);
        List<Block> copper = new ArrayList<>();
        for (DyeColor dye : DyeColor.values()) {
            copper.add(block(dye.getSerializedName() + "_copper_light_pane", dye.getMapColor(), CopperLightPaneBlock::light, CopperLightPaneBlock::new));
        }
        COPPER_COLORED_LIGHT_PANES = List.copyOf(copper);
        COPPER_REDSTONE_LIGHT_PANE = block("copper_redstone_light_pane", MapColor.COLOR_RED, CopperRedstoneLightPaneBlock::light, CopperRedstoneLightPaneBlock::new);
    }

    public static void registerItems() {
        List<Item> out = new ArrayList<>();
        for (Block block : lightPanes()) {
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            boolean multicolor = block == REDSTONE_LIGHT_PANE || block == COPPER_REDSTONE_LIGHT_PANE;
            out.add(item(name, p -> multicolor ? new LightPaneItem(block, p) : new BlockItem(block, p)));
        }
        ITEMS = List.copyOf(out);
        com.simplelights.guide.LightsGuide.register();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleLights.id("lights"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplelights.lights"))
                .icon(() -> new ItemStack(LIGHT_PANE))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** All Light Panes in tab order: normal, transparent, the sixteen colors, redstone, copper, the sixteen copper colors, copper redstone. */
    public static List<Block> lightPanes() {
        if (LIGHT_PANE == null) return List.of();
        List<Block> out = new ArrayList<>(List.of(LIGHT_PANE, TRANSPARENT_LIGHT_PANE));
        out.addAll(COLORED_LIGHT_PANES);
        out.add(REDSTONE_LIGHT_PANE);
        out.add(COPPER_LIGHT_PANE);
        out.addAll(COPPER_COLORED_LIGHT_PANES);
        out.add(COPPER_REDSTONE_LIGHT_PANE);
        return List.copyOf(out);
    }

    /** Every pane, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = paneStacks();
        if (!out.isEmpty()) out.add(com.simplelights.guide.LightsGuide.stack());
        return out;
    }

    /** The panes only, for Vanilla's functional tab (empty while the module is switched off). */
    public static List<ItemStack> paneStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleLights.on()) ITEMS.forEach(item -> out.add(new ItemStack(item)));
        return out;
    }

    private static Block plain(String name, MapColor color) {
        return block(name, color, LightPaneBlock::light, LightPaneBlock::new);
    }

    private static Block block(String name, MapColor color, ToIntFunction<BlockState> light, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleLights.id(name));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).setId(key).strength(0.3F).sound(SoundType.GLASS)
                .mapColor(color).noCollision().noOcclusion().lightLevel(light).pushReaction(PushReaction.POPPED);
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLights.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}

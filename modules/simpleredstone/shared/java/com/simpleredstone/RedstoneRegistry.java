package com.simpleredstone;

import com.simpleredstone.block.SignalDampenerBlock;
import com.simpleredstone.piston.DoublePistonArmBlock;
import com.simpleredstone.piston.DoublePistonBarrelBlock;
import com.simpleredstone.piston.DoublePistonBlock;
import com.simpleredstone.piston.DoublePistonHeadBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything Simple Redstone registers (Fabric: directly in the initializer), in this order: blocks, items, creative tab.
 * The Double Piston is four blocks (base, barrel, head, arm) with one item; only the base and the Dampener have loot.
 */
public final class RedstoneRegistry {
    public static Block DOUBLE_PISTON;
    public static Block PISTON_BARREL;
    public static Block PISTON_HEAD;
    public static Block PISTON_ARM;
    public static Block SIGNAL_DAMPENER;
    public static Item DOUBLE_PISTON_ITEM;
    public static Item SIGNAL_DAMPENER_ITEM;
    public static CreativeModeTab TAB;

    private RedstoneRegistry() {}

    public static void registerBlocks() {
        DOUBLE_PISTON = block("double_piston", DoublePistonBlock::new, false);
        PISTON_BARREL = block("double_piston_barrel", DoublePistonBarrelBlock::new, false);
        PISTON_HEAD = block("double_piston_head", DoublePistonHeadBlock::new, true);
        PISTON_ARM = block("double_piston_arm", DoublePistonArmBlock::new, true);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleRedstone.id("signal_dampener"));
        SIGNAL_DAMPENER = Registry.register(BuiltInRegistries.BLOCK, key, new SignalDampenerBlock(BlockBehaviour.Properties.of()
                .setId(key).instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)));
    }

    public static void registerItems() {
        DOUBLE_PISTON_ITEM = item("double_piston", p -> new BlockItem(DOUBLE_PISTON, p));
        SIGNAL_DAMPENER_ITEM = item("signal_dampener", p -> new BlockItem(SIGNAL_DAMPENER, p));
        com.simpleredstone.guide.RedstoneGuide.register();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleRedstone.id("redstone"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simpleredstone.redstone"))
                .icon(() -> new ItemStack(DOUBLE_PISTON_ITEM))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** The two parts, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = partStacks();
        if (!out.isEmpty()) out.add(com.simpleredstone.guide.RedstoneGuide.stack());
        return out;
    }

    /** The parts only, for Vanilla's redstone tab (empty while the module is switched off). */
    public static List<ItemStack> partStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleRedstone.on()) {
            out.add(new ItemStack(DOUBLE_PISTON_ITEM));
            out.add(new ItemStack(SIGNAL_DAMPENER_ITEM));
        }
        return out;
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, boolean thin) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleRedstone.id(name));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).strength(1.5F)
                .sound(SoundType.STONE).pushReaction(PushReaction.IMMOVEABLE).isRedstoneConductor((state, level, pos) -> false);
        if (thin) properties = properties.noOcclusion();
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleRedstone.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}

package com.simplemachines;

import com.simplemachines.block.SignalDampenerBlock;
import com.simplemachines.item.MachinePartItem;
import com.simplemachines.piston.DoublePistonArmBlock;
import com.simplemachines.piston.DoublePistonBlock;
import com.simplemachines.piston.DoublePistonHeadBlock;
import com.simplemachines.piston.DoublePistonVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything Simple Machines registers (Fabric: directly in the initializer), in this order: blocks, items, creative tab.
 * The Double Piston is three blocks (base, head, arm; one variant, {@link DoublePistonVariant#BASE}) with one item; only the
 * base and the Dampener have loot. Numbers as in SimpleBuilding before the move (IG31-12).
 */
public final class MachinesRegistry {
    public static Block DOUBLE_PISTON;
    public static Block SIGNAL_DAMPENER;
    public static Item DOUBLE_PISTON_ITEM;
    public static Item SIGNAL_DAMPENER_ITEM;
    public static CreativeModeTab TAB;

    private MachinesRegistry() {}

    public static void registerBlocks() {
        DOUBLE_PISTON = doublePiston(DoublePistonVariant.BASE);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMachines.id("signal_dampener"));
        SIGNAL_DAMPENER = Registry.register(BuiltInRegistries.BLOCK, key, new SignalDampenerBlock(BlockBehaviour.Properties.of()
                .setId(key).instabreak().sound(SoundType.STONE))); // default push reaction: a piston moves it like a block
    }

    public static void registerItems() {
        DOUBLE_PISTON_ITEM = item("double_piston", p -> new MachinePartItem(DOUBLE_PISTON, "double_piston", p));
        SIGNAL_DAMPENER_ITEM = item("signal_dampener", p -> new MachinePartItem(SIGNAL_DAMPENER, "signal_dampener", p));
        com.simplemachines.guide.MachinesGuide.register();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleMachines.id("machines"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplemachines.machines"))
                .icon(() -> new ItemStack(DOUBLE_PISTON_ITEM))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** The two parts, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = partStacks();
        if (!out.isEmpty()) out.add(com.simplemachines.guide.MachinesGuide.stack());
        return out;
    }

    /** The parts only, for Vanilla's redstone tab (empty while the module is switched off). */
    public static List<ItemStack> partStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleMachines.on()) {
            out.add(new ItemStack(DOUBLE_PISTON_ITEM));
            out.add(new ItemStack(SIGNAL_DAMPENER_ITEM));
        }
        return out;
    }

    public static Block head() {
        return ((DoublePistonBlock) DOUBLE_PISTON).head();
    }

    public static Block arm() {
        return ((DoublePistonBlock) DOUBLE_PISTON).arm();
    }

    /** The base block of a variant; head and arm are registered with it ({@link DoublePistonBlock#head()}, {@link DoublePistonBlock#arm()}). */
    private static Block doublePiston(DoublePistonVariant variant) {
        DoublePistonHeadBlock head = (DoublePistonHeadBlock) part(variant.headId(), p -> new DoublePistonHeadBlock(variant, p.noLootTable()));
        DoublePistonArmBlock arm = (DoublePistonArmBlock) part(variant.armId(), p -> new DoublePistonArmBlock(variant, p.noLootTable()));
        return part(variant.id(), p -> new DoublePistonBlock(variant, head, arm, p.pushReaction(PushReaction.PUSH_PULL)));
    }

    /**
     * A piston part like Vanilla's piston (strength 1.5, stone sound, no conductor); immovable for pistons and machines
     * (a part torn off would leave a broken piston; only the base is movable again, at stage 0, see DoublePistonPushableMixin) and without occlusion (parts are partial blocks). Head and arm have no
     * loot table, like Vanilla's piston head: the base drops the piston.
     */
    private static Block part(String path, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMachines.id(path));
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).strength(1.5F)
                .sound(SoundType.STONE).pushReaction(PushReaction.IMMOVEABLE).isRedstoneConductor((state, level, pos) -> false)
                .noOcclusion();
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMachines.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}

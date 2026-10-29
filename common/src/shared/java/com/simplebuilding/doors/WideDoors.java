package com.simplebuilding.doors;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Registration of Simple Doors (WP-17 / BS28-2). Only oak for now (owner choice ABN29-3: the other woods wait for the approval of
 * the oak doors); {@link #PREPARED} lists them for the texture generator and the later datagen loop, nothing of them is registered.
 */
public final class WideDoors {
    /** The wood whose doors are registered. */
    public static final String OAK = "oak";
    /** Woods whose door textures the generator already derives (tools/textures/doors_2026_10_29.py) but that are not registered yet. */
    public static final List<String> PREPARED = List.of("spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo",
            "crimson", "warped");

    private WideDoors() {}

    /** The oak door block of a format; immovable for pistons (a door torn in two would be worse than a door that stays). */
    public static Block registerOak(DoorFormat format) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, format.id(OAK));
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).setId(key).pushReaction(McVersion.PUSH_BLOCKED);
        return Registry.register(BuiltInRegistries.BLOCK, id, new WideDoorBlock(format, BlockSetType.OAK, properties));
    }

    public static Item registerItem(Block block, DoorFormat format) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, format.id(OAK));
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties().setId(key)));
    }
}

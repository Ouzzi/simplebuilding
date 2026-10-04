package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.block.CuttingBoardBlock;
import com.simplesandwiches.block.MilkCauldronBlock;
import com.simplesandwiches.block.SliceBlock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Blocks; created inside {@link #register()} so every loader builds them during its block registration. */
public final class ModBlocks {
    /** Owner decision F9 = B: one board per Vanilla wood type. */
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
            "cherry", "pale_oak", "poplar", "bamboo", "crimson", "warped");
    /** Butter block friction (owner decision F7 = B; normal 0.6, ice 0.98). */
    public static final float BUTTER_FRICTION = 0.9F;

    public static final Map<String, CuttingBoardBlock> CUTTING_BOARDS = new LinkedHashMap<>();
    public static SliceBlock CHEESE_BLOCK, BUTTER_BLOCK;
    public static MilkCauldronBlock MILK_CAULDRON;

    public static void register() {
        for (String wood : WOODS) {
            Block planks = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(wood + "_planks"));
            SoundType sound = switch (wood) {
                case "bamboo" -> SoundType.BAMBOO_WOOD;
                case "cherry" -> SoundType.CHERRY_WOOD;
                case "crimson", "warped" -> SoundType.NETHER_WOOD;
                default -> SoundType.WOOD;
            };
            BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(planks.defaultMapColor()).strength(0.8F)
                    .sound(sound).noOcclusion().instrument(NoteBlockInstrument.BASS).pushReaction(PushReaction.POPPED);
            if (!wood.equals("crimson") && !wood.equals("warped")) p = p.ignitedByLava();
            String name = wood + "_cutting_board";
            CUTTING_BOARDS.put(wood, register(name, new CuttingBoardBlock(p.setId(key(name)))));
        }
        CHEESE_BLOCK = register("cheese_block", new SliceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                .strength(0.5F).sound(SoundType.WOOL).setId(key("cheese_block")), () -> ModItems.CHEESE_SLICE));
        BUTTER_BLOCK = register("butter_block", new SliceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                .strength(0.5F).sound(SoundType.HONEY_BLOCK).friction(BUTTER_FRICTION).setId(key("butter_block")), () -> ModItems.BUTTER_SLICE));
        MILK_CAULDRON = register("milk_cauldron", new MilkCauldronBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.CAULDRON)
                .setId(key("milk_cauldron"))));
    }

    public static Block[] boards() {
        return CUTTING_BOARDS.values().toArray(new Block[0]);
    }

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Sandwiches.id(name));
    }

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, key(name), block);
    }

    private ModBlocks() {}
}

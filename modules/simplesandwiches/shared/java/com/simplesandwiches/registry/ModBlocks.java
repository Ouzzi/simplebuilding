package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.block.CuttingBoardBlock;
import com.simplesandwiches.block.MilkCauldronBlock;
import com.simplesandwiches.block.SliceBlock;
import com.simplesandwiches.block.SliceLayerBlock;
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
    /** Simple End's woods: boards only while Simple End is loaded ({@link Sandwiches#simpleEnd}). */
    public static final List<String> END_WOODS = List.of("ashwood", "duskwood");
    /** Butter block friction (owner decision F7 = B; normal 0.6, ice 0.98). */
    public static final float BUTTER_FRICTION = 0.9F;
    /** WA24-3: the butter carpet has no collision (the walker sinks its 1 px) and slows like soul sand (0.4) but gentler. */
    public static final float BUTTER_CARPET_SPEED = 0.75F;
    /** Cheese is springy like a bed (owner 2026-10-05): Vanilla bed bounce restitution and fall distance reduction. */
    public static final float CHEESE_BOUNCE = 0.75F, CHEESE_FALL_REDUCTION = 0.5F;

    public static final Map<String, CuttingBoardBlock> CUTTING_BOARDS = new LinkedHashMap<>();
    public static SliceBlock CHEESE_BLOCK, BUTTER_BLOCK;
    public static SliceLayerBlock BUTTER_CARPET, CHEESE_CARPET;
    public static MilkCauldronBlock MILK_CAULDRON;

    public static void register() {
        List<String> woods = Sandwiches.simpleEnd ? java.util.stream.Stream.concat(WOODS.stream(), END_WOODS.stream()).toList() : WOODS;
        for (String wood : woods) {
            // Simple End's planks may not be registered yet (no load order between mods): their map colour is fixed here.
            MapColor color = switch (wood) {
                case "ashwood" -> MapColor.COLOR_LIGHT_GRAY;
                case "duskwood" -> MapColor.TERRACOTTA_LIGHT_GRAY;
                default -> BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(wood + "_planks")).defaultMapColor();
            };
            SoundType sound = switch (wood) {
                case "bamboo" -> SoundType.BAMBOO_WOOD;
                case "cherry" -> SoundType.CHERRY_WOOD;
                case "crimson", "warped" -> SoundType.NETHER_WOOD;
                default -> SoundType.WOOD;
            };
            BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(color).strength(0.8F)
                    .sound(sound).noOcclusion().instrument(NoteBlockInstrument.BASS).pushReaction(PushReaction.POPPED);
            if (!wood.equals("crimson") && !wood.equals("warped")) p = p.ignitedByLava();
            String name = wood + "_cutting_board";
            CUTTING_BOARDS.put(wood, register(name, new CuttingBoardBlock(p.setId(key(name)))));
        }
        CHEESE_BLOCK = register("cheese_block", new SliceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                .strength(0.5F).sound(SoundType.WOOL).bounceRestitution(CHEESE_BOUNCE).fallDistanceReduction(CHEESE_FALL_REDUCTION)
                .setId(key("cheese_block")), () -> ModItems.CHEESE_SLICE, () -> ModSounds.CHEESE_CUT));
        BUTTER_BLOCK = register("butter_block", new SliceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                .strength(0.5F).sound(SoundType.HONEY_BLOCK).friction(BUTTER_FRICTION).setId(key("butter_block")), () -> ModItems.BUTTER_SLICE, () -> ModSounds.BUTTER_CUT));
        BUTTER_CARPET = register("butter_carpet", new SliceLayerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                .strength(0.1F).sound(SoundType.HONEY_BLOCK).friction(BUTTER_FRICTION).speedFactor(BUTTER_CARPET_SPEED).noCollision().pushReaction(PushReaction.POPPED)
                .setId(key("butter_carpet")), () -> ModItems.BUTTER_SLICE, () -> BUTTER_BLOCK, true));
        // BS25-14: cheese layers, walkable like snow (no noCollision), same bounce as the block only once it is whole.
        CHEESE_CARPET = register("cheese_carpet", new SliceLayerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                .strength(0.1F).sound(SoundType.WOOL).pushReaction(PushReaction.POPPED)
                .setId(key("cheese_carpet")), () -> ModItems.CHEESE_SLICE, () -> CHEESE_BLOCK, false));
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

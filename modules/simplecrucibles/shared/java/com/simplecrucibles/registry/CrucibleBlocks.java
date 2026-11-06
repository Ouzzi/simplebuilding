package com.simplecrucibles.registry;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.crucible.CrucibleBlankBlock;
import com.simplecrucibles.crucible.CrucibleBlock;
import com.simplecrucibles.crucible.CrucibleTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Blocks; created inside {@link #register()} so every loader builds them during its block registration. */
public final class CrucibleBlocks {
    public static CrucibleBlock IRON_CRUCIBLE, REINFORCED_CRUCIBLE, NETHERITE_CRUCIBLE, ENDERITE_CRUCIBLE;
    public static CrucibleBlankBlock CRUCIBLE_BLANK;
    public static com.simplecrucibles.cauldron.ReinforcedCauldronBlock REINFORCED_CAULDRON;
    /** Every crucible block, including those partners register through the API (Enderite in SimpleBuilding). */
    public static final List<CrucibleBlock> CRUCIBLES = new ArrayList<>();

    public static void register() {
        IRON_CRUCIBLE = crucible("iron_crucible", CrucibleTier.IRON, props(MapColor.METAL, 5.0F, 6.0F, SoundType.METAL));
        REINFORCED_CRUCIBLE = crucible("reinforced_crucible", CrucibleTier.REINFORCED, props(MapColor.DIAMOND, 5.0F, 6.0F, SoundType.METAL));
        NETHERITE_CRUCIBLE = crucible("netherite_crucible", CrucibleTier.NETHERITE, props(MapColor.COLOR_BLACK, 5.0F, 1200.0F, SoundType.NETHERITE_BLOCK));
        // Enderite stage (owner F3: stays in this module; split stage 5): 30 slots, 8x speed, double stacks.
        ENDERITE_CRUCIBLE = crucible("enderite_crucible", CrucibleTier.ENDERITE, props(MapColor.COLOR_PURPLE, 6.0F, 1500.0F, SoundType.NETHERITE_BLOCK));
        CRUCIBLE_BLANK = Registry.register(BuiltInRegistries.BLOCK, key("crucible_blank"),
                new CrucibleBlankBlock(props(MapColor.METAL, 5.0F, 6.0F, SoundType.METAL).setId(key("crucible_blank"))));
        REINFORCED_CAULDRON = Registry.register(BuiltInRegistries.BLOCK, key("reinforced_cauldron"), new com.simplecrucibles.cauldron.ReinforcedCauldronBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()
                        .lightLevel(state -> state.getValue(com.simplecrucibles.cauldron.ReinforcedCauldronBlock.CONTENT).hot() ? 15 : 0)
                        .setId(key("reinforced_cauldron"))));
    }

    /** Properties shared by all crucibles: needs a pickaxe, lit crucibles glow like a furnace. */
    public static BlockBehaviour.Properties props(MapColor color, float hardness, float resistance, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, resistance).sound(sound)
                .requiresCorrectToolForDrops().noOcclusion()
                .lightLevel(state -> state.hasProperty(CrucibleBlock.LIT) && state.getValue(CrucibleBlock.LIT) ? 13 : 0);
    }

    private static CrucibleBlock crucible(String name, CrucibleTier tier, BlockBehaviour.Properties properties) {
        CrucibleBlock block = Registry.register(BuiltInRegistries.BLOCK, key(name), new CrucibleBlock(properties.setId(key(name)), tier));
        CRUCIBLES.add(block);
        return block;
    }

    public static Block[] crucibles() {
        return CRUCIBLES.toArray(new Block[0]);
    }

    public static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, SimpleCrucibles.id(name));
    }

    private CrucibleBlocks() {}
}

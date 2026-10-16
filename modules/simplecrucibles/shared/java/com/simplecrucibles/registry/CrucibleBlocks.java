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
    public static com.simplecrucibles.crucible.CrucibleBarrelBlock COPPER_BARREL, REINFORCED_BARREL, NETHERITE_BARREL, ENDERITE_BARREL;
    /** Every barrel block, including partner tiers (Enderite in SimpleBuilding). */
    public static final List<com.simplecrucibles.crucible.CrucibleBarrelBlock> BARRELS = new ArrayList<>();
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
        COPPER_BARREL = barrel("copper_barrel", com.simplecrucibles.crucible.BarrelTier.COPPER, MapColor.COLOR_ORANGE, 3.0F);
        REINFORCED_BARREL = barrel("reinforced_barrel", com.simplecrucibles.crucible.BarrelTier.REINFORCED, MapColor.DIAMOND, 4.0F);
        NETHERITE_BARREL = barrel("netherite_barrel", com.simplecrucibles.crucible.BarrelTier.NETHERITE, MapColor.COLOR_BLACK, 5.0F);
        ENDERITE_BARREL = barrel("enderite_barrel", com.simplecrucibles.crucible.BarrelTier.ENDERITE, MapColor.COLOR_PURPLE, 6.0F);
    }

    private static com.simplecrucibles.crucible.CrucibleBarrelBlock barrel(String name, com.simplecrucibles.crucible.BarrelTier tier, MapColor color, float hardness) {
        boolean netherite = tier == com.simplecrucibles.crucible.BarrelTier.NETHERITE || tier == com.simplecrucibles.crucible.BarrelTier.ENDERITE;
        float resistance = tier == com.simplecrucibles.crucible.BarrelTier.ENDERITE ? 1500.0F : netherite ? 1200.0F : 6.0F;
        var block = Registry.register(BuiltInRegistries.BLOCK, key(name), new com.simplecrucibles.crucible.CrucibleBarrelBlock(
                BlockBehaviour.Properties.of().mapColor(color).strength(hardness, resistance)
                        .sound(netherite ? SoundType.NETHERITE_BLOCK : SoundType.COPPER).requiresCorrectToolForDrops()
                        .setId(key(name)), tier));
        BARRELS.add(block);
        return block;
    }

    public static Block[] barrels() {
        return BARRELS.toArray(new Block[0]);
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

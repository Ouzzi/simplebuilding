package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlankBlock;
import com.simplelib.crucible.CrucibleBlock;
import com.simplelib.crucible.CrucibleTier;
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
public final class LibBlocks {
    public static CrucibleBlock IRON_CRUCIBLE, REINFORCED_CRUCIBLE, NETHERITE_CRUCIBLE;
    public static CrucibleBlankBlock CRUCIBLE_BLANK;
    public static com.simplelib.crucible.CrucibleBarrelBlock COPPER_BARREL, REINFORCED_BARREL;
    /** Every barrel block, including partner tiers (Enderite in SimpleBuilding). */
    public static final List<com.simplelib.crucible.CrucibleBarrelBlock> BARRELS = new ArrayList<>();
    /** Every crucible block, including those partners register through the API (Enderite in SimpleBuilding). */
    public static final List<CrucibleBlock> CRUCIBLES = new ArrayList<>();

    public static void register() {
        IRON_CRUCIBLE = crucible("iron_crucible", CrucibleTier.IRON, props(MapColor.METAL, 5.0F, 6.0F, SoundType.METAL));
        REINFORCED_CRUCIBLE = crucible("reinforced_crucible", CrucibleTier.REINFORCED, props(MapColor.DIAMOND, 5.0F, 6.0F, SoundType.METAL));
        NETHERITE_CRUCIBLE = crucible("netherite_crucible", CrucibleTier.NETHERITE, props(MapColor.COLOR_BLACK, 5.0F, 1200.0F, SoundType.NETHERITE_BLOCK));
        CRUCIBLE_BLANK = Registry.register(BuiltInRegistries.BLOCK, key("crucible_blank"),
                new CrucibleBlankBlock(props(MapColor.METAL, 5.0F, 6.0F, SoundType.METAL).setId(key("crucible_blank"))));
        COPPER_BARREL = barrel("copper_barrel", com.simplelib.crucible.BarrelTier.COPPER, MapColor.COLOR_ORANGE, 3.0F);
        REINFORCED_BARREL = barrel("reinforced_barrel", com.simplelib.crucible.BarrelTier.REINFORCED, MapColor.DIAMOND, 4.0F);
    }

    private static com.simplelib.crucible.CrucibleBarrelBlock barrel(String name, com.simplelib.crucible.BarrelTier tier, MapColor color, float hardness) {
        var block = Registry.register(BuiltInRegistries.BLOCK, key(name), new com.simplelib.crucible.CrucibleBarrelBlock(
                BlockBehaviour.Properties.of().mapColor(color).strength(hardness, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops()
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
        return ResourceKey.create(Registries.BLOCK, SimpleLib.id(name));
    }

    private LibBlocks() {}
}

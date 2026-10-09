package com.simplebuilding.blocks.entity;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModSmokerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<ModHopperBlockEntity> MOD_HOPPER_BE;
    public static BlockEntityType<ModBlastFurnaceBlockEntity> MOD_BLAST_FURNACE_BE;
    public static BlockEntityType<ModFurnaceBlockEntity> MOD_FURNACE_BE;
    public static BlockEntityType<ModSmokerBlockEntity> MOD_SMOKER_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.BackpackBlockEntity> BACKPACK_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity> PLACED_TEMPLATE_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity> PLACED_BUNDLE_BE;
    /** Kleinteile auf einem Fleck; nur, wenn es den Block gibt (McVersion.SMALL_PLACEABLES). */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity> PLACED_SMALL_PARTS_BE;
    /** Schachfiguren auf einem Block; nur mit McVersion.CHESS. */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity> CHESS_PIECES_BE;
    /** Haengematten (Tuch und Seil kennen ihre Matte); nur mit McVersion.HAMMOCK. */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.HammockBlockEntity> HAMMOCK_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity> TIERED_CHEST_BE;
    /** Auto-Schmied; nur, wenn es den Block gibt (McVersion.AUTO_SMITHER). */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity> AUTO_SMITHER_BE;
    /** Autonomer Crafter; nur, wenn es den Block gibt (McVersion.AUTONOMOUS_CRAFTER). */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.AutonomousCrafterBlockEntity> AUTONOMOUS_CRAFTER_BE;
    /** Werkbank mit Lager; nur, wenn es den Block gibt (McVersion.STORAGE_CRAFTING_TABLE). */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.StorageCraftingTableBlockEntity> STORAGE_CRAFTING_TABLE_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity> TIERED_SHULKER_BOX_BE;

    private ModBlockEntities() {
    }

    public static void registerBlockEntities() {
        Simplebuilding.LOGGER.info("Registering Block Entities for {}", Simplebuilding.MOD_ID);
    }
}

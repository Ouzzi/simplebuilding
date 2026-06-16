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

    private ModBlockEntities() {
    }

    public static void registerBlockEntities() {
        Simplebuilding.LOGGER.info("Registering Block Entities for {}", Simplebuilding.MOD_ID);
    }
}

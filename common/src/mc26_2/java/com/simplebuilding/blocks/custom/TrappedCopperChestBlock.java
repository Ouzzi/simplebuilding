package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Compile-only twin; trapped copper chests are not registered until the 26.2 port run. */
public class TrappedCopperChestBlock extends ChestBlock {
    private final WeatheringCopper.WeatherState weatherState;
    private final boolean waxed;

    public TrappedCopperChestBlock(WeatheringCopper.WeatherState weatherState, boolean waxed, BlockBehaviour.Properties properties) {
        super(() -> ModBlockEntities.TRAPPED_COPPER_CHEST_BE, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
        this.weatherState = weatherState;
        this.waxed = waxed;
    }

    public WeatheringCopper.WeatherState getAge() {
        return weatherState;
    }

    public boolean isWaxed() {
        return waxed;
    }
}

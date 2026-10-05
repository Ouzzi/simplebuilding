package com.simplebuilding.neoforge;

import com.simplebuilding.fluid.ModFluids;
import com.simplebuilding.fluid.SoulLavaFluid;
import com.simplebuilding.version.McVersion;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge needs a {@link FluidType} for every mod fluid (swimming, sounds, light). Soul lava gets lava's
 * type values; the fluid classes come from {@link ModFluids#factory}, swapped here for subclasses that
 * return the type. Rendering: {@code NeoForgeSoulLavaClient}.
 */
public final class NeoForgeSoulLava {
    public static FluidType TYPE;

    public static void install(IEventBus modBus) {
        if (!McVersion.CRUCIBLE) return;
        ModFluids.factory = new ModFluids.Factory() {
            @Override
            public FlowingFluid source() {
                return new SoulLavaFluid.Source() {
                    @Override
                    public FluidType getFluidType() {
                        return TYPE;
                    }
                };
            }

            @Override
            public FlowingFluid flowing() {
                return new SoulLavaFluid.Flowing() {
                    @Override
                    public FluidType getFluidType() {
                        return TYPE;
                    }
                };
            }
        };
        modBus.addListener((RegisterEvent event) -> event.register(NeoForgeRegistries.Keys.FLUID_TYPES, ModFluids.id("soul_lava"), () -> {
            TYPE = new FluidType(FluidType.Properties.create()
                    .descriptionId("block.simplebuilding.soul_lava")
                    .canSwim(false)
                    .canDrown(false)
                    .canConvertToSource(false)
                    .pathType(PathType.LAVA)
                    .adjacentPathType(null)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .lightLevel(15)
                    .density(3000)
                    .viscosity(6000)
                    .temperature(1300)) {
                @Override
                public double motionScale(Entity entity) {
                    return Entity.LAVA_SLOW_FLOW_SCALE;
                }

                @Override
                public void setItemMovement(ItemEntity entity) {
                    entity.setFluidMovement(0.95F);
                }
            };
            return TYPE;
        }));
    }

    private NeoForgeSoulLava() {}
}

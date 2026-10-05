package com.simplebuilding.forge;

import com.simplebuilding.fluid.ModFluids;
import com.simplebuilding.fluid.SoulLavaFluid;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge needs a {@link FluidType} for every mod fluid: soul lava gets lava's values plus its turquoise textures.
 * The fluid classes come from {@link ModFluids#factory}, swapped here for subclasses that return the type.
 */
public final class ForgeSoulLava {
    public static FluidType TYPE;

    public static void install(BusGroup modBus) {
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
        RegisterEvent.getBus(modBus).addListener(event -> event.register(ForgeRegistries.Keys.FLUID_TYPES, ModFluids.id("soul_lava"), () -> {
            TYPE = new FluidType(FluidType.Properties.create()
                    .descriptionId("block.simplebuilding.soul_lava")
                    .canSwim(false)
                    .canDrown(false)
                    .canConvertToSource(false)
                    .pathType(PathType.LAVA)
                    .adjacentPathType(null)
                    .motionScale(0.0023333333333333335D)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .lightLevel(15)
                    .density(3000)
                    .viscosity(6000)
                    .temperature(1300)) {
                @Override
                public void setItemMovement(ItemEntity entity) {
                    net.minecraft.world.phys.Vec3 motion = entity.getDeltaMovement();
                    entity.setDeltaMovement(motion.x * 0.95F, motion.y + (motion.y < 0.06F ? 5.0E-4F : 0.0F), motion.z * 0.95F);
                }

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public Identifier getStillTexture() {
                            return ModFluids.id("block/soul_lava_still");
                        }

                        @Override
                        public Identifier getFlowingTexture() {
                            return ModFluids.id("block/soul_lava_flow");
                        }
                    });
                }
            };
            return TYPE;
        }));
    }

    private ForgeSoulLava() {}
}

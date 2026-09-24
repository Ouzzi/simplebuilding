package com.simplebuilding.mixin.forge;

import com.simplebuilding.forge.gametest.ForgeGameTests;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the shared test suite into the data driven test instance registry on Forge - see
 * {@link ForgeGameTests}.
 *
 * <p>{@code RegistryDataLoader} freezes its load tasks in list order, and the test environments
 * come right before the test instances, so the environment registry is remembered when it freezes
 * and used when the instance registry is about to. Only for the load from data packs
 * ({@link ResourceManagerRegistryLoadTask}): a client receiving the registry over the network
 * already gets the entries from the server, and adding them again would be a duplicate key.
 */
@Mixin(RegistryLoadTask.class)
public abstract class RegistryLoadTaskMixin<T> {

    @Unique
    private static volatile Registry<TestEnvironmentDefinition<?>> simplebuilding$environments;

    @Shadow
    @Final
    private WritableRegistry<T> registry;

    @SuppressWarnings("unchecked")
    @Inject(method = "freezeRegistry", at = @At("HEAD"))
    private void simplebuilding$addTheSharedGameTests(Map<ResourceKey<?>, Exception> loadingErrors,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ForgeGameTests.enabled() || !((Object) this instanceof ResourceManagerRegistryLoadTask<?>)) {
            return;
        }
        if (registry.key().equals(Registries.TEST_ENVIRONMENT)) {
            simplebuilding$environments = (Registry<TestEnvironmentDefinition<?>>) (Object) registry;
        } else if (registry.key().equals(Registries.TEST_INSTANCE) && simplebuilding$environments != null) {
            ForgeGameTests.registerInstances(simplebuilding$environments, (Registry<GameTestInstance>) (Object) registry);
        }
    }
}

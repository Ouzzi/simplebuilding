package com.simplebuilding.mixin.client;

import com.simplebuilding.items.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

@Mixin(ItemModelResolver.class)
public class ClientSpeedometerMixin {

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void injectSpeed(ItemStackRenderState renderState, ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext, Level world, ItemOwner heldItemContext, int seed, CallbackInfo ci) {
        if (!stack.is(ModItems.VELOCITY_GAUGE)) return;

        Entity entity = heldItemContext != null ? heldItemContext.asLivingEntity() : net.minecraft.client.Minecraft.getInstance().player;
        if (entity == null) return;

        Entity targetEntity = entity.getVehicle() != null ? entity.getVehicle() : entity;

        double velX = targetEntity.getDeltaMovement().x;
        double velZ = targetEntity.getDeltaMovement().z;
        double speed = Math.sqrt(velX * velX + velZ * velZ);

        // Wir skalieren den Speed so, dass er in den Bereich passt.
        // Angenommen, 0.0 = Stand, 1.0 = Sehr schnell.
        // Wir multiplizieren mit 20, damit wir einen Float-Wert haben, den wir im JSON abfragen können.
        float speedValue = (float) (speed * 20.0f);

        // Wir speichern diesen Float im CustomModelData (Index 0)
        CustomModelData currentData = stack.get(DataComponents.CUSTOM_MODEL_DATA);

        // Kleiner Check um Objekt-Erzeugung zu sparen
        if (currentData != null && !currentData.floats().isEmpty()) {
            if (Math.abs(currentData.floats().get(0) - speedValue) < 0.1f) return;
        }

        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of((float)speedValue), // Float Liste
                List.of(), List.of(), List.of()
        ));
    }
}
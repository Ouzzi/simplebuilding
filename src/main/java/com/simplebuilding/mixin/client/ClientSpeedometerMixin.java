package com.simplebuilding.mixin.client;

import com.simplebuilding.items.ModItems;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemModelManager.class)
public class ClientSpeedometerMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void injectSpeed(ItemRenderState renderState, ItemStack stack, net.minecraft.item.ItemDisplayContext displayContext, World world, HeldItemContext heldItemContext, int seed, CallbackInfo ci) {
        if (!stack.isOf(ModItems.VELOCITY_GAUGE)) return;

        Entity entity = heldItemContext != null ? heldItemContext.getEntity() : net.minecraft.client.MinecraftClient.getInstance().player;
        if (entity == null) return;

        Entity targetEntity = entity.getVehicle() != null ? entity.getVehicle() : entity;

        double velX = targetEntity.getVelocity().x;
        double velZ = targetEntity.getVelocity().z;
        double speed = Math.sqrt(velX * velX + velZ * velZ);

        // Wir skalieren den Speed so, dass er in den Bereich passt.
        // Angenommen, 0.0 = Stand, 1.0 = Sehr schnell.
        // Wir multiplizieren mit 20, damit wir einen Float-Wert haben, den wir im JSON abfragen können.
        float speedValue = (float) (speed * 20.0f);

        // Wir speichern diesen Float im CustomModelData (Index 0)
        CustomModelDataComponent currentData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);

        // Kleiner Check um Objekt-Erzeugung zu sparen
        if (currentData != null && !currentData.floats().isEmpty()) {
            if (Math.abs(currentData.floats().get(0) - speedValue) < 0.1f) return;
        }

        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                List.of((float)speedValue), // Float Liste
                List.of(), List.of(), List.of()
        ));
    }
}
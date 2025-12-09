package com.simplebuilding.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;

@Environment(EnvType.CLIENT)
// ÄNDERUNG 1: <Float> hinzufügen!
// Das sagt dem Spiel, dass unser "Status" für die Animation eine Fließkommazahl (openProgress) ist.
public class NetheriteShulkerBoxModel extends Model<Float> {

    private final ModelPart lid;

    public NetheriteShulkerBoxModel(ModelPart root) {
        // ÄNDERUNG 2: Lambda-Ausdruck (id) -> ... statt :: benutzen.
        // Das löst den "Cannot resolve method" Fehler, weil es eindeutig ist.
        super(root, (id) -> RenderLayer.getEntityCutoutNoCull(id));

        this.lid = root.getChild("lid");
    }

    // ÄNDERUNG 3: @Override hinzufügen und Typ auf Float (Objekt) ändern, passend zu Model<Float>
    @Override
    public void setAngles(Float openProgress) {
        this.lid.setOrigin(0.0F, 24.0F - openProgress * 0.5F * 16.0F, 0.0F);
        this.lid.yaw = 270.0F * openProgress * ((float)Math.PI / 180F);
    }
}
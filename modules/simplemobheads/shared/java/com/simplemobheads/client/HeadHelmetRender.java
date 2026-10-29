package com.simplemobheads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * BS28b-1: the helmet of a placed head, drawn as it is on a player or a mob: the armor model's head part with the helmet's
 * equipment texture, trim, dye and glint (Vanilla's {@link EquipmentLayerRenderer}, so resource packs and mods that change
 * armor change this too). The armor head sits exactly where the skull model's head does (both are an 8 px cube from the
 * foot up), so the helmet only needs the head's size as a scale around the foot.
 */
public final class HeadHelmetRender {
    private static final HumanoidRenderState STATE = new HumanoidRenderState();
    private static volatile @Nullable EquipmentLayerRenderer renderer;
    private static @Nullable EntityModelSet baked;
    private static @Nullable HumanoidModel<HumanoidRenderState> model;

    private HeadHelmetRender() {
    }

    /** Vanilla's equipment renderer of the newest entity renderer context ({@code EntityRendererContextMixin}). */
    public static void renderer(EquipmentLayerRenderer value) {
        renderer = value;
    }

    private static @Nullable HumanoidModel<HumanoidRenderState> model() {
        EntityModelSet set = Minecraft.getInstance().getEntityModels();
        if (model == null || baked != set) {
            model = new HumanoidModel<>(set.bakeLayer(ModelLayers.PLAYER_ARMOR.head()));
            baked = set;
        }
        return model;
    }

    public static void draw(ItemStack helmet, float size, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        EquipmentLayerRenderer equipment = renderer;
        Equippable equippable = helmet.get(DataComponents.EQUIPPABLE);
        HumanoidModel<HumanoidRenderState> armor = model();
        if (equipment == null || equippable == null || equippable.assetId().isEmpty() || armor == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        equipment.renderLayers(EquipmentClientInfo.LayerType.HUMANOID, equippable.assetId().get(), armor, STATE, helmet, poseStack,
                collector, lightCoords, 0);
        poseStack.popPose();
    }
}

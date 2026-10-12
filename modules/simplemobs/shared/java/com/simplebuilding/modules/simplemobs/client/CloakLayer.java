package com.simplebuilding.modules.simplemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.modules.simplemobs.DisguiseRegistry;
import com.simplebuilding.modules.simplemobs.MobsRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Cloak of deception (owner 12.10.): worn in the head slot (hood = equipment layer), its cloak hangs on the back as a
 * 3D cape like the Elytra. An Elytra on the chest is drawn under it: the cloak moves out over the folded wings, and in
 * flight the wings spread out sideways while the cloak stays on the back.
 */
public class CloakLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "textures/entity/deceiver_cloak_cape.png");
    /** Folded Elytra wings reach 4 px behind the body; the cloak (1 px thick at 2 px) moves 2.5 px out over them. */
    public static final float OVER_WINGS = 0.15625F;
    private final HumanoidModel<AvatarRenderState> model;
    private final EquipmentAssetManager equipmentAssets;

    public CloakLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models, EquipmentAssetManager equipmentAssets) {
        super(parent);
        this.model = new PlayerCapeModel(models.bakeLayer(ModelLayers.PLAYER_CAPE));
        this.equipmentAssets = equipmentAssets;
    }

    public static boolean wears(AvatarRenderState state) {
        return state.headEquipment.is(DisguiseRegistry.CLOAK);
    }

    private boolean hasLayer(ItemStack stack, LayerType type) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.assetId().isPresent()
                && !equipmentAssets.get(equippable.assetId().get()).getLayers(type).isEmpty();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !wears(state)) return;
        poseStack.pushPose();
        if (hasLayer(state.chestEquipment, LayerType.WINGS)) poseStack.translate(0.0F, 0.0F, OVER_WINGS);
        else if (hasLayer(state.chestEquipment, LayerType.HUMANOID)) poseStack.translate(0.0F, -0.053125F, 0.06875F);
        collector.submitModel(model, state, poseStack, RenderTypes.entityCutout(TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}

package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.util.HeadLooks;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.Nullable;

/**
 * The head in the extra head slot (owner N49, {@link ExtraHead}) on the player, drawn like Vanilla's
 * {@link CustomHeadLayer} draws a worn head: skulls with their skull model (mod heads through Simple Mobheads' skull
 * models, with the variant of the stack), everything else (the carved pumpkin) as its item in the {@code HEAD} display
 * context. A mod head is drawn at the size of the mob's own head ({@link HeadLooks#skullFactor}) and replaces the
 * wearer's head ({@code HumanoidModelExtraHeadMixin}) exactly as in the helmet slot. A helmet is scaled over it
 * ({@code HumanoidArmorLayerExtraHeadMixin}, {@link ExtraHead#fit}).
 *
 * <p>The extra head has precedence (owner N55): when it is shown, {@code AvatarRendererExtraHeadMixin} removes a head in
 * the helmet slot from the render state, so only the extra head's model is drawn; the effects of both heads still apply.
 *
 * <p>The head comes from the player behind {@link AvatarRenderState#id} in the client level - the synced entity data
 * of every visible player - like {@link QuiverLayer} finds its quiver.
 */
public class ExtraHeadLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final Map<SkullBlock.Type, SkullModelBase> skullModels = new java.util.HashMap<>();

    public ExtraHeadLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        ItemStack head = shownHead(state);
        if (head.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        PlayerModel model = this.getParentModel();
        model.root().translateAndRotate(poseStack);
        model.translateToHead(poseStack);
        if (head.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skull) {
            SkullBlock.Type type = skull.getType();
            if (HeadLooks.skullType(head, type) instanceof SkullBlock.Type shown) {
                type = shown; // the variant of the stack (cat colors, villager types)
            }
            float scale = 1.1875F * HeadLooks.skullFactor(type); // CustomHeadLayer.SKULL_SCALE (private on 26.2) x mob head size
            poseStack.scale(scale, scale, scale);
            SkullModelBase skullModel = this.skullModels.computeIfAbsent(type,
                    t -> SkullBlockRenderer.createModel(Minecraft.getInstance().getEntityModels(), t));
            SkullBlockRenderer.submitSkull(state.walkAnimationPos, poseStack, collector, lightCoords, skullModel,
                    renderType(type, head.get(DataComponents.PROFILE)), state.outlineColor, null);
        } else {
            ItemStackRenderState item = new ItemStackRenderState();
            Entity entity = entity(state);
            Minecraft.getInstance().getItemModelResolver().updateForLiving(item, head, ItemDisplayContext.HEAD,
                    entity instanceof Player player ? player : null);
            CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
            item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        poseStack.popPose();
    }

    private static RenderType renderType(SkullBlock.Type type, @Nullable ResolvableProfile profile) {
        if (type == SkullBlock.Types.PLAYER && profile != null) {
            return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).renderType();
        }
        return SkullBlockRenderer.getSkullRenderType(type, null);
    }

    /** The extra head drawn for {@code state}: empty when invisible or without one (it wins over a head in the helmet slot). */
    public static ItemStack shownHead(AvatarRenderState state) {
        if (state.isInvisible) {
            return ItemStack.EMPTY;
        }
        return entity(state) instanceof Player player ? ExtraHead.get(player) : ItemStack.EMPTY;
    }

    private static @Nullable Entity entity(AvatarRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntity(state.id);
    }
}

package com.simplebuilding.modules.simpleend.client;

import com.simplebuilding.modules.simpleend.entity.Nibblemite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Nibblemite in the colours of its biome (Ash Valley pale grey with lavender, Dawnlands taupe with rose). A tamed one
 * (B23-M15) shows its spots glowing ({@link GlowLayer}: only the spot pixels, full-bright, slowly pulsing).
 */
public class NibblemiteRenderer extends MobRenderer<Nibblemite, NibblemiteRenderer.State, NibblemiteModel> {
    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath("simpleend", "textures/entity/nibblemite/ash.png"),
            Identifier.fromNamespaceAndPath("simpleend", "textures/entity/nibblemite/dusk.png")};
    private static final Identifier[] GLOW = {
            Identifier.fromNamespaceAndPath("simpleend", "textures/entity/nibblemite/ash_glow.png"),
            Identifier.fromNamespaceAndPath("simpleend", "textures/entity/nibblemite/dusk_glow.png")};

    public static class State extends LivingEntityRenderState {
        public int variant;
        /** Per-mob phase offset (radians) from the entity id, so neighbors do not move in step. */
        public float phase;
        /** 0 not chewing, else 0..1 progress of the chew cycle. */
        public float chew;
        /** Tamed: the spots glow. */
        public boolean tame;
    }

    public NibblemiteRenderer(EntityRendererProvider.Context context) {
        super(context, new NibblemiteModel(context.bakeLayer(NibblemiteModel.LAYER)), 0.25F);
        this.addLayer(new GlowLayer(this));
    }

    @Override
    protected float getFlipDegrees() { return 180.0F; }

    @Override
    public Identifier getTextureLocation(State state) { return TEXTURES[state.variant == 1 ? 1 : 0]; }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(Nibblemite entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
        state.chew = entity.chewProgress(partialTicks);
        state.phase = (entity.getId() * 1.618F) % ((float) Math.PI * 2.0F);
        state.tame = entity.isTame();
    }

    /** The glowing spots of a tamed Nibblemite: drawn like Vanilla's eyes layer, brightness pulsing with the own phase. */
    static final class GlowLayer extends RenderLayer<State, NibblemiteModel> {
        GlowLayer(RenderLayerParent<State, NibblemiteModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, State state, float yRot, float xRot) {
            if (!state.tame || state.isInvisible) return;
            int v = (int) (255 * (0.7F + 0.3F * Mth.sin(state.ageInTicks * 0.12F + state.phase)));
            int color = 0xFF000000 | v << 16 | v << 8 | v;
            collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(GLOW[state.variant == 1 ? 1 : 0]),
                    lightCoords, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
        }
    }
}

package com.simpledinos.client;

import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Trex;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * T. rex: one texture per armor tier (plain, leather, iron, diamond); the saddle and the armor plates are model parts that
 * show when worn. The culling box is far larger than the 3 x 4 block hitbox, otherwise head and tail vanish when the core
 * leaves the screen (concept 3.7).
 */
public class TrexRenderer extends MobRenderer<Trex, TrexRenderer.State, TrexModel> {
    private static final Identifier[] TEXTURES = {
            SimpleDinos.id("textures/entity/trex/plain.png"), SimpleDinos.id("textures/entity/trex/leather.png"),
            SimpleDinos.id("textures/entity/trex/iron.png"), SimpleDinos.id("textures/entity/trex/diamond.png")};

    public static class State extends LivingEntityRenderState {
        public boolean saddled;
        public int armorTier;
    }

    public TrexRenderer(EntityRendererProvider.Context context) {
        super(context, new TrexModel(context.bakeLayer(TrexModel.LAYER)), 2.2F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, state.armorTier))];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Trex entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.armorTier = entity.armorTier();
    }

    @Override
    protected AABB getBoundingBoxForCulling(Trex entity, float partialTicks) {
        return entity.getBoundingBox().inflate(7.0, 1.0, 7.0);
    }
}

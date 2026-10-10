package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.DeceiverEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class DeceiverRenderer extends MobRenderer<DeceiverEntity, DeceiverRenderState, DeceiverModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/deceiver/deceiver.png");
    private static final Identifier EYES = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/deceiver/deceiver_eyes.png");

    public DeceiverRenderer(EntityRendererProvider.Context context) {
        super(context, new DeceiverModel(context.bakeLayer(DeceiverModel.LAYER)), 0.35F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES, (state, age) -> 1.0F,
                new DeceiverModel(context.bakeLayer(DeceiverModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(DeceiverRenderState state) { return TEXTURE; }

    @Override
    public DeceiverRenderState createRenderState() { return new DeceiverRenderState(); }

    @Override
    public void extractRenderState(DeceiverEntity entity, DeceiverRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.armored = entity.isArmored();
        state.hostile = entity.isHostileMode();
        state.idle.copyFrom(entity.idleState);
        state.summon.copyFrom(entity.summonState);
        state.teleport.copyFrom(entity.teleportState);
        state.drink.copyFrom(entity.drinkState);
        state.unmask.copyFrom(entity.unmaskState);
    }
}

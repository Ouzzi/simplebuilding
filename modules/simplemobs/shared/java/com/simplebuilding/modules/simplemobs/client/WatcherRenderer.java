package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.HollowWatcherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class WatcherRenderer extends MobRenderer<HollowWatcherEntity, WatcherRenderState, WatcherModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_watcher/hollow_watcher.png");
    private static final Identifier EYES = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_watcher/hollow_watcher_eyes.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherModel(context.bakeLayer(WatcherModel.LAYER)), 0.6F);
        // many eyes: bright while it stalks, narrowed when watched, dim when frozen
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES,
                (state, age) -> !state.triggered ? 0.0F : state.frozen ? 0.25F : state.watched ? 0.6F : 0.85F + 0.15F * (float) Math.sin(age * 0.2F),
                new WatcherModel(context.bakeLayer(WatcherModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(WatcherRenderState state) { return TEXTURE; }

    @Override
    public WatcherRenderState createRenderState() { return new WatcherRenderState(); }

    @Override
    public void extractRenderState(HollowWatcherEntity entity, WatcherRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int action = entity.action();
        state.reach = action == HollowWatcherEntity.ACT_GRIP || action == HollowWatcherEntity.ACT_GRIP_WINDUP ? 1.0F
                : action == HollowWatcherEntity.ACT_CLAW ? 0.7F : 0.0F;
        state.scream = action == HollowWatcherEntity.ACT_SCREAM ? 1.0F : 0.0F;
        state.frozen = entity.frozen();
        state.watched = entity.watched();
        state.triggered = entity.triggered();
        if (state.frozen || state.watched) state.walkAnimationSpeed = 0.0F;
    }
}

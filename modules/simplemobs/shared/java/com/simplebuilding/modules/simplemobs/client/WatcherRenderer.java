package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.FearNet;
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

    static {
        FearNet.fovSupplier = () -> net.minecraft.client.Minecraft.getInstance().options.fov().get();
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
        state.calm = net.minecraft.util.Mth.lerp(partialTicks, entity.calmO, entity.calm);
        state.shoulder = shoulder(entity);
        if (state.frozen || state.watched) state.walkAnimationSpeed = 0.0F;
    }

    /** Close behind the local player (within ~4.5 blocks, outside the view): 0..1 by nearness and how far behind it is. */
    private static float shoulder(HollowWatcherEntity entity) {
        var me = net.minecraft.client.Minecraft.getInstance().player;
        if (me == null || !entity.triggered() || entity.frozen()) return 0.0F;
        double dx = entity.getX() - me.getX(), dz = entity.getZ() - me.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5 || dist > 4.5 || Math.abs(entity.getY() - me.getY()) > 3.0) return 0.0F;
        var look = me.getLookAngle();
        double lh = Math.sqrt(look.x * look.x + look.z * look.z);
        if (lh < 1.0e-3) return 0.0F;
        double cos = (look.x * dx + look.z * dz) / (lh * dist);
        float behind = net.minecraft.util.Mth.clamp((float) (-cos - 0.2) / 0.4F, 0.0F, 1.0F);
        float near = net.minecraft.util.Mth.clamp((4.5F - (float) dist) / 1.5F, 0.0F, 1.0F);
        return behind * near;
    }
}

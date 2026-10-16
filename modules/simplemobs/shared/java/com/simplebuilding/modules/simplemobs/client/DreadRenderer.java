package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.DreadEntity;
import com.simplebuilding.modules.simplemobs.FearLogic;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class DreadRenderer extends MobRenderer<DreadEntity, DreadRenderState, DreadModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/dread/dread.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/dread/dread_glow.png");

    public DreadRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadModel(context.bakeLayer(DreadModel.LAYER)), 0.9F);
        // soul flames behind the visor flicker; in bright light they burn low
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> GLOW,
                (state, age) -> (state.weak ? 0.35F : 0.8F) + 0.2F * (float) Math.sin(age * 0.3F) * (float) Math.sin(age * 0.11F),
                new DreadModel(context.bakeLayer(DreadModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(DreadRenderState state) { return TEXTURE; }

    @Override
    public DreadRenderState createRenderState() { return new DreadRenderState(); }

    @Override
    public void extractRenderState(DreadEntity entity, DreadRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.swing = pose(entity, DreadEntity.ACT_SWING, FearLogic.SWING_WINDUP - 2, partialTicks);
        state.stomp = pose(entity, DreadEntity.ACT_STOMP, FearLogic.STOMP_WINDUP - 4, partialTicks);
        int rise = entity.riseTicks();
        state.rise = rise > 0 ? FearLogic.ease(1.0F - Math.max(0.0F, rise - partialTicks) / FearLogic.RISE_TICKS) : 1.0F;
        // the chain trails the arm by 3 ticks while it lifts; after the strike it keeps swinging and settles
        float lagged = Math.max(pose(entity, DreadEntity.ACT_SWING, FearLogic.SWING_WINDUP - 2, partialTicks - 3.0F),
                pose(entity, DreadEntity.ACT_STOMP, FearLogic.STOMP_WINDUP - 4, partialTicks - 3.0F));
        float now = Math.max(state.swing, state.stomp);
        state.chainDrag = (lagged - now) * 0.9F
                + (entity.action() == DreadEntity.ACT_NONE ? (FearLogic.chainPendulum(entity.clientSinceAction() + partialTicks) * 0.5F
                + FearLogic.chainPendulum(entity.clientRattleSince() + partialTicks) * 0.3F) : 0.0F);
        state.weak = entity.weakened();
    }

    /** 0..1 pose of an action: eased wind-up while it runs, then an eased recovery instead of snapping back. */
    private static float pose(DreadEntity entity, int kind, int windup, float partialTicks) {
        int action = entity.action();
        if (action == kind) return FearLogic.ease((entity.clientActionAge() + partialTicks) / windup);
        if (action == DreadEntity.ACT_NONE && entity.clientRecoverKind() == kind && entity.clientRecoverTicks() > 0) {
            return FearLogic.ease((entity.clientRecoverTicks() - partialTicks) / FearLogic.RECOVER_TICKS);
        }
        return 0.0F;
    }
}

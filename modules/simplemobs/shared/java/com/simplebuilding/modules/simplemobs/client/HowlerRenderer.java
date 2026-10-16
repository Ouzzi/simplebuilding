package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.HowlerEntity;
import com.simplebuilding.modules.simplemobs.HowlerLogic;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class HowlerRenderer extends MobRenderer<HowlerEntity, HowlerRenderState, HowlerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler_glow.png");
    private static final Identifier HEART = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler_heart.png");
    private static final Identifier ANTENNAE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler_antennae.png");

    public HowlerRenderer(EntityRendererProvider.Context context) {
        super(context, new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), 0.8F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> GLOW, (state, age) -> state.heartGlow,
                new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), RenderTypes::eyes, false));
        // glowing heart on the rib plate: same beat envelope as the glow layer (min 0.45, faster per phase, off on death)
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> HEART, (state, age) -> state.heartGlow,
                new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), RenderTypes::eyes, false));
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> ANTENNAE, (state, age) -> state.tendril,
                new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(HowlerRenderState state) { return TEXTURE; }

    /** No tipping over on death: the model plays its own 2 s collapse. */
    @Override
    protected float getFlipDegrees() { return 0.0F; }

    @Override
    public HowlerRenderState createRenderState() { return new HowlerRenderState(); }

    @Override
    public void extractRenderState(HowlerEntity entity, HowlerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int bite = entity.biteTicks(), shriek = entity.shriekTicks(), emerge = entity.emergeTicks();
        float base = entity.phase() == HowlerLogic.Phase.ONE ? 0.0f : 0.35f, open = base;
        if (bite > 0) open = Math.max(open, HowlerLogic.biteJaw(HowlerLogic.BITE_WINDUP_TICKS - bite + partialTicks));
        else open = Math.max(-0.15f, open + entity.snapRecoil(partialTicks) * 1.0f);
        state.shriek = shriek > 0 ? Math.min(1.0f, shriek / 6.0f) : 0.0f;
        // telegraphs: arms spread for the ground shockwave, head low with jaws wide for the charge
        HowlerLogic.Attack attack = entity.attack();
        if (attack == HowlerLogic.Attack.SHOCKWAVE) state.shriek = Math.max(state.shriek, 0.7f);
        if (attack == HowlerLogic.Attack.CHARGE) open = 1.0f;
        state.jawOpen = Math.max(open, state.shriek);
        state.tendril = entity.tendrilAnimation(partialTicks);
        float linear = emerge > 0 ? 1.0f - Math.max(0.0f, emerge - partialTicks) / HowlerLogic.EMERGE_TICKS : 1.0f;
        state.emerge = HowlerLogic.emergeHeight(linear);
        state.emergeTwitch = linear > 0.3f && linear < 0.55f ? 0.12f * Mth.sin(state.ageInTicks * 2.7f) * Mth.sin(state.ageInTicks * 1.1f) : 0.0f;
        state.seed = entity.getId() * 1.7f;
        state.charging = attack == HowlerLogic.Attack.CHARGE && state.walkAnimationSpeed > 0.3f;
        int stagger = entity.staggerLeft();
        state.staggerAge = stagger > 0 ? HowlerLogic.STAGGER_TICKS - stagger + partialTicks : -1.0f;
        state.death = entity.isDeadOrDying() ? Math.min(1.0f, (entity.deathTime + partialTicks) / HowlerLogic.DEATH_TICKS) : 0.0f;
        if (state.death > 0.0f) state.jawOpen = Math.max(state.jawOpen, Mth.sin(state.death * Mth.HALF_PI));
        int cycle = HowlerLogic.heartbeatCycle(entity.tickCount, entity.getId(), entity.phase());
        state.heartGlow = HowlerLogic.heartGlow(cycle + partialTicks) * (1.0f - state.death);
    }
}

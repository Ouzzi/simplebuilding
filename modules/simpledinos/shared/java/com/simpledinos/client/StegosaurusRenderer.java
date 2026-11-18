package com.simpledinos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpledinos.DinoVariants;
import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Stegosaurus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Stegosaurus (a-dinorest2): one texture per coat (rearing biome, {@link DinoVariants}) and armor tier, like the T. rex and the Triceratops;
 * the model grows with the hitbox (age scale). The plow is drawn while worn, lowered or raised (synchronized), the tail swipes on hits.
 */
public class StegosaurusRenderer extends MobRenderer<Stegosaurus, StegosaurusRenderer.State, StegosaurusModel> {
    /** N89A: every tier name by its code (DinosRegistry.ARMOR_TIER_NAMES); only the tiers this dinosaur wears have a texture. */
    private static final String[] TIERS = com.simpledinos.DinosRegistry.ARMOR_TIER_NAMES.toArray(String[]::new);
    /** [coat][armor tier]: textures/entity/stegosaurus/<coat>/<tier>.png. */
    private static final Identifier[][] TEXTURES = DinoVariants.COATS.stream()
            .map(c -> java.util.Arrays.stream(TIERS).map(t -> SimpleDinos.id("textures/entity/stegosaurus/" + c.name() + "/" + t + ".png")).toArray(Identifier[]::new))
            .toArray(Identifier[][]::new);

    public static class State extends LivingEntityRenderState {
        public boolean saddled;
        public int armorTier;
        public int coat;
        public float headTuck;
        public float tailBend;
        /** Plow worn; its lift 0 (lowered) .. 1 (raised); the tail swipe 0..1..0. */
        public boolean plow;
        public float plowLift;
        public float swipe;
    }

    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new StegosaurusModel(context.bakeLayer(StegosaurusModel.LAYER)), 1.3F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES[DinoVariants.clamp(state.coat)][Math.max(0, Math.min(TIERS.length - 1, state.armorTier))];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Stegosaurus entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.armorTier = entity.armorTier();
        state.coat = entity.coat();
        state.headTuck = entity.headTuck(partialTicks);
        state.tailBend = entity.tailBend(partialTicks);
        state.plow = entity.hasPlow();
        state.plowLift = entity.plowLift(partialTicks);
        state.swipe = entity.swipe(partialTicks);
        state.scale *= Stegosaurus.MODEL_SCALE;
    }

    /** The model grows with the hitbox (Vanilla's renderer does not apply the age scale); the feet stay on the ground. */
    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Stegosaurus entity, float partialTicks) {
        return entity.getBoundingBox().inflate(3.0, 0.5, 3.0);
    }
}

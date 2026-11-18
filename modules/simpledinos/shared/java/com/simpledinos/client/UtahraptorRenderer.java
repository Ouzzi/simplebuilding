package com.simpledinos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simpledinos.DinoVariants;
import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Utahraptor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Utahraptor (b-utah48): one texture per coat (rearing biome, {@link DinoVariants}) and armor tier, like the other dinosaurs; the model
 * grows with the hitbox (age scale). At a wall (climbing or sliding) the whole raptor turns up around its hips (concept: body about 60 degrees
 * raised, arms forward).
 */
public class UtahraptorRenderer extends MobRenderer<Utahraptor, UtahraptorRenderer.State, UtahraptorModel> {
    /** N89A: every tier name by its code (DinosRegistry.ARMOR_TIER_NAMES); only the tiers this dinosaur wears have a texture. */
    private static final String[] TIERS = com.simpledinos.DinosRegistry.ARMOR_TIER_NAMES.toArray(String[]::new);
    /** [coat][armor tier]: textures/entity/utahraptor/<coat>/<tier>.png. */
    private static final Identifier[][] TEXTURES = DinoVariants.COATS.stream()
            .map(c -> java.util.Arrays.stream(TIERS).map(t -> SimpleDinos.id("textures/entity/utahraptor/" + c.name() + "/" + t + ".png")).toArray(Identifier[]::new))
            .toArray(Identifier[][]::new);
    /** Climb pose: the body turns up by this much (radians, about 60 degrees) around the hips. */
    public static final float CLIMB_PITCH = 1.05F;
    /** Hip pivot of the climb pose, grown up, in model blocks (the render scale is applied before the rotations): height, behind the centre. */
    private static final float HIP_Y = 25.0F / 16.0F, HIP_Z = 3.0F / 16.0F;

    public static class State extends LivingEntityRenderState {
        public boolean saddled;
        public int armorTier;
        public int coat;
        public float headTuck;
        public float tailBend;
        /** The climb pose 0..1. */
        public float climb;
    }

    public UtahraptorRenderer(EntityRendererProvider.Context context) {
        super(context, new UtahraptorModel(context.bakeLayer(UtahraptorModel.LAYER)), 0.6F);
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
    public void extractRenderState(Utahraptor entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.armorTier = entity.armorTier();
        state.coat = entity.coat();
        state.headTuck = entity.headTuck(partialTicks);
        state.tailBend = entity.tailBend(partialTicks);
        state.climb = entity.climbPose(partialTicks);
        state.scale *= Utahraptor.MODEL_SCALE;
    }

    /** The climb pose turns the raptor up around its hips (front toward the sky). */
    @Override
    protected void setupRotations(State state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.climb > 0.0F) {
            float y = HIP_Y * state.ageScale, z = HIP_Z * state.ageScale;
            poseStack.translate(0.0F, y, z);
            poseStack.rotate(Axis.XP, CLIMB_PITCH * state.climb);
            poseStack.translate(0.0F, -y, -z);
        }
    }

    /** The model grows with the hitbox (Vanilla's renderer does not apply the age scale); the feet stay on the ground. */
    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Utahraptor entity, float partialTicks) {
        return entity.getBoundingBox().inflate(2.5, 1.0, 2.5);
    }
}

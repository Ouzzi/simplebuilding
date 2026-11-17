package com.simpledinos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpledinos.DinoVariants;
import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Parasaurolophus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Parasaurolophus (a-dinorest3): one texture per coat (rearing biome, {@link DinoVariants}) and armor tier, like the other dinosaurs; the
 * model grows with the hitbox (age scale); the call pose comes from the synchronized call event.
 */
public class ParasaurolophusRenderer extends MobRenderer<Parasaurolophus, ParasaurolophusRenderer.State, ParasaurolophusModel> {
    /** N89A: every tier name by its code (DinosRegistry.ARMOR_TIER_NAMES); only the tiers this dinosaur wears have a texture. */
    private static final String[] TIERS = com.simpledinos.DinosRegistry.ARMOR_TIER_NAMES.toArray(String[]::new);
    /** [coat][armor tier]: textures/entity/parasaurolophus/<coat>/<tier>.png. */
    private static final Identifier[][] TEXTURES = DinoVariants.COATS.stream()
            .map(c -> java.util.Arrays.stream(TIERS).map(t -> SimpleDinos.id("textures/entity/parasaurolophus/" + c.name() + "/" + t + ".png")).toArray(Identifier[]::new))
            .toArray(Identifier[][]::new);

    public static class State extends LivingEntityRenderState {
        public boolean saddled;
        public int armorTier;
        public int coat;
        public float headTuck;
        public float tailBend;
        /** The call pose 0..1 (head up, bill open). */
        public float call;
    }

    public ParasaurolophusRenderer(EntityRendererProvider.Context context) {
        super(context, new ParasaurolophusModel(context.bakeLayer(ParasaurolophusModel.LAYER)), 1.1F);
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
    public void extractRenderState(Parasaurolophus entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.armorTier = entity.armorTier();
        state.coat = entity.coat();
        state.headTuck = entity.headTuck(partialTicks);
        state.tailBend = entity.tailBend(partialTicks);
        state.call = entity.callPose(partialTicks);
        state.scale *= Parasaurolophus.MODEL_SCALE;
    }

    /** The model grows with the hitbox (Vanilla's renderer does not apply the age scale); the feet stay on the ground. */
    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Parasaurolophus entity, float partialTicks) {
        return entity.getBoundingBox().inflate(3.0, 1.0, 3.0);
    }
}

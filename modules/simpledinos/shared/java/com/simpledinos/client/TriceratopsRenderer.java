package com.simpledinos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpledinos.DinoVariants;
import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Triceratops;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Triceratops (a-dinorest1): one texture per coat (rearing biome, {@link DinoVariants}) and armor tier, like the T. rex; the model grows
 * with the hitbox (age scale). The rider's own client lowers the head while the jump key charges the run (the charge is known only
 * there), every client during the run (synchronized).
 */
public class TriceratopsRenderer extends MobRenderer<Triceratops, TriceratopsRenderer.State, TriceratopsModel> {
    private static final String[] TIERS = {"plain", "leather", "iron", "diamond"};
    /** [coat][armor tier]: textures/entity/triceratops/<coat>/<tier>.png. */
    private static final Identifier[][] TEXTURES = DinoVariants.COATS.stream()
            .map(c -> java.util.Arrays.stream(TIERS).map(t -> SimpleDinos.id("textures/entity/triceratops/" + c.name() + "/" + t + ".png")).toArray(Identifier[]::new))
            .toArray(Identifier[][]::new);

    public static class State extends LivingEntityRenderState {
        public boolean saddled;
        public int armorTier;
        public int coat;
        public float headTuck;
        public float tailBend;
        /** 1 during the run, 0 otherwise (smoothed); the rider's charge of the jump key 0..1. */
        public float ram;
        public float charge;
    }

    public TriceratopsRenderer(EntityRendererProvider.Context context) {
        super(context, new TriceratopsModel(context.bakeLayer(TriceratopsModel.LAYER)), 1.3F);
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
    public void extractRenderState(Triceratops entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.armorTier = entity.armorTier();
        state.coat = entity.coat();
        state.headTuck = entity.headTuck(partialTicks);
        state.tailBend = entity.tailBend(partialTicks);
        state.ram = entity.ramAnim(partialTicks);
        var player = Minecraft.getInstance().player;
        state.charge = player != null && player.getControlledVehicle() == entity ? player.getJumpRidingScale() : 0.0F;
        state.scale *= Triceratops.MODEL_SCALE;
    }

    /** The model grows with the hitbox (Vanilla's renderer does not apply the age scale); the feet stay on the ground. */
    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Triceratops entity, float partialTicks) {
        return entity.getBoundingBox().inflate(3.5, 1.0, 3.5);
    }
}

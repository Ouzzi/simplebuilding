package com.simpledinos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpledinos.DinoVariants;
import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Trex;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * T. rex: one texture per coat (rearing biome, {@link DinoVariants}) and armor tier (plain, leather, iron, diamond); the saddle and the armor plates are model parts that
 * show when worn. The culling box is far larger than the 3 x 4 block hitbox, otherwise head and tail vanish when the core
 * leaves the screen (concept 3.7).
 */
public class TrexRenderer extends MobRenderer<Trex, TrexRenderer.State, TrexModel> {
    private static final String[] TIERS = {"plain", "leather", "iron", "diamond"};
    /** [coat][armor tier]: textures/entity/trex/<coat>/<tier>.png. */
    private static final Identifier[][] TEXTURES = DinoVariants.COATS.stream()
            .map(c -> java.util.Arrays.stream(TIERS).map(t -> SimpleDinos.id("textures/entity/trex/" + c.name() + "/" + t + ".png")).toArray(Identifier[]::new))
            .toArray(Identifier[][]::new);

    static final float MODEL_SCALE = Trex.MODEL_SCALE;

    public static class State extends LivingEntityRenderState {
        public boolean saddled;
        public int armorTier;
        public int coat;
        /** a-dino36b: head pulled in 0..1 and tail bend in steps, as chosen by the server (part boxes). */
        public float headTuck;
        public float tailBend;
    }

    public TrexRenderer(EntityRendererProvider.Context context) {
        super(context, new TrexModel(context.bakeLayer(TrexModel.LAYER)), 1.65F);
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
    public void extractRenderState(Trex entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddled = entity.isSaddled();
        state.armorTier = entity.armorTier();
        state.coat = entity.coat();
        state.headTuck = entity.headTuck(partialTicks);
        state.tailBend = entity.tailBend(partialTicks);
        // TREX32 T1 (owner 01.11.): model drawn 25 % smaller so the turning head stays near the 3.6 x 3.5 hitbox.
        state.scale *= MODEL_SCALE;
    }

    /**
     * a-dino36b: the model grows with the hitbox. A hatchling's hitbox is 25 % of the size ({@code getAgeScale}), but Vanilla's renderer
     * does not apply the age scale, so hatchlings were drawn full size and stuck in walls and players. Scaled here (feet stay on the
     * ground); the shadow already follows the age scale ({@code MobRenderer}).
     */
    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Trex entity, float partialTicks) {
        return entity.getBoundingBox().inflate(7.0, 1.0, 7.0);
    }
}

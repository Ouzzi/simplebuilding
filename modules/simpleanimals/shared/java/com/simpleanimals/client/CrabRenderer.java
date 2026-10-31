package com.simpleanimals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpleanimals.AnimalVariants;
import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Crab;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * Crab with two models (owner choice 31.10.): model A in two shell colours (red, coral) and model B, the fiddler crab, in
 * its own colour. The model is picked per animal like the Vanilla cow models ({@code CowRenderer}).
 */
public class CrabRenderer extends MobRenderer<Crab, CrabRenderer.State, CrabModel> {
    private static final Identifier[] TEXTURES = AnimalVariants.CRAB.stream()
            .map(v -> SimpleAnimals.id("textures/entity/crab/" + v.name() + ".png")).toArray(Identifier[]::new);
    private static final Identifier FIDDLER = SimpleAnimals.id("textures/entity/crab/fiddler.png");
    private final CrabModel crab, fiddler;

    public static class State extends LivingEntityRenderState {
        public int variant;
        public int model;
    }

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER)), 0.3F);
        this.crab = this.model;
        this.fiddler = new CrabModel(context.bakeLayer(CrabModel.FIDDLER_LAYER));
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.model == Crab.MODEL_FIDDLER ? FIDDLER : TEXTURES[AnimalVariants.clamp(AnimalVariants.CRAB, state.variant)];
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = state.model == Crab.MODEL_FIDDLER ? this.fiddler : this.crab;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Crab entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
        state.model = entity.model();
    }
}

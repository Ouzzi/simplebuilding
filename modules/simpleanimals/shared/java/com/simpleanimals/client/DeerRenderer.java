package com.simpleanimals.client;

import com.simpleanimals.AnimalVariants;
import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Deer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Deer with coat variants by biome ({@link AnimalVariants}). */
public class DeerRenderer extends MobRenderer<Deer, DeerRenderer.State, DeerModel> {
    private static final Identifier[] TEXTURES = AnimalVariants.DEER.stream()
            .map(v -> SimpleAnimals.id("textures/entity/deer/" + v.name() + ".png")).toArray(Identifier[]::new);
    private static final Identifier FAWN = SimpleAnimals.id("textures/entity/deer/fawn.png");

    public static class State extends LivingEntityRenderState {
        public int variant;
        public boolean antlers;
    }

    public DeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeerModel(context.bakeLayer(DeerModel.LAYER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.isBaby ? FAWN : TEXTURES[AnimalVariants.clamp(AnimalVariants.DEER, state.variant)];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Deer entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
        state.antlers = entity.hasAntlers();
    }
}

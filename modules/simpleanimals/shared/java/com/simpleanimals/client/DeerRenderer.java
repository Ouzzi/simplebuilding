package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Deer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Deer with two coat variants (red, fallow) and the spotted fawn. */
public class DeerRenderer extends MobRenderer<Deer, DeerRenderer.State, DeerModel> {
    private static final Identifier[] TEXTURES = {SimpleAnimals.id("textures/entity/deer/red.png"), SimpleAnimals.id("textures/entity/deer/fallow.png")};
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
        return state.isBaby ? FAWN : TEXTURES[state.variant == 1 ? 1 : 0];
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

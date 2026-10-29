package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import com.simpleanimals.entity.Boar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Boar with two coat variants (dark, light) and the striped piglet. */
public class BoarRenderer extends MobRenderer<Boar, BoarRenderer.State, BoarModel> {
    private static final Identifier[] TEXTURES = {SimpleAnimals.id("textures/entity/boar/dark.png"), SimpleAnimals.id("textures/entity/boar/light.png")};
    private static final Identifier YOUNG = SimpleAnimals.id("textures/entity/boar/young.png");

    public static class State extends LivingEntityRenderState {
        public int variant;
    }

    public BoarRenderer(EntityRendererProvider.Context context) {
        super(context, new BoarModel(context.bakeLayer(BoarModel.LAYER)), 0.6F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.isBaby ? YOUNG : TEXTURES[state.variant == 1 ? 1 : 0];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Boar entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
    }
}

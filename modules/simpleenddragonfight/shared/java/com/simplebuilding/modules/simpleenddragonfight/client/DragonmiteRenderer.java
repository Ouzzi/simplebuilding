package com.simplebuilding.modules.simpleenddragonfight.client;

import com.simplebuilding.modules.simpleenddragonfight.Dragonmite;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Dragonmite with glowing magenta eyes (emissive layer). */
public class DragonmiteRenderer extends MobRenderer<Dragonmite, DragonmiteRenderState, DragonmiteModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simpleenddragonfight", "textures/entity/dragonmite/dragonmite.png");
    private static final Identifier EYES = Identifier.fromNamespaceAndPath("simpleenddragonfight", "textures/entity/dragonmite/dragonmite_eyes.png");

    public DragonmiteRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonmiteModel(context.bakeLayer(DragonmiteModel.LAYER)), 0.4F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES, (state, age) -> 1.0F,
                new DragonmiteModel(context.bakeLayer(DragonmiteModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    protected float getFlipDegrees() { return 180.0F; }

    @Override
    public Identifier getTextureLocation(DragonmiteRenderState state) { return TEXTURE; }

    @Override
    public DragonmiteRenderState createRenderState() { return new DragonmiteRenderState(); }

    @Override
    public void extractRenderState(Dragonmite entity, DragonmiteRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.phase = (entity.getId() * 1.618F) % (Mth.PI * 2.0F);
        state.lunge = Mth.sin(entity.getSwingAnimation(partialTicks) * Mth.PI);
    }
}

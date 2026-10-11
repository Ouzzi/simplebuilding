package com.simplebuilding.modules.simpleenddragonfight.client;

import com.simplebuilding.modules.simpleenddragonfight.Dragonmite;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Dragonmite with glowing magenta eyes (emissive layer). */
public class DragonmiteRenderer extends MobRenderer<Dragonmite, LivingEntityRenderState, DragonmiteModel> {
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
    public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }

    @Override
    public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
}

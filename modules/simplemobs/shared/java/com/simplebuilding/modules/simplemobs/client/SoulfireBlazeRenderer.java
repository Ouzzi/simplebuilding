package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Soulfire Blaze: the Blaze model with its own soul-fire texture. */
public class SoulfireBlazeRenderer extends BlazeRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/soulfire_blaze/soulfire_blaze.png");

    public SoulfireBlazeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}

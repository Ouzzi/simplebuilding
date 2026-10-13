package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;

/** Blighted Wither: the Vanilla Wither model with its own blighted skin (built from the Wither skin, never committed). */
public class BlightedWitherRenderer extends WitherBossRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/blighted_wither/blighted_wither.png");

    public BlightedWitherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(WitherRenderState state) {
        int invulnerable = (int) Math.floor(state.invulnerableTicks);
        boolean flash = invulnerable > 0 && (invulnerable > 80 || invulnerable / 5 % 2 != 1);
        return flash ? super.getTextureLocation(state) : TEXTURE;
    }
}

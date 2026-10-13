package com.simplebuilding.modules.simplemobs.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;

/** Blighted and Abyssal Wither: the Vanilla Wither model with their own skins (built from the Wither skin, never committed). */
public class BlightedWitherRenderer extends WitherBossRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/blighted_wither/blighted_wither.png");
    private static final Identifier ABYSSAL = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/blighted_wither/abyssal_wither.png");
    private final Identifier texture;

    public BlightedWitherRenderer(EntityRendererProvider.Context context) {
        this(context, TEXTURE);
    }

    private BlightedWitherRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context);
        this.texture = texture;
    }

    /** The Abyssal Wither (Enderite stage): its own end-violet skin. */
    public static BlightedWitherRenderer abyssal(EntityRendererProvider.Context context) {
        return new BlightedWitherRenderer(context, ABYSSAL);
    }

    @Override
    public Identifier getTextureLocation(WitherRenderState state) {
        int invulnerable = (int) Math.floor(state.invulnerableTicks);
        boolean flash = invulnerable > 0 && (invulnerable > 80 || invulnerable / 5 % 2 != 1);
        return flash ? super.getTextureLocation(state) : texture;
    }
}

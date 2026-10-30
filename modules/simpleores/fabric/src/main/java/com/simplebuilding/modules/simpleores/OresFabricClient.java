package com.simplebuilding.modules.simpleores;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import com.simpleores.entity.SilverArrow;

/** Fabric client entry: the silver arrow is drawn with Vanilla's arrow model and its own (Vanilla-derived) texture. */
public final class OresFabricClient implements ClientModInitializer {
    private static final Identifier TEXTURE = SimpleOres.id("textures/entity/projectiles/silver_arrow.png");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(OresRegistry.SILVER_ARROW_ENTITY, SilverArrowRenderer::new);
    }

    static final class SilverArrowRenderer extends ArrowRenderer<SilverArrow, ArrowRenderState> {
        SilverArrowRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        protected Identifier getTextureLocation(ArrowRenderState state) {
            return TEXTURE;
        }

        @Override
        public ArrowRenderState createRenderState() {
            return new ArrowRenderState();
        }
    }
}

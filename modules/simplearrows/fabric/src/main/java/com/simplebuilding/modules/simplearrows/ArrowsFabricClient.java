package com.simplebuilding.modules.simplearrows;

import com.simplearrows.ArrowsRegistry;
import com.simplearrows.SimpleArrows;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** Fabric client entry: the arrows are drawn with Vanilla's arrow model (Light/Silver with their own Vanilla-derived texture); the Fletching Table screen. */
public final class ArrowsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ArrowsRegistry.SILVER_ARROW_ENTITY, c -> new TexturedArrowRenderer<>(c, "silver_arrow"));
        EntityRendererRegistry.register(ArrowsRegistry.LIGHT_ARROW_ENTITY, c -> new TexturedArrowRenderer<>(c, "light_arrow"));
        // The Fletching Table's arrows fly like a Vanilla arrow; its menu opens the table screen (b-arrows03, from SimpleBuilding).
        EntityRendererRegistry.register(com.simplearrows.fletching.Fletchings.CRAFTED_ARROW_ENTITY, com.simplearrows.client.CraftedArrowRenderer::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.simplearrows.fletching.Fletchings.MENU, com.simplearrows.client.FletchingScreen::new);
        // Real-mouse test of the Fletching Table's potion slot (inert without its request file in the game folder).
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.simplearrows.client.FletchingProbe::tick);
    }

    static final class TexturedArrowRenderer<T extends AbstractArrow> extends ArrowRenderer<T, ArrowRenderState> {
        private final Identifier texture;

        TexturedArrowRenderer(EntityRendererProvider.Context context, String name) {
            super(context);
            this.texture = SimpleArrows.id("textures/entity/projectiles/" + name + ".png");
        }

        @Override
        protected Identifier getTextureLocation(ArrowRenderState state) {
            return this.texture;
        }

        @Override
        public ArrowRenderState createRenderState() {
            return new ArrowRenderState();
        }
    }
}

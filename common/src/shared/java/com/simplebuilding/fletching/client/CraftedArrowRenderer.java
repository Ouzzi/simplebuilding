package com.simplebuilding.fletching.client;

import com.simplebuilding.fletching.CraftedArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** Fliegt und steckt wie ein Vanilla-Pfeil; die Teile zeigt das Item, nicht das Geschoss. */
public class CraftedArrowRenderer extends ArrowRenderer<CraftedArrow, ArrowRenderState> {
    public CraftedArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TippableArrowRenderer.NORMAL_ARROW_LOCATION;
    }
}

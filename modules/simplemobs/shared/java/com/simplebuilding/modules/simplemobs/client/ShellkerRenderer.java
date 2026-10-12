package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.ShellkerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.shulker.ShulkerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Shulker;

/**
 * Shellker: the Shulker model in a bedrock shell. The lid only lifts to a thin slit while the Shellker is open and a
 * violet eye glows inside; closed it looks like a block of bedrock.
 */
public class ShellkerRenderer extends ShulkerRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/shellker/shellker.png");
    private static final Identifier EYES = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/shellker/shellker_eyes.png");
    /** Model peek of the fully open slit (Vanilla opens to 1.0): about 1.5 px. */
    public static final float SLIT = 0.2F;

    public ShellkerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES,
                (state, age) -> state instanceof ShellkerRenderState s ? s.openness : 0.0F,
                new ShulkerModel(context.bakeLayer(ModelLayers.SHULKER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(ShulkerRenderState state) {
        return TEXTURE;
    }

    @Override
    public ShulkerRenderState createRenderState() {
        return new ShellkerRenderState();
    }

    @Override
    public void extractRenderState(Shulker entity, ShulkerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float peek = entity.getClientPeekAmount(partialTicks);
        float open = Math.min(1.0F, peek / (ShellkerEntity.OPEN_PEEK * 0.01F));
        state.peekAmount = open * SLIT;
        if (state instanceof ShellkerRenderState s) s.openness = open;
    }
}

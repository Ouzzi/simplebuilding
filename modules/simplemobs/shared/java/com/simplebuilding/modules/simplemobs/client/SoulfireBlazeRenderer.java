package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.SoulfireBlaze;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Blaze;

/** Soulfire Blaze: the Blaze model with its own soul-fire texture. */
public class SoulfireBlazeRenderer extends BlazeRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/soulfire_blaze/soulfire_blaze.png");

    public SoulfireBlazeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(Blaze entity, LivingEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // the rods spin faster while the Blaze charges soul fire (own clock, so the pace changes without a jump)
        if (entity instanceof SoulfireBlaze soul) state.ageInTicks = soul.animTime(partialTicks);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}

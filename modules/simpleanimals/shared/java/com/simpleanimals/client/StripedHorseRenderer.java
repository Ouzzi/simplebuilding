package com.simpleanimals.client;

import com.simpleanimals.SimpleAnimals;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Zebra and zorse: the compact {@link ZebraModel} with the Vanilla horse saddle and armor layers (these keep the Vanilla horse
 * shape, a little off the deeper body). The foal is the same model at half size (the entity's age scale).
 */
public class StripedHorseRenderer<T extends AbstractHorse> extends AbstractHorseRenderer<T, EquineRenderState, ZebraModel> {
    public static final Identifier ZEBRA = SimpleAnimals.id("textures/entity/zebra.png");
    public static final Identifier ZORSE = SimpleAnimals.id("textures/entity/zorse.png");
    private final Identifier texture;

    public StripedHorseRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new ZebraModel(context.bakeLayer(ZebraModel.LAYER)), new ZebraModel(context.bakeLayer(ZebraModel.LAYER)));
        this.texture = texture;
        this.addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY,
                state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)), null, 2));
        this.addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_SADDLE,
                state -> state.saddle, new EquineSaddleModel(context.bakeLayer(ModelLayers.HORSE_SADDLE)), null, 2));
    }

    @Override
    public Identifier getTextureLocation(EquineRenderState state) {
        return texture;
    }

    @Override
    public EquineRenderState createRenderState() {
        return new EquineRenderState();
    }
}

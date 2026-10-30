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
 * Zebra and zorse: the Vanilla horse model, saddle and armor layers with an own texture on the Vanilla UV layout. The foal is
 * the same model at half size (the entity's age scale).
 */
public class StripedHorseRenderer<T extends AbstractHorse> extends AbstractHorseRenderer<T, EquineRenderState, HorseModel> {
    public static final Identifier ZEBRA = SimpleAnimals.id("textures/entity/zebra.png");
    public static final Identifier ZORSE = SimpleAnimals.id("textures/entity/zorse.png");
    private final Identifier texture;

    public StripedHorseRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new HorseModel(context.bakeLayer(ModelLayers.HORSE)), new HorseModel(context.bakeLayer(ModelLayers.HORSE)));
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

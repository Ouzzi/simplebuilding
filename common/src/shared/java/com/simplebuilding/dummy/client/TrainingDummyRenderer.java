package com.simplebuilding.dummy.client;

import com.simplebuilding.dummy.DummySkins;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Vanillas Ruestungsstaender-Modell mit Stroh-Textur; je Entity-Art eine Textur (Stroh-Staender, Trainingspuppe).
 * Die Puppe mit Spielernamen (2026-10-09) schlaegt die Haut wie ein Vanilla-Spielerkopf nach
 * ({@link PlayerSkinRenderCache#lookup}, nicht blockierend); bis sie da ist oder ohne Treffer bleibt die normale Puppe.
 */
public class TrainingDummyRenderer extends ArmorStandRenderer {
    private final Identifier texture;
    private final boolean stuffed;
    private final PlayerSkinRenderCache skins;
    private final Map<String, ResolvableProfile> profiles = new HashMap<>();

    public TrainingDummyRenderer(EntityRendererProvider.Context context, String name, boolean stuffed) {
        super(context);
        this.texture = Identifier.fromNamespaceAndPath("simplebuilding", "textures/entity/training_dummy/" + name + ".png");
        this.stuffed = stuffed;
        this.skins = context.getPlayerSkinRenderCache();
        if (stuffed) {
            this.addLayer(new DummySkinLayer(this));
            this.addLayer(new DummyStuffingLayer(this));
        }
    }

    public static EntityRendererProvider<ArmorStand> straw() {
        return context -> new TrainingDummyRenderer(context, "straw_armor_stand", false);
    }

    public static EntityRendererProvider<ArmorStand> dummy() {
        return context -> new TrainingDummyRenderer(context, "training_dummy", true);
    }

    @Override
    public ArmorStandRenderState createRenderState() {
        return new DummyRenderState();
    }

    @Override
    public void extractRenderState(ArmorStand stand, ArmorStandRenderState state, float partialTick) {
        super.extractRenderState(stand, state, partialTick);
        if (!(state instanceof DummyRenderState dummy)) {
            return;
        }
        dummy.skin = null;
        if (this.stuffed) {
            DummySkins.playerName(stand.getCustomName()).ifPresent(name -> {
                if (this.profiles.size() > 256) {
                    this.profiles.clear();
                }
                ResolvableProfile profile = this.profiles.computeIfAbsent(name, ResolvableProfile::createUnresolved);
                dummy.skin = this.skins.lookup(profile).getNow(java.util.Optional.empty()).orElse(null);
            });
        }
    }

    @Override
    public Identifier getTextureLocation(ArmorStandRenderState state) {
        return this.texture;
    }
}

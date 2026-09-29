package com.simplebuilding.tweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Modelle und Texturen der Mod-Koepfe (Lohenkopf, Endermankopf) fuer {@code SkullModelMixin}.
 *
 * <p>Beide Koepfe zeigen die <b>echten Vanilla-Mob-Texturen</b> (Besitzer 2026-09-29: der frueher
 * handgezeichnete Endermankopf sah falsch aus) und verweisen per Resource-Location darauf, statt Pixel
 * zu kopieren - Ressourcenpakete, die Lohe oder Enderman umfaerben, faerben damit auch die Koepfe.
 * <ul>
 *   <li>Lohenkopf: {@code minecraft:textures/entity/blaze/blaze.png}. Der Kopf der Lohe ist in
 *   {@code BlazeModel} genau Vanillas Mob-Kopf-Wuerfel (8x8x8 bei UV 0,0, Textur 64x32), also reicht
 *   {@link SkullModel#createMobHeadLayer()} wie beim Creeper-Kopf.</li>
 *   <li>Endermankopf: {@code minecraft:textures/entity/enderman/enderman.png} mit dem Kopf aus
 *   {@code EndermanModel}: Kopfwuerfel bei UV 0,0 (unten offen) plus der innere Kiefer-Wuerfel
 *   ("hat", UV 0,16, 0,5 px eingezogen). Dazu die leuchtende Augenschicht
 *   {@code textures/entity/enderman/enderman_eyes.png} wie Vanillas {@code EnderEyesLayer}
 *   ({@link RenderTypes#eyes}).</li>
 * </ul>
 */
public final class ModSkullModels {
    public static final Identifier BLAZE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/blaze/blaze.png");
    public static final Identifier ENDERMAN_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
    public static final Identifier ENDERMAN_EYES_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png");

    /**
     * Die Augenschicht sitzt um diesen Betrag (Modell-Pixel) vor dem Gesicht (nur in der Tiefe verlaengert,
     * seitlich deckungsgleich - die Augen stehen nicht ueber den Rand). Der Kopf selbst wird mit
     * {@code entityCutoutZOffset} gezeichnet, das ihn per View-Offset um Abstand/4096 zur Kamera
     * zieht; Vanillas {@code eyes}-Typ hat diesen Versatz nicht und laege sonst knapp hinter dem
     * Gesicht (Tiefentest verloren). 0,25 px reichen bis ueber 64 Bloecke Abstand und sind von vorn
     * nicht zu sehen.
     */
    private static final float EYES_INFLATE = 0.25F;

    private static RenderType endermanEyes;

    private ModSkullModels() {
    }

    public static Identifier texture(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> BLAZE_TEXTURE;
            case ENDERMAN -> ENDERMAN_TEXTURE;
        };
    }

    public static SkullModelBase createModel(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> new SkullModel(SkullModel.createMobHeadLayer().bakeRoot());
            case ENDERMAN -> new EndermanSkullModel(endermanHeadLayer().bakeRoot(), endermanEyesLayer().bakeRoot());
        };
    }

    /** Kopf aus {@code EndermanModel#createBodyLayer}: Kopfwuerfel + eingezogener Kiefer, Textur 64x32. */
    public static LayerDefinition endermanHeadLayer() {
        MeshDefinition mesh = SkullModel.createHeadModel();
        mesh.getRoot().getChild("head").addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(-0.5F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Nur der Kopfwuerfel, nach vorn/hinten um EYES_INFLATE verlaengert (seitlich exakt): traegt die Augen aus {@code enderman_eyes.png} (UV wie der Kopf). */
    public static LayerDefinition endermanEyesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F, 0.0F, EYES_INFLATE)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Zweiter Durchgang nach dem Kopf (Block, Item/GUI und getragen - alle laufen ueber
     * {@code SkullBlockRenderer#submitSkull}): die leuchtenden Augen wie {@code EyesLayer}
     * ({@code order(1)}, damit sie nach dem Kopf gezeichnet werden).
     */
    public static void submitGlow(SkullModelBase model, SkullModelBase.State state, PoseStack poseStack,
                                  SubmitNodeCollector collector, int lightCoords, int outlineColor) {
        if (!(model instanceof EndermanSkullModel enderman)) {
            return;
        }
        if (endermanEyes == null) {
            endermanEyes = RenderTypes.eyes(ENDERMAN_EYES_TEXTURE);
        }
        McClientVersion.submitModel(collector.order(1), enderman.eyes(), state, poseStack, endermanEyes, lightCoords,
                OverlayTexture.NO_OVERLAY, outlineColor);
    }

    /** Endermankopf: das Kopfmodell traegt das Augenmodell mit, damit {@link #submitGlow} es findet. */
    public static final class EndermanSkullModel extends SkullModel {
        private final SkullModel eyes;

        EndermanSkullModel(ModelPart root, ModelPart eyesRoot) {
            super(root);
            this.eyes = new SkullModel(eyesRoot);
        }

        public SkullModel eyes() {
            return eyes;
        }
    }
}

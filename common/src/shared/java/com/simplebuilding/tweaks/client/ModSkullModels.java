package com.simplebuilding.tweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.version.McClientVersion;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Modelle und Texturen der Mod-Koepfe fuer {@code SkullModelMixin}.
 *
 * <p>Alle Koepfe zeigen die <b>echten Vanilla-Mob-Texturen</b> (Besitzer 2026-09-29) und verweisen per
 * Resource-Location darauf, statt Pixel zu kopieren - Ressourcenpakete, die einen Mob umfaerben, faerben damit
 * auch seinen Kopf. Die Geometrie ist jeweils der Kopf aus dem Vanilla-Entity-Modell (UV-Versatz und
 * Texturgroesse von dort), auf die Kopf-Grundlinie (Unterkante y = 0) geschoben:
 * <ul>
 *   <li>Lohe: {@code BlazeModel}-Kopf = Vanillas Mob-Kopf-Wuerfel (UV 0,0, 64x32), wie der Creeper-Kopf.</li>
 *   <li>Enderman: Kopf + eingezogener Kiefer aus {@code EndermanModel}, dazu die leuchtenden Augen
 *   ({@code enderman_eyes.png}, {@link RenderTypes#eyes}) wie {@code EnderEyesLayer}.</li>
 *   <li>Wuestenzombie: Menschenkopf mit Hut-Schicht (64x64) wie der Zombiekopf.</li>
 *   <li>Ertrunkener: Menschenkopf (64x64) plus die aeussere Schicht {@code drowned_outer_layer.png}
 *   (0,25 aufgeblaeht wie {@code DROWNED_OUTER_LAYER}).</li>
 *   <li>Spinne/Hoehlenspinne: {@code SpiderModel}-Kopf (UV 32,4, 64x32), Augen {@code spider_eyes.png} wie
 *   {@code SpiderEyesLayer} (die Hoehlenspinne benutzt dieselben).</li>
 *   <li>Eiswanderer/Sumpfskelett: Skelettkopf (64x32) plus Kleidungsschicht {@code stray_overlay.png} (0,25) bzw.
 *   {@code bogged_overlay.png} (0,2) wie {@code SkeletonClothingLayer}; das Sumpfskelett mit seinen Pilzen aus
 *   {@code BoggedModel}.</li>
 *   <li>Schleim: innerer Wuerfel mit Augen und Mund (ausgeschnitten) plus der aeussere Wuerfel durchscheinend
 *   ({@link RenderTypes#entityTranslucent}) wie {@code SlimeOuterLayer}.</li>
 *   <li>Silberfischchen: die zwei vorderen Koerpersegmente aus {@code SilverfishModel} in Originalgroesse (bis
 *   2026-10-02 doppelt so gross, dem Besitzer viel zu gross).</li>
 *   <li>Breeze: Kopf mit Stirnband aus {@code BreezeModel} (32x32), Augen {@code breeze_eyes.png} mit
 *   {@link RenderTypes#breezeEyes}.</li>
 *   <li>Shulker: Schale (Deckel halb offen) und Kopf aus {@code ShulkerModel}, halb so gross.</li>
 * </ul>
 */
public final class ModSkullModels {
    public static final Identifier BLAZE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/blaze/blaze.png");
    public static final Identifier ENDERMAN_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
    public static final Identifier ENDERMAN_EYES_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png");
    public static final Identifier HUSK_TEXTURE = Identifier.withDefaultNamespace("textures/entity/zombie/husk.png");
    public static final Identifier DROWNED_TEXTURE = Identifier.withDefaultNamespace("textures/entity/zombie/drowned.png");
    public static final Identifier DROWNED_OUTER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png");
    public static final Identifier SPIDER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/spider/spider.png");
    public static final Identifier CAVE_SPIDER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/spider/cave_spider.png");
    public static final Identifier SPIDER_EYES_TEXTURE = Identifier.withDefaultNamespace("textures/entity/spider/spider_eyes.png");
    public static final Identifier STRAY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/stray.png");
    public static final Identifier STRAY_OVERLAY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/stray_overlay.png");
    public static final Identifier BOGGED_TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged.png");
    public static final Identifier BOGGED_OVERLAY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png");
    public static final Identifier SLIME_TEXTURE = Identifier.withDefaultNamespace("textures/entity/slime/slime.png");
    public static final Identifier SILVERFISH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/silverfish/silverfish.png");
    public static final Identifier BREEZE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/breeze/breeze.png");
    public static final Identifier BREEZE_EYES_TEXTURE = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png");
    public static final Identifier SHULKER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/shulker/shulker.png");

    /**
     * Augenschichten sitzen um diesen Betrag (Modell-Pixel) vor dem Gesicht (nur in der Tiefe verlaengert,
     * seitlich deckungsgleich - die Augen stehen nicht ueber den Rand). Der Kopf selbst wird mit
     * {@code entityCutoutZOffset} gezeichnet, das ihn per View-Offset um Abstand/4096 zur Kamera zieht;
     * Vanillas {@code eyes}-Typ hat diesen Versatz nicht und laege sonst knapp hinter dem Gesicht (Tiefentest
     * verloren). 0,25 px reichen bis ueber 64 Bloecke Abstand und sind von vorn nicht zu sehen.
     */
    private static final float EYES_INFLATE = 0.25F;

    private ModSkullModels() {
    }

    public static Identifier texture(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> BLAZE_TEXTURE;
            case ENDERMAN -> ENDERMAN_TEXTURE;
            case HUSK -> HUSK_TEXTURE;
            case SPIDER -> SPIDER_TEXTURE;
            case CAVE_SPIDER -> CAVE_SPIDER_TEXTURE;
            case STRAY -> STRAY_TEXTURE;
            case BOGGED -> BOGGED_TEXTURE;
            case SLIME -> SLIME_TEXTURE;
            case SILVERFISH -> SILVERFISH_TEXTURE;
            case BREEZE -> BREEZE_TEXTURE;
            case SHULKER -> SHULKER_TEXTURE;
            case DROWNED -> DROWNED_TEXTURE;
        };
    }

    public static SkullModelBase createModel(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> new SkullModel(SkullModel.createMobHeadLayer().bakeRoot());
            case ENDERMAN -> layered(endermanHeadLayer(), layer(endermanEyesLayer(), RenderTypes::eyes, ENDERMAN_EYES_TEXTURE));
            case HUSK -> new SkullModel(SkullModel.createHumanoidHeadLayer().bakeRoot());
            case DROWNED -> layered(SkullModel.createHumanoidHeadLayer(),
                    layer(humanoidHeadLayer(new CubeDeformation(0.25F), 64, 64), RenderTypes::entityCutoutZOffset, DROWNED_OUTER_TEXTURE));
            case SPIDER, CAVE_SPIDER -> layered(spiderHeadLayer(0.0F), layer(spiderHeadLayer(EYES_INFLATE), RenderTypes::eyes, SPIDER_EYES_TEXTURE));
            case STRAY -> layered(SkullModel.createMobHeadLayer(),
                    layer(humanoidHeadLayer(new CubeDeformation(0.25F), 64, 32), RenderTypes::entityCutoutZOffset, STRAY_OVERLAY_TEXTURE));
            case BOGGED -> layered(boggedHeadLayer(),
                    layer(humanoidHeadLayer(new CubeDeformation(0.2F), 64, 32), RenderTypes::entityCutoutZOffset, BOGGED_OVERLAY_TEXTURE));
            case SLIME -> layered(slimeInnerLayer(), layer(slimeOuterLayer(), RenderTypes::entityTranslucent, SLIME_TEXTURE));
            case SILVERFISH -> new SkullModel(silverfishHeadLayer().bakeRoot());
            case BREEZE -> layered(breezeHeadLayer(0.0F), layer(breezeHeadLayer(EYES_INFLATE), RenderTypes::breezeEyes, BREEZE_EYES_TEXTURE));
            case SHULKER -> new SkullModel(shulkerHeadLayer().bakeRoot());
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Geometrie
    // ---------------------------------------------------------------------------------------------

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

    /** Menschenkopf wie {@code HumanoidModel#createMesh}: Kopf (UV 0,0) und Hut (UV 32,0, 0,5 weiter aufgeblaeht). */
    private static LayerDefinition humanoidHeadLayer(CubeDeformation deformation, int width, int height) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation), PartPose.ZERO);
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation.extend(0.5F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, width, height);
    }

    /** Kopf aus {@code SpiderModel} (UV 32,4, Textur 64x32); {@code depth} verlaengert ihn nur nach vorn/hinten (Augen). */
    private static LayerDefinition spiderHeadLayer(float depth) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(32, 4).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F, 0.0F, depth)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Skelettkopf (64x32) mit den Pilzen aus {@code BoggedModel#createBodyLayer} (dort am Kopf, hier genauso). */
    private static LayerDefinition boggedHeadLayer() {
        MeshDefinition mesh = SkullModel.createHeadModel();
        PartDefinition mushrooms = mesh.getRoot().getChild("head").addOrReplaceChild("mushrooms", CubeListBuilder.create(), PartPose.ZERO);
        mushrooms.addOrReplaceChild("red_mushroom_1",
                CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) (Math.PI / 4), 0.0F));
        mushrooms.addOrReplaceChild("red_mushroom_2",
                CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0), 0.0F));
        mushrooms.addOrReplaceChild("brown_mushroom_1",
                CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) (Math.PI / 4), 0.0F));
        mushrooms.addOrReplaceChild("brown_mushroom_2",
                CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0), 0.0F));
        mushrooms.addOrReplaceChild("brown_mushroom_3",
                CubeListBuilder.create().texOffs(50, 28).addBox(-3.0F, -4.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, (float) (Math.PI / 4)));
        mushrooms.addOrReplaceChild("brown_mushroom_4",
                CubeListBuilder.create().texOffs(50, 28).addBox(-3.0F, -4.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, (float) (Math.PI * 3.0 / 4.0)));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Innerer Schleim aus {@code SlimeModel#createInnerBodyLayer}, um 24 nach oben auf die Kopf-Grundlinie geschoben. */
    private static LayerDefinition slimeInnerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(32, 0).addBox(-3.25F, -6.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(32, 4).addBox(1.25F, -6.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(32, 8).addBox(0.0F, -3.0F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Aeusserer Schleimwuerfel aus {@code SlimeModel#createOuterBodyLayer} (UV 0,0), durchscheinend gezeichnet. */
    private static LayerDefinition slimeOuterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Die zwei vorderen Segmente aus {@code SilverfishModel} (Kopf 3x2x2 bei UV 0,0, Hals 4x3x2 bei UV 0,4) in
     * Originalgroesse, Kopf nach Norden wie bei allen Koepfen: zusammen 4 breit, 3 hoch, 4 tief - so klein wie am
     * Silberfischchen selbst, im Inventar, auf dem Kopf und abgestellt (Besitzer 2026-10-02; vorher doppelt so gross).
     */
    private static LayerDefinition silverfishHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("segment0", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        head.addOrReplaceChild("segment1", CubeListBuilder.create().texOffs(0, 4).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Kopf mit Stirnband aus {@code BreezeModel} (32x32); {@code depth} verlaengert nur nach vorn/hinten (Augen). */
    private static LayerDefinition breezeHeadLayer(float depth) {
        MeshDefinition mesh = new MeshDefinition();
        CubeDeformation deformation = new CubeDeformation(0.0F, 0.0F, depth);
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(4, 24).addBox(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F, deformation)
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /**
     * Shulker aus {@code ShulkerModel} (64x64): Unterschale (UV 0,28), Deckel (UV 0,0) acht Pixel angehoben wie
     * beim Hervorschauen, dazwischen der Kopf (UV 0,52) - alles halb so gross, damit es zu einem Kopf passt.
     */
    private static LayerDefinition shulkerHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO.withScale(0.5F));
        head.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 8.0F, 16.0F), PartPose.ZERO);
        head.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 12.0F, 16.0F),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        head.addOrReplaceChild("shulker_head", CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // ---------------------------------------------------------------------------------------------
    // Zusaetzliche Durchgaenge (Augen, Kleidung, durchscheinender Schleim)
    // ---------------------------------------------------------------------------------------------

    /** Ein zusaetzlicher Durchgang: Modell und Render-Typ (Augen leuchten, Kleidung ausgeschnitten, Schleim durchscheinend). */
    public record Layer(SkullModel model, RenderType renderType) {
    }

    private static Layer layer(LayerDefinition definition, Function<Identifier, RenderType> type, Identifier texture) {
        return new Layer(new SkullModel(definition.bakeRoot()), type.apply(texture));
    }

    private static SkullModelBase layered(LayerDefinition base, Layer... layers) {
        return new LayeredSkullModel(base.bakeRoot(), List.of(layers));
    }

    /**
     * Zweiter Durchgang nach dem Kopf (Block, Item/GUI und getragen - alle laufen ueber
     * {@code SkullBlockRenderer#submitSkull}): die Schichten des Kopfes ({@code order(1)}, damit sie nach dem Kopf
     * gezeichnet werden).
     */
    public static void submitLayers(SkullModelBase model, SkullModelBase.State state, PoseStack poseStack,
                                    SubmitNodeCollector collector, int lightCoords, int outlineColor) {
        if (!(model instanceof LayeredSkullModel layered)) {
            return;
        }
        for (Layer layer : layered.layers()) {
            McClientVersion.submitModel(collector.order(1), layer.model(), state, poseStack, layer.renderType(), lightCoords,
                    OverlayTexture.NO_OVERLAY, outlineColor);
        }
    }

    /** Kopfmodell, das seine zusaetzlichen Schichten mittraegt, damit {@link #submitLayers} sie findet. */
    public static final class LayeredSkullModel extends SkullModel {
        private final List<Layer> layers;

        LayeredSkullModel(ModelPart root, List<Layer> layers) {
            super(root);
            this.layers = layers;
        }

        public List<Layer> layers() {
            return layers;
        }
    }
}

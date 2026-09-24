package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Der getragene Rucksack auf dem Ruecken von Spielern (und Mannequins).
 *
 * <p>Das Modell ist das des platzierten Rucksacks ({@code block/template_backpack}): Korpus, Deckel,
 * Vordertasche, Griff und zwei Riemen, um die x-Achse gedreht - die Tasche zeigt vom Spieler weg,
 * die Riemen liegen am Ruecken - und auf 80 % verkleinert, damit es so breit ist wie der Oberkoerper.
 * Die Texturen {@code textures/entity/backpack/*.png} setzt {@code tools/textures/generate_textures.py}
 * Pixel fuer Pixel aus den Blockflaechen derselben Stufe zusammen; die Quader hier und
 * {@code BACKPACK_ENTITY_BOXES} dort muessen uebereinstimmen.
 *
 * <p>Ein gefaerbter Rucksack ({@code minecraft:dyed_color}, siehe {@link DyedStorage}) wird wie
 * Vanillas Lederruestung in zwei Durchgaengen gezeichnet: die Leder-Ebene {@code *_dyed} mit der
 * Farbe multipliziert, darueber {@code *_dyed_overlay} mit Umriss, Riemen und Beschlaegen ungefaerbt.
 * Beide Ebenen ergaenzen sich pixelgenau, es gibt also nichts, was sich ueberdeckt.
 *
 * <p>Der Rucksack traegt keine Ausruestungs-Grafik ({@code EQUIPPABLE} ohne {@code asset_id}), daher
 * liefert der Render-Zustand ihn nicht als {@code chestEquipment}. Die Ebene fragt stattdessen die
 * Entity ueber {@link AvatarRenderState#id} im Client-Level - fuer Spieler in der Welt ebenso wie fuer
 * das Spielerbild im Inventar.
 *
 * <p>Registriert wird die Ebene je Loader auf allen Avatar-Renderern (Fabric
 * {@code LivingEntityRenderLayerRegistrationCallback} bzw. auf 1.21.11
 * {@code LivingEntityFeatureRendererRegistrationCallback}, NeoForge und Forge
 * {@code EntityRenderersEvent.AddLayers}).
 */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** Verkleinerung des Blockmodells: 10 Pixel breit werden 8, so breit wie der Oberkoerper. */
    public static final float SCALE = 0.8F;

    private static final Identifier[] TEXTURES = {
            texture("backpack"), texture("reinforced_backpack"), texture("netherite_backpack"), texture("enderite_backpack")};
    /** Gefaerbt: das Leder in Grau, wird mit der Farbe multipliziert ... */
    private static final Identifier[] DYED_TEXTURES = {
            texture("backpack_dyed"), texture("reinforced_backpack_dyed"), texture("netherite_backpack_dyed"),
            texture("enderite_backpack_dyed")};
    /** ... und alles andere (Umriss, Riemen, Schnallen, Beschlaege) ungefaerbt. */
    private static final Identifier[] DYED_OVERLAY_TEXTURES = {
            texture("backpack_dyed_overlay"), texture("reinforced_backpack_dyed_overlay"),
            texture("netherite_backpack_dyed_overlay"), texture("enderite_backpack_dyed_overlay")};

    private final ModelPart model = createLayer().bakeRoot();

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/entity/backpack/" + name + ".png");
    }

    /**
     * Die Quader des Blockmodells ({@code von}/{@code bis} in Blockpixeln) als Entity-Quader:
     * x - 8 (mittig), y von oben (14 - y2, Entity-y zeigt nach unten), z von den Riemen aus
     * (12 - z2, die Riemen liegen bei 0 am Ruecken).
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 5.0F, 1.0F, 10.0F, 9.0F, 6.0F), PartPose.ZERO);
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 15).addBox(-5.0F, 1.0F, 1.0F, 10.0F, 4.0F, 7.0F), PartPose.ZERO);
        root.addOrReplaceChild("pocket", CubeListBuilder.create().texOffs(34, 0).addBox(-4.0F, 7.0F, 7.0F, 8.0F, 6.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(34, 8).addBox(-2.0F, 0.0F, 4.0F, 4.0F, 1.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("strap_left", CubeListBuilder.create().texOffs(34, 10).addBox(-4.0F, 3.0F, 0.0F, 2.0F, 10.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("strap_right", CubeListBuilder.create().texOffs(44, 10).addBox(2.0F, 3.0F, 0.0F, 2.0F, 10.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state,
                       float yRot, float xRot) {
        ItemStack worn = wornBackpack(state);
        if (worn.isEmpty()) {
            return;
        }
        int tier = ((BackpackItem) worn.getItem()).getTier().ordinal();
        int dyeColor = DyedStorage.colour(worn);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        // Die Riemen liegen an der Rueckseite des Oberkoerpers (z = 2 Pixel), der Griff auf Schulterhoehe.
        poseStack.translate(0.0F, 0.0F, 2.0F / 16.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        if (dyeColor == DyedStorage.UNDYED) {
            submitNodeCollector.submitModelPart(this.model, poseStack, RenderTypes.entityCutout(TEXTURES[tier]), lightCoords,
                    overlay, null);
        } else {
            submitNodeCollector.submitModelPart(this.model, poseStack, RenderTypes.entityCutout(DYED_TEXTURES[tier]), lightCoords,
                    overlay, null, ARGB.opaque(dyeColor), null);
            submitNodeCollector.submitModelPart(this.model, poseStack, RenderTypes.entityCutout(DYED_OVERLAY_TEXTURES[tier]),
                    lightCoords, overlay, null);
        }
        poseStack.popPose();
    }

    /**
     * Die Textur des Rucksacks, den die Entity hinter {@code state} traegt, oder null. Bei einem
     * gefaerbten Rucksack die Leder-Ebene ({@code *_dyed}); die Beschlag-Ebene kommt in
     * {@link #submit} dazu.
     */
    @Nullable
    public static Identifier textureFor(AvatarRenderState state) {
        ItemStack worn = wornBackpack(state);
        if (worn.isEmpty()) {
            return null;
        }
        int tier = ((BackpackItem) worn.getItem()).getTier().ordinal();
        return DyedStorage.colour(worn) == DyedStorage.UNDYED ? TEXTURES[tier] : DYED_TEXTURES[tier];
    }

    /** Der sichtbar getragene Rucksack der Entity hinter {@code state}, sonst ein leerer Stapel. */
    public static ItemStack wornBackpack(AvatarRenderState state) {
        if (state.isInvisible) {
            return ItemStack.EMPTY;
        }
        ItemStack chest = wornChest(state.id);
        return chest.getItem() instanceof BackpackItem ? chest : ItemStack.EMPTY;
    }

    /** Die Stufe des Rucksacks im Brust-Slot der Entity mit dieser Id, oder null. */
    @Nullable
    public static BackpackTier wornTier(int entityId) {
        ItemStack chest = wornChest(entityId);
        return chest.getItem() instanceof BackpackItem backpack ? backpack.getTier() : null;
    }

    private static ItemStack wornChest(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = level == null ? null : level.getEntity(entityId);
        return entity instanceof LivingEntity living ? living.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY;
    }
}

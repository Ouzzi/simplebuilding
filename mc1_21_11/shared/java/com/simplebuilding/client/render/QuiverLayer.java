package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.items.custom.QuiverItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Der getragene Koecher (Brust-Slot) auf dem Ruecken von Spielern (und Mannequins).
 *
 * <p>Gezeichnet wird das Item-Modell des Koechers selbst ({@code item/generated}, also die
 * extrudierte Pixelgrafik mit einem Pixel Tiefe) - flach wie in einem Rahmen, in z etwas gestreckt.
 * Die Grafik liegt schon diagonal im Bild (oben rechts die Pfeile, unten links der Boden); von hinten
 * gesehen laufen die Pfeile so ueber die rechte Schulter, der Boden sitzt an der linken Huefte.
 * {@link #TILT_DEGREES} stellt die 45 Grad der Grafik etwas steiler, auf die Diagonale des
 * Oberkoerpers (8 x 12 Pixel).
 *
 * <p>Wie beim {@link BackpackLayer}: Der Koecher traegt keine Ausruestungs-Grafik ({@code EQUIPPABLE}
 * ohne {@code asset_id}), der Render-Zustand liefert ihn also nicht als {@code chestEquipment}. Die
 * Ebene fragt die Entity ueber {@link AvatarRenderState#id} im Client-Level und loest das Item-Modell
 * beim Zeichnen auf - je Aufruf ein eigener {@link ItemStackRenderState}, weil der Collector die
 * Quad-Liste nur referenziert und erst spaeter zeichnet.
 *
 * <p>Registriert wird die Ebene neben dem Rucksack auf allen Avatar-Renderern. Rucksack und Koecher
 * teilen sich den Brust-Slot, es zeichnet also hoechstens eine der beiden Ebenen etwas.
 */
public class QuiverLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** 16 Pixel Grafik werden knapp 13 - die Diagonale reicht so von der Schulter bis zur Huefte. */
    public static final float SCALE = 0.8F;
    /** Streckung in z: aus einem Pixel Tiefe werden 1,5 (vor {@link #SCALE}). */
    public static final float DEPTH = 1.5F;
    /** Zusaetzliche Neigung gegen den Uhrzeigersinn (von hinten gesehen) auf die 45 Grad der Grafik. */
    public static final float TILT_DEGREES = 10.0F;
    /** Mitte des Koechers hinter der Rueckenflaeche (z = 2 Pixel), mit etwas Luft gegen Durchstechen. */
    private static final float BACK_OFFSET = 3.0F / 16.0F;

    public QuiverLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state,
                       float yRot, float xRot) {
        LivingEntity wearer = wearer(state);
        if (wearer == null) {
            return;
        }
        ItemStackRenderState item = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(item, wearer.getItemBySlot(EquipmentSlot.CHEST),
                ItemDisplayContext.NONE, wearer);
        if (item.isEmpty()) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        // Mitte des Oberkoerpers (y = 6 Pixel, Entity-y zeigt nach unten), hinter dem Ruecken.
        poseStack.translate(0.0F, 6.0F / 16.0F, BACK_OFFSET);
        // 180 Grad um die Vorwaertsachse: Modell-y zeigt nach unten und Modell-x nach links, das
        // Item-Modell erwartet beides umgekehrt. So sieht man die Grafik von hinten aufrecht und
        // ungespiegelt; TILT_DEGREES kommt obendrauf.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + TILT_DEGREES));
        poseStack.scale(SCALE, SCALE, SCALE * DEPTH);
        // Das Item-Modell fuellt den Einheitswuerfel; seine Mitte auf den Ursprung.
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        item.submit(poseStack, submitNodeCollector, lightCoords, overlay, state.outlineColor);
        poseStack.popPose();
    }

    /** Der sichtbar getragene Koecher der Entity hinter {@code state}, sonst ein leerer Stapel. */
    public static ItemStack wornQuiver(AvatarRenderState state) {
        LivingEntity wearer = wearer(state);
        return wearer == null ? ItemStack.EMPTY : wearer.getItemBySlot(EquipmentSlot.CHEST);
    }

    /** Die Entity hinter {@code state}, wenn sie sichtbar ist und einen Koecher im Brust-Slot traegt. */
    @Nullable
    private static LivingEntity wearer(AvatarRenderState state) {
        if (state.isInvisible) {
            return null;
        }
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = level == null ? null : level.getEntity(state.id);
        return entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof QuiverItem
                ? living : null;
    }
}

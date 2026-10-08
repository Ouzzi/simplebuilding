package com.simplebuilding.blueprint;

import org.joml.Quaternionf;

/**
 * Reine Logik der 3D-Vorschau im Blaupausen-Editor: Drehen beim Ziehen, Verschieben bei gehaltener
 * Strg-Taste, Zoomen mit dem Mausrad, Zuruecksetzen der Ansicht.
 *
 * <p>Bewusst ohne jede Minecraft-Client-Abhaengigkeit (nur JOML), damit die ganze Zahlenlogik ohne
 * Spiel-Boot getestet werden kann - der Test liegt in {@code src/test/java}. Der Bildschirm hlt
 * genau ein Exemplar und reicht die Mausbewegungen durch; das Zeichnen bleibt in
 * {@code BlueprintView}.
 */
public final class BlueprintViewControls {

    /** Bogenmass pro GUI-Pixel beim Ziehen. Waagerecht um die Bild-Hochachse, senkrecht um die Querachse. */
    public static final float DRAG_RAD_PER_PIXEL = 0.012f;

    /** Zoomfaktor je Mausrad-Schritt; derselbe Wert wie seit jeher im Editor. */
    public static final double ZOOM_STEP = 1.15;

    /** Zoomgrenzen: unten passt das Bauwerk immer noch ins Rueckchen, oben bleibt es erkennbar. */
    public static final double ZOOM_MIN = 0.2;
    public static final double ZOOM_MAX = 12.0;

    /** Hoechstverschiebung in GUI-Pixeln, damit die Ansicht nicht aus dem Fenster wandert. */
    public static final float PAN_LIMIT = 96f;

    private final Quaternionf rotation = defaultRotation();
    private float zoom = 1f;
    private float panX;
    private float panY;

    /**
     * Standard-Blick: leicht von oben, von vorn rechts auf die Suedseite. Einzige Quelle fr diesen
     * Winkel - der Editor und das Zeichnen greifen beide hierher zu.
     */
    public static Quaternionf defaultRotation() {
        return new Quaternionf().rotateX((float) Math.toRadians(28)).rotateY((float) Math.toRadians(-35));
    }

    public Quaternionf rotation() {
        return rotation;
    }

    public float zoom() {
        return zoom;
    }

    public float panX() {
        return panX;
    }

    public float panY() {
        return panY;
    }

    /** Ziehen ohne Strg: die Ansicht dreht sich wie eine Kugel unter der Hand. */
    public void drag(double dx, double dy) {
        rotation.set(new Quaternionf()
                .rotateX((float) (dy * DRAG_RAD_PER_PIXEL))
                .rotateY((float) (dx * DRAG_RAD_PER_PIXEL))
                .mul(rotation));
    }

    /** Ziehen mit Strg: die Ansicht wandert mit dem Finger; die Drehung bleibt unangetastet. */
    public void pan(double dx, double dy) {
        panX = clamp(panX + (float) dx);
        panY = clamp(panY + (float) dy);
    }

    /** Mausrad ber der Ansicht: nach oben hinauszoomen, nach unten hinein. */
    public void zoom(double scrollY) {
        zoom = (float) Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * Math.pow(ZOOM_STEP, scrollY)));
    }

    /** Drehung, Zoom und Verschiebung auf den Ausgangszustand. */
    public void reset() {
        rotation.set(defaultRotation());
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    /** Ob die Ansicht schon genau so steht, wie sie neu startet. */
    public boolean isDefault() {
        return rotation.equals(defaultRotation()) && zoom == 1f && panX == 0f && panY == 0f;
    }

    private static float clamp(float value) {
        return Math.max(-PAN_LIMIT, Math.min(PAN_LIMIT, value));
    }
}

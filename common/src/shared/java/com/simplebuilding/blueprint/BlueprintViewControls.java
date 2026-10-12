package com.simplebuilding.blueprint;

import org.joml.Quaternionf;

/**
 * Reine Logik der 3D-Vorschau im Blaupausen-Editor: Drehen beim Ziehen (Drehteller), Verschieben bei gehaltener
 * Strg-Taste, Zoomen mit dem Mausrad, Zuruecksetzen der Ansicht.
 *
 * <p>Bewusst ohne jede Minecraft-Client-Abhaengigkeit (nur JOML), damit die ganze Zahlenlogik ohne
 * Spiel-Boot getestet werden kann - der Test liegt in {@code src/test/java}. Der Bildschirm hlt
 * genau ein Exemplar und reicht die Mausbewegungen durch; das Zeichnen bleibt in
 * {@code BlueprintView}.
 */
public final class BlueprintViewControls {

    /**
     * Bogenmass pro GUI-Pixel beim Ziehen. Waagerecht dreht um die Hochachse des Bauwerks (Drehteller),
     * senkrecht kippt um die Bild-Querachse.
     */
    public static final float DRAG_RAD_PER_PIXEL = 0.012f;

    /** Zoomfaktor je Mausrad-Schritt; derselbe Wert wie seit jeher im Editor. */
    public static final double ZOOM_STEP = 1.15;

    /** Zoomgrenzen: unten passt das Bauwerk immer noch ins Rueckchen, oben bleibt es erkennbar. */
    public static final double ZOOM_MIN = 0.2;
    public static final double ZOOM_MAX = 12.0;

    /** Hoechstverschiebung in GUI-Pixeln, damit die Ansicht nicht aus dem Fenster wandert. */
    public static final float PAN_LIMIT = 96f;

    /** Neigung (um die Bild-Querachse) und Drehung (um die Hochachse des Bauwerks) des Standard-Blicks. */
    public static final float DEFAULT_PITCH = (float) Math.toRadians(28);
    public static final float DEFAULT_YAW = (float) Math.toRadians(-35);

    /** Die Neigung bleibt zwischen senkrecht von oben und senkrecht von unten, damit nichts ueberschlaegt. */
    public static final float PITCH_LIMIT = (float) (Math.PI / 2);

    private float pitch = DEFAULT_PITCH;
    private float yaw = DEFAULT_YAW;
    private final Quaternionf rotation = defaultRotation();
    private float zoom = 1f;
    private float panX;
    private float panY;

    /**
     * Standard-Blick: leicht von oben, von vorn rechts auf die Suedseite. Einzige Quelle fr diesen
     * Winkel - der Editor und das Zeichnen greifen beide hierher zu.
     */
    public static Quaternionf defaultRotation() {
        return orbit(DEFAULT_PITCH, DEFAULT_YAW);
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

    /**
     * Ziehen ohne Strg: Drehteller statt Kugel (Queue N40). Waagerecht dreht das Bauwerk nur um seine
     * eigene Hochachse, senkrecht kippt es nur um die Bild-Querachse; beide Winkel werden getrennt
     * gefuehrt. Frueher wurde jede Mausbewegung als Bild-Drehung vorn an die Gesamtdrehung multipliziert:
     * weil der Standard-Blick schon geneigt ist, liegt die Bild-Hochachse schraeg zur Hochachse des
     * Bauwerks - ein gerades Ziehen nach rechts drehte es deshalb auch schraeg weg, und jedes kleine
     * Zittern sammelte sich als Rollen um die Blickachse an.
     */
    public void drag(double dx, double dy) {
        yaw = (float) ((yaw + dx * DRAG_RAD_PER_PIXEL) % (2 * Math.PI));
        pitch = Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, (float) (pitch + dy * DRAG_RAD_PER_PIXEL)));
        rotation.set(orbit(pitch, yaw));
    }

    public float pitch() {
        return pitch;
    }

    public float yaw() {
        return yaw;
    }

    /** Erst um die Hochachse des Bauwerks drehen, dann um die Bild-Querachse neigen. */
    public static Quaternionf orbit(float pitch, float yaw) {
        return new Quaternionf().rotateX(pitch).rotateY(yaw);
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
        pitch = DEFAULT_PITCH;
        yaw = DEFAULT_YAW;
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

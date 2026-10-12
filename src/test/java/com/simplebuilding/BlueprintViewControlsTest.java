package com.simplebuilding.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Die Zahlenlogik der Blaupausen-Vorschau: Drehen, Verschieben, Zoomen, Zaehlen, Zurueckstellen.
 *
 * <p>Die Klasse liegt bewusst ohne Minecraft-Client-Abhaengigkeit, deshalb laeuft dieser Test ohne
 * Spiel-Boot; der Fahrweg von der Maus bis hierher ist mit dem Client-Test
 * {@code BlueprintViewClientTest} abgedeckt.
 */
class BlueprintViewControlsTest {

    /** JOML rechnet in Floats; ein paar ULPen Abweichung sind kein Unterschied. */
    private static final float EPS = 1.0e-5f;

    @Test
    void startetAufDemStandardblick() {
        BlueprintViewControls controls = new BlueprintViewControls();

        assertTrue(controls.isDefault(), "eine frisch gebaute Vorschau ist die Voreinstellung");
        assertTrue(BlueprintViewControls.defaultRotation().equals(controls.rotation(), EPS),
                "die Drehung beginnt bei " + BlueprintViewControls.defaultRotation());
        assertEquals(1f, controls.zoom(), EPS, "Zoom beginnt bei 1");
        assertEquals(0f, controls.panX(), EPS, "x-Verschiebung beginnt bei 0");
        assertEquals(0f, controls.panY(), EPS, "y-Verschiebung beginnt bei 0");
    }

    @Test
    void ziehenDrehtNurDieDrehung() {
        BlueprintViewControls controls = new BlueprintViewControls();
        double dx = 100;
        double dy = -40;

        controls.drag(dx, dy);

        Quaternionf expected = BlueprintViewControls.orbit(
                (float) (BlueprintViewControls.DEFAULT_PITCH + dy * BlueprintViewControls.DRAG_RAD_PER_PIXEL),
                (float) (BlueprintViewControls.DEFAULT_YAW + dx * BlueprintViewControls.DRAG_RAD_PER_PIXEL));
        assertTrue(expected.equals(controls.rotation(), EPS),
                () -> "Ziehen um " + dx + "/" + dy + " ergibt " + controls.rotation()
                        + ", erwartet " + expected);
        assertEquals(0f, controls.panX(), EPS, "Ziehen verschiebt nicht");
        assertEquals(0f, controls.panY(), EPS, "Ziehen verschiebt nicht");
        assertEquals(1f, controls.zoom(), EPS, "Ziehen zoomt nicht");
        assertFalse(controls.isDefault(), "nach einem Ziehen ist die Ansicht nicht mehr die Voreinstellung");
    }

    /** Queue N40: gerades Ziehen nach rechts dreht nur um die Hochachse des Bauwerks - keine Neigung, kein Rollen. */
    @Test
    void geradesZiehenDrehtNurUmDieHochachse() {
        BlueprintViewControls controls = new BlueprintViewControls();
        Vector3f upBefore = controls.rotation().transform(new Vector3f(0, 1, 0));
        float pitchBefore = controls.pitch();
        for (int i = 0; i < 200; i++) {
            controls.drag(7, 0);
        }
        Vector3f upAfter = controls.rotation().transform(new Vector3f(0, 1, 0));
        assertEquals(pitchBefore, controls.pitch(), EPS, "waagerechtes Ziehen neigt nicht");
        assertTrue(upBefore.equals(upAfter, EPS), () -> "die Hochachse des Bauwerks bleibt im Bild stehen: " + upBefore + " -> " + upAfter);
        assertNotEquals(BlueprintViewControls.DEFAULT_YAW, controls.yaw(), EPS, "waagerechtes Ziehen dreht");
    }

    /** Queue N40: senkrecht ziehen kippt nur; die Drehung um die Hochachse bleibt, und es ueberschlaegt nicht. */
    @Test
    void senkrechtesZiehenKipptNurUndBleibtInDenGrenzen() {
        BlueprintViewControls controls = new BlueprintViewControls();
        controls.drag(0, 10_000);
        assertEquals(BlueprintViewControls.DEFAULT_YAW, controls.yaw(), EPS, "senkrechtes Ziehen dreht nicht um die Hochachse");
        assertEquals(BlueprintViewControls.PITCH_LIMIT, controls.pitch(), EPS, "die Neigung endet senkrecht von oben");
        controls.drag(0, -20_000);
        assertEquals(-BlueprintViewControls.PITCH_LIMIT, controls.pitch(), EPS, "und senkrecht von unten");
    }

    /** Queue N40: hin und her ziehen (auch im Kreis) bringt die Ansicht genau zurueck - kein angesammeltes Rollen. */
    @Test
    void kreisZiehenSammeltKeinRollenAn() {
        BlueprintViewControls controls = new BlueprintViewControls();
        double[][] moves = {{30, 0}, {0, 20}, {-30, 0}, {0, -20}};
        for (int round = 0; round < 50; round++) {
            for (double[] m : moves) {
                controls.drag(m[0], m[1]);
            }
        }
        assertTrue(BlueprintViewControls.defaultRotation().equals(controls.rotation(), 1.0e-3f),
                () -> "nach 50 Kreisen steht die Ansicht bei " + controls.rotation());
    }

    @Test
    void ziehenOhneBewegungAendertNichts() {
        BlueprintViewControls controls = new BlueprintViewControls();
        Quaternionf before = new Quaternionf(controls.rotation());

        controls.drag(0, 0);

        assertTrue(before.equals(controls.rotation(), EPS), "ein Ziehen ohne Bewegung dreht nichts");
        assertTrue(controls.isDefault(), "ein Ziehen ohne Bewegung laesst die Voreinstellung stehen");
    }

    @Test
    void verschiebenBleibtBeiDerDrehungUndDemZoom() {
        BlueprintViewControls controls = new BlueprintViewControls();
        controls.drag(75, -30);
        controls.zoom(2);
        Quaternionf rotated = new Quaternionf(controls.rotation());
        float zoomed = controls.zoom();

        controls.pan(25, 15);

        assertTrue(rotated.equals(controls.rotation(), EPS),
                "Verschieben darf die Drehung nicht anfassen");
        assertEquals(zoomed, controls.zoom(), EPS, "Verschieben darf den Zoom nicht anfassen");
        assertEquals(25f, controls.panX(), EPS, "x wandert um den Ziehweg");
        assertEquals(15f, controls.panY(), EPS, "y wandert um den Ziehweg");
        assertFalse(controls.isDefault(), "eine verschobene Ansicht ist nicht die Voreinstellung");
    }

    @Test
    void verschiebenBleibtAufDemFeld() {
        BlueprintViewControls controls = new BlueprintViewControls();

        controls.pan(100_000, -100_000);

        assertEquals(BlueprintViewControls.PAN_LIMIT, controls.panX(), EPS,
                "die Verschiebung begrenzt, damit die Ansicht nicht aus dem Fenster wandert");
        assertEquals(-BlueprintViewControls.PAN_LIMIT, controls.panY(), EPS,
                "die Verschiebung begrenzt, damit die Ansicht nicht aus dem Fenster wandert");

        controls.pan(-100_000, 100_000);

        assertEquals(-BlueprintViewControls.PAN_LIMIT, controls.panX(), EPS, "auch nach unten begrenzt");
        assertEquals(BlueprintViewControls.PAN_LIMIT, controls.panY(), EPS, "auch nach oben begrenzt");
    }

    @Test
    void mausradBleibtZwischenDenGrenzen() {
        BlueprintViewControls controls = new BlueprintViewControls();

        controls.zoom(1);
        assertEquals((float) BlueprintViewControls.ZOOM_STEP, controls.zoom(), EPS,
                "ein Schritt nach oben zoomt um den Faktor");

        controls.zoom(-1);
        assertEquals(1f, controls.zoom(), EPS, "ein Schritt zurueck ist wieder der Ausgangszoom");

        for (int i = 0; i < 200; i++) {
            controls.zoom(10);
        }
        assertEquals((float) BlueprintViewControls.ZOOM_MAX, controls.zoom(), EPS,
                "der Zoom hat eine Obergrenze, die Vanilla nicht gefaehrdet");

        for (int i = 0; i < 200; i++) {
            controls.zoom(-10);
        }
        assertEquals((float) BlueprintViewControls.ZOOM_MIN, controls.zoom(), EPS,
                "der Zoom hat eine Untergrenze, damit das Bauwerk im Raster bleibt");
    }

    @Test
    void zurueckstellenStelltAllesHer() {
        BlueprintViewControls controls = new BlueprintViewControls();
        controls.drag(120, -60);
        controls.zoom(-3);
        controls.pan(40, -40);
        assertFalse(controls.isDefault(), "Vorbereitung: nichts davon ist die Voreinstellung");

        controls.reset();

        assertTrue(controls.isDefault(), "nach dem Zurueckstellen ist alles wieder Voreinstellung");
        assertTrue(BlueprintViewControls.defaultRotation().equals(controls.rotation(), EPS),
                "die Drehung steht wieder auf dem Standardblick");
        assertEquals(1f, controls.zoom(), EPS, "der Zoom steht wieder auf 1");
        assertEquals(0f, controls.panX(), EPS, "die x-Verschiebung steht wieder auf 0");
        assertEquals(0f, controls.panY(), EPS, "die y-Verschiebung steht wieder auf 0");
    }
}

package com.simplevisuals.config;

public class SimplevisualsConfig {

    public Visuals visuals = new Visuals();
    public Particles particles = new Particles();
    public static class Particles {
        public com.simplevisuals.effects.Intensity globalLevel = com.simplevisuals.effects.Intensity.SUBTLE;
        public java.util.Map<String, com.simplevisuals.effects.Intensity> overrides = new java.util.LinkedHashMap<>();
    }

    public enum SlideActivationMode {
        CAMERA, ALWAYS
    }

    public enum PickupLayout {
        ICON_NAME_COUNT,
        COUNT_ICON_NAME,
        NAME_ICON_COUNT,
        ICON_COUNT_NAME
    }

    public enum PickupSide {
        LEFT, RIGHT
    }



    public static class Visuals {

        public boolean enablePlayerLocator = true;

        public boolean enableChatHeads = true;

        public boolean enableStatusEffectBars = true;

        public boolean enableElytraPitchHelper = true;

        public float elytraTargetAngleUp = -40.0f;

        public float elytraTargetAngleDown = 40.0f;

        public float elytraPitchTolerance = 10.0f;

        public float elytraSensitivity = 4.0f;

        public boolean enableRenamedItemTextures = false;

        public boolean enableAnvilFormatting = true;

        public boolean enhanceDeathMessages = true;

        public enum DeathCoordsMode {
            DISABLED,
            APPEND,    // "Public" Style: Wird direkt an die Nachricht angehängt
            SEPARATE   // "Private" Style: Eigene Chat-Nachricht nur für dich
        }


        public DeathCoordsMode deathCoordsMode = DeathCoordsMode.SEPARATE;

        public boolean enableMapTooltips = true;

        public SpeedLines speedLines = new SpeedLines();

        // Unterkategorie: Pickup Notifier

        public PickupNotifier pickupNotifier = new PickupNotifier();

        public DamageIndicators damageIndicators = new DamageIndicators();

        public BiomeInfo biomeInfo = new BiomeInfo();

        public static class DamageIndicators {

            public boolean enable = true;

            public float scale = 1.0f;

            public int colorNormal = 0xFFFFFF; // Weiß

            public int colorSpecial = 0xFFD700; // Gold (für Smite/Arthropods)

            public boolean showBorder = true;
        }

        public static class BiomeInfo {

            public boolean enable = false;

            public int displayDuration = 60; // Wie lange die Nachricht bleibt (Ticks)

            public int cooldownSeconds = 60; // Wie lange ein Biom ignoriert wird nach Verlassen

            public int yOffset = 50; // Versatz von oben
        }

        public static class SpeedLines {

            public boolean enableSpeedLines = true;

            public int speedLinesColor = 0xFFFFFF;

            public float speedLinesAlpha = 0.7f;

            public float speedLinesAmount = 1.0f;

            public float speedLinesRadius = 0.7f;

            public float speedLinesWidth = 8.0f;

            public float speedLinesSpeed = 1.0f;

            public float speedLinesScale = 4.0f;

            public float speedThreshold = 0.6f;
        }

        public HeldItemTooltips heldItemTooltips = new HeldItemTooltips();

        public static class HeldItemTooltips {

            public boolean enable = true;

            public boolean showDurability = true;

            public boolean showEnchantments = true;

            public int maxEnchantments = 3; // Begrenzung, damit der Bildschirm nicht vollgespammt wird
        }

        public static class PickupNotifier {

            public boolean enablePickupNotifier = true;

            public int pickupNotifierOffsetX = 10;

            public int pickupNotifierOffsetY = 10;

            public float pickupNotifierScale = 1.0f;

            public int pickupNotifierDuration = 120; // Dauer Einstellung

            public boolean pickupNotifierShowXp = true;

            // NEU: Seite


            public PickupSide pickupNotifierSide = PickupSide.RIGHT;

            // Anordnung


            public PickupLayout pickupNotifierLayout = PickupLayout.COUNT_ICON_NAME;

            public boolean pickupShowItem = true;

            public boolean pickupShowName = true;

            public boolean pickupShowCount = true;

            // Stil

            public boolean pickupUseRarityColor = true;

            public boolean pickupVanillaStyle = true;

            // NEU: Hintergrund Opazität

            public float pickupBackgroundOpacity = 1.0f;
        }

    }
}

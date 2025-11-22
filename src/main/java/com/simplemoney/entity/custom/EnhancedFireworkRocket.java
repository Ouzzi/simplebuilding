package com.simplemoney.entity.custom;

import com.simplemoney.entity.ModEntities; // Importieren Sie Ihre Entity-Registrierungsklasse
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// Wir erweitern die Vanilla-Klasse
public class EnhancedFireworkRocket extends FireworkRocketEntity {

    // Ihre neuen Felder (nicht final, falls sie aus NBT/DataComponentTypes gelesen werden müssten)
    private int flightDuration;
    private double speedMultiplier;

    // --- 1. Der Basis-Konstruktor (Für das Laden der Entity aus der Welt) ---
    // Dieser Konstruktor wird immer benötigt.
    public EnhancedFireworkRocket(EntityType<? extends EnhancedFireworkRocket> entityType, World world) {
        super(entityType, world);
        // Setzen Sie Standardwerte, falls die Entity geladen wird und keine Custom-Daten vorhanden sind.
        this.flightDuration = 30;
        this.speedMultiplier = 15.5;
    }

    // --- 2. Der Konstruktor (Für das Spawnen des Items, ohne Elytra-Boost-Logik) ---
    // Der Vanilla-Konstruktor: FireworkRocketEntity(World world, double x, double y, double z, ItemStack stack)
    public EnhancedFireworkRocket(World world, double x, double y, double z, ItemStack stack) {
        // Rufen Sie den Basis-Konstruktor mit Ihrem EntityType auf.
        super(ModEntities.ENHANCED_FIREWORK_ROCKET_ENTITY, world);

        // Initialisieren Sie Ihre Mod-spezifischen Werte
        this.flightDuration = 30; // Ihre Custom Flugdauer
        this.speedMultiplier = 15.5; // Ihr Custom Geschwindigkeits-Multiplikator

        // Nachbau der Vanilla-Logik für dieses Spawning
        // Diese Zeilen setzen die Lebensdauer und Start-Geschwindigkeit (life, lifeTime, setVelocity)
        // und speichern das ItemStack. WICHTIG: Sie müssen die LifeTime manuell neu berechnen,
        // da Ihre 'flightDuration' verwendet werden soll.

        // Annahme: Sie haben die notwendigen Access Wideners für life und lifeTime.
        // this.life = 0;
        // this.setPosition(x, y, z);
        // this.dataTracker.set(ITEM, stack.copy());
        // this.lifeTime = this.calculateLifeTime(this.flightDuration);
        // this.setVelocity(this.random.nextTriangular(0.0, 0.002297), 0.05, this.random.nextTriangular(0.0, 0.002297));

        // Da die Super-Klasse private Felder hat, ist es am sichersten,
        // nur die Custom-Werte zu setzen und die Super-Logik nach dem Spawnen zu nutzen.
    }

    // --- 3. Der Konstruktor mit Besitzer (aus Vanilla) ---
    public EnhancedFireworkRocket(World world, @Nullable Entity entity, double x, double y, double z, ItemStack stack) {
        // Ruft Konstruktor 2 auf (setzt Ihre Custom-Felder und Vanilla-Startwerte)
        this(world, x, y, z, stack);
        this.setOwner(entity);
    }


    // --- 4. Der Konstruktor für Elytra-Flug (Wird von Ihrem Item verwendet!) ---
    // Dies ist der Konstruktor, den Ihr Item in der use()-Methode aufruft.
    public EnhancedFireworkRocket(World world, ItemStack stack, LivingEntity shooter) {
        // Ruft den Super-Konstruktor auf, der die Rakete an den Shooter bindet.
        // (Dieser Super-Konstruktor ruft intern Konstruktor 3 auf, was Konstruktor 2 und damit Ihre Felder initialisiert)
        super(world, stack, shooter);

        // Initialisieren Sie IHRE Felder HIER (zur Sicherheit, falls sie im vorherigen Aufruf nicht gesetzt wurden)
        this.flightDuration = 30;
        this.speedMultiplier = 15.5;
    }

    // --- Custom Methoden ---

    // Getter-Methode für den Mixin, um auf den Multiplikator zuzugreifen.
    public double getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    // Hilfsmethode, um die Lebensdauer basierend auf IHRER Flugdauer zu berechnen
    private int calculateLifeTime(int flightDuration) {
        return 10 * flightDuration + this.random.nextInt(6) + this.random.nextInt(7);
    }

    // Überschreiben Sie die Methoden zum Speichern/Laden von NBT, um Ihre Custom-Daten zu persistieren.
    // Entfernen Sie 'final' von Ihren Feldern, damit diese funktionieren.

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        // Speichern Sie Ihre eigenen Daten
        view.putInt("EnhancedFlightDuration", this.flightDuration);
        view.putDouble("EnhancedSpeedMultiplier", this.speedMultiplier);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        // Laden Sie Ihre eigenen Daten
        this.flightDuration = view.getInt("EnhancedFlightDuration", 30); // Laden oder Default-Wert
        this.speedMultiplier = view.getDouble("EnhancedSpeedMultiplier", 15.5); // Laden oder Default-Wert
    }
}
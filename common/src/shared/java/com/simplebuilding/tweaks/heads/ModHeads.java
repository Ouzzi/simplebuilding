package com.simplebuilding.tweaks.heads;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Was zu jedem Mod-Kopf gehoert (docs/MOBKOEPFE.md): der Mob, dessen Tod durch einen geladenen Creeper ihn
 * fallen laesst, sein Spawn-Ei (JEI-Seite "Mob drops"), der Notenblock-Klang und das Tag
 * {@code simplebuilding:trial_chamber_heads} (eine beliebige Zutat von Chunk-Loader I und Launchpad I).
 */
public final class ModHeads {

    /**
     * Koepfe der Trial-Chamber-Mobs (Besitzer 2026-09-29): Zombie, Wuestenzombie, Spinne, Hoehlenspinne,
     * Skelett, Eiswanderer, Sumpfskelett, Schleim, Silberfischchen, Breeze - Zombie und Skelett sind
     * Vanilla-Koepfe, der Rest kommt von der Mod. Eine der Zutaten von Chunk-Loader I und Launchpad I.
     */
    public static final TagKey<Item> TRIAL_CHAMBER_HEADS = TagKey.create(Registries.ITEM, SimpleTweaks.id("trial_chamber_heads"));

    private ModHeads() {
    }

    /** Der Mob, dessen Kopf das ist. */
    public static EntityType<?> source(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> EntityTypes.BLAZE;
            case ENDERMAN -> EntityTypes.ENDERMAN;
            case HUSK -> EntityTypes.HUSK;
            case SPIDER -> EntityTypes.SPIDER;
            case CAVE_SPIDER -> EntityTypes.CAVE_SPIDER;
            case STRAY -> EntityTypes.STRAY;
            case BOGGED -> EntityTypes.BOGGED;
            case SLIME -> EntityTypes.SLIME;
            case SILVERFISH -> EntityTypes.SILVERFISH;
            case BREEZE -> EntityTypes.BREEZE;
            case SHULKER -> EntityTypes.SHULKER;
            case DROWNED -> EntityTypes.DROWNED;
        };
    }

    /** Spawn-Ei des Mobs (Symbol in JEI). */
    public static Item spawnEgg(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> Items.BLAZE_SPAWN_EGG;
            case ENDERMAN -> Items.ENDERMAN_SPAWN_EGG;
            case HUSK -> Items.HUSK_SPAWN_EGG;
            case SPIDER -> Items.SPIDER_SPAWN_EGG;
            case CAVE_SPIDER -> Items.CAVE_SPIDER_SPAWN_EGG;
            case STRAY -> Items.STRAY_SPAWN_EGG;
            case BOGGED -> Items.BOGGED_SPAWN_EGG;
            case SLIME -> Items.SLIME_SPAWN_EGG;
            case SILVERFISH -> Items.SILVERFISH_SPAWN_EGG;
            case BREEZE -> Items.BREEZE_SPAWN_EGG;
            case SHULKER -> Items.SHULKER_SPAWN_EGG;
            case DROWNED -> Items.DROWNED_SPAWN_EGG;
        };
    }

    /** Was ein Notenblock unter dem Kopf spielt (Instrument CUSTOM_HEAD liest {@code note_block_sound}). */
    public static SoundEvent noteBlockSound(BlazeHeadType type) {
        return switch (type) {
            case BLAZE -> SoundEvents.BLAZE_AMBIENT;
            case ENDERMAN -> SoundEvents.ENDERMAN_AMBIENT;
            case HUSK -> SoundEvents.HUSK_AMBIENT;
            case SPIDER, CAVE_SPIDER -> SoundEvents.SPIDER_AMBIENT;
            case STRAY -> SoundEvents.STRAY_AMBIENT;
            case BOGGED -> SoundEvents.BOGGED_AMBIENT;
            case SLIME -> SoundEvents.SLIME_SQUISH;
            case SILVERFISH -> SoundEvents.SILVERFISH_AMBIENT;
            case BREEZE -> SoundEvents.BREEZE_IDLE_GROUND;
            case SHULKER -> SoundEvents.SHULKER_AMBIENT;
            case DROWNED -> SoundEvents.DROWNED_AMBIENT;
        };
    }

    /** Ob der Kopf zu einem Trial-Chamber-Mob gehoert (dann steht er im Tag {@link #TRIAL_CHAMBER_HEADS}). */
    public static boolean isTrialChamberMob(BlazeHeadType type) {
        return switch (type) {
            case HUSK, SPIDER, CAVE_SPIDER, STRAY, BOGGED, SLIME, SILVERFISH, BREEZE -> true;
            case BLAZE, ENDERMAN, SHULKER, DROWNED -> false;
        };
    }

    /** Inhalt des Tags {@link #TRIAL_CHAMBER_HEADS}: die Vanilla-Koepfe von Zombie und Skelett, dann die Mod-Koepfe. */
    public static List<Item> trialChamberHeads() {
        List<Item> heads = new ArrayList<>();
        heads.add(Items.ZOMBIE_HEAD);
        heads.add(Items.SKELETON_SKULL);
        for (BlazeHeadType type : BlazeHeadType.values()) {
            if (isTrialChamberMob(type)) {
                heads.add(TweaksItems.head(type));
            }
        }
        return heads;
    }
}

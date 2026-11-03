package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.framework.api.TrimContracts;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilities;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilityAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.phys.Vec3;

/**
 * MOB27-3 / ZO9 A (docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md): moved from the SimpleBuilding core test class {@code TrimBonusTests} together with
 * the trim abilities, bodies unchanged except for the inputs (see the helpers at the end); each method comment says what it pins.
 */
final class TrimBonusRuleTests {
    private TrimBonusRuleTests() {
    }

    /** The hit every measurement is taken on. Ten makes each percentage readable as a decimal. */
    private static final float HIT = 10.0F;

    /** The four slots {@code TrimEffectUtil} walks, and a carrier item for each. */
    private static final EquipmentSlot[] ARMOUR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Chainmail on purpose: the mod reads the trim material, never the armour item, and
     * "chainmail" collides with none of the material names {@code TrimEffectUtil} looks for.
     * Iron or diamond armour would leave a reader guessing which of the two a count came from.
     */
    private static final Item[] ARMOUR_ITEMS = {
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
            Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS
    };

    // =====================================================================================
    // PATTERNS KEYED ON A DAMAGE TYPE TAG
    // =====================================================================================

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("trim_bonus_tag_keyed_patterns_cover_the_whole_damage_family", TrimBonusRuleTests::tagKeyedPatternsCoverTheWholeDamageFamily);
        add.accept("trim_bonus_exactly_keyed_patterns_ignore_their_neighbours", TrimBonusRuleTests::exactlyKeyedPatternsIgnoreTheirNeighbours);
        add.accept("trim_bonus_magic_is_softened_by_the_vex_pattern_and_the_gold_and_lapis_materials", TrimBonusRuleTests::magicIsSoftenedByTheVexPatternAndTheGoldAndLapisMaterials);
        add.accept("trim_bonus_wild_and_silence_ride_on_the_damage_message_id", TrimBonusRuleTests::wildAndSilenceRideOnTheDamageMessageId);
        add.accept("trim_bonus_flow_reads_the_type_name_of_the_projectile_that_landed", TrimBonusRuleTests::flowReadsTheTypeNameOfTheProjectileThatLanded);
        add.accept("trim_bonus_armour_bypassing_hits_skip_the_three_physical_materials", TrimBonusRuleTests::armourBypassingHitsSkipTheThreePhysicalMaterials);
        add.accept("trim_bonus_iron_and_quartz_materials_add_to_their_own_patterns", TrimBonusRuleTests::ironAndQuartzMaterialsAddToTheirOwnPatterns);
        add.accept("trim_bonus_attacker_keyed_materials_read_the_entity_behind_the_hit", TrimBonusRuleTests::attackerKeyedMaterialsReadTheEntityBehindTheHit);
    }

    /**
     * Sentry, dune, snout and spire are written against {@code DamageTypeTags}, not against a
     * single damage type, and that is the whole point of them: sentry is meant to stop arrows
     * <em>and</em> tridents and thrown potions, snout to stop being on fire <em>and</em> standing
     * in lava. Each is therefore measured on two members of its family and on one hit from a
     * different family.
     *
     * <p>The second family member is the assertion that matters. A branch narrowed from
     * {@code is(IS_FIRE)} to {@code is(ON_FIRE)} keeps the first case green and quietly halves
     * what the trim is worth; the same goes for a tag swapped for its neighbour, since the
     * cross-family case only catches a swap between two families that are both worn.
     *
     * <p>The family memberships themselves are vanilla's, not the mod's, so each of them is
     * checked as a premise first: if Mojang moves thrown damage out of the projectile tag, this
     * test says so in one line instead of blaming the sentry pattern.
     *
     * <p>What breaks this test: changing any of the four tags, changing any of the four rates
     * (5% sentry, 8% dune, 5% snout, 8% spire per piece), or dropping a branch entirely.
     */
    static void tagKeyedPatternsCoverTheWholeDamageFamily(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            DamageSources sources = helper.getLevel().damageSources();

            Arrow arrow = detached(helper, EntityTypes.ARROW, "arrow");
            DamageSource arrowHit = sources.arrow(arrow, null);
            DamageSource thrownHit = sources.thrown(null, null);
            DamageSource blast = sources.explosion(null, null);
            DamageSource rocket = sources.fireworks(null, null);
            DamageSource burning = sources.onFire();
            DamageSource lava = sources.lava();
            DamageSource fall = sources.fall();
            DamageSource stalagmite = sources.stalagmite();
            DamageSource drown = sources.drown();

            // The premises. Everything below reads "the mod keys on this tag"; these four lines
            // say "and this hit really is in that tag", so a vanilla retag is not misreported.
            requireTag(helper, thrownHit, DamageTypeTags.IS_PROJECTILE, "thrown damage");
            requireTag(helper, rocket, DamageTypeTags.IS_EXPLOSION, "firework damage");
            requireTag(helper, lava, DamageTypeTags.IS_FIRE, "lava damage");
            requireTag(helper, stalagmite, DamageTypeTags.IS_FALL, "stalagmite damage");

            pinProgressMultiplier(helper, player, 1.0);

            // --- sentry: 5% per piece against everything tagged as a projectile ---
            wear(player, copper, pattern(helper, TrimPatterns.SENTRY), 4);
            assertDamage(helper, player, arrowHit, 8.0, "a full sentry set against an arrow");
            assertDamage(helper, player, thrownHit, 8.0,
                    "a full sentry set against thrown damage - sentry is keyed on the projectile "
                            + "TAG, so it has to cover more than arrows");
            assertDamage(helper, player, drown, 10.0,
                    "a sentry set reduced drowning, which is the coast pattern's job");

            // --- dune: 8% per piece against everything tagged as an explosion ---
            wear(player, copper, pattern(helper, TrimPatterns.DUNE), 4);
            assertDamage(helper, player, blast, 6.8, "a full dune set against an explosion");
            assertDamage(helper, player, rocket, 6.8,
                    "a full dune set against a firework - fireworks are tagged as an explosion");
            assertDamage(helper, player, arrowHit, 10.0,
                    "a dune set reduced an arrow, which is the sentry pattern's job");

            // --- snout: 5% per piece against everything tagged as fire ---
            wear(player, copper, pattern(helper, TrimPatterns.SNOUT), 4);
            assertDamage(helper, player, burning, 8.0, "a full snout set while on fire");
            assertDamage(helper, player, lava, 8.0,
                    "a full snout set in lava - snout is keyed on the fire TAG, so being on fire "
                            + "must not be the only case it covers");
            assertDamage(helper, player, blast, 10.0,
                    "a snout set reduced an explosion, which is the dune pattern's job");

            // --- spire: 8% per piece against everything tagged as a fall ---
            wear(player, copper, pattern(helper, TrimPatterns.SPIRE), 4);
            assertDamage(helper, player, fall, 6.8, "a full spire set against fall damage");
            // Stalagmite damage is also one of the three message ids the wild pattern watches
            // for, but a spire set scores zero wild pieces, so this stays a pure spire reading.
            assertDamage(helper, player, stalagmite, 6.8,
                    "a full spire set landing on a stalagmite - that damage is tagged as a fall");
            assertDamage(helper, player, burning, 10.0,
                    "a spire set reduced fire damage, which is the snout pattern's job");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * Coast, eye and bolt are written against a single damage type instead of a tag, and each is
     * paired here with the nearest thing that is <em>not</em> that type: drying out next to
     * drowning, plain magic next to dragon breath, being on fire next to a lightning strike.
     * Widening one of these to its tag - or to its family in spirit - is the regression the
     * near-miss case catches; nothing else in the suite would.
     *
     * <p>Bolt is measured on one and on two pieces rather than on a full set. At 25% per piece a
     * four-piece bolt set removes 100% of the hit and lands on the 0.1 floor, so a full set
     * would read 1.0 no matter what the rate below it was, and the number would stop being
     * evidence. One and two pieces also pin that the rate is per piece rather than per set.
     *
     * <p>What breaks this test: keying coast on the drowning tag, eye on anything wider than
     * dragon breath, or bolt on the lightning tag or on fire; and any change to the three rates
     * (10% coast, 10% eye, 25% bolt per piece).
     */
    static void exactlyKeyedPatternsIgnoreTheirNeighbours(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            DamageSources sources = helper.getLevel().damageSources();

            DamageSource drown = sources.drown();
            DamageSource dryOut = sources.dryOut();
            DamageSource dragonBreath = sources.dragonBreath();
            DamageSource magic = sources.magic();
            DamageSource lightning = sources.lightningBolt();
            DamageSource burning = sources.onFire();

            pinProgressMultiplier(helper, player, 1.0);

            // --- coast: 10% per piece, drowning only ---
            wear(player, copper, pattern(helper, TrimPatterns.COAST), 4);
            assertDamage(helper, player, drown, 6.0, "a full coast set against drowning");
            assertDamage(helper, player, dryOut, 10.0,
                    "a coast set reduced drying out; it is meant to answer to drowning alone");

            // --- eye: 10% per piece, dragon breath only ---
            wear(player, copper, pattern(helper, TrimPatterns.EYE), 4);
            assertDamage(helper, player, dragonBreath, 6.0,
                    "a full eye set against dragon breath");
            assertDamage(helper, player, magic, 10.0,
                    "an eye set reduced plain magic damage; dragon breath is the only type it is "
                            + "meant to cover");

            // --- bolt: 25% per piece, lightning only (on resin, which has no damage branch -
            // copper has its own lightning bonus since 2026-09) ---
            Holder<TrimMaterial> resin = material(helper, TrimMaterials.RESIN);
            wear(player, resin, pattern(helper, TrimPatterns.BOLT), 1);
            assertDamage(helper, player, lightning, 7.5,
                    "one bolt piece against a lightning strike");
            wear(player, resin, pattern(helper, TrimPatterns.BOLT), 2);
            assertDamage(helper, player, lightning, 5.0,
                    "two bolt pieces should take twice as much off as one; the 25% is per piece");

            // --- copper as a material: 10% per piece against lightning, and it adds to bolt ---
            wear(player, copper, pattern(helper, TrimPatterns.SHAPER), 4);
            assertDamage(helper, player, lightning, 6.0,
                    "a full copper set against a lightning strike (4 x 10%)");
            assertDamage(helper, player, burning, 10.0,
                    "a copper set reduced fire damage; its bonus is for lightning alone");
            wear(player, copper, pattern(helper, TrimPatterns.BOLT), 1);
            assertDamage(helper, player, lightning, 6.5,
                    "one copper bolt piece: 25% from the pattern plus 10% from the material");
            wear(player, copper, pattern(helper, TrimPatterns.BOLT), 4);
            assertDamage(helper, player, burning, 10.0,
                    "a bolt set reduced fire damage - lightning starts fires, but the two are "
                            + "different damage types and only one of them is bolt's");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * The vex branch is the only one in {@code modifyDamage} with three separate triggers - the
     * magic type, the indirect magic type, and "the thing that hit me is a vex" - so all three
     * are exercised, each on a hit the other two would not answer to. The vex's own melee is
     * plain mob damage, which is checked as a premise before it is used, so the third case
     * really does prove the entity clause rather than sneaking in through the magic clause.
     *
     * <p>Gold and lapis are the material half of the same question and are measured on the inert
     * shaper pattern, then once together on a split set. Wearing them one at a time would pass
     * with the other summand deleted - both lines sit inside the same {@code if} and neither is
     * reachable from any other test.
     *
     * <p>Each of the two is read against <em>both</em> magic types, and so is the split set. The
     * two summands share one condition, so moving either of them out onto a single damage type
     * is a one-line edit that leaves the shared {@code if} looking untouched; a material only
     * ever measured against plain {@code magic} would not notice it.
     *
     * <p>Note that the two magic damage types are tagged as armour-bypassing in vanilla, so the
     * diamond, astralit and nihilith materials cannot contribute here even if they were worn.
     * That is what makes these readings clean.
     *
     * <p>What breaks this test: dropping any of the three vex triggers, changing the 6% vex rate,
     * dropping or reweighting the gold (6%) or lapis (4%) summand, or restricting either
     * material to only one of the two magic types.
     */
    static void magicIsSoftenedByTheVexPatternAndTheGoldAndLapisMaterials(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            Holder<TrimMaterial> gold = material(helper, TrimMaterials.GOLD);
            Holder<TrimMaterial> lapis = material(helper, TrimMaterials.LAPIS);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());
            DamageSources sources = helper.getLevel().damageSources();

            LivingEntity vex = detached(helper, EntityTypes.VEX, "vex");
            LivingEntity zombie = detached(helper, EntityTypes.ZOMBIE, "zombie");
            DamageSource magic = sources.magic();
            DamageSource indirectMagic = sources.indirectMagic(null, null);
            DamageSource vexMelee = sources.mobAttack(vex);
            DamageSource zombieBite = sources.mobAttack(zombie);

            // Premises for the third vex trigger: a vex's melee must not already be magic, or
            // the entity clause below would be proven by the wrong half of the condition.
            helper.assertTrue(!vexMelee.is(DamageTypes.MAGIC) && !vexMelee.is(DamageTypes.INDIRECT_MAGIC),
                    "a vex's melee attack now counts as magic damage, so the case below no longer "
                            + "isolates the \"direct entity is a vex\" clause");
            helper.assertTrue(vexMelee.getDirectEntity() == vex,
                    "the vex is not the direct entity of its own melee attack, so this test would "
                            + "not reach the clause it is written for");

            pinProgressMultiplier(helper, player, 1.0);

            // --- the vex pattern, once per trigger ---
            wear(player, copper, pattern(helper, TrimPatterns.VEX), 4);
            assertDamage(helper, player, magic, 7.6, "a full vex set against magic damage");
            assertDamage(helper, player, indirectMagic, 7.6,
                    "a full vex set against indirect magic - the second of the three triggers");
            assertDamage(helper, player, vexMelee, 7.6,
                    "a full vex set hit by an actual vex - the third trigger reads the entity, "
                            + "not the damage type");
            assertDamage(helper, player, zombieBite, 10.0,
                    "a vex set reduced an ordinary mob's melee; only a vex is meant to trigger it");

            // --- gold: 6% per piece, both magic types ---
            wear(player, gold, blank, 4);
            assertDamage(helper, player, magic, 7.6, "a full gold trim against magic damage");
            assertDamage(helper, player, indirectMagic, 7.6,
                    "a full gold trim against indirect magic - the material covers both types");
            assertDamage(helper, player, zombieBite, 10.0,
                    "a gold trim reduced a plain melee hit; it is meant to be magic only");

            // --- lapis: 4% per piece, on both magic types as well ---
            wear(player, lapis, blank, 4);
            assertDamage(helper, player, magic, 8.4, "a full lapis trim against magic damage");
            assertDamage(helper, player, indirectMagic, 8.4,
                    "a full lapis trim against indirect magic - lapis sits in the same condition "
                            + "as gold, so lifting it out onto the plain magic type alone has to "
                            + "be caught here");
            assertDamage(helper, player, zombieBite, 10.0,
                    "a lapis trim reduced a plain melee hit; it is meant to be magic only");

            // --- and both at once, because they share one branch ---
            wearSplit(player, gold, lapis, blank);
            assertDamage(helper, player, magic, 8.0,
                    "two gold pieces plus two lapis pieces (2 x 6% + 2 x 4%) - one of the two "
                            + "summands has gone missing");
            assertDamage(helper, player, indirectMagic, 8.0,
                    "the same split set against indirect magic - this is the sharper reading of "
                            + "the two, because it needs both summands on the second magic type "
                            + "at once");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * Four of the branches in {@code modifyDamage} do not ask the damage type registry anything;
     * they compare {@code source.getMsgId()} with a literal. Wild watches for {@code "cactus"},
     * {@code "sweetBerryBush"} and {@code "stalagmite"}, silence for {@code "sonic_boom"}. Those
     * literals are the most breakable thing in the file: they are unchecked by the compiler,
     * they are vanilla's to rename, and three of the four are not even spelled like the damage
     * type they stand for.
     *
     * <p>This is therefore the one test in the class whose job is partly to catch a vanilla
     * change rather than a mod change. If a future Minecraft renames the sweet berry bush
     * message id to snake case - which is the obvious cleanup - the bonus silently stops
     * applying and only this test notices.
     *
     * <p>The last case builds a damage type of its own, with a message id that <em>starts</em>
     * with {@code "cactus"}, to pin that the comparison is an equality and not a
     * {@code contains}. That matters because the pattern and material lookups a few lines above
     * in the same class are substring tests, so the loose form is the one a maintainer is
     * primed to write.
     *
     * <p>What breaks this test: a typo in any of the four literals, a vanilla message-id rename,
     * loosening equals to contains or startsWith, and any change to the 10% wild or 20% silence
     * rate.
     */
    static void wildAndSilenceRideOnTheDamageMessageId(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            DamageSources sources = helper.getLevel().damageSources();

            LivingEntity zombie = detached(helper, EntityTypes.ZOMBIE, "zombie");
            DamageSource cactus = sources.cactus();
            DamageSource berryBush = sources.sweetBerryBush();
            DamageSource stalagmite = sources.stalagmite();
            DamageSource sonicBoom = sources.sonicBoom(zombie);
            DamageSource zombieBite = sources.mobAttack(zombie);
            DamageSource magic = sources.magic();

            // A damage type that exists only here. Its holder is direct, so it belongs to no tag
            // and matches no damage type key; the only thing about it that can reach the mod is
            // its message id, which is "cactus" with a suffix.
            DamageSource looksLikeCactus =
                    new DamageSource(Holder.direct(new DamageType("cactus_thorns", 0.0F)));

            pinProgressMultiplier(helper, player, 1.0);

            // --- wild: 10% per piece, on three message ids ---
            wear(player, copper, pattern(helper, TrimPatterns.WILD), 4);
            assertDamage(helper, player, cactus, 6.0, "a full wild set against a cactus");
            assertDamage(helper, player, berryBush, 6.0,
                    "a full wild set in a sweet berry bush - the message id for that damage is "
                            + "the camel-cased \"sweetBerryBush\", which is what the mod compares "
                            + "against");
            assertDamage(helper, player, stalagmite, 6.0,
                    "a full wild set landing on a stalagmite");
            assertDamage(helper, player, zombieBite, 10.0,
                    "a wild set reduced a plain melee hit");
            assertDamage(helper, player, looksLikeCactus, 10.0,
                    "a damage type whose message id merely begins with \"cactus\" got the wild "
                            + "bonus - the comparison is supposed to be an equality, not the "
                            + "substring test the trim lookups use");

            // --- silence: 20% per piece, on one message id ---
            wear(player, copper, pattern(helper, TrimPatterns.SILENCE), 4);
            assertDamage(helper, player, sonicBoom, 2.0,
                    "a full silence set against a warden's sonic boom");
            assertDamage(helper, player, magic, 10.0,
                    "a silence set reduced plain magic damage; only the sonic boom is meant to "
                            + "trigger it");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * Flow is the one branch that identifies a hit by the <em>name of the entity type</em> that
     * delivered it: it asks whether {@code getDirectEntity().getType().toString()} contains
     * {@code "wind_charge"}. That is deliberate - a breeze's wind charge is a different entity
     * type from a player's, and the substring covers both. It is also fragile in a way nothing
     * else in the file is; see the known-defect note on the class.
     *
     * <p>Both wind charge entity types are measured, and an arrow is measured next to them: the
     * wind charge damage type is tagged as a projectile, so a flow branch rewritten to key on
     * that tag would look identical on the first two cases and only the arrow would give it
     * away. The reverse reading is taken too - a sentry set does answer to a wind charge -
     * because the two branches are meant to overlap on that hit, and losing the overlap is the
     * other way this could quietly go wrong.
     *
     * <p>The last case is the null guard. Most damage in the game has no direct entity at all,
     * and this branch dereferences one; without the guard, a flow trim would turn every fall,
     * every drowning and every starvation into a crash rather than a bonus.
     *
     * <p>What breaks this test: keying flow on the projectile tag or on the wind charge damage
     * type, matching the entity type exactly instead of by substring (which would drop the
     * breeze's charge), dropping the null check, and changing the 10% rate.
     */
    static void flowReadsTheTypeNameOfTheProjectileThatLanded(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> copper = material(helper, TrimMaterials.COPPER);
            DamageSources sources = helper.getLevel().damageSources();

            Entity playerCharge = detached(helper, EntityTypes.WIND_CHARGE, "wind charge");
            Entity breezeCharge =
                    detached(helper, EntityTypes.BREEZE_WIND_CHARGE, "breeze wind charge");
            Arrow arrow = detached(helper, EntityTypes.ARROW, "arrow");

            DamageSource thrownCharge = sources.windCharge(playerCharge, null);
            DamageSource breezeGust = sources.windCharge(breezeCharge, null);
            DamageSource arrowHit = sources.arrow(arrow, null);
            DamageSource noEntity = sources.generic();

            // The premise: the branch reads the DIRECT entity, so the test has to know which of
            // the two entity slots on the source it landed in.
            helper.assertTrue(thrownCharge.getDirectEntity() == playerCharge,
                    "the wind charge is not the direct entity of its own damage source, so this "
                            + "test would not reach the clause it is written for");
            helper.assertTrue(noEntity.getDirectEntity() == null,
                    "generic damage now carries a direct entity, so the null-guard case below "
                            + "proves nothing");

            pinProgressMultiplier(helper, player, 1.0);
            wear(player, copper, pattern(helper, TrimPatterns.FLOW), 4);

            assertDamage(helper, player, thrownCharge, 6.0,
                    "a full flow set against a player's wind charge");
            assertDamage(helper, player, breezeGust, 6.0,
                    "a full flow set against a breeze's gust - that is a different entity type, "
                            + "and the substring match on the type name is what covers it");
            assertDamage(helper, player, arrowHit, 10.0,
                    "a flow set reduced an arrow; wind charge damage is tagged as a projectile, "
                            + "but flow is meant to look at the entity, not at that tag");
            assertDamage(helper, player, noEntity, 10.0,
                    "a flow set changed damage that has no entity behind it at all");

            // --- the deliberate overlap, from the other side ---
            wear(player, copper, pattern(helper, TrimPatterns.SENTRY), 4);
            assertDamage(helper, player, thrownCharge, 8.0,
                    "a sentry set did not answer to a wind charge - that damage is tagged as a "
                            + "projectile, so both patterns are supposed to cover it");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * Diamond, astralit and nihilith share one condition: they only apply to damage that is not
     * tagged as armour-bypassing. That guard is the difference between a physical resistance and
     * a universal one - without it these three would soften starvation, the void and the border,
     * which is what the enderite material is for and what these three are explicitly not.
     *
     * <p>Both sides of the guard are measured for each material, and the two hits used are
     * checked against the tag first, because the guard is stated in terms of a vanilla tag whose
     * contents vanilla owns. Worth knowing while reading the numbers: that tag is broad - it
     * holds generic, magic, wither, fall, fire, drowning, dragon breath and more - so these
     * three materials reach far less damage than "3% off everything except armour-piercing hits"
     * suggests. A plain mob bite is used as the non-bypassing hit for exactly that reason.
     *
     * <p>Astralit and nihilith carry the same rate, so a split set of astralit and diamond is
     * measured as well: it is the only reading here that distinguishes "both summands are
     * present" from "one of them is doing the whole job".
     *
     * <p>What breaks this test: dropping the bypass guard, dropping one of the three materials,
     * changing 3% diamond or 2% astralit/nihilith, or making the material lookup exact so that
     * the mod's own two materials stop being found.
     */
    static void armourBypassingHitsSkipTheThreePhysicalMaterials(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> diamond = material(helper, TrimMaterials.DIAMOND);
            Holder<TrimMaterial> astralit = material(helper, SB_ASTRALIT);
            Holder<TrimMaterial> nihilith = material(helper, SB_NIHILITH);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());
            DamageSources sources = helper.getLevel().damageSources();

            LivingEntity zombie = detached(helper, EntityTypes.ZOMBIE, "zombie");
            DamageSource zombieBite = sources.mobAttack(zombie);
            DamageSource fall = sources.fall();

            helper.assertTrue(!zombieBite.is(DamageTypeTags.BYPASSES_ARMOR),
                    "a mob's melee attack now bypasses armour, so every positive case below would "
                            + "read zero for a reason that has nothing to do with the mod");
            requireTag(helper, fall, DamageTypeTags.BYPASSES_ARMOR, "fall damage");

            pinProgressMultiplier(helper, player, 1.0);

            // --- diamond: 3% per piece ---
            wear(player, diamond, blank, 4);
            assertDamage(helper, player, zombieBite, 8.8,
                    "a full diamond trim against a mob's melee");
            assertDamage(helper, player, fall, 10.0,
                    "a diamond trim softened fall damage, which bypasses armour");

            // --- astralit: 2% per piece ---
            wear(player, astralit, blank, 4);
            assertDamage(helper, player, zombieBite, 9.2,
                    "a full astralit trim against a mob's melee");
            assertDamage(helper, player, fall, 10.0,
                    "an astralit trim softened fall damage, which bypasses armour");

            // --- nihilith: 2% per piece ---
            wear(player, nihilith, blank, 4);
            assertDamage(helper, player, zombieBite, 9.2,
                    "a full nihilith trim against a mob's melee");
            assertDamage(helper, player, fall, 10.0,
                    "a nihilith trim softened fall damage, which bypasses armour");

            // --- two materials at once, on the same set ---
            wearSplit(player, astralit, diamond, blank);
            assertDamage(helper, player, zombieBite, 9.0,
                    "two astralit pieces plus two diamond pieces (2 x 2% + 2 x 3%) - one of the "
                            + "two summands has gone missing");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * Iron answers to the same projectile tag as the sentry pattern, and quartz to the same fire
     * tag as the snout pattern. Each is measured three ways: alone on the inert shaper pattern,
     * against a hit from the other family, and then worn on its matching pattern so that both
     * halves have to be present to reach the number. A set that only ever wore one of the two
     * would pass with the other deleted - and a pattern bonus is easy to delete by accident,
     * because it sits eleven lines away from the material bonus it doubles.
     *
     * <p>Quartz is also the only trim material in the mod with a bonus outside the damage path:
     * it adds 5% per piece to experience, next to raiser and lapis. That summand is measured
     * here rather than with the other experience sources because it is the same material and
     * the same registry lookup; the existing utility test measures raiser and lapis only, so the
     * quartz line can be deleted today without anything turning red.
     *
     * <p>What breaks this test: changing 5% iron, 5% quartz or the 5% quartz experience share;
     * moving iron off the projectile tag or quartz off the fire tag; and deleting either the
     * pattern or the material half of the two combined readings.
     */
    static void ironAndQuartzMaterialsAddToTheirOwnPatterns(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> iron = material(helper, TrimMaterials.IRON);
            Holder<TrimMaterial> quartz = material(helper, TrimMaterials.QUARTZ);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());
            DamageSources sources = helper.getLevel().damageSources();

            Arrow arrow = detached(helper, EntityTypes.ARROW, "arrow");
            DamageSource arrowHit = sources.arrow(arrow, null);
            DamageSource burning = sources.onFire();
            LivingEntity zombie = detached(helper, EntityTypes.ZOMBIE, "zombie");
            DamageSource zombieBite = sources.mobAttack(zombie);

            pinProgressMultiplier(helper, player, 1.0);

            // --- iron: 5% per piece against projectiles ---
            wear(player, iron, blank, 4);
            assertDamage(helper, player, arrowHit, 8.0, "a full iron trim against an arrow");
            assertDamage(helper, player, zombieBite, 10.0,
                    "an iron trim softened a melee hit; it is meant to be projectiles only");

            wear(player, iron, pattern(helper, TrimPatterns.SENTRY), 4);
            assertDamage(helper, player, arrowHit, 6.0,
                    "an iron sentry set against an arrow (4 x 5% pattern + 4 x 5% material) - "
                            + "one of the two halves is gone");

            // --- quartz: 5% per piece against fire ---
            wear(player, quartz, blank, 4);
            assertDamage(helper, player, burning, 8.0, "a full quartz trim while on fire");
            assertDamage(helper, player, arrowHit, 10.0,
                    "a quartz trim softened an arrow; it is meant to be fire only");

            wear(player, quartz, pattern(helper, TrimPatterns.SNOUT), 4);
            assertDamage(helper, player, burning, 6.0,
                    "a quartz snout set while on fire (4 x 5% pattern + 4 x 5% material) - one "
                            + "of the two halves is gone");

            // --- and the other thing quartz does ---
            wear(player, quartz, blank, 4);
            assertClose(helper, TrimAbilities.getXPMultiplier(player), 1.2,
                    "the quartz experience bonus (4 x 5%)");
            wear(player, iron, blank, 4);
            assertClose(helper, TrimAbilities.getXPMultiplier(player), 1.0,
                    "an iron trim granted an experience bonus; only raiser, lapis and quartz do");
            // Progress 0.5 keeps the pair below the +50% experience cap, so both halves show.
            pinProgressMultiplier(helper, player, 0.5);
            wear(player, quartz, pattern(helper, TrimPatterns.RAISER), 4);
            assertClose(helper, TrimAbilities.getXPMultiplier(player), 1.3,
                    "a quartz raiser set (4 x 10% pattern + 4 x 5% material) - one of the two "
                            + "halves is gone");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * Emerald and netherite are the two materials that ignore the damage type and look at who
     * threw the hit. Emerald asks whether the attacker is an {@code AbstractIllager}; netherite
     * takes either an enchantment-bypassing hit or a wither.
     *
     * <p>The emerald half is measured on two different illagers and on two mobs that are not
     * illagers, one of which is a witch - a raider, a spellcaster, an obvious illager to the
     * eye, and not an {@code AbstractIllager} in the class tree. That pair is what pins the
     * condition to the abstract class rather than to one concrete mob or to "raiders".
     *
     * <p>The last emerald case is an arrow shot by a pillager, and it is the important one: the
     * source's direct entity is then the arrow and only its causing entity is the illager. The
     * vex branch a few lines up reads {@code getDirectEntity()} and this one reads
     * {@code getEntity()}; swapping them is a one-word mistake that would leave every melee
     * reading unchanged and quietly stop the trim from working against the mob it exists for.
     *
     * <p>The netherite half is measured on each of its two triggers separately - a sonic boom
     * for the tag, a wither's melee for the class - and the wither's melee is checked not to
     * carry the tag first, so the two clauses cannot cover for each other.
     *
     * <p>Each netherite clause then gets a second reading, because that first pair is blind in
     * two directions. The class clause is measured on a wither skull too: a wither fights mostly
     * by firing those, and the skull's damage source is the only netherite case where the skull
     * is the direct entity and the wither merely the causing one - so it is what catches, on
     * this material, the same one-word slip that the pillager's arrow catches on emerald. A
     * melee source carries the same entity in both slots and can never see it.
     *
     * <p>The tag clause gets a negative on fall damage, which is in {@code BYPASSES_ARMOR} but
     * not in {@code BYPASSES_ENCHANTMENTS}. The mob melee used as the other negative is in
     * neither tag, and the sonic boom is in both, so nothing else here can tell the two apart -
     * and telling them apart matters, because {@code BYPASSES_ARMOR} stands twice more in the
     * same method body and is the neighbour a maintainer would reach for by mistake. In 26.2
     * that swap would take netherite from one damage type to nineteen.
     *
     * <p>What breaks this test: narrowing the illager check to a concrete class or widening it
     * to raiders, reading the direct entity instead of the causing one on either material,
     * swapping netherite's tag for the armour-bypass one, dropping either netherite trigger, and
     * changing the 8% emerald or 5% netherite rate.
     */
    static void attackerKeyedMaterialsReadTheEntityBehindTheHit(GameTestHelper helper) {
        TrimContracts.Host configuredBase = unpinned();
        try {
            ServerPlayer player = mockPlayer(helper);
            Holder<TrimMaterial> emerald = material(helper, TrimMaterials.EMERALD);
            Holder<TrimMaterial> netherite = material(helper, TrimMaterials.NETHERITE);
            Holder<TrimPattern> blank = pattern(helper, inertPattern());
            DamageSources sources = helper.getLevel().damageSources();

            LivingEntity pillager = detached(helper, EntityTypes.PILLAGER, "pillager");
            LivingEntity vindicator = detached(helper, EntityTypes.VINDICATOR, "vindicator");
            LivingEntity witch = detached(helper, EntityTypes.WITCH, "witch");
            LivingEntity zombie = detached(helper, EntityTypes.ZOMBIE, "zombie");
            LivingEntity wither = detached(helper, EntityTypes.WITHER, "wither");
            Arrow arrow = detached(helper, EntityTypes.ARROW, "arrow");
            WitherSkull skull = detached(helper, EntityTypes.WITHER_SKULL, "wither skull");

            DamageSource pillagerMelee = sources.mobAttack(pillager);
            DamageSource vindicatorMelee = sources.mobAttack(vindicator);
            DamageSource witchMelee = sources.mobAttack(witch);
            DamageSource zombieBite = sources.mobAttack(zombie);
            DamageSource pillagerArrow = sources.arrow(arrow, pillager);
            DamageSource sonicBoom = sources.sonicBoom(zombie);
            DamageSource witherMelee = sources.mobAttack(wither);
            DamageSource witherSkullHit = sources.witherSkull(skull, wither);
            DamageSource fall = sources.fall();

            // Premises. The two projectile cases only mean something if the entity slots really
            // are filled the way they assume; the two wither cases only isolate the class clause
            // if neither of them is already an enchantment-bypassing hit; and fall damage only
            // separates the two armour-related tags if vanilla still has it in exactly one.
            helper.assertTrue(pillagerArrow.getDirectEntity() == arrow
                            && pillagerArrow.getEntity() == pillager,
                    "an arrow damage source no longer carries the arrow as its direct entity and "
                            + "the shooter as its causing entity, so the case below cannot tell "
                            + "the two apart any more");
            helper.assertTrue(witherSkullHit.getDirectEntity() == skull
                            && witherSkullHit.getEntity() == wither,
                    "a wither skull damage source no longer carries the skull as its direct "
                            + "entity and the wither as its causing entity, so the netherite case "
                            + "below cannot tell the two apart any more");
            helper.assertTrue(!witherMelee.is(DamageTypeTags.BYPASSES_ENCHANTMENTS),
                    "a wither's melee now bypasses enchantments, so it no longer isolates the "
                            + "\"attacker is a wither\" half of the netherite condition");
            helper.assertTrue(!witherSkullHit.is(DamageTypeTags.BYPASSES_ENCHANTMENTS),
                    "wither skull damage now bypasses enchantments, so it no longer isolates the "
                            + "\"attacker is a wither\" half of the netherite condition either");
            requireTag(helper, sonicBoom, DamageTypeTags.BYPASSES_ENCHANTMENTS, "sonic boom damage");
            requireTag(helper, fall, DamageTypeTags.BYPASSES_ARMOR, "fall damage");
            helper.assertTrue(!fall.is(DamageTypeTags.BYPASSES_ENCHANTMENTS),
                    "fall damage now bypasses enchantments as well, so it can no longer tell "
                            + "netherite's tag apart from the armour-bypass tag next to it");

            pinProgressMultiplier(helper, player, 1.0);

            // --- emerald: 8% per piece, against illagers ---
            wear(player, emerald, blank, 4);
            assertDamage(helper, player, pillagerMelee, 6.8,
                    "a full emerald trim against a pillager");
            assertDamage(helper, player, vindicatorMelee, 6.8,
                    "a full emerald trim against a vindicator - the check is on the shared "
                            + "AbstractIllager class, not on one mob");
            assertDamage(helper, player, witchMelee, 10.0,
                    "an emerald trim softened a witch's attack; a witch is a raider but not an "
                            + "illager, and widening the check to raiders is the likely slip");
            assertDamage(helper, player, zombieBite, 10.0,
                    "an emerald trim softened an ordinary mob's melee");
            assertDamage(helper, player, pillagerArrow, 6.8,
                    "an emerald trim did not answer to an arrow shot by a pillager - the branch "
                            + "has to read the causing entity, not the arrow that arrived");

            // --- netherite: 5% per piece, on two unrelated triggers ---
            wear(player, netherite, blank, 4);
            assertDamage(helper, player, sonicBoom, 8.0,
                    "a full netherite trim against a sonic boom (an enchantment-bypassing hit)");
            assertDamage(helper, player, witherMelee, 8.0,
                    "a full netherite trim against the wither itself - the second trigger reads "
                            + "the attacker's class");
            assertDamage(helper, player, witherSkullHit, 8.0,
                    "a full netherite trim against a wither skull - that is how the boss actually "
                            + "fights, and on a skull's damage source the wither is only the "
                            + "CAUSING entity, so this is the reading a getDirectEntity() slip "
                            + "would quietly lose");
            assertDamage(helper, player, zombieBite, 10.0,
                    "a netherite trim softened an ordinary mob's melee, which meets neither of "
                            + "its two conditions");
            assertDamage(helper, player, fall, 10.0,
                    "a netherite trim softened fall damage - fall bypasses ARMOUR, not "
                            + "enchantments, and only the enchantment tag is netherite's");

            bare(player);
            helper.succeed();
        } finally {
            restore(configuredBase);
        }
    }

    /**
     * The inert carrier pattern. {@code shaper} is the one vanilla pattern whose asset path is
     * not a substring query anywhere in {@code TrimEffectUtil}, so wearing it means whatever a
     * test then measures came from the trim material alone. If a future pattern bonus is keyed
     * on "shaper", every material reading here starts failing, which is the correct outcome.
     */
    private static ResourceKey<TrimPattern> inertPattern() {
        return TrimPatterns.SHAPER;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the
        // player list non-empty and the gametest server then stalls on shutdown.
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Builds an entity without putting it into the world.
     *
     * <p>Every entity in this class exists only to be hung off a {@link DamageSource}; none of
     * them is ever ticked, hit or looked at by anything else. Spawning them would add a pillager,
     * a vex and a wither to a test room eight blocks from its neighbours, with a boss bar and an
     * attack goal each, for no gain - and the entity search hazard that comes with it. A
     * constructed entity answers {@code getType()} and {@code instanceof} exactly the same way.
     */
    private static <E extends Entity> E detached(GameTestHelper helper, EntityType<E> type, String what) {
        E entity = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(entity != null,
                "could not build a " + what + " to hang a damage source on");
        return entity;
    }

    /**
     * States the vanilla half of a claim separately from the mod half, so that a retagged damage
     * type is reported as what it is instead of as a broken trim.
     */
    private static void requireTag(GameTestHelper helper, DamageSource source,
                                   TagKey<DamageType> tag, String what) {
        helper.assertTrue(source.is(tag),
                "vanilla no longer puts " + what + " in the damage type tag this case is built "
                        + "on, so the reading below would say nothing about the mod");
    }

    /** Runs one hit of {@link #HIT} through {@code modifyDamage} and checks what comes out. */
    private static void assertDamage(GameTestHelper helper, LivingEntity victim,
                                     DamageSource source, double expected, String what) {
        float actual = TrimAbilities.modifyDamage(victim, HIT, source);
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-4,
                what + ": expected " + expected + " of the " + HIT + " hit to land, but "
                        + actual + " did");
    }

    /** Puts {@code pieces} trimmed armour pieces on and empties the remaining armour slots. */
    private static void wear(LivingEntity entity, Holder<TrimMaterial> trimMaterial,
                             Holder<TrimPattern> trimPattern, int pieces) {
        ArmorTrim trim = new ArmorTrim(trimMaterial, trimPattern);
        for (int i = 0; i < ARMOUR_SLOTS.length; i++) {
            entity.setItemSlot(ARMOUR_SLOTS[i],
                    i < pieces ? trimmed(ARMOUR_ITEMS[i], trim) : ItemStack.EMPTY);
        }
    }

    /**
     * Two pieces of one material and two of another, on the same pattern. This is the only way
     * to have two material summands of the same branch contribute to one reading.
     */
    private static void wearSplit(LivingEntity entity, Holder<TrimMaterial> first,
                                  Holder<TrimMaterial> second, Holder<TrimPattern> trimPattern) {
        ArmorTrim firstTrim = new ArmorTrim(first, trimPattern);
        ArmorTrim secondTrim = new ArmorTrim(second, trimPattern);
        for (int i = 0; i < ARMOUR_SLOTS.length; i++) {
            entity.setItemSlot(ARMOUR_SLOTS[i],
                    trimmed(ARMOUR_ITEMS[i], i < 2 ? firstTrim : secondTrim));
        }
    }

    private static void bare(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            entity.setItemSlot(slot, ItemStack.EMPTY);
        }
        entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static ItemStack trimmed(Item item, ArmorTrim trim) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.TRIM, trim);
        return stack;
    }

    private static Holder<TrimPattern> pattern(GameTestHelper helper, ResourceKey<TrimPattern> key) {
        Registry<TrimPattern> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        Optional<Holder.Reference<TrimPattern>> found = registry.get(key);
        helper.assertTrue(found.isPresent(),
                "the trim pattern " + key.identifier() + " is not in the datapack registry");
        return found.orElseThrow();
    }

    /**
     * Everything measured here is a {@code float} internally, so a double-tight epsilon would
     * fail on rounding alone; 1.0e-4 is still far below every step this class asserts.
     */
    private static void assertClose(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-4,
                what + ": expected " + expected + " but got " + actual);
    }

    // ---------------------------------------------------------------- MOB27-3 / ZO9 A test inputs
    // The bodies above came from the SimpleBuilding core (docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md). There the resonance was
    // pinned through the core config (trimBenefitBaseMultiplier) and the player switch through TrimBenefitUser; here both go through a
    // test host (framework TrimContracts.Host), which works with and without SimpleBuilding loaded and puts the old host back.

    /** Players switched off through {@link #gate}; cleared when a body restores its host. */
    private static final java.util.Set<java.util.UUID> OFF = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The test host: a pinned resonance for players (NaN = ask the previous host), the player switch, the rest from the previous host. */
    private static final class Pinned implements TrimContracts.Host {
        final double target;
        final TrimContracts.Host previous;

        Pinned(double target, TrimContracts.Host previous) {
            this.target = target;
            this.previous = previous;
        }

        @Override
        public double multiplier(Object livingEntity) {
            if (livingEntity instanceof net.minecraft.world.entity.player.Player && !Double.isNaN(target)) return target;
            return previous == null ? Double.NaN : previous.multiplier(livingEntity);
        }

        @Override
        public float strength(String bonusKey) {
            return previous == null ? 1.0f : previous.strength(bonusKey);
        }

        @Override
        public boolean enabled(Object livingEntity) {
            if (livingEntity instanceof net.minecraft.world.entity.player.Player p && OFF.contains(p.getUUID())) return false;
            return previous == null || previous.enabled(livingEntity);
        }
    }

    private static TrimContracts.Host unpinned() {
        TrimContracts.Host current = TrimContracts.host();
        return current instanceof Pinned pinned ? pinned.previous : current;
    }

    /** Puts the host a body started with back (and every player switch on). */
    private static void restore(TrimContracts.Host host) {
        TrimContracts.setHost(host);
        OFF.clear();
    }

    /** Pins the resonance of every player to {@code target} and checks that the abilities read exactly that. */
    private static void pinProgressMultiplier(GameTestHelper helper, ServerPlayer player, double target) {
        TrimContracts.setHost(new Pinned(target, unpinned()));
        double actual = TrimAbilities.resonance(player);
        helper.assertTrue(Math.abs(actual - target) < 1.0e-6,
                "the test host no longer feeds the resonance: wanted " + target + " but got " + actual);
    }

    /** The player's trim switch (formerly the core's TrimBenefitUser on the player), now the host's {@code enabled}. */
    private static Gate gate(ServerPlayer player) {
        return new Gate(player.getUUID());
    }

    private record Gate(java.util.UUID id) {
        boolean simplebuilding$areTrimBenefitsEnabled() {
            return !OFF.contains(id);
        }

        void simplebuilding$setTrimBenefitsEnabled(boolean enabled) {
            TrimContracts.Host current = TrimContracts.host();
            if (!(current instanceof Pinned)) TrimContracts.setHost(new Pinned(Double.NaN, current));
            if (enabled) OFF.remove(id);
            else OFF.add(id);
        }
    }

    // The SimpleBuilding trim materials by key: Simple Mobs reads only the key (getMaterialCount/getTrimCount), so without
    // SimpleBuilding a stand-alone holder with that key stands in (see material()).
    private static final ResourceKey<TrimMaterial> SB_ASTRALIT = sbMaterial("astralit");
    private static final ResourceKey<TrimMaterial> SB_NIHILITH = sbMaterial("nihilith");
    private static final ResourceKey<TrimMaterial> SB_ENDERITE = sbMaterial("enderite");

    private static ResourceKey<TrimMaterial> sbMaterial(String name) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath("simplebuilding", name));
    }

    private static Holder<TrimMaterial> material(GameTestHelper helper, ResourceKey<TrimMaterial> key) {
        Registry<TrimMaterial> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        Optional<Holder.Reference<TrimMaterial>> found = registry.get(key);
        if (found.isPresent()) return found.get();
        helper.assertTrue(key.identifier().getNamespace().equals("simplebuilding"), "the trim material " + key.identifier()
                + " is not in the datapack registry");
        // Simple Mobs alone: the SimpleBuilding material is not registered; the abilities read only its key.
        return Holder.Reference.createStandAlone(registry, key);
    }
}

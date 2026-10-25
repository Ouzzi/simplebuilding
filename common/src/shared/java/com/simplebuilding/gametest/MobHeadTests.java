package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.EnchantedShapelessRecipe;
import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.heads.HeadAbilities;
import com.simplebuilding.tweaks.heads.ModHeads;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests der Mob-Koepfe (Besitzer 2026-09-29, docs/MOBKOEPFE.md): die neuen Koepfe fallen nur, wenn ein geladener
 * Creeper den Mob sprengt; Flypad I verlangt Shulkerkopf und eine Elytra mit Reparatur; jede geheime Faehigkeit
 * wirkt mit dem Kopf im Kopf-Slot - und ohne ihn nicht. Die Rezepte der anderen Pads prueft
 * {@code PadOverhaulTests#tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem}, dass jeder Katalog-Kopf im
 * Spiel wirklich faellt {@code InWorldExportTests#mobDropCatalogMatchesTheGame}.
 * Fabric-Adapter {@code MobHeadGameTest}, Test-IDs {@code simplebuilding:mob_head_game_test_*}.
 */
public final class MobHeadTests {

    private MobHeadTests() {
    }

    // =====================================================================================
    // Beschaffung und Grunddaten
    // =====================================================================================

    /**
     * Jeder der zehn neuen Koepfe faellt genau einmal, wenn die Explosion eines geladenen Creepers seinen Mob toetet
     * (ein eigener Creeper je Mob, Vanilla gibt einen Kopf je Explosion); eine Spinne, die ein Spieler oder ein
     * ungeladener Creeper toetet, laesst keinen Kopf fallen.
     */
    public static void chargedCreepersDropTheNewHeadsAndNoOtherDeathDoes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        for (BlazeHeadType type : BlazeHeadType.values()) {
            if (type == BlazeHeadType.BLAZE || type == BlazeHeadType.ENDERMAN) {
                continue; // PotionPadTests und InWorldExportTests
            }
            Creeper creeper = chargedCreeper(helper, new BlockPos(1, 2, 1));
            LivingEntity victim = spawn(helper, ModHeads.source(type), new BlockPos(4, 2, 4));
            victim.hurtServer(level, level.damageSources().explosion(creeper, creeper), 1000.0F);
            int heads = count(helper, TweaksItems.head(type));
            if (!victim.isDeadOrDying()) {
                problems.add(type + ": the victim survived");
            } else if (heads != 1) {
                problems.add(type + ": " + heads + " heads instead of 1");
            }
            clearItems(helper);
            creeper.discard();
        }
        Creeper plain = helper.spawn(EntityTypes.CREEPER, new BlockPos(1, 2, 1));
        LivingEntity byCreeper = spawn(helper, EntityTypes.SPIDER, new BlockPos(4, 2, 4));
        byCreeper.hurtServer(level, level.damageSources().explosion(plain, plain), 1000.0F);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 5.5));
        LivingEntity byPlayer = spawn(helper, EntityTypes.SPIDER, new BlockPos(5, 2, 5));
        byPlayer.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(byCreeper.isDeadOrDying() && byPlayer.isDeadOrDying(), "the spiders did not die");
        if (count(helper, TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SPIDER)) != 0) {
            problems.add("a spider killed by a player or an uncharged creeper dropped a head");
        }
        plain.discard();
        helper.assertTrue(problems.isEmpty(), "head drops: " + problems);
        helper.succeed();
    }

    /**
     * Alle zwoelf Koepfe sind Koepfe wie die von Vanilla: tragbar (Kopf-Slot), im Tag {@code minecraft:skulls},
     * stehend und an der Wand setzbar; ihre Attribut-Zeilen (Ortungsleiste, Silberfischchen-Groesse,
     * Schleim-Fallhoehe) stehen versteckt im Tooltip, damit das Geheimnis eines bleibt. Die Trial-Chamber-Koepfe
     * stehen im Tag, Shulker/Ertrunkener/Lohe/Enderman nicht.
     */
    public static void everyModHeadIsWearableLikeVanillaSkullsAndKeepsItsSecret(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        helper.assertValueEqual(TweaksItems.heads().size(), 12, "mob heads of the mod");
        for (BlazeHeadType type : BlazeHeadType.values()) {
            ItemStack stack = new ItemStack(TweaksItems.head(type));
            var equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.slot() != EquipmentSlot.HEAD) {
                problems.add(type + " is not worn on the head");
            }
            if (!stack.is(ItemTags.SKULLS)) {
                problems.add(type + " is not in #minecraft:skulls");
            }
            if (stack.is(ModHeads.TRIAL_CHAMBER_HEADS) != ModHeads.isTrialChamberMob(type)) {
                problems.add(type + " is wrongly (not) in #simplebuilding:trial_chamber_heads");
            }
            ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            if (modifiers.modifiers().isEmpty()) {
                problems.add(type + " lost the waypoint hiding modifier");
            }
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (!(entry.display() instanceof ItemAttributeModifiers.Display.Hidden)) {
                    problems.add(type + " shows its " + entry.attribute().getRegisteredName() + " modifier in the tooltip");
                }
            }
            if (TweaksBlocks.head(type) == null || TweaksBlocks.wallHead(type) == null
                    || TweaksBlocks.wallHead(type).getLootTable() != TweaksBlocks.head(type).getLootTable()) {
                problems.add(type + " has no standing/wall block pair sharing one loot table");
            }
        }
        helper.assertTrue(problems.isEmpty(), "mob heads: " + problems);
        helper.succeed();
    }

    // =====================================================================================
    // Flypad I
    // =====================================================================================

    /**
     * Flypad I entsteht formlos an der Werkbank aus Enderit-Kern, Enderit-Druckplatte, Shulkerkopf und einer Elytra
     * mit Reparatur (Besitzer 2026-09-29) - in jeder Anordnung. Eine Elytra ohne Verzauberung oder nur mit
     * Haltbarkeit passt nicht, ohne Shulkerkopf (ein anderer Kopf) auch nicht. Rezeptbuch und JEI zeigen die Elytra
     * verzaubert, mit dem Namen der Verzauberung als Zeile.
     */
    public static void flypadOneNeedsTheShulkerHeadAndAnElytraWithMending(GameTestHelper helper) {
        ItemStack mending = enchanted(helper, Enchantments.MENDING);
        ItemStack unbreaking = enchanted(helper, Enchantments.UNBREAKING);
        ItemStack core = new ItemStack(ModItems.ENDERITE_CORE);
        ItemStack plate = new ItemStack(TweaksBlocks.ENDERITE_PRESSURE_PLATE);
        ItemStack shulker = new ItemStack(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SHULKER));

        Optional<RecipeHolder<CraftingRecipe>> match = crafting(helper, List.of(core, plate, shulker, mending));
        helper.assertTrue(match.isPresent(), "core + plate + shulker head + mending elytra made nothing");
        helper.assertTrue(match.get().value() instanceof EnchantedShapelessRecipe, "the flypad recipe is " + match.get().value().getClass());
        ItemStack out = match.get().value().assemble(input(List.of(core, plate, shulker, mending)));
        helper.assertTrue(out.is(TweaksBlocks.FLYPAD.asItem()), "the recipe made " + out + " instead of a flypad");
        helper.assertTrue(crafting(helper, List.of(mending, shulker, plate, core)).isPresent(), "the flypad recipe is not shapeless");

        helper.assertTrue(crafting(helper, List.of(core, plate, shulker, new ItemStack(Items.ELYTRA))).isEmpty(),
                "a plain elytra made a flypad");
        helper.assertTrue(crafting(helper, List.of(core, plate, shulker, unbreaking)).isEmpty(),
                "an elytra with Unbreaking but no Mending made a flypad");
        helper.assertTrue(crafting(helper, List.of(core, plate, new ItemStack(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SPIDER)), mending)).isEmpty(),
                "a spider head instead of the shulker head made a flypad");
        helper.assertTrue(crafting(helper, List.of(core, plate, mending)).isEmpty(), "three ingredients made a flypad");

        EnchantedShapelessRecipe recipe = (EnchantedShapelessRecipe) match.get().value();
        helper.assertTrue(recipe.enchantment().equals(Enchantments.MENDING) && recipe.enchantedItem() == Items.ELYTRA,
                "the recipe asks for " + recipe.enchantment() + " on " + recipe.enchantedItem());
        boolean shown = false;
        for (SlotDisplay slot : recipe.ingredientDisplays()) {
            if (slot instanceof SlotDisplay.ItemStackSlotDisplay stackSlot && stackSlot.stack().item().value() == Items.ELYTRA) {
                ItemStack shownStack = stackSlot.stack().create();
                shown = Boolean.TRUE.equals(shownStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
                        && shownStack.get(DataComponents.LORE) != null;
            }
        }
        helper.assertTrue(shown, "recipe book and JEI do not show the elytra as enchanted");

        // Elytra-Pad I genauso (Besitzer 2026-10-01): Diamantkern + Diamant-Druckplatte + Elytra mit Reparatur.
        ItemStack diamondCore = new ItemStack(ModItems.DIAMOND_CORE);
        ItemStack diamondPlate = new ItemStack(TweaksBlocks.DIAMOND_PRESSURE_PLATE);
        Optional<RecipeHolder<CraftingRecipe>> pad = crafting(helper, List.of(diamondPlate, mending, diamondCore));
        helper.assertTrue(pad.isPresent() && pad.get().value().assemble(input(List.of(diamondPlate, mending, diamondCore)))
                .is(TweaksBlocks.ELYTRA_PAD.asItem()), "diamond core + diamond plate + mending elytra made no elytra pad");
        helper.assertTrue(crafting(helper, List.of(diamondCore, diamondPlate, new ItemStack(Items.ELYTRA))).isEmpty(),
                "a plain elytra made an elytra pad");
        helper.assertTrue(crafting(helper, List.of(diamondCore, diamondPlate, unbreaking)).isEmpty(),
                "an elytra with Unbreaking but no Mending made an elytra pad");
        helper.succeed();
    }

    // =====================================================================================
    // Geheime Faehigkeiten
    // =====================================================================================

    /** Lohenkopf: Magmablock und Lagerfeuer brennen nicht; Feuer selbst und alles ohne Kopf schon. */
    public static void blazeHeadWearersIgnoreMagmaBlocksAndCampfires(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = vulnerable(helper, mockPlayer(helper, new Vec3(2.5, 2.0, 2.5)));
        wear(player, BlazeHeadType.BLAZE);
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().hotFloor()), "the blaze head did not stop the magma block");
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().campfire()), "the blaze head did not stop the campfire");
        helper.assertFalse(player.isInvulnerableTo(level, level.damageSources().inFire()), "the blaze head made its wearer fireproof");
        float health = player.getHealth();
        player.hurtServer(level, level.damageSources().hotFloor(), 2.0F);
        helper.assertValueEqual(player.getHealth(), health, "health after a magma block with the blaze head");
        wear(player, null);
        helper.assertFalse(player.isInvulnerableTo(level, level.damageSources().hotFloor()), "the magma block spares a player without the head");
        helper.succeed();
    }

    /** Endermankopf: der Enderperlen-Teleport kostet keine Herzen; Fallschaden bleibt, ohne Kopf kostet die Perle. */
    public static void endermanHeadWearersTakeNoEnderPearlDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = vulnerable(helper, mockPlayer(helper, new Vec3(2.5, 2.0, 2.5)));
        wear(player, BlazeHeadType.ENDERMAN);
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().enderPearl()), "the enderman head did not stop the pearl damage");
        helper.assertFalse(player.isInvulnerableTo(level, level.damageSources().fall()), "the enderman head stopped fall damage");
        wear(player, null);
        helper.assertFalse(player.isInvulnerableTo(level, level.damageSources().enderPearl()), "the pearl spares a player without the head");
        helper.succeed();
    }

    /**
     * Wuestenzombiekopf: Essen gibt keinen Hunger-Effekt - verdorbenes Fleisch (echte Verzehr-Wirkung, 30 Bissen)
     * und ein fester Test mit sicherer Wirkung; andere Effekte desselben Essens bleiben (Kugelfisch), ein Trank mit
     * Hunger wirkt weiter. Ohne Kopf macht verdorbenes Fleisch wie gewohnt Hunger.
     */
    public static void huskHeadSkipsTheHungerOfFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        wear(player, BlazeHeadType.HUSK);
        eat(level, player, Items.ROTTEN_FLESH, 30);
        helper.assertFalse(player.hasEffect(MobEffects.HUNGER), "rotten flesh gave Hunger to a player wearing the husk head");
        new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 600)).apply(level, new ItemStack(Items.ROTTEN_FLESH), player);
        helper.assertFalse(player.hasEffect(MobEffects.HUNGER), "a sure Hunger effect of food reached the husk head wearer");
        eat(level, player, Items.PUFFERFISH, 1);
        helper.assertTrue(player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.HUNGER),
                "a pufferfish should still poison the husk head wearer, but without Hunger");
        new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 600)).apply(level, new ItemStack(Items.POTION), player);
        helper.assertTrue(player.hasEffect(MobEffects.HUNGER), "the husk head also blocked Hunger from something that is no food");
        player.removeAllEffects();
        wear(player, null);
        eat(level, player, Items.ROTTEN_FLESH, 30);
        helper.assertTrue(player.hasEffect(MobEffects.HUNGER), "30 bites of rotten flesh gave no Hunger without the head");
        helper.succeed();
    }

    /** Sumpfskelettschaedel: Essen vergiftet nicht (giftige Kartoffel, Kugelfisch - dessen Hunger bleibt); ohne ihn schon. */
    public static void boggedSkullSkipsThePoisonOfFood(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        wear(player, BlazeHeadType.BOGGED);
        eat(level, player, Items.POISONOUS_POTATO, 30);
        helper.assertFalse(player.hasEffect(MobEffects.POISON), "a poisonous potato poisoned the bogged skull wearer");
        eat(level, player, Items.PUFFERFISH, 1);
        helper.assertTrue(!player.hasEffect(MobEffects.POISON) && player.hasEffect(MobEffects.HUNGER),
                "a pufferfish should give the bogged skull wearer Hunger but no Poison");
        player.removeAllEffects();
        wear(player, null);
        eat(level, player, Items.POISONOUS_POTATO, 30);
        helper.assertTrue(player.hasEffect(MobEffects.POISON), "30 poisonous potatoes did not poison a player without the skull");
        helper.succeed();
    }

    /** Spinnenkopf: ein Spinnennetz bremst nicht (volle Strecke statt einem Viertel); ohne Kopf bremst es. */
    public static void spiderHeadWearersAreNotSlowedByCobwebs(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 4.0, 2.5));
        wear(player, BlazeHeadType.SPIDER);
        helper.assertValueEqual(stuckStep(player), 1.0, "distance of a 1 block step in a cobweb with the spider head");
        wear(player, null);
        helper.assertValueEqual(stuckStep(player), 0.25, "distance of a 1 block step in a cobweb without the head");
        helper.succeed();
    }

    /** Hoehlenspinnenkopf: die blosse Hand baut Spinnennetze so schnell ab wie ein Schwert; anderes nicht schneller. */
    public static void caveSpiderHeadCutsCobwebsAsFastAsSwords(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        player.setOnGround(true);
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        BlockState cobweb = Blocks.COBWEB.defaultBlockState();
        float bare = player.getDestroySpeed(cobweb);
        wear(player, BlazeHeadType.CAVE_SPIDER);
        helper.assertValueEqual(player.getDestroySpeed(cobweb), 15.0F, "grounded cobweb speed with the cave spider head");
        helper.assertValueEqual(player.getDestroySpeed(Blocks.STONE.defaultBlockState()), 1.0F, "bare hand speed on stone with the head");
        player.setOnGround(false);
        float airborneHeadSpeed = player.getDestroySpeed(cobweb);
        wear(player, null);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        helper.assertValueEqual(airborneHeadSpeed, player.getDestroySpeed(cobweb), "head and sword share the airborne penalty");
        player.setOnGround(true);
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        wear(player, null);
        helper.assertTrue(bare < HeadAbilities.COBWEB_SPEED, "a bare hand already cuts cobwebs at " + bare);
        helper.assertValueEqual(player.getDestroySpeed(cobweb), bare, "bare hand speed on a cobweb without the head");
        helper.succeed();
    }

    /** Eiswandererschaedel: Pulverschnee friert nicht (wie ein Lederhelm, Tag freeze_immune_wearables); ohne ihn schon. */
    public static void straySkullKeepsPowderSnowFromFreezing(GameTestHelper helper) {
        ServerPlayer player = vulnerable(helper, mockPlayer(helper, new Vec3(2.5, 2.0, 2.5)));
        helper.assertTrue(player.canFreeze(), "a player without the skull cannot freeze - the test proves nothing");
        wear(player, BlazeHeadType.STRAY);
        helper.assertTrue(new ItemStack(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.STRAY)).is(ItemTags.FREEZE_IMMUNE_WEARABLES), "the stray skull is not freeze immune");
        helper.assertFalse(player.canFreeze(), "the stray skull wearer can still freeze");
        helper.succeed();
    }

    /**
     * Schleimkopf: ein Block mehr sicherer Fall (4 statt 3) - ein Fall ueber vier Bloecke kostet nichts, ohne Kopf
     * ein halbes Herz.
     */
    public static void slimeHeadAddsOneBlockOfSafeFall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = vulnerable(helper, mockPlayer(helper, new Vec3(2.5, 2.0, 2.5)));
        wear(player, BlazeHeadType.SLIME);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 4.0, "safe fall distance with the slime head");
        float health = player.getHealth();
        player.causeFallDamage(4.0, 1.0F, level.damageSources().fall());
        helper.assertValueEqual(player.getHealth(), health, "health after a 4 block fall with the slime head");
        wear(player, null);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 3.0, "safe fall distance without the head");
        player.causeFallDamage(4.0, 1.0F, level.damageSources().fall());
        helper.assertTrue(player.getHealth() < health, "a 4 block fall without the slime head did not hurt");
        helper.succeed();
    }

    /** Silberfischchenkopf: der Traeger ist halb so gross (Attribut minecraft:scale, Hitbox mit); abgesetzt wieder normal. */
    public static void silverfishHeadShrinksTheWearerToHalfSize(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        float height = player.getBbHeight();
        wear(player, BlazeHeadType.SILVERFISH);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 0.5, "scale with the silverfish head");
        helper.assertTrue(Math.abs(player.getBbHeight() - height / 2.0F) < 0.01F,
                "hitbox height with the silverfish head is " + player.getBbHeight() + " instead of " + height / 2.0F);
        wear(player, null);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 1.0, "scale after taking the silverfish head off");
        helper.succeed();
    }

    /** Breezekopf: wer ihn traegt, zertrampelt kein Ackerland, auch nicht aus 10 Bloecken; ohne ihn schon. */
    public static void breezeHeadWearersDoNotTrampleFarmland(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos farmland = new BlockPos(2, 1, 2);
        helper.setBlock(farmland, Blocks.FARMLAND);
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        wear(player, BlazeHeadType.BREEZE);
        BlockState state = helper.getBlockState(farmland);
        state.getBlock().fallOn(level, state, helper.absolutePos(farmland), player, 10.0);
        helper.assertTrue(helper.getBlockState(farmland).is(Blocks.FARMLAND), "the breeze head wearer trampled the farmland");
        wear(player, null);
        state.getBlock().fallOn(level, state, helper.absolutePos(farmland), player, 10.0);
        helper.assertFalse(helper.getBlockState(farmland).is(Blocks.FARMLAND), "a 10 block fall without the head did not trample the farmland");
        helper.succeed();
    }

    /** Shulkerkopf: eine Shulkerkiste mit Stein auf dem Deckel geht auf; ohne Kopf bleibt sie zu. */
    public static void shulkerHeadOpensBlockedShulkerBoxes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos box = new BlockPos(2, 1, 2);
        helper.setBlock(box, Blocks.SHULKER_BOX);
        helper.setBlock(box.above(), Blocks.STONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 1.0, 4.5));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(box)), Direction.UP, helper.absolutePos(box), false);
        helper.getBlockState(box).useWithoutItem(level, player, hit);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "a blocked shulker box opened without the head");
        wear(player, BlazeHeadType.SHULKER);
        helper.getBlockState(box).useWithoutItem(level, player, hit);
        helper.assertTrue(player.containerMenu instanceof ShulkerBoxMenu, "the shulker head did not open the blocked box: " + player.containerMenu);
        player.closeContainer();
        helper.succeed();
    }

    /** Ertrunkenenkopf: eine Magma-Blasensaeule zieht nicht nach unten; eine aufwaerts und ohne Kopf wirken wie immer. */
    public static void drownedHeadResistsDownwardBubbleColumns(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        wear(player, BlazeHeadType.DROWNED);
        player.setDeltaMovement(Vec3.ZERO);
        player.onInsideBubbleColumn(true);
        helper.assertValueEqual(player.getDeltaMovement().y, 0.0, "downward speed in a magma bubble column with the drowned head");
        player.onInsideBubbleColumn(false);
        helper.assertTrue(player.getDeltaMovement().y > 0.0, "a soul sand bubble column no longer lifts the drowned head wearer");
        wear(player, null);
        player.setDeltaMovement(Vec3.ZERO);
        player.onInsideBubbleColumn(true);
        helper.assertTrue(player.getDeltaMovement().y < 0.0, "a magma bubble column does not pull a player without the head");
        helper.succeed();
    }

    // =====================================================================================
    // Helfer
    // =====================================================================================

    private static void wear(ServerPlayer player, BlazeHeadType type) {
        player.setItemSlot(EquipmentSlot.HEAD, type == null ? ItemStack.EMPTY : new ItemStack(TweaksItems.head(type)));
    }

    /** Ein 1-Block-Schritt, nachdem ein Spinnennetz den Spieler gepackt hat (wie {@code WebBlock#entityInside}). */
    private static double stuckStep(ServerPlayer player) {
        Vec3 start = player.position();
        player.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25, 0.05, 0.25));
        player.move(MoverType.SELF, new Vec3(1.0, 0.0, 0.0));
        double moved = player.position().x - start.x;
        player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
        return Math.round(moved * 1000.0) / 1000.0;
    }

    /** Wendet die Verzehr-Wirkungen des Essens {@code times} Mal an (wie ein fertiger Bissen, ohne Essanimation). */
    private static void eat(ServerLevel level, ServerPlayer player, Item food, int times) {
        for (int i = 0; i < times; i++) {
            ItemStack stack = new ItemStack(food);
            Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            for (ConsumeEffect effect : consumable.onConsumeEffects()) {
                effect.apply(level, stack, player);
            }
        }
    }

    private static ItemStack enchanted(GameTestHelper helper, net.minecraft.resources.ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemStack stack = new ItemStack(Items.ELYTRA);
        stack.enchant(holder, 1);
        return stack;
    }

    private static CraftingInput input(List<ItemStack> stacks) {
        List<ItemStack> grid = new ArrayList<>(stacks);
        while (grid.size() < 4) {
            grid.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(2, 2, grid);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> crafting(GameTestHelper helper, List<ItemStack> stacks) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input(stacks), helper.getLevel());
    }

    private static Creeper chargedCreeper(GameTestHelper helper, BlockPos pos) {
        Creeper creeper = helper.spawn(EntityTypes.CREEPER, pos);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        creeper.thunderHit(helper.getLevel(), bolt);
        creeper.clearFire();
        creeper.setHealth(creeper.getMaxHealth());
        return creeper;
    }

    private static LivingEntity spawn(GameTestHelper helper, EntityType<?> type, BlockPos pos) {
        return (LivingEntity) helper.spawn(type, pos);
    }

    private static int count(GameTestHelper helper, Item item) {
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    private static void clearItems(GameTestHelper helper) {
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            entity.discard();
        }
    }

    /** Macht den Mock-Spieler verwundbar wie einen Ueberlebensspieler (siehe docs der Gametest-Fallen). */
    private static ServerPlayer vulnerable(GameTestHelper helper, ServerPlayer player) {
        player.getAbilities().invulnerable = false;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        McVersion.setInvulnerable(player, false);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return player;
    }

    @SuppressWarnings("removal")
    // =====================================================================================
    // N49: the extra head slot of the inventory (ExtraHead)
    // =====================================================================================

    /** The slot sits above the off-hand slot, level with the helmet, and takes only mob heads and carved pumpkins. */
    public static void extraHeadSlotTakesOnlyHeadsAndCarvedPumpkins(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        net.minecraft.world.inventory.InventoryMenu menu = player.inventoryMenu;
        helper.assertTrue(menu.slots.size() >= 47, "inventory menu has no slot after Vanilla's 46");
        net.minecraft.world.inventory.Slot slot = menu.slots.get(46);
        helper.assertTrue(slot instanceof com.simplebuilding.util.ExtraHead.HeadSlot, "slot 46 is not the extra head slot");
        helper.assertValueEqual(slot.x, 77, "head slot x (off-hand column)");
        helper.assertValueEqual(slot.y, 8, "head slot y (helmet row)");
        for (Item ok : List.of(Items.ZOMBIE_HEAD, Items.SKELETON_SKULL, Items.DRAGON_HEAD, Items.PIGLIN_HEAD, Items.CARVED_PUMPKIN,
                TweaksItems.head(BlazeHeadType.SILVERFISH), TweaksItems.head(BlazeHeadType.BLAZE))) {
            helper.assertTrue(slot.mayPlace(new ItemStack(ok)), "the extra head slot refuses " + ok);
        }
        for (Item no : List.of(Items.IRON_HELMET, Items.PUMPKIN, Items.DIRT, Items.TURTLE_HELMET)) {
            helper.assertFalse(slot.mayPlace(new ItemStack(no)), "the extra head slot takes " + no);
        }
        helper.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.ZOMBIE_HEAD, 64)), 1, "the head slot holds one head");
        helper.succeed();
    }

    /** Shift-click: with a helmet on, a head goes into the extra slot and back out again - never duplicated. */
    public static void extraHeadShiftClickMovesWithoutDuplicating(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.getInventory().setItem(9, new ItemStack(Items.ZOMBIE_HEAD));
        net.minecraft.world.inventory.InventoryMenu menu = player.inventoryMenu;
        menu.quickMoveStack(player, 9);
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).is(Items.ZOMBIE_HEAD), "shift-click did not fill the extra head slot");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "the head stayed in the inventory as well");
        menu.quickMoveStack(player, 46);
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).isEmpty(), "shift-click did not empty the extra head slot");
        helper.assertValueEqual(player.getInventory().countItem(Items.ZOMBIE_HEAD), 1, "zombie heads after moving in and out");
        helper.succeed();
    }

    /** Saved with the player, kept through the End portal, dropped on death. */
    public static void extraHeadIsSavedKeptAndDroppedOnDeath(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(Items.CREEPER_HEAD));
        net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        net.minecraft.nbt.CompoundTag saved = output.buildResult();
        helper.assertTrue(saved.contains(com.simplebuilding.util.ExtraHead.SAVE_KEY), "the player save has no extra head");
        ServerPlayer loaded = mockPlayer(helper, new Vec3(3.5, 2.0, 2.5));
        loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), saved));
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(loaded).is(Items.CREEPER_HEAD), "the extra head was not loaded");

        ServerPlayer travelled = mockPlayer(helper, new Vec3(4.5, 2.0, 2.5));
        travelled.restoreFrom(player, true);
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(travelled).is(Items.CREEPER_HEAD), "the extra head was lost in the End portal");

        // The death drop step itself (Player.dropEquipment, what die() runs without keepInventory): a mock player's
        // die() is not reliable across loaders (Forge's drop capture, Simple Respawn's downed state).
        try {
            java.lang.reflect.Method drop = net.minecraft.world.entity.player.Player.class.getDeclaredMethod("dropEquipment", ServerLevel.class);
            drop.setAccessible(true);
            drop.invoke(player, helper.getLevel());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).isEmpty(), "the dead player still has the extra head");
        boolean dropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4.0),
                e -> e.getItem().is(Items.CREEPER_HEAD)).isEmpty();
        helper.assertTrue(dropped, "the extra head was not dropped on death");
        helper.succeed();
    }

    /**
     * BS25-19: a real death (not the drop step alone) with the soulfire blaze head in the extra head slot and on the helmet
     * slot: the player dies, the extra head drops, the dead player ticks on until the respawn, and the respawned player lives.
     */
    public static void extraHeadDeathAndRespawnKeepTheServerRunning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item head = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.Identifier.parse("simplemobheads:soulfire_blaze_head")).orElse(Items.CREEPER_HEAD);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(head));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
        player.hurtServer(level, level.damageSources().lava(), 100000.0F);
        helper.assertTrue(player.isDeadOrDying(), "the wearer did not die");
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).isEmpty(), "the dead player still has the extra head");
        for (int i = 0; i < 40; i++) player.doTick();
        ServerPlayer fresh = level.getServer().getPlayerList().respawn(player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        for (int i = 0; i < 3; i++) fresh.doTick();
        helper.assertTrue(fresh.isAlive(), "the respawned player is not alive");
        level.getServer().getPlayerList().remove(fresh);
        long start = level.getGameTime();
        helper.succeedWhen(() -> helper.assertTrue(level.getGameTime() >= start + 5, "the server stopped ticking"));
    }

    /** Effects: a carved pumpkin calms endermen, mod heads keep their ability, a zombie head hides from zombies. */
    public static void extraHeadEffectsWorkLikeTheHelmetSlot(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(TweaksItems.head(BlazeHeadType.BLAZE)));
        helper.assertTrue(HeadAbilities.wears(player, BlazeHeadType.BLAZE), "the blaze head ability ignores the extra head slot");

        net.minecraft.world.entity.Mob enderman = (net.minecraft.world.entity.Mob) helper.spawn(EntityTypes.ENDERMAN, new Vec3(1.5, 2.0, 5.5));
        enderman.setNoAi(true);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, enderman.getEyePosition());
        try {
            java.lang.reflect.Method stared = enderman.getClass().getDeclaredMethod("isBeingStaredBy", net.minecraft.world.entity.player.Player.class);
            stared.setAccessible(true);
            com.simplebuilding.util.ExtraHead.set(player, ItemStack.EMPTY);
            helper.assertTrue((Boolean) stared.invoke(enderman, player), "test setup: the enderman does not see the stare");
            com.simplebuilding.util.ExtraHead.set(player, new ItemStack(Items.CARVED_PUMPKIN));
            helper.assertFalse((Boolean) stared.invoke(enderman, player), "a carved pumpkin in the extra head slot does not calm endermen");

            net.minecraft.world.entity.Mob zombie = (net.minecraft.world.entity.Mob) helper.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
            zombie.setNoAi(true);
            java.lang.reflect.Method visibility = null;
            for (java.lang.reflect.Method m : LivingEntity.class.getMethods()) {
                if (m.getName().equals("getVisibilityPercent") && m.getParameterCount() == 2) visibility = m;
            }
            if (visibility != null) { // 26.3 (MOB_VISIBILITY); the 26.2 twin has no effect yet
                com.simplebuilding.util.ExtraHead.set(player, ItemStack.EMPTY);
                double plain = (Double) visibility.invoke(player, helper.getLevel(), zombie);
                com.simplebuilding.util.ExtraHead.set(player, new ItemStack(Items.ZOMBIE_HEAD));
                double hidden = (Double) visibility.invoke(player, helper.getLevel(), zombie);
                helper.assertTrue(Math.abs(hidden - plain * 0.5) < 1.0e-6, "zombie head in the extra slot: " + hidden + " instead of half of " + plain);
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.ZOMBIE_HEAD));
                double both = (Double) visibility.invoke(player, helper.getLevel(), zombie);
                helper.assertTrue(Math.abs(both - plain * 0.5) < 1.0e-6, "two zombie heads stack: " + both);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.succeed();
    }

    /**
     * N55: what Vanilla only gives the helmet slot works from the extra head slot too - the head item's attribute modifiers
     * (slime head: one block more safe fall, silverfish head: half size, every mob head: hidden from the locator bar) and
     * the freeze immunity of the stray head. The same head in both slots counts once; taking the extra head out removes it.
     */
    public static void extraHeadAttributesAndFreezeWorkLikeTheHelmetSlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = vulnerable(helper, mockPlayer(helper, new Vec3(2.5, 2.0, 2.5)));
        helper.assertTrue(player.canFreeze(), "a player without a head cannot freeze - the test proves nothing");
        double locator = player.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE);
        float health = player.getHealth();

        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(TweaksItems.head(BlazeHeadType.SLIME)));
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 4.0, "safe fall distance with the slime head in the extra slot");
        player.causeFallDamage(4.0, 1.0F, level.damageSources().fall());
        helper.assertValueEqual(player.getHealth(), health, "health after a 4 block fall with the slime head in the extra slot");
        helper.assertTrue(player.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE) < locator, "the extra head does not hide the wearer from the locator bar");

        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(TweaksItems.head(BlazeHeadType.SILVERFISH)));
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 3.0, "the slime head's safe fall stayed after it left the extra slot");
        helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 0.5, "scale with the silverfish head in the extra slot");

        // the same head in the helmet slot: one effect, not two - and it stays when only the extra head goes
        wear(player, BlazeHeadType.SILVERFISH);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 0.5, "scale with the silverfish head in both slots");
        com.simplebuilding.util.ExtraHead.set(player, ItemStack.EMPTY);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 0.5, "scale after taking only the extra head out");
        wear(player, null);
        player.doTick();
        helper.assertValueEqual(player.getAttributeValue(Attributes.SCALE), 1.0, "scale without any head");
        helper.assertValueEqual(player.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE), locator, "locator range without any head");

        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(TweaksItems.head(BlazeHeadType.STRAY)));
        helper.assertFalse(player.canFreeze(), "the stray head in the extra slot does not protect from powder snow");
        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(Items.ZOMBIE_HEAD));
        helper.assertTrue(player.canFreeze(), "a zombie head in the extra slot protects from powder snow");
        helper.succeed();
    }

    /**
     * N55: with both slots full the effects of both heads apply (one effect per head kind), and the extra head comes first
     * in {@link com.simplebuilding.util.ExtraHead#heads} - the order the model is picked in (the client draws the first).
     */
    public static void helmetHeadAndExtraHeadBothApplyAndTheExtraHeadComesFirst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = vulnerable(helper, mockPlayer(helper, new Vec3(2.5, 2.0, 2.5)));
        wear(player, BlazeHeadType.BLAZE);
        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(TweaksItems.head(BlazeHeadType.ENDERMAN)));
        helper.assertTrue(HeadAbilities.wears(player, BlazeHeadType.BLAZE) && HeadAbilities.wears(player, BlazeHeadType.ENDERMAN),
                "not both heads count");
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().hotFloor()), "the blaze head in the helmet slot lost its effect");
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().enderPearl()), "the enderman head in the extra slot has no effect");
        List<ItemStack> heads = com.simplebuilding.util.ExtraHead.heads(player);
        helper.assertValueEqual(heads.size(), 2, "worn heads");
        helper.assertTrue(heads.get(0).is(TweaksItems.head(BlazeHeadType.ENDERMAN)) && heads.get(1).is(TweaksItems.head(BlazeHeadType.BLAZE)),
                "the extra head is not drawn first: " + heads);
        // a helmet in the helmet slot is armor, not a head: only the extra head is listed, and it is drawn under the helmet
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        helper.assertValueEqual(com.simplebuilding.util.ExtraHead.heads(player).size(), 1, "worn heads under an iron helmet");
        helper.assertFalse(player.isInvulnerableTo(level, level.damageSources().hotFloor()), "an iron helmet kept the blaze effect");
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().enderPearl()), "the enderman head lost its effect under a helmet");
        // a pumpkin in the extra slot and a skull in the helmet slot: both count, the pumpkin is drawn
        wear(player, BlazeHeadType.BLAZE);
        com.simplebuilding.util.ExtraHead.set(player, new ItemStack(Items.CARVED_PUMPKIN));
        helper.assertTrue(com.simplebuilding.util.ExtraHead.heads(player).getFirst().is(Items.CARVED_PUMPKIN), "the pumpkin is not drawn first");
        helper.assertTrue(player.isInvulnerableTo(level, level.damageSources().hotFloor()), "the blaze head lost its effect next to a pumpkin");

        // camouflage of two different mob heads: zombies and skeletons both lose sight (26.3: MOB_VISIBILITY)
        try {
            java.lang.reflect.Method visibility = null;
            for (java.lang.reflect.Method m : LivingEntity.class.getMethods()) {
                if (m.getName().equals("getVisibilityPercent") && m.getParameterCount() == 2) visibility = m;
            }
            if (visibility != null) {
                net.minecraft.world.entity.Mob zombie = (net.minecraft.world.entity.Mob) helper.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 2.0, 1.5));
                net.minecraft.world.entity.Mob skeleton = (net.minecraft.world.entity.Mob) helper.spawn(EntityTypes.SKELETON, new Vec3(4.5, 2.0, 1.5));
                zombie.setNoAi(true);
                skeleton.setNoAi(true);
                wear(player, null);
                com.simplebuilding.util.ExtraHead.set(player, ItemStack.EMPTY);
                double plainZombie = (Double) visibility.invoke(player, level, zombie);
                double plainSkeleton = (Double) visibility.invoke(player, level, skeleton);
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.ZOMBIE_HEAD));
                com.simplebuilding.util.ExtraHead.set(player, new ItemStack(Items.SKELETON_SKULL));
                double zombieSees = (Double) visibility.invoke(player, level, zombie);
                double skeletonSees = (Double) visibility.invoke(player, level, skeleton);
                helper.assertTrue(Math.abs(zombieSees - plainZombie * 0.5) < 1.0e-6, "zombie head in the helmet slot next to an extra head: " + zombieSees + " of " + plainZombie);
                helper.assertTrue(Math.abs(skeletonSees - plainSkeleton * 0.5) < 1.0e-6, "skeleton skull in the extra slot next to a helmet head: " + skeletonSees + " of " + plainSkeleton);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.succeed();
    }

    /**
     * Owner SP22-11: a right-click into the air with a mob head puts it into the extra head slot like a helmet (equip
     * sound), swaps a worn one, takes one from a stack; the same head, a carved pumpkin and a click on a block stay Vanilla.
     */
    public static void extraHeadRightClickInTheAirPutsOnAndSwaps(GameTestHelper helper) {
        // The in-level mock ServerPlayer is always creative (keeps the item like Vanilla's helmet swap), so the
        // survival behaviour runs on a survival mock player through Item#use; the equip sound has its own test.
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.assertFalse(player.isCreative(), "the test player is in creative mode");
        ServerLevel level = helper.getLevel();
        net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        java.util.function.Function<ItemStack, net.minecraft.world.InteractionResult> use = stack -> {
            net.minecraft.world.InteractionResult result = stack.use(level, player, hand);
            if (result instanceof net.minecraft.world.InteractionResult.Success success && success.heldItemTransformedTo() != null) {
                player.setItemInHand(hand, success.heldItemTransformedTo());
            }
            return result;
        };
        player.setItemInHand(hand, new ItemStack(Items.ZOMBIE_HEAD));
        helper.assertTrue(use.apply(player.getItemInHand(hand)).consumesAction(), "right-click in the air did nothing");
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).is(Items.ZOMBIE_HEAD) && player.getItemInHand(hand).isEmpty(),
                "the zombie head did not go into the empty slot: " + com.simplebuilding.util.ExtraHead.get(player) + " / " + player.getItemInHand(hand));
        player.setItemInHand(hand, new ItemStack(Items.CREEPER_HEAD));
        use.apply(player.getItemInHand(hand));
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).is(Items.CREEPER_HEAD) && player.getItemInHand(hand).is(Items.ZOMBIE_HEAD),
                "the heads did not swap: " + com.simplebuilding.util.ExtraHead.get(player) + " / " + player.getItemInHand(hand));
        player.setItemInHand(hand, new ItemStack(Items.CREEPER_HEAD));
        helper.assertFalse(use.apply(player.getItemInHand(hand)).consumesAction(), "the same head swapped");
        helper.assertTrue(player.getItemInHand(hand).is(Items.CREEPER_HEAD), "the same head left the hand");
        Item blaze = TweaksItems.head(BlazeHeadType.BLAZE);
        player.setItemInHand(hand, new ItemStack(blaze, 3));
        use.apply(player.getItemInHand(hand));
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).is(blaze) && player.getItemInHand(hand).getCount() == 2,
                "one mod head from the stack: " + com.simplebuilding.util.ExtraHead.get(player) + " / " + player.getItemInHand(hand));
        helper.assertValueEqual(player.getInventory().countItem(Items.CREEPER_HEAD), 1, "the old head went into the inventory");
        player.setItemInHand(hand, new ItemStack(Items.CARVED_PUMPKIN));
        use.apply(player.getItemInHand(hand));
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).is(blaze) && player.getItemInHand(hand).is(Items.CARVED_PUMPKIN), "a carved pumpkin was swapped in");
        BlockPos floor = new BlockPos(0, 1, 0);
        helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
        BlockPos abs = helper.absolutePos(floor);
        player.setItemInHand(hand, new ItemStack(Items.SKELETON_SKULL));
        net.minecraft.world.phys.BlockHitResult top = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0), Direction.UP, abs, false);
        helper.assertTrue(Items.SKELETON_SKULL.useOn(new net.minecraft.world.item.context.UseOnContext(player, hand, top)).consumesAction(), "the skull was not placed on the block");
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.SKELETON_SKULL, floor.above());
        helper.assertTrue(com.simplebuilding.util.ExtraHead.get(player).is(blaze), "placing a skull changed the slot");
        helper.succeed();
    }

    /** N55: putting a head into the extra slot sounds like putting on a helmet (once, not on login, not on taking it out). */
    public static void extraHeadPutOnPlaysTheEquipSound(GameTestHelper helper) {
        List<net.minecraft.network.protocol.Packet<?>> sink = new ArrayList<>();
        ServerPlayer listener = MusicDiscTests.capturingPlayer(helper, sink);
        ServerPlayer wearer = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        listener.snapTo(wearer.getX() + 1.0, wearer.getY(), wearer.getZ(), 0.0F, 0.0F);
        Holder<net.minecraft.sounds.SoundEvent> expected = new ItemStack(Items.ZOMBIE_HEAD).get(DataComponents.EQUIPPABLE).equipSound();
        java.util.function.Supplier<Long> heard = () -> {
            synchronized (sink) {
                return sink.stream().filter(net.minecraft.network.protocol.game.ClientboundSoundPacket.class::isInstance)
                        .map(net.minecraft.network.protocol.game.ClientboundSoundPacket.class::cast)
                        .filter(p -> p.getSound().value().equals(expected.value()) && Math.abs(p.getX() - wearer.getX()) < 1.0 && Math.abs(p.getZ() - wearer.getZ()) < 1.0)
                        .count();
            }
        };
        com.simplebuilding.util.ExtraHead.set(wearer, new ItemStack(Items.CREEPER_HEAD));
        wearer.doTick(); // login: the head was there before, nothing to hear
        helper.assertValueEqual(heard.get(), 0L, "equip sounds when the player already wears the head");
        com.simplebuilding.util.ExtraHead.set(wearer, new ItemStack(Items.ZOMBIE_HEAD));
        wearer.doTick();
        helper.assertValueEqual(heard.get(), 1L, "equip sounds when the head goes into the extra slot");
        com.simplebuilding.util.ExtraHead.set(wearer, new ItemStack(Items.ZOMBIE_HEAD));
        wearer.doTick();
        helper.assertValueEqual(heard.get(), 1L, "equip sounds when the same head is put in again");
        com.simplebuilding.util.ExtraHead.set(wearer, ItemStack.EMPTY);
        wearer.doTick();
        helper.assertValueEqual(heard.get(), 1L, "equip sounds when the head is taken out");
        helper.succeed();
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}

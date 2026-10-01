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
        if (count(helper, TweaksItems.SPIDER_HEAD) != 0) {
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
        ItemStack shulker = new ItemStack(TweaksItems.SHULKER_HEAD);

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
        helper.assertTrue(crafting(helper, List.of(core, plate, new ItemStack(TweaksItems.SPIDER_HEAD), mending)).isEmpty(),
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
        helper.assertTrue(new ItemStack(TweaksItems.STRAY_SKULL).is(ItemTags.FREEZE_IMMUNE_WEARABLES), "the stray skull is not freeze immune");
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
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}

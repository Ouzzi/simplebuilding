package com.simplebuilding.dummy;

import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Stroh-Ruestungsstaender und Trainingspuppe (Besitzer 2026-10-02, docs/ai/PLAN-TRAINING-DUMMY-2026-10-02.md).
 * Eine Klasse, zwei Entity-Arten: {@code straw_armor_stand} benimmt sich wie ein Ruestungsstaender und wird mit einem
 * aufgesetzten geschnitzten Kuerbis zur {@code training_dummy}. Die Puppe nimmt nie Schaden und stirbt nie (kein XP,
 * kein Loot), zeigt aber jeden Treffer als schwebende Zahl: so viel, wie der Mob ihres Kopfes mit ihrer Ruestung
 * bekommen haette. Abbauen nur mit einem Schleich-Schlag.
 *
 * <p>Die Zahlen sind Vanilla-{@code text_display}-Entities (nur Server, keine Client-Klasse noetig); sie leben eine
 * Sekunde, tragen {@link #NUMBER_TAG} und werden nach dem Laden eines Chunks als Waisen weggeraeumt.
 */
public class TrainingDummy extends ArmorStand {
    public static final String NUMBER_TAG = "simplebuilding_dummy_number";
    /** Trefferpause wie {@code LivingEntity}: in den ersten 10 Ticks zaehlt nur ein staerkerer Treffer, und nur die Differenz. */
    public static final int COOLDOWN_TICKS = 10;
    /** So lange ohne Treffer, dann erscheint die Summe (und die Zaehlung beginnt neu). */
    public static final int IDLE_TICKS = 60;
    public static final int NUMBER_TICKS = 20;
    public static final int SUMMARY_TICKS = 60;
    /** Obergrenze gleichzeitig sichtbarer Zahlen je Puppe (Mehrfachschuss, Feuer, Explosionen). */
    public static final int MAX_NUMBERS = 12;
    private static final int ORPHAN_SCAN_TICKS = 100;

    private @Nullable EntityType<?> proxyType;
    private @Nullable Entity proxy;
    private float notedStrength;
    private long notedTick = Long.MIN_VALUE;
    private int notedBy = -1;
    private float lastHurtAmount;
    private long cooldownStart = Long.MIN_VALUE;
    private float total;
    private int hits;
    private long firstHit;
    private long lastHitTick;
    private float lastShown = Float.NaN;
    private boolean lastCrit;
    private final List<Number> numbers = new ArrayList<>();
    private @Nullable TextDisplay summary;
    private int summaryAge;
    private int scanIn = 1;

    private record Number(Display.TextDisplay display, int[] age) {
    }

    public TrainingDummy(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
    }

    /** Wie der Ruestungsstaender, nur ohne Rueckstoss: die Puppe bleibt stehen, auch bei Schlag, Bogen-Schlag und Windladung. */
    public static AttributeSupplier.Builder createAttributes() {
        return ArmorStand.createAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public boolean isTrainingDummy() {
        return ModEntities.TRAINING_DUMMY != null && this.getType() == ModEntities.TRAINING_DUMMY;
    }

    // ------------------------------------------------------------------ Stellvertreter

    /** Nie gespawnter Mob der Kopf-Art an der Stelle der Puppe (fuer Verzauberungs-Bedingungen), sonst {@code null}. */
    public @Nullable Entity proxy() {
        if (!this.isTrainingDummy() || !(this.level() instanceof ServerLevel server)) {
            return null;
        }
        EntityType<?> type = DummyTargets.typeForHead(this.getItemBySlot(EquipmentSlot.HEAD));
        if (type != this.proxyType) {
            this.proxyType = type;
            this.proxy = type == null ? null : type.create(server, EntitySpawnReason.COMMAND);
        }
        if (this.proxy != null) {
            this.proxy.setPos(this.getX(), this.getY(), this.getZ());
        }
        return this.proxy;
    }

    /** Vom {@code PlayerAttackDummyMixin}: Schlagstaerke des Spielers, bevor Vanilla sie zuruecksetzt. */
    public void noteAttack(Player player, float strength) {
        this.notedStrength = strength;
        this.notedTick = this.level().getGameTime();
        this.notedBy = player.getId();
    }

    // ------------------------------------------------------------------ Benutzen

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = super.interact(player, hand, location);
        if (!this.isTrainingDummy() && this.level() instanceof ServerLevel server && !this.isRemoved()
                && this.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN) && ModEntities.TRAINING_DUMMY != null) {
            convert(server);
        }
        return result;
    }

    /** Stroh-Ruestungsstaender mit Kuerbis wird zur Puppe: gleiche Stelle, Drehung, Ausruestung, Name. */
    private void convert(ServerLevel server) {
        TrainingDummy dummy = ModEntities.TRAINING_DUMMY.create(server, EntitySpawnReason.CONVERSION);
        if (dummy == null) {
            return;
        }
        dummy.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        dummy.setYBodyRot(this.getYRot());
        dummy.setYHeadRot(this.getYRot());
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            dummy.setItemSlot(slot, this.getItemBySlot(slot).copy());
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
        dummy.setCustomName(this.getCustomName());
        dummy.setCustomNameVisible(this.isCustomNameVisible());
        dummy.setShowArms(this.showArms());
        dummy.setNoBasePlate(!this.showBasePlate());
        dummy.setInvisible(this.isInvisible());
        dummy.setNoGravity(this.isNoGravity());
        this.discard();
        server.addFreshEntity(dummy);
        server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PUMPKIN_CARVE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()),
                this.getX(), this.getY(1.0), this.getZ(), 12, 0.2, 0.4, 0.2, 0.05);
        dummy.gameEvent(GameEvent.ENTITY_PLACE);
    }

    @Override
    public ItemStack getPickResult() {
        return ModItems.STRAW_ARMOR_STAND == null ? super.getPickResult() : new ItemStack(ModItems.STRAW_ARMOR_STAND);
    }

    // ------------------------------------------------------------------ Schaden

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isRemoved()) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        Player breaker = source.getEntity() instanceof Player p && source.getDirectEntity() == p
                && source.is(DamageTypeTags.IS_PLAYER_ATTACK) ? p : null;
        if (!this.isTrainingDummy()) {
            return hurtStraw(level, source, breaker);
        }
        if (breaker != null && breaker.isShiftKeyDown()) {
            if (!breaker.getAbilities().mayBuild) {
                return false;
            }
            breakApart(level, source, !breaker.hasInfiniteMaterials());
            return true;
        }
        return showHit(level, source, damage);
    }

    /** Der Stroh-Ruestungsstaender: zwei Schlaege kurz hintereinander (Kreativ einer) oder eine Explosion. */
    private boolean hurtStraw(ServerLevel level, DamageSource source, @Nullable Player breaker) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            breakApart(level, source, true);
            return false;
        }
        if (breaker == null || !breaker.getAbilities().mayBuild) {
            return false;
        }
        if (breaker.hasInfiniteMaterials()) {
            breakApart(level, source, false);
            return true;
        }
        long time = level.getGameTime();
        if (time - this.lastHit > 5L) {
            level.broadcastEntityEvent(this, (byte) 32);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
            this.lastHit = time;
        } else {
            breakApart(level, source, true);
        }
        return true;
    }

    /** Abbauen: Stroh-Ruestungsstaender und Ausruestung fallen (Kreativ wie Vanilla ohne Drops). */
    private void breakApart(ServerLevel level, DamageSource source, boolean drops) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_BREAK, this.getSoundSource(), 1.0F, 1.0F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()),
                this.getX(), this.getY(0.6666666666666666), this.getZ(), 10, this.getBbWidth() / 4.0F, this.getBbHeight() / 4.0F,
                this.getBbWidth() / 4.0F, 0.05);
        if (drops) {
            ItemStack item = new ItemStack(ModItems.STRAW_ARMOR_STAND);
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, this.getCustomName());
            Block.popResource(level, this.blockPosition(), item);
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack stack = this.getItemBySlot(slot);
                this.setItemSlot(slot, ItemStack.EMPTY);
                if (!stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                    Block.popResource(level, this.blockPosition().above(), stack);
                }
            }
        }
        this.remove(RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE, source.getEntity());
    }

    /**
     * Ein Treffer auf die Puppe: rechnet wie {@code LivingEntity#hurtServer} fuer den Mob ihres Kopfes und zeigt die
     * Zahl. Abgewiesene Treffer (Trefferpause) liefern {@code false} wie bei einem Mob; Pfeile prallen dann ab.
     */
    private boolean showHit(ServerLevel level, DamageSource source, float damage) {
        Entity mob = proxy();
        if (mob != null && (mob instanceof LivingEntity livingMob && livingMob.isInvulnerableTo(level, source)
                || mob.getType() == net.minecraft.world.entity.EntityTypes.ENDERMAN && source.is(DamageTypeTags.IS_PROJECTILE))) {
            this.lastShown = 0.0F;
            this.lastCrit = false;
            spawnNumber(level, Component.translatable("entity.simplebuilding.training_dummy.immune").withStyle(ChatFormatting.GRAY));
            level.broadcastEntityEvent(this, (byte) 32);
            return false;
        }
        if (damage < 0.0F || Float.isNaN(damage)) {
            damage = 0.0F;
        }
        if (mob != null && source.is(DamageTypeTags.IS_FREEZING)
                && mob.getType().builtInRegistryHolder().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            damage *= 5.0F;
        }
        if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            damage *= 0.75F;
        }
        long now = level.getGameTime();
        float dealt;
        if (now - this.cooldownStart < COOLDOWN_TICKS && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (damage <= this.lastHurtAmount) {
                return false;
            }
            dealt = damage - this.lastHurtAmount;
            this.lastHurtAmount = damage;
        } else {
            this.lastHurtAmount = damage;
            this.cooldownStart = now;
            dealt = damage;
        }
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            float armor = this.getArmorValue();
            float toughness = (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            if (mob instanceof LivingEntity living) {
                armor += (float) living.getAttributeBaseValue(Attributes.ARMOR);
                toughness += (float) living.getAttributeBaseValue(Attributes.ARMOR_TOUGHNESS);
            }
            dealt = CombatRules.getDamageAfterAbsorb(this, dealt, source, armor, toughness);
        }
        dealt = this.getDamageAfterMagicAbsorb(source, dealt);

        boolean crit = isCrit(source, now);
        record(level, source, dealt, crit, now);
        level.broadcastEntityEvent(this, (byte) 32);
        this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        return true;
    }

    private boolean isCrit(DamageSource source, long now) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            return arrow.isCritArrow();
        }
        if (source.getDirectEntity() instanceof Player p && source.getEntity() == p && source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
            return this.notedTick == now && this.notedBy == p.getId() && this.notedStrength > 0.9F
                    && p.fallDistance > 0.0 && !p.onGround() && !p.onClimbable() && !p.isInWater()
                    && !p.hasEffect(MobEffects.BLINDNESS) && !p.isPassenger() && !p.isSprinting();
        }
        return false;
    }

    private void record(ServerLevel level, DamageSource source, float dealt, boolean crit, long now) {
        if (this.hits == 0) {
            this.firstHit = now;
        }
        this.hits++;
        this.total += dealt;
        this.lastHitTick = now;
        this.lastShown = dealt;
        this.lastCrit = crit;
        MutableComponent text = Component.literal(format(dealt));
        if (crit) {
            text.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else if (source.is(DamageTypeTags.IS_FIRE)) {
            text.withStyle(ChatFormatting.GOLD);
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            text.withStyle(ChatFormatting.YELLOW);
        } else if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            text.withStyle(ChatFormatting.LIGHT_PURPLE);
        } else {
            text.withStyle(ChatFormatting.WHITE);
        }
        spawnNumber(level, text);
    }

    static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ------------------------------------------------------------------ Anzeige

    private @Nullable TextDisplay textDisplay(ServerLevel level, Component text, Vec3 at) {
        Display.TextDisplay display = net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("text", ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), text)
                .getOrThrow());
        tag.putString("billboard", "center");
        tag.putInt("background", 0);
        tag.putBoolean("shadow", true);
        tag.putInt("teleport_duration", 2);
        tag.putFloat("view_range", 0.5F);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        display.setPos(at.x, at.y, at.z);
        display.addTag(NUMBER_TAG);
        level.addFreshEntity(display);
        return display;
    }

    private void spawnNumber(ServerLevel level, Component text) {
        while (this.numbers.size() >= MAX_NUMBERS) {
            this.numbers.removeFirst().display().discard();
        }
        Vec3 at = new Vec3(this.getX() + (this.random.nextDouble() - 0.5) * 0.6, this.getY() + this.getBbHeight() + 0.1,
                this.getZ() + (this.random.nextDouble() - 0.5) * 0.6);
        Display.TextDisplay display = textDisplay(level, text, at);
        if (display != null) {
            this.numbers.add(new Number(display, new int[]{0}));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = this.numbers.size() - 1; i >= 0; i--) {
            Number number = this.numbers.get(i);
            int age = ++number.age()[0];
            Display.TextDisplay display = number.display();
            if (age >= NUMBER_TICKS || display.isRemoved()) {
                display.discard();
                this.numbers.remove(i);
            } else {
                display.setPos(display.getX(), display.getY() + 0.04, display.getZ());
            }
        }
        if (this.summary != null && ++this.summaryAge >= SUMMARY_TICKS) {
            this.summary.discard();
            this.summary = null;
        }
        long now = level.getGameTime();
        if (this.hits > 0 && now - this.lastHitTick >= IDLE_TICKS) {
            if (this.hits >= 2) {
                showSummary(level);
            }
            resetSession();
        }
        if (--this.scanIn <= 0) {
            this.scanIn = ORPHAN_SCAN_TICKS;
            removeOrphans(level);
        }
    }

    /** Summe seit dem ersten Treffer: Schaden, Dauer erster bis letzter Treffer, Schaden je Sekunde. */
    private void showSummary(ServerLevel level) {
        if (this.summary != null) {
            this.summary.discard();
        }
        float seconds = Math.max(20.0F, this.lastHitTick - this.firstHit) / 20.0F;
        Component text = Component.translatable("entity.simplebuilding.training_dummy.summary", format(this.total), format(seconds),
                format(this.total / seconds)).withStyle(ChatFormatting.AQUA);
        this.summary = textDisplay(level, text, new Vec3(this.getX(), this.getY() + this.getBbHeight() + 0.5, this.getZ()));
        this.summaryAge = 0;
    }

    private void resetSession() {
        this.total = 0.0F;
        this.hits = 0;
    }

    private void removeOrphans(ServerLevel level) {
        AABB area = this.getBoundingBox().inflate(2.0, 3.0, 2.0);
        for (Display.TextDisplay display : level.getEntitiesOfClass(Display.TextDisplay.class, area,
                d -> d.entityTags().contains(NUMBER_TAG))) {
            boolean ours = display == this.summary || this.numbers.stream().anyMatch(n -> n.display() == display);
            if (!ours) {
                display.discard();
            }
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (reason.shouldDestroy()) {
            this.numbers.forEach(n -> n.display().discard());
            this.numbers.clear();
            if (this.summary != null) {
                this.summary.discard();
            }
        }
    }

    // ------------------------------------------------------------------ fuer Tests

    /** Zuletzt gezeigter Schaden (NaN vor dem ersten Treffer, 0 bei Immunitaet). */
    public float lastShown() {
        return this.lastShown;
    }

    public boolean lastCrit() {
        return this.lastCrit;
    }

    public float sessionTotal() {
        return this.total;
    }

    public int sessionHits() {
        return this.hits;
    }

    public int visibleNumbers() {
        return this.numbers.size();
    }

    public @Nullable TextDisplay summaryDisplay() {
        return this.summary;
    }
}

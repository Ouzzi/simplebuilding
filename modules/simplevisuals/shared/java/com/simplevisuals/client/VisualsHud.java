package com.simplevisuals.client;

import com.simplevisuals.Visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** HUD state expires on client ticks, never on render frames. */
public final class VisualsHud {
    public static final int MAX_NOTIFICATIONS = 8, MAX_DAMAGE_INDICATORS = 32, MAX_CACHE = 64;
    private record Pickup(ItemStack stack, int count, boolean xp, int expires) {}
    private record Damage(Vec3 pos, int amount, boolean special, int expires) {}
    private record Cached(ItemStack stack, int xp, int expires) {}
    private static final List<Pickup> pickups = new ArrayList<>();
    private static final List<Damage> damages = new ArrayList<>();
    private static final Map<Integer, Cached> cache = new LinkedHashMap<>();
    private static final Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, Integer> durations = new HashMap<>();
    private static final Map<Identifier, Integer> biomeCooldowns = new HashMap<>();
    private static Identifier biome;
    private static int biomeUntil, ticks;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static float speedOpacity, animation;
    private static double lastSpeed;
    public static void tick(Minecraft mc) {
        if (mc.level != level) { reset(); level = mc.level; }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        ticks++;
        pickups.removeIf(p -> p.expires <= ticks); damages.removeIf(p -> p.expires <= ticks); cache.values().removeIf(p -> p.expires <= ticks);
        durations.keySet().removeIf(e -> !mc.player.hasEffect(e));
        for (var effect : mc.player.getActiveEffects()) if (!effect.isInfiniteDuration()) durations.merge(effect.getEffect(), effect.getDuration(), Math::max);
        var c = Visuals.CONFIG.visuals;
        double speed = mc.player.isFallFlying() ? mc.player.getDeltaMovement().length() : mc.player.getDeltaMovement().horizontalDistance();
        float opacity = (float)Math.max(Math.max(0, Math.min(1, (speed - c.speedLines.speedThreshold) * 2)), Math.max(0, Math.min(1, (speed - lastSpeed) * 15)));
        speedOpacity += (opacity - speedOpacity) * .1f; lastSpeed = speed;
        animation = (animation + c.speedLines.speedLinesSpeed * 4) % 10000;
        if (ticks % 10 == 0 && c.biomeInfo.enable) {
            var id = mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k -> k.identifier()).orElse(null);
            if (id != null && !id.equals(biome)) {
                if (biome != null) biomeCooldowns.put(biome, ticks + c.biomeInfo.cooldownSeconds * 20);
                if (biomeCooldowns.getOrDefault(id, 0) <= ticks) biomeUntil = ticks + c.biomeInfo.displayDuration;
                biome = id;
                if (biomeCooldowns.size() > 128) biomeCooldowns.clear();
            }
        }
    }
    public static void reset() { pickups.clear(); damages.clear(); cache.clear(); durations.clear(); biomeCooldowns.clear(); biome = null; biomeUntil = ticks = 0; speedOpacity = animation = 0; lastSpeed = 0; }
    public static int notificationCount() { return pickups.size(); }
    public static void cache(int id, net.minecraft.world.entity.Entity entity) {
        if (cache.size() >= MAX_CACHE) cache.remove(cache.keySet().iterator().next());
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) cache.put(id, new Cached(item.getItem().copy(), 0, ticks + 20));
        else if (entity instanceof net.minecraft.world.entity.ExperienceOrb xp) cache.put(id, new Cached(ItemStack.EMPTY, xp.getValue(), ticks + 20));
    }
    public static void pickup(Minecraft mc, int id, int collector, int count) {
        if (mc.player == null || mc.level == null || mc.player.getId() != collector) return;
        var entity = mc.level.getEntity(id); if (entity != null) cache(id, entity);
        var entry = cache.remove(id); if (entry == null) return;
        if (!entry.stack.isEmpty()) addPickup(entry.stack, Math.max(1, Math.min(count, entry.stack.getCount())), false);
        else if (entry.xp > 0 && Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierShowXp) addPickup(new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE), entry.xp, true);
    }
    public static void addPickup(ItemStack stack, int count, boolean xp) {
        if (count <= 0 || stack.isEmpty()) return;
        for (int i = 0; i < pickups.size(); i++) {
            var old = pickups.get(i);
            if (old.xp == xp && ItemStack.isSameItemSameComponents(old.stack, stack)) { pickups.set(i, new Pickup(old.stack, (int)Math.min(999999L, (long)old.count + count), xp, ticks + Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierDuration)); return; }
        }
        if (pickups.size() == MAX_NOTIFICATIONS) pickups.removeFirst();
        pickups.add(new Pickup(stack.copyWithCount(1), Math.min(999999, count), xp, ticks + Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierDuration));
    }
    public static void damage(LivingEntity entity, float damage) {
        if (!Visuals.CONFIG.visuals.damageIndicators.enable || !Float.isFinite(damage) || damage <= 0) return;
        if (damages.size() == MAX_DAMAGE_INDICATORS) damages.removeFirst();
        damages.add(new Damage(entity.position().add(0, entity.getBbHeight() + .3, 0), (int)Math.min(9999, Math.ceil(damage)), damage > 8, ticks + 40));
    }
    public static void render(GuiGraphicsExtractor g) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gui.hud.isHidden()) return;
        var c = Visuals.CONFIG.visuals;
        int w = g.guiWidth(), h = g.guiHeight();
        if (c.speedLines.enableSpeedLines && speedOpacity > .02 && mc.options.getCameraType().isFirstPerson()) {
            var s = c.speedLines; int n = Math.min(60, (int)(30 * s.speedLinesAmount));
            for (int i = 0; i < n; i++) {
                float angle = i * 2.399963f; float distance = h * s.speedLinesRadius + (animation + i * 71) % Math.max(1, h);
                g.pose().pushMatrix(); g.pose().translate(w / 2f, h / 2f); g.pose().rotate(angle); g.pose().translate(distance, 0);
                int alpha = (int)(100 * speedOpacity * s.speedLinesAlpha);
                g.fill(0, 0, (int)(30 * s.speedLinesScale), Math.max(1, (int)s.speedLinesWidth), (alpha << 24) | s.speedLinesColor); g.pose().popMatrix();
            }
        }
        if (c.enableElytraPitchHelper && mc.player.isFallFlying()) for (float target : new float[]{c.elytraTargetAngleUp, c.elytraTargetAngleDown}) {
            float diff = target - mc.player.getXRot(); if (Math.abs(diff) <= c.elytraPitchTolerance) { int y = h / 2 + (int)(diff * c.elytraSensitivity); g.fill(w / 2 - 30, y, w / 2 + 30, y + 1, 0x9900ffff); }
        }
        if (c.biomeInfo.enable && biome != null && biomeUntil > ticks) {
            var name = Component.translatable("biome." + biome.getNamespace() + "." + biome.getPath());
            g.text(mc.font, name, (w - mc.font.width(name)) / 2, Math.min(h - 10, c.biomeInfo.yOffset), 0xffffffff);
        }
        if (c.pickupNotifier.enablePickupNotifier) {
            var p = c.pickupNotifier; g.pose().pushMatrix(); g.pose().scale(p.pickupNotifierScale);
            int sw = (int)(w / p.pickupNotifierScale), sh = (int)(h / p.pickupNotifierScale), row = 0;
            for (var entry : pickups) {
                var name = entry.xp ? Component.translatable("simplevisuals.experience") : entry.stack.getHoverName().copy();
                if (p.pickupUseRarityColor) name = name.copy().withStyle(entry.xp ? net.minecraft.ChatFormatting.GREEN : entry.stack.getRarity().color());
                String count = "+" + entry.count;
                int textWidth = (p.pickupShowName ? mc.font.width(name) : 0) + (p.pickupShowCount ? mc.font.width(count) + 4 : 0), width = textWidth + (p.pickupShowItem ? 20 : 0);
                int x = p.pickupNotifierSide == com.simplevisuals.config.SimplevisualsConfig.PickupSide.RIGHT ? sw - p.pickupNotifierOffsetX - width : p.pickupNotifierOffsetX;
                x = Math.max(0, Math.min(sw - width, x)); int y = Math.max(0, sh - p.pickupNotifierOffsetY - 18 - row++ * 20);
                int bg = ((int)(p.pickupBackgroundOpacity * (p.pickupVanillaStyle ? 90 : 180)) << 24);
                g.fill(x - 2, y - 2, x + width + 2, y + 18, bg);
                String[] order = p.pickupNotifierLayout.name().split("_"); int cursor = x;
                for (String element : order) switch (element) {
                    case "ICON" -> { if (p.pickupShowItem) { g.item(entry.stack, cursor, y); cursor += 20; } }
                    case "NAME" -> { if (p.pickupShowName) { g.text(mc.font, name, cursor, y + 4, 0xffffffff); cursor += mc.font.width(name) + 4; } }
                    case "COUNT" -> { if (p.pickupShowCount) { g.text(mc.font, count, cursor, y + 4, 0xffaaaaaa); cursor += mc.font.width(count) + 4; } }
                }
            }
            g.pose().popMatrix();
        }
        if (c.damageIndicators.enable) for (var damage : damages) {
            // Project into the HUD; no see-through text and no render-thread state mutation.
            var camera = mc.gameRenderer.mainCamera(); Vec3 delta = damage.pos.subtract(camera.position());
            if (delta.lengthSqr() > 256 || mc.level.clip(new net.minecraft.world.level.ClipContext(camera.position(), damage.pos, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player)).getType() != net.minecraft.world.phys.HitResult.Type.MISS) continue;
            var v = new org.joml.Vector3f((float)delta.x, (float)delta.y, (float)delta.z).rotate(new org.joml.Quaternionf(camera.rotation()).conjugate());
            if (v.z >= -.1f) continue; double focal = h / (2 * Math.tan(Math.toRadians(mc.options.fov().get()) / 2));
            int x = (int)(w / 2 + v.x * focal / -v.z), y = (int)(h / 2 - v.y * focal / -v.z - (40 - damage.expires + ticks) / 2);
            g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(c.damageIndicators.scale); g.text(mc.font, Integer.toString(damage.amount), 0, 0, 0xff000000 | (damage.special ? c.damageIndicators.colorSpecial : c.damageIndicators.colorNormal), c.damageIndicators.showBorder); g.pose().popMatrix();
        }
    }
    public static void held(GuiGraphicsExtractor g, ItemStack stack, int fade) {
        var mc = Minecraft.getInstance(); var c = Visuals.CONFIG.visuals.heldItemTooltips;
        if (!c.enable || fade <= 0 || stack.isEmpty()) return;
        var lines = new ArrayList<Component>();
        if (c.showDurability && stack.isDamageableItem()) lines.add(Component.translatable("simplevisuals.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()));
        var enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (c.showEnchantments && enchants != null) { int count = 0; for (var entry : enchants.entrySet()) { if (count++ >= c.maxEnchantments) { lines.add(Component.translatable("simplevisuals.more", enchants.size() - c.maxEnchantments)); break; } lines.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue())); } }
        int y = g.guiHeight() - 70 - lines.size() * 10; for (var line : lines) { g.text(mc.font, line, (g.guiWidth() - mc.font.width(line)) / 2, y, (Math.min(255, fade * 25) << 24) | 0xffffff); y += 10; }
    }
    public static float effectProgress(net.minecraft.world.effect.MobEffectInstance effect) {
        return effect.isInfiniteDuration() ? 1 : Math.min(1, (float)effect.getDuration() / Math.max(1, durations.getOrDefault(effect.getEffect(), effect.getDuration())));
    }
    public static void effectBar(GuiGraphicsExtractor g,net.minecraft.world.effect.MobEffectInstance e,int x,int y,int width) {
        if (!Visuals.CONFIG.visuals.enableStatusEffectBars || e.isInfiniteDuration()) return;
        g.fill(x,y,x+width,y+1,0xff000000);
        g.fill(x,y,x+(int)(width*effectProgress(e)),y+1,0xff000000|e.getEffect().value().getColor());
    }
}

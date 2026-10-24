package com.simplemobheads.drop;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.MobDrop;
import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.data.HeadDef;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Mobheads in the recipe viewer's "Mob Drops" tab (JEI24): every head that drops the normal way - its mob killed
 * by a charged creeper's explosion ({@link HeadDrops#apply}) - with the victim's and the creeper's spawn egg. Heads of
 * rituals, bosses and the Easter eggs keep their riddle info pages only (rule A6). Free of JEI classes.
 */
public final class HeadsViewer {
    private HeadsViewer() {
    }

    public static void register() {
        RecipeViewer.mobDrops(SimpleMobHeads.MOD_ID, HeadsViewer::drops);
    }

    public static List<MobDrop> drops() {
        List<MobDrop> out = new ArrayList<>();
        if (!SimpleMobHeads.config().creeperDrops) return out;
        for (HeadDef head : MobHeads.heads()) {
            if (!"default".equals(head.drop()) || !HeadDropCondition.allowed(head)) continue;
            Item item = MobHeads.item(head.id());
            Identifier mob = Identifier.tryParse(head.mob());
            if (item == null || item == Items.AIR || mob == null) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mob).orElse(null);
            if (type == null) continue;
            out.add(new MobDrop(SimpleMobHeads.MOD_ID + "/charged_creeper/" + head.id(), type, RecipeViewer.egg(type),
                    new ItemStack(Items.CREEPER_SPAWN_EGG), List.of(new ItemStack(item)),
                    List.of(Component.translatable("jei.simplemobheads.note.charged_creeper", type.getDescription()))));
        }
        return out;
    }
}

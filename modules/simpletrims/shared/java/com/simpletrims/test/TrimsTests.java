package com.simpletrims.test;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TemplateTools;
import com.simpletrims.config.TrimsConfig;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Loader independent module tests; the Fabric/NeoForge adapters register them as module_game_test_<name>. */
public final class TrimsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();

    static {
        ALL.put("config_defaults", h -> {
            TrimsConfig.apply(new JsonObject());
            h.assertTrue(TrimsConfig.enabled, "enabled by default");
            JsonObject off = new JsonObject();
            off.addProperty("enabled", false);
            TrimsConfig.apply(off);
            h.assertTrue(!TrimsConfig.enabled, "enabled=false is read");
            JsonObject junk = new JsonObject();
            junk.addProperty("enabled", "maybe");
            TrimsConfig.apply(junk);
            h.assertTrue(TrimsConfig.enabled, "junk falls back to the default");
            TrimsConfig.reset();
            h.succeed();
        });
        ALL.put("template_tool_without_simplebuilding", h -> {
            h.assertTrue(TemplateTools.isTemplateTool(new ItemStack(Items.IRON_AXE), false), "axe works without SimpleBuilding");
            h.assertTrue(!TemplateTools.isTemplateTool(new ItemStack(Items.IRON_PICKAXE), false), "pickaxe does not");
            h.succeed();
        });
        ALL.put("template_tool_with_simplebuilding", h -> {
            h.assertTrue(!TemplateTools.isTemplateTool(new ItemStack(Items.IRON_AXE), true), "with SimpleBuilding the axe steps back");
            h.succeed();
        });
        ALL.put("ids", h -> {
            h.assertTrue("simpletrims:x".equals(SimpleTrims.id("x").toString()), "namespace");
            h.succeed();
        });
    }

    private TrimsTests() {}
}

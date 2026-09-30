package com.simplefun.config;

import com.simplefun.Constants;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Constants.MOD_ID)
public class SimplefunConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public Fun fun = new Fun();

    public static class Fun {

        @ConfigEntry.Gui.Tooltip
        public boolean playerHeadDrops = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enablePiggyEffect = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableNoDamage = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableYeet = true;
        @ConfigEntry.Gui.Tooltip
        public float yeetStrength = 3.0f;

        @ConfigEntry.Gui.Tooltip
        public boolean enableThrowableBricks = true;
        @ConfigEntry.Gui.Tooltip
        public boolean throwableBricksBreakBlocks = false;
        @ConfigEntry.Gui.Tooltip
        public float brickDamage = 2.0f;
        @ConfigEntry.Gui.Tooltip
        public float brickSnowballDamage = 2.0f;
        @ConfigEntry.Gui.Tooltip public boolean enableHigherKnockback = true;
        @ConfigEntry.Gui.Tooltip public float maxKnockback = 4.0f;
        @ConfigEntry.Gui.Tooltip public boolean enableBrickSnowball = true;
        @ConfigEntry.Gui.Tooltip public boolean enableNoDamageTrades = true;
        @ConfigEntry.Gui.Tooltip public boolean enableAnimalHeads = true;
        @ConfigEntry.Gui.Tooltip public boolean pigHeadGreeting = true;
        @ConfigEntry.Gui.Tooltip public boolean cowHeadGreeting = true;
        @ConfigEntry.Gui.Tooltip public boolean chickenHeadGreeting = true;
        @ConfigEntry.Gui.Tooltip public boolean sheepHeadGreeting = true;
        @ConfigEntry.Gui.Tooltip public boolean flowerSniff = true;
        @ConfigEntry.Gui.Tooltip public boolean cookieCrumbs = true;
        @ConfigEntry.Gui.Tooltip public boolean appleSparkle = true;
        @ConfigEntry.Gui.Tooltip public boolean carrotCrunch = true;
        @ConfigEntry.Gui.Tooltip public boolean melonSplash = true;
        @ConfigEntry.Gui.Tooltip public boolean honeyBubbles = true;
        @ConfigEntry.Gui.Tooltip public boolean breadCrumbs = true;
        @ConfigEntry.Gui.Tooltip public boolean berryBlush = true;
    }
    public static final float MAX_YEET_STRENGTH=3, MAX_DAMAGE=4, MAX_ITEM_SPEED=1.5f;
    public static final int THROW_COOLDOWN=10, MAX_PROJECTILE_TICKS=200, COSMETIC_COOLDOWN=100, MAX_PARTICLES=6;
    public static float bounded(float v,float fallback,float min,float max) {return Float.isFinite(v)?Math.max(min,Math.min(max,v)):fallback;}
    public void normalize() {if(fun==null)fun=new Fun();fun.yeetStrength=bounded(fun.yeetStrength,3,.1f,MAX_YEET_STRENGTH);fun.brickDamage=bounded(fun.brickDamage,2,0,MAX_DAMAGE);fun.brickSnowballDamage=bounded(fun.brickSnowballDamage,2,0,MAX_DAMAGE);fun.maxKnockback=bounded(fun.maxKnockback,4,0,4);}
}

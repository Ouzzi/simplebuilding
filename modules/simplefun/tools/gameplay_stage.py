from pathlib import Path
import json,zipfile
M=Path(__file__).resolve().parents[1]
def w(p,s):p=M/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def r(p,a,b):p=M/p;s=p.read_text();assert a in s,(p,a);p.write_text(s.replace(a,b))
w('shared/java/com/simplefun/SimplefunCommon.java','''package com.simplefun;
import com.simplefun.config.SimplefunConfig;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
public final class SimplefunCommon {
 private static SimplefunConfig config;
 public static void init(){Constants.LOG.info("Initializing Simple Fun 26.3");}
 public static synchronized void registerConfig(){if(config!=null)return;var p=Path.of("config/simplefun.json");var gson=new GsonBuilder().setPrettyPrinting().create();try{config=Files.exists(p)?gson.fromJson(Files.readString(p),SimplefunConfig.class):new SimplefunConfig();if(config==null)config=new SimplefunConfig();config.normalize();if(!Files.exists(p))saveConfig();}catch(Exception e){throw new IllegalStateException("Invalid Simple Fun server config",e);}}
 public static SimplefunConfig getConfig(){registerConfig();config.normalize();return config;}
 public static void saveConfig(){try{config.normalize();Files.createDirectories(Path.of("config"));Files.writeString(Path.of("config/simplefun.json"),new GsonBuilder().setPrettyPrinting().create().toJson(config));}catch(Exception e){throw new IllegalStateException(e);}}
}''')
# 26.3 removed imperative trade APIs. Equivalent offer is data-driven.
w('shared/java/com/simplefun/trade/SimplefunTrades.java','''package com.simplefun.trade;
public final class SimplefunTrades {public static final int LIBRARIAN_LEVEL=1, EMERALD_COST=25, MAX_USES=3, VILLAGER_XP=15;public static final float PRICE_MULTIPLIER=.3f;}
''')
for loader in ['fabric','neoforge']:
 p=M/loader/f'src/main/java/com/simplefun/Simplefun{loader.capitalize() if loader=="fabric" else "NeoForge"}.java';s=p.read_text()
 s='\n'.join(x for x in s.splitlines() if 'import net.fabricmc.fabric.api.object.builder.v1.trade' not in x and 'import net.neoforged.neoforge.event.village' not in x)
 if loader=='fabric':s=s.replace('        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, SimplefunTrades.LIBRARIAN_LEVEL,\n                factories -> factories.add(SimplefunTrades.NO_DAMAGE_BOOK));','')
 else:
  start=s.index('        // Fired once per profession');end=s.index('        // Server-side:',start);s=s[:start]+s[end:]
 p.write_text(s)
jars=list((M.parents[1]/'.gradle/loom-cache/minecraftMaven').rglob('*26.3.jar'))
with zipfile.ZipFile(jars[0]) as z:
 for name in ['EntityModel','HumanoidModel']:
  found=next(n for n in z.namelist() if n.endswith('/'+name+'.class'));print(name,found)
  p=M/'shared/java/com/simplefun/client/PigHeadFeatureRenderer.java';s=p.read_text().replace('net.minecraft.client.model.entity.'+name,found[:-6].replace('/','.'));p.write_text(s)
r('shared/java/com/simplefun/event/PlayerHeadDrop.java','victim.drop(head, true);','victim.drop(head, true, net.minecraft.util.Prediction.SERVER_ONLY);')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','new ItemParticleOption(ParticleTypes.ITEM, stack)','new ItemParticleOption(ParticleTypes.ITEM, stack.getItem())')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','        level.playSound(null,','''        if (!(level instanceof ServerLevel) || stack.isEmpty()) return;
        var cfg=SimplefunCommon.getConfig().fun;
        if (stack.is(ModItems.BRICK_SNOWBALL) ? !cfg.enableBrickSnowball : !cfg.enableThrowableBricks) return;
        if (player.getCooldowns().isOnCooldown(stack)) return;
        player.getCooldowns().addCooldown(stack, com.simplefun.config.SimplefunConfig.THROW_COOLDOWN);
        level.playSound(null,''')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','projectile.setItem(stack);','projectile.setItem(stack.copyWithCount(1));')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','    @Override\n    protected Item getDefaultItem()', '''    @Override public void tick(){super.tick();if(!level().isClientSide() && tickCount>com.simplefun.config.SimplefunConfig.MAX_PROJECTILE_TICKS)discard();}
    @Override
    protected Item getDefaultItem()''')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','if (canBreak && shouldBreakBlock(state))','if (canBreak && shouldBreakBlock(state) && this.getOwner() instanceof net.minecraft.server.level.ServerPlayer player && player.mayInteract((ServerLevel)level(), pos) && level().getWorldBorder().isWithinBounds(pos))')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','return state.getSoundType() == SoundType.GLASS;','return state.getBlock() instanceof net.minecraft.world.level.block.GlassBlock || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;')
r('shared/java/com/simplefun/mixin/NoDamageMixin.java','if (source.getEntity() instanceof Player player)','if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player)')
r('shared/java/com/simplefun/mixin/YeetMixin.java','drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;','drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;')
r('shared/java/com/simplefun/mixin/YeetMixin.java','boolean throwRandomly, boolean retainOwnership','boolean throwRandomly, net.minecraft.util.Prediction prediction')
r('shared/java/com/simplefun/mixin/YeetMixin.java','player.isAlive() && player.isShiftKeyDown()','player.isAlive() && player.isShiftKeyDown() && !throwRandomly')
r('shared/java/com/simplefun/mixin/YeetMixin.java','itemEntity.setDeltaMovement(vel.multiply(strength, strength * 0.5, strength));','Vec3 boosted=vel.multiply(strength,strength*.5,strength); if(boosted.length()>com.simplefun.config.SimplefunConfig.MAX_ITEM_SPEED)boosted=boosted.normalize().scale(com.simplefun.config.SimplefunConfig.MAX_ITEM_SPEED); itemEntity.setDeltaMovement(boosted);')
r('shared/java/com/simplefun/command/SimplefunCommands.java','FloatArgumentType.floatArg(0.1f)','FloatArgumentType.floatArg(0.1f, com.simplefun.config.SimplefunConfig.MAX_YEET_STRENGTH)')
r('shared/java/com/simplefun/command/SimplefunCommands.java','FloatArgumentType.floatArg(0.0f)','FloatArgumentType.floatArg(0.0f, com.simplefun.config.SimplefunConfig.MAX_DAMAGE)')

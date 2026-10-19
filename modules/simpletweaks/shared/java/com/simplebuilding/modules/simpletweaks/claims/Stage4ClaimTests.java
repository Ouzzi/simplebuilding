package com.simplebuilding.modules.simpletweaks.claims;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimPistonInvoker;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Real server entrypoints; the only custom-mod lookup is through public registry IDs. */
final class Stage4ClaimTests {
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:"+id));}
    private static void modes(GameTestHelper h,java.util.function.BiConsumer<Fixture,Boolean> test){
        for(int mode:new int[]{1,0,-1})with(h,mode==1?on():ClaimConfig.OFF,c->{
            boolean enabled=mode==1;
            if(mode==-1)Claims.install(h.getLevel().getServer(),null);
            try(var f=new Fixture(h,c,enabled)){test.accept(f,enabled);if(!enabled)yes(h,!c.dataLoaded(),"Disabled automation never opens the ledger");}
        });
        h.succeed();
    }
    /**
     * Entity tests on the high workspace: a freshly loaded chunk only lists new entities once its entity section
     * is ticking (flaky boats/indirect cloud on tick 0). Force-load both sides of the border and wait until a probe
     * on each side is queryable, as the attractor test does, then run the synchronous modes.
     */
    private static void warmEntityModes(GameTestHelper h,java.util.function.BiConsumer<Fixture,Boolean> test){
        var edge=ToolClaimTests.boundary(h).above(90);var forced=new ArrayList<ChunkPos>();var probes=new ArrayList<ItemEntity>();
        for(var chunk:List.of(ChunkPos.containing(edge),ChunkPos.containing(edge.west(16)))){
            if(h.getLevel().setChunkForced(chunk.x(),chunk.z(),true))forced.add(chunk);h.getLevel().getChunk(chunk.x(),chunk.z());
        }
        for(var pos:List.of(edge.west(5),edge.east(3))){
            var probe=new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,new ItemStack(Items.STICK),0,0,0);
            probe.setNoGravity(true);probe.setNeverPickUp();h.getLevel().addFreshEntity(probe);probes.add(probe);
        }
        h.runBeforeTestEnd(()->{probes.forEach(Entity::discard);for(var chunk:forced)h.getLevel().setChunkForced(chunk.x(),chunk.z(),false);});
        h.startSequence().thenWaitUntil(()->yes(h,h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(edge).inflate(8)).containsAll(probes),"Workspace entities are not queryable yet"))
                .thenExecute(()->{probes.forEach(Entity::discard);modes(h,test);});
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel l;final BlockPos edge;final Claims claims;
        final net.minecraft.server.level.ServerPlayer owner;
        final List<Entity> entities=new ArrayList<>();final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        Fixture(GameTestHelper h,Claims c,boolean enabled){
            this.h=h;this.l=h.getLevel();this.claims=c;
            // High, isolated workspace. Each synchronous test restores every cell before returning.
            edge=ToolClaimTests.boundary(h).above(90);
            owner=player(h,edge.east(12).above(6));entities.add(owner);
            if(enabled)yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(edge)),owner.getUUID(),100),"Stage 4 owner fixture");
            for(int x=-10;x<=12;x++)for(int y=-1;y<=4;y++)for(int z=-5;z<=5;z++){
                var p=edge.offset(x,y,z);saved.put(p,l.getBlockState(p));l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            }
        }
        void set(BlockPos p,BlockState s){saved.putIfAbsent(p,l.getBlockState(p));l.setBlock(p,s,2);}
        void set(BlockPos p,Block b){set(p,b.defaultBlockState());}
        <T extends Entity> T add(T entity,BlockPos p){entity.setPos(Vec3.atBottomCenterOf(p));l.addFreshEntity(entity);entities.add(entity);return entity;}
        @Override public void close(){
            entities.forEach(Entity::discard);
            l.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(edge.offset(-12,-2,-7)),Vec3.atLowerCornerOf(edge.offset(15,8,8)))).forEach(Entity::discard);
            // Clear containers without dropping fixture inventory.
            for(var p:saved.keySet())if(l.getBlockEntity(p) instanceof Container container)container.clearContent();
            saved.forEach((p,s)->l.setBlock(p,s,2));
            l.getEntitiesOfClass(ItemEntity.class,new AABB(edge).inflate(16)).forEach(Entity::discard);
        }
    }
    static void crafter(GameTestHelper h){
        // Newly loaded neighboring chunks need a server tick before item entities are queryable.
        var edge=ToolClaimTests.boundary(h);h.getLevel().getChunk(edge);h.getLevel().getChunk(edge.west(16));
        h.runAfterDelay(5,()->modes(h,(f,enabled)->{
        for(boolean border:new boolean[]{true,false})for(boolean container:new boolean[]{false,true}){
            var pos=border?f.edge.west():f.edge.east(3);
            f.set(pos,Blocks.AIR);f.set(pos.east(),Blocks.AIR);
            var state=Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION,FrontAndTop.EAST_UP);
            f.set(pos,state);if(container)f.set(pos.east(),Blocks.CHEST);
            var crafter=(CrafterBlockEntity)f.l.getBlockEntity(pos);crafter.setItem(0,new ItemStack(Items.OAK_LOG,2));
            var area=new AABB(pos).inflate(3);f.l.getEntitiesOfClass(ItemEntity.class,area).forEach(Entity::discard);
            state.tick(f.l,pos,RandomSource.create(0));boolean denied=enabled&&border;
            yes(h,crafter.getItem(0).getCount()==(denied?2:1),"Crafter consumes only permitted recipe inputs");
            int output=container?((Container)f.l.getBlockEntity(pos.east())).countItem(Items.OAK_PLANKS):f.l.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(Items.OAK_PLANKS)).mapToInt(e->e.getItem().getCount()).sum();
            yes(h,output==(denied?0:4),"Actual Crafter output respects boundary, including fallback ejection: enabled="+enabled+", border="+border+", container="+container+", output="+output);
            if(denied)yes(h,f.l.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Denied crafter never falls back to spawned items");
            crafter.clearContent();if(container)((Container)f.l.getBlockEntity(pos.east())).clearContent();
        }
        var near=f.edge.west();var far=f.edge;var pos=near.south();
        f.set(near,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT));
        f.set(far,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.RIGHT));
        var state=Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION,FrontAndTop.NORTH_UP);f.set(pos,state);
        var crafter=(CrafterBlockEntity)f.l.getBlockEntity(pos);crafter.setItem(0,new ItemStack(Items.OAK_LOG,2));
        state.tick(f.l,pos,RandomSource.create(0));
        yes(h,crafter.getItem(0).getCount()==(enabled?2:1),"Crafter checks both halves of a boundary double chest before consuming inputs");
        int output=((Container)f.l.getBlockEntity(near)).countItem(Items.OAK_PLANKS)+((Container)f.l.getBlockEntity(far)).countItem(Items.OAK_PLANKS);
        yes(h,output==(enabled?0:4),"Boundary double chest insertion preserves off behavior");
    }));}
    static void copperGolem(GameTestHelper h){modes(h,(f,enabled)->{
        var behavior=new net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers(1,s->true,s->true,16,8,Map.of(),mob->{},target->false);
        var access=(com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimTransportInvoker)behavior;
        var claimed=f.edge.east(3);var wild=f.edge.west(4);f.set(claimed,Blocks.CHEST);f.set(wild,Blocks.CHEST);
        var inside=(Container)f.l.getBlockEntity(claimed);var outside=(Container)f.l.getBlockEntity(wild);
        // Provenance: whatever land a load came from, it can only be put down on that same land.
        var golem=f.add(EntityTypes.COPPER_GOLEM.create(f.l,EntitySpawnReason.COMMAND),wild.east());
        inside.setItem(0,new ItemStack(Items.APPLE,20));access.claims$pickup(golem,inside);
        yes(h,inside.countItem(Items.APPLE)==4&&golem.getMainHandItem().getCount()==16,"Actual golem pickup works next to any chest");
        access.claims$putdown(golem,outside);
        yes(h,outside.countItem(Items.APPLE)==(enabled?0:16)&&golem.getMainHandItem().getCount()==(enabled?16:0),"Claimed items never leave the claim by golem");
        if(enabled){access.claims$putdown(golem,inside);yes(h,inside.countItem(Items.APPLE)==20&&golem.getMainHandItem().isEmpty(),"Claimed load returns to a same-owner container");}
        inside.clearContent();outside.clearContent();
        outside.setItem(0,new ItemStack(Items.APPLE,5));access.claims$pickup(golem,outside);access.claims$putdown(golem,inside);
        yes(h,inside.countItem(Items.APPLE)==(enabled?0:5),"Wilderness loads cannot be dumped into a claim");
        access.claims$putdown(golem,outside);yes(h,golem.getMainHandItem().isEmpty(),"Wilderness load still works in the wilderness");
        inside.clearContent();outside.clearContent();golem.discard();
        var legacy=f.add(EntityTypes.COPPER_GOLEM.create(f.l,EntitySpawnReason.COMMAND),claimed.north());legacy.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.APPLE,7));
        access.claims$putdown(legacy,outside);yes(h,outside.countItem(Items.APPLE)==(enabled?0:7),"Untagged load cannot leave the land the golem stands on");
        if(enabled){access.claims$putdown(legacy,inside);yes(h,inside.countItem(Items.APPLE)==7,"Untagged load may stay on its current land");}
        inside.clearContent();outside.clearContent();legacy.discard();
        var near=f.edge.west();var far=f.edge;f.set(near,Blocks.CHEST);f.set(far,Blocks.CHEST);
        var first=(Container)f.l.getBlockEntity(near);var second=(Container)f.l.getBlockEntity(far);
        first.setItem(0,new ItemStack(Items.APPLE,3));var combined=new CompoundContainer(first,second);
        var visitor=f.add(EntityTypes.COPPER_GOLEM.create(f.l,EntitySpawnReason.COMMAND),near.west());
        access.claims$pickup(visitor,combined);
        yes(h,first.countItem(Items.APPLE)==(enabled?3:0),"Golem refuses a container whose halves belong to different land");
        visitor.discard();first.clearContent();second.clearContent();
        // Unknown wrappers cannot supply trustworthy positions while protection is enabled.
        var unknownGolem=f.add(EntityTypes.COPPER_GOLEM.create(f.l,EntitySpawnReason.COMMAND),f.edge.west(4));
        var unknown=new SimpleContainer(new ItemStack(Items.APPLE,3));access.claims$pickup(unknownGolem,unknown);
        yes(h,unknown.countItem(Items.APPLE)==(enabled?3:0),"Positionless mobile target fails closed only while enabled");
    });}
    static void lightning(GameTestHelper h){modes(h,(f,enabled)->{
        for(int x=-3;x<=3;x++)for(int z=-2;z<=2;z++){f.set(f.edge.offset(x,-1,z),Blocks.NETHERRACK);f.set(f.edge.offset(x,0,z),Blocks.AIR);}
        for(var pos:List.of(f.edge.west(),f.edge.east())){
            var bolt=f.add(EntityTypes.LIGHTNING_BOLT.create(f.l,EntitySpawnReason.COMMAND),pos);
            ((com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimLightningInvoker)bolt).claims$fire(100);
            yes(h,f.l.getBlockState(pos).is(Blocks.FIRE)==(!enabled||pos.getX()<f.edge.getX()),"Actual lightning ignition checks each central target");
            bolt.discard();
        }
        if(enabled)for(int x=0;x<=2;x++)for(int z=-1;z<=1;z++)yes(h,f.l.getBlockState(f.edge.offset(x,0,z)).isAir(),"Random lightning ignition cannot cross into claim");
        for(int x=-4;x<=4;x++)for(int z=-3;z<=3;z++)f.set(f.edge.offset(x,0,z),BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:oxidized_copper")));
        for(int i=0;i<30;i++){
            com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimLightningInvoker.claims$clean(f.l,f.edge.west());
            com.simplebuilding.modules.simpletweaks.mixin.claims.ClaimLightningInvoker.claims$clean(f.l,f.edge.east());
        }
        yes(h,f.l.getBlockState(f.edge.west()).is(BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:copper_block"))),"Lightning cleans wilderness copper");
        yes(h,f.l.getBlockState(f.edge.east()).is(enabled?BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:oxidized_copper")):BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:copper_block"))),"Actual lightning copper strike retains protected oxidation");
        if(enabled)for(int x=0;x<=4;x++)for(int z=-3;z<=3;z++)yes(h,f.l.getBlockState(f.edge.offset(x,0,z)).is(BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:oxidized_copper"))),"Random copper cleaning cannot cross into claim");
    });}
    static void naturalDamage(GameTestHelper h){modes(h,(f,enabled)->{
        for(var p:List.of(f.edge.west(3),f.edge.east(3))){
            var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),p);float before=cow.getHealth();
            yes(h,cow.causeFallDamage(8,1,f.l.damageSources().fall())&&cow.getHealth()<before,"Real fall damage remains on either side of a claim");cow.discard();
            var swimmer=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),p);swimmer.setNoAi(true);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)f.set(p.offset(x,y,z),Blocks.WATER);
            swimmer.tick();swimmer.setAirSupply(-19);before=swimmer.getHealth();swimmer.tick();
            yes(h,swimmer.getHealth()<before,"Actual submerged entity tick still causes drowning");swimmer.discard();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)f.set(p.offset(x,y,z),Blocks.AIR);
        }
        for(var source:List.of(f.l.damageSources().starve(),f.l.damageSources().inWall(),f.l.damageSources().freeze(),f.l.damageSources().magic())){
            var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),f.edge.east(3));float before=cow.getHealth();
            yes(h,cow.hurtServer(f.l,source,2)&&cow.getHealth()<before,"Owner-independent natural damage: "+source);cow.discard();
        }
        var stranger=player(h,f.edge.east(4));f.entities.add(stranger);float before=stranger.getHealth();
        stranger.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        yes(h,stranger.hurtServer(f.l,f.l.damageSources().starve(),2)&&stranger.getHealth()<before,"Claim does not make a visiting player immune to starvation");
    });}
    static void explosionMultipart(GameTestHelper h){modes(h,(f,enabled)->{
        for(var bed:List.of(((BlockItem)Items.BED.red()).getBlock(),((BlockItem)Items.STRAW_BED).getBlock())){
            for(boolean border:new boolean[]{true,false}){
                var head=border?f.edge:f.edge.west(6);var foot=head.west();
                f.set(foot.below(),Blocks.OBSIDIAN);f.set(head.below(),Blocks.OBSIDIAN);
                f.set(foot,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.EAST).setValue(BedBlock.PART,BedPart.FOOT));
                f.set(head,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.EAST).setValue(BedBlock.PART,BedPart.HEAD));
                yes(h,f.l.getBlockState(head).is(bed)&&f.l.getBlockState(foot).is(bed),"Complete bed fixture");
                new ServerExplosion(f.l,null,null,null,Vec3.atCenterOf(foot),3,false,Explosion.BlockInteraction.DESTROY).explode();
                yes(h,enabled&&border?f.l.getBlockState(head).is(bed)&&f.l.getBlockState(foot).is(bed):f.l.getBlockState(head).isAir()&&f.l.getBlockState(foot).isAir(),"Real explosion checks both bed halves; border="+border+", enabled="+enabled);
                f.set(head,Blocks.AIR);f.set(foot,Blocks.AIR);
            }
        }
    });}
    static void indirectCloud(GameTestHelper h){warmEntityModes(h,(f,enabled)->{
        var stranger=player(h,f.edge.west(5));f.entities.add(stranger);
        for(var actor:List.of(stranger,f.owner)){
            var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),f.edge.east(3));cow.setNoAi(true);
            var cloud=f.add(EntityTypes.AREA_EFFECT_CLOUD.create(f.l,EntitySpawnReason.COMMAND),f.edge.east(3));
            cloud.setOwner(actor);cloud.setWaitTime(0);cloud.setRadius(2);cloud.setRadiusOnUse(0);
            yes(h,cloud.getOwner()==actor&&f.l.getEntitiesOfClass(LivingEntity.class,cloud.getBoundingBox()).contains(cow),"Cloud fixture has a resolved owner and a visible target");
            cloud.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
            for(int i=0;i<10;i++)cloud.tick();
            yes(h,cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON)==(!enabled||actor==f.owner),"Actual cloud ticks retain player attribution; enabled="+enabled+", owner="+(actor==f.owner));
            float before=cow.getHealth();
            boolean hit=cow.hurtServer(f.l,f.l.damageSources().indirectMagic(cloud,actor),3);
            yes(h,hit==(!enabled||actor==f.owner)&&(hit?cow.getHealth()<before:cow.getHealth()==before),"Actual indirect damage retains player attribution");
            var item=f.add(new ItemEntity(f.l,0,0,0,new ItemStack(Items.APPLE),0,0,0),f.edge.east(2));
            yes(h,item.hurtServer(f.l,f.l.damageSources().indirectMagic(cloud,actor),10)==(!enabled||actor==f.owner)
                    &&item.isRemoved()==(!enabled||actor==f.owner),"Final ItemEntity damage override retains indirect authority");item.discard();
            cow.discard();cloud.discard();
        }
    });}
    static void connectedPistons(GameTestHelper h){modes(h,(f,enabled)->{
        for(var sticky:List.of(Blocks.SLIME_BLOCK,Blocks.HONEY_BLOCK)){
            var p=f.edge.west().north(2);var s=Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.SOUTH);
            f.set(p,s);f.set(p.south(),sticky);f.set(p.south().east(),Blocks.STONE);
            boolean moved=((ClaimPistonInvoker)Blocks.PISTON).claims$move(f.l,p,Direction.SOUTH,true);
            yes(h,moved==!enabled,"Connected "+sticky+" branch cannot pull claimed block");
            if(enabled)yes(h,f.l.getBlockState(p.south()).is(sticky)&&f.l.getBlockState(p.south().east()).is(Blocks.STONE),"Denied connected movement is atomic");
            for(int x=0;x<=1;x++)for(int z=0;z<=3;z++)f.set(p.offset(x,0,z),Blocks.AIR);
            var own=f.edge.east(3).north(2);f.set(own,s);f.set(own.south(),sticky);f.set(own.south().east(),Blocks.STONE);
            yes(h,((ClaimPistonInvoker)Blocks.PISTON).claims$move(f.l,own,Direction.SOUTH,true),"Same-owner connected movement works");
            for(int x=0;x<=1;x++)for(int z=0;z<=3;z++)f.set(own.offset(x,0,z),Blocks.AIR);
            // The initial block is unclaimed; only a connected side branch crosses the border.
            f.set(p,Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.SOUTH).setValue(PistonBaseBlock.EXTENDED,true));
            f.set(p.south(),Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING,Direction.SOUTH).setValue(PistonHeadBlock.TYPE,PistonType.STICKY));
            f.set(p.south(2),sticky);f.set(p.south(2).east(),Blocks.STONE);
            yes(h,((ClaimPistonInvoker)Blocks.STICKY_PISTON).claims$move(f.l,p,Direction.SOUTH,false)==!enabled,"Retraction previews connected "+sticky+" branch without removing the head");
            if(enabled)yes(h,f.l.getBlockState(p.south()).is(Blocks.PISTON_HEAD)&&f.l.getBlockState(p.south(2).east()).is(Blocks.STONE),"Denied connected retraction preserves both head and foreign block");
            for(int x=0;x<=1;x++)for(int z=0;z<=3;z++)f.set(p.offset(x,0,z),Blocks.AIR);
        }
        for(boolean own:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west(2);var s=Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST).setValue(PistonBaseBlock.EXTENDED,true);
            f.set(p,s);f.set(p.east(),Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING,Direction.EAST).setValue(PistonHeadBlock.TYPE,PistonType.STICKY));f.set(p.east(2),Blocks.STONE);
            yes(h,((ClaimPistonInvoker)Blocks.STICKY_PISTON).claims$move(f.l,p,Direction.EAST,false)==(!enabled||own),"Actual sticky retraction checks source ownership; enabled="+enabled+", own="+own);
        }
    });}
    static void customPistons(GameTestHelper h){if(skipWithoutSimpleBuilding(h,"claims customPistons"))return;modes(h,(f,enabled)->{
        for(String id:List.of("reinforced_piston","netherite_piston","enderite_piston"))for(boolean own:new boolean[]{false,true})for(boolean breach:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west();var machine=block(id);var state=machine.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST);
            for(int x=-1;x<=4;x++){f.set(p.east(x),Blocks.AIR);f.set(p.east(x).below(),Blocks.AIR);}
            f.set(p,state);f.set(p.east(),breach?Blocks.BEDROCK:Blocks.STONE);f.set(p.below(),Blocks.REDSTONE_BLOCK);
            state.triggerEvent(f.l,p,0,Direction.EAST.get3DDataValue());
            if(enabled&&!own)yes(h,f.l.getBlockState(p).equals(state)&&f.l.getBlockState(p.east()).is(breach?Blocks.BEDROCK:Blocks.STONE)&&f.l.getBlockState(p.below()).is(Blocks.REDSTONE_BLOCK),"Denied custom piston preserves target, wear and fuel: "+id);
            else yes(h,!f.l.getBlockState(p.east()).is(breach?Blocks.BEDROCK:Blocks.STONE),"Real custom piston positive/off control: "+id+", breach="+breach);
        }
        // Reinforced piston fuel can be across a different boundary from its movement.
        var p=f.edge;var machine=block("reinforced_piston");var state=machine.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST);
        f.set(p,state);f.set(p.east(),Blocks.BEDROCK);f.set(p.east(2),Blocks.AIR);f.set(p.west(),Blocks.REDSTONE_BLOCK);
        state.triggerEvent(f.l,p,0,Direction.EAST.get3DDataValue());
        yes(h,enabled?f.l.getBlockState(p.west()).is(Blocks.REDSTONE_BLOCK)&&f.l.getBlockState(p).equals(state):f.l.getBlockState(p.west()).isAir(),"Reinforced breach checks fuel before movement");
    });}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void tick(ServerLevel l,BlockPos p){
        var state=l.getBlockState(p);var be=l.getBlockEntity(p);
        var ticker=(BlockEntityTicker<BlockEntity>)(BlockEntityTicker)((EntityBlock)state.getBlock()).getTicker(l,state,be.getType());ticker.tick(l,p,state,be);
    }
    static void customHoppers(GameTestHelper h){if(skipWithoutSimpleBuilding(h,"claims customHoppers"))return;modes(h,(f,enabled)->{
        if(enabled)yes(h,f.claims.create(new ClaimStore.Key(f.l.dimension().identifier().toString(),ChunkPos.pack(f.edge.west())),UUID.randomUUID(),100),"Distinct neighboring machine owner");
        for(String id:List.of("reinforced_hopper","netherite_hopper","enderite_hopper"))for(boolean own:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west();var target=p.east();
            f.set(p,Blocks.AIR);f.set(target,Blocks.AIR);f.set(p,block(id).defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST));f.set(target,Blocks.CHEST);
            var from=(Container)f.l.getBlockEntity(p);var into=(Container)f.l.getBlockEntity(target);from.setItem(0,new ItemStack(Items.APPLE,3));
            for(int t=0;t<16;t++)tick(f.l,p);
            yes(h,enabled&&!own?into.isEmpty()&&from.getItem(0).getCount()==3:!into.isEmpty(),"Actual custom hopper transfer: "+id+", own="+own);
            from.clearContent();into.clearContent();f.set(p,Blocks.AIR);f.set(target,Blocks.AIR);
        }
        // Dropper has its own dispenseFrom override, including an ejection path without a container.
        for(boolean own:new boolean[]{false,true})for(boolean container:new boolean[]{false,true}){
            var p=own?f.edge.east(3):f.edge.west();f.set(p,Blocks.AIR);f.set(p.east(),container?Blocks.CHEST:Blocks.AIR);
            var state=Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);f.set(p,state);
            var from=(Container)f.l.getBlockEntity(p);from.setItem(0,new ItemStack(Items.APPLE,3));state.tick(f.l,p,RandomSource.create(0));
            yes(h,from.getItem(0).getCount()==(enabled&&!own?3:2),"Real dropper source/destination check, container="+container);
        }
        var p=f.edge.west();f.set(p,Blocks.AIR);f.set(p.east(),Blocks.AIR);
        var dispenser=Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);f.set(p,dispenser);
        var source=(Container)f.l.getBlockEntity(p);source.setItem(0,new ItemStack(Items.WATER_BUCKET));dispenser.tick(f.l,p,RandomSource.create(0));
        yes(h,enabled?source.getItem(0).is(Items.WATER_BUCKET)&&f.l.getFluidState(p.east()).isEmpty():source.getItem(0).is(Items.BUCKET)&&!f.l.getFluidState(p.east()).isEmpty(),"Dispenser denial preserves its item; disabled and absent providers retain bucket behavior");
    });}
    static void attractor(GameTestHelper h){if(skipWithoutSimpleBuilding(h,"claims attractor"))return;
        // getChunk alone does not make new entities visible to the attractor's query.
        // Await actual query visibility on both sides before creating any claim fixture.
        var edge=ToolClaimTests.boundary(h).above(90);
        var chunks=List.of(ChunkPos.containing(edge),ChunkPos.containing(edge.west(16)));
        var forced=new ArrayList<ChunkPos>();
        for(var chunk:chunks){
            if(h.getLevel().setChunkForced(chunk.x(),chunk.z(),true))forced.add(chunk);
            h.getLevel().getChunk(chunk.x(),chunk.z());
        }
        var probes=new ArrayList<ItemEntity>();
        for(var pos:List.of(edge.west(5),edge.east())){
            var probe=new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,
                    new ItemStack(Items.APPLE),0,0,0);
            probe.setNoGravity(true);
            h.getLevel().addFreshEntity(probe);
            probes.add(probe);
        }
        Runnable release=()->{
            probes.forEach(Entity::discard);
            for(var chunk:forced)h.getLevel().setChunkForced(chunk.x(),chunk.z(),false);
        };
        h.runBeforeTestEnd(release);
        h.startSequence().thenWaitUntil(()->{
            yes(h,h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(edge).inflate(8)).containsAll(probes),
                    "Attractor boundary probes are not queryable yet");
        }).thenExecute(()->{try {
        probes.forEach(Entity::discard);
        modes(h,(f,enabled)->{
        var p=f.edge.west(2);
        if(Math.floorMod(f.l.getGameTime()+p.asLong(),2)!=0)p=p.above();
        f.set(p.below(),Blocks.STONE);f.owner.setShiftKeyDown(true);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ToolClaimTests.item("magnet")));
        var hit=new BlockHitResult(Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false);
        yes(h,f.owner.getMainHandItem().getItem().useOn(new UseOnContext(f.owner,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Place attractor through real item use");
        var foreign=f.add(new ItemEntity(f.l,0,0,0,new ItemStack(Items.APPLE),0,0,0),new BlockPos(f.edge.getX()+1,p.getY(),p.getZ()));
        var local=f.add(new ItemEntity(f.l,0,0,0,new ItemStack(Items.APPLE),0,0,0),p.west(3));
        yes(h,f.l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(6)).containsAll(List.of(local,foreign)),
                "Both attractor probes must be visible before testing claim denial");
        tick(f.l,p);
        yes(h,foreign.getDeltaMovement().equals(Vec3.ZERO)==enabled&&!local.getDeltaMovement().equals(Vec3.ZERO),"Actual placed-attractor ticker preserves foreign items and pulls local items");
        if(enabled){
            yes(h,f.claims.create(new ClaimStore.Key(f.l.dimension().identifier().toString(),ChunkPos.pack(p)),f.owner.getUUID(),120),"Adjacent same-owner chunk fixture");
            tick(f.l,p);yes(h,!foreign.getDeltaMovement().equals(Vec3.ZERO),"Attractor works across adjacent same-owner chunks");
        }
        });}finally{release.run();}});
    }

    // ---- Completion (2026-10-12): mob griefing, damage policy, vehicles, provenance, positioned wrappers ----
    private static Object inner(String name,Class<?> type,Object arg){
        try{var c=Class.forName(name).getDeclaredConstructor(type);c.setAccessible(true);return c.newInstance(arg);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void field(Object target,Class<?> owner,String name,Object value){
        try{var f=owner.getDeclaredField(name);f.setAccessible(true);f.set(target,value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void loaded(net.minecraft.server.level.ServerPlayer p){p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());}
    static void mobDamage(GameTestHelper h){warmEntityModes(h,(f,enabled)->{
        var inside=f.edge.east(4);
        var zombie=EntityTypes.ZOMBIE.create(f.l,EntitySpawnReason.COMMAND);zombie.setPos(Vec3.atBottomCenterOf(f.edge.west(3)));
        var cow=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),inside);cow.setNoAi(true);float before=cow.getHealth();
        yes(h,cow.hurtServer(f.l,f.l.damageSources().mobAttack(zombie),2)&&cow.getHealth()<before,"Mob attacks on claimed animals stay Vanilla");
        var visitor=player(h,inside.south(2));f.entities.add(visitor);loaded(visitor);before=visitor.getHealth();
        yes(h,visitor.hurtServer(f.l,f.l.damageSources().mobAttack(zombie),2)&&visitor.getHealth()<before,"A claim is never a mob-immune zone for players");
        var skeleton=EntityTypes.SKELETON.create(f.l,EntitySpawnReason.COMMAND);skeleton.setPos(Vec3.atBottomCenterOf(f.edge.west(3)));
        var arrow=EntityTypes.ARROW.create(f.l,EntitySpawnReason.COMMAND);arrow.setOwner(skeleton);visitor.setInvulnerableTime(0);visitor.damageCooldownTime=0;before=visitor.getHealth();
        var arrowSource=f.l.damageSources().arrow(arrow,skeleton);boolean arrowHit=visitor.hurtServer(f.l,arrowSource,2);
        yes(h,Claims.damage(arrowSource,visitor)&&Claims.action(arrow,visitor)&&arrowHit&&visitor.getHealth()<before,"Mob projectiles still hit players inside a claim (hit="+arrowHit+", health "+before+"->"+visitor.getHealth()+")");
        cow.setInvulnerableTime(0);cow.damageCooldownTime=0;visitor.setInvulnerableTime(0);visitor.damageCooldownTime=0;float cowBefore=cow.getHealth();before=visitor.getHealth();
        new ServerExplosion(f.l,null,null,null,Vec3.atCenterOf(inside.south()),2,false,Explosion.BlockInteraction.KEEP).explode();
        yes(h,visitor.getHealth()<before,"Players inside a claim still take unattributed explosion damage");
        yes(h,(cow.getHealth()==cowBefore)==enabled,"Unattributed explosions spare claimed animals only while enabled");
        var stranger=player(h,f.edge.west(2));f.entities.add(stranger);
        // A fresh claimed cow: the explosion-hit one keeps Vanilla hurt state that refuses a weaker follow-up hit.
        var fresh=f.add(EntityTypes.COW.create(f.l,EntitySpawnReason.COMMAND),inside.north(2));fresh.setNoAi(true);
        boolean strangerHit=fresh.hurtServer(f.l,f.l.damageSources().playerAttack(stranger),1);
        yes(h,strangerHit==!enabled,"Untrusted player attacks on claimed animals are refused (enabled="+enabled+", hit="+strangerHit+")");
        var bolt=EntityTypes.LIGHTNING_BOLT.create(f.l,EntitySpawnReason.COMMAND);bolt.setCause(stranger);cow.removeAllEffects();
        yes(h,cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100),bolt)==!enabled,"A lightning bolt counts as its causing player");
    });}
    static void mobBlocks(GameTestHelper h){modes(h,(f,enabled)->{
        var wither=EntityTypes.WITHER.create(f.l,EntitySpawnReason.COMMAND);wither.setPos(Vec3.atCenterOf(f.edge.west(3)));
        var ravager=EntityTypes.RAVAGER.create(f.l,EntitySpawnReason.COMMAND);ravager.setPos(Vec3.atCenterOf(f.edge.east(3)));
        for(var hostile:List.<Entity>of(wither,ravager))for(boolean claimed:new boolean[]{true,false}){
            var p=claimed?f.edge.east(2):f.edge.west(2);f.set(p,Blocks.STONE);boolean denied=enabled&&claimed;
            yes(h,f.l.destroyBlock(p,false,hostile)!=denied&&f.l.getBlockState(p).isAir()!=denied,"Hostile block destruction respects claims: "+hostile.getType()+" claimed="+claimed+" enabled="+enabled);
        }
        var target=f.edge.east(2);var villager=EntityTypes.VILLAGER.create(f.l,EntitySpawnReason.COMMAND);
        villager.setPos(Vec3.atCenterOf(f.edge.east(5)));f.set(target,Blocks.STONE);
        yes(h,f.l.destroyBlock(target,false,villager),"Non-hostile mobs work on the land they stand on");
        villager.setPos(Vec3.atCenterOf(f.edge.west(2)));f.set(target,Blocks.STONE);
        yes(h,f.l.destroyBlock(target,false,villager)==!enabled,"Non-hostile mobs cannot reach across a claim border");
        var stranger=player(h,f.edge.west(2));f.entities.add(stranger);f.set(target,Blocks.STONE);
        yes(h,f.l.destroyBlock(target,false,stranger)==!enabled,"Player-attributed destruction needs rights");
        f.set(target,Blocks.STONE);yes(h,f.l.destroyBlock(target,false,f.owner),"Owner-attributed destruction allowed");
        // Ender Dragon wall check through the real private method.
        var dragon=EntityTypes.ENDER_DRAGON.create(f.l,EntitySpawnReason.COMMAND);dragon.setPos(Vec3.atCenterOf(f.edge));
        f.set(f.edge.west(),Blocks.STONE);f.set(f.edge,Blocks.STONE);
        try{var m=net.minecraft.world.entity.boss.enderdragon.EnderDragon.class.getDeclaredMethod("checkWalls",net.minecraft.server.level.ServerLevel.class,AABB.class);m.setAccessible(true);
            m.invoke(dragon,f.l,new AABB(Vec3.atLowerCornerOf(f.edge.west()),Vec3.atLowerCornerOf(f.edge.east()).add(0,1,1)).deflate(0.01));}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        yes(h,f.l.getBlockState(f.edge.west()).isAir()&&f.l.getBlockState(f.edge).is(Blocks.STONE)==enabled,"Ender Dragon breaks wilderness walls only");
    });}
    static void enderman(GameTestHelper h){modes(h,(f,enabled)->{
        for(boolean claimed:new boolean[]{true,false}){
            var at=claimed?f.edge.east(6):f.edge.west(6);boolean denied=enabled&&claimed;
            for(int x=-2;x<=1;x++)for(int y=0;y<=2;y++)for(int z=-2;z<=1;z++)if(x!=0||z!=0)f.set(at.offset(x,y,z),Blocks.DIRT);
            var enderman=EntityTypes.ENDERMAN.create(f.l,EntitySpawnReason.COMMAND);enderman.setPos(Vec3.atBottomCenterOf(at));
            var take=(net.minecraft.world.entity.ai.goal.Goal)inner("net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal",net.minecraft.world.entity.monster.Enderman.class,enderman);
            int removed=0;for(int t=0;t<200;t++)take.tick();
            for(int x=-2;x<=1;x++)for(int y=0;y<=2;y++)for(int z=-2;z<=1;z++)if((x!=0||z!=0)&&f.l.getBlockState(at.offset(x,y,z)).isAir())removed++;
            yes(h,(removed==0)==denied&&(enderman.getCarriedBlock()==null)==denied,"Actual Enderman take goal respects claims: claimed="+claimed+" removed="+removed);
            for(int x=-2;x<=1;x++)for(int y=0;y<=2;y++)for(int z=-2;z<=1;z++)f.set(at.offset(x,y,z),Blocks.AIR);
            for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)f.set(at.offset(x,-1,z),Blocks.STONE);
            enderman.setCarriedBlock(Blocks.DIRT.defaultBlockState());
            var leave=(net.minecraft.world.entity.ai.goal.Goal)inner("net.minecraft.world.entity.monster.Enderman$EndermanLeaveBlockGoal",net.minecraft.world.entity.monster.Enderman.class,enderman);
            for(int t=0;t<200&&enderman.getCarriedBlock()!=null;t++)leave.tick();
            yes(h,(enderman.getCarriedBlock()!=null)==denied,"Actual Enderman leave goal never places into a claim: claimed="+claimed);
            for(int x=-1;x<=0;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=0;z++)f.set(at.offset(x,y,z),Blocks.AIR);
        }
    });}
    static void doorsSilverfish(GameTestHelper h){modes(h,(f,enabled)->{
        for(boolean claimed:new boolean[]{true,false}){
            var door=claimed?f.edge.east(3):f.edge.west(3);boolean denied=enabled&&claimed;
            f.set(door.below(),Blocks.STONE);
            f.set(door,Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
            f.set(door.above(),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
            var zombie=EntityTypes.ZOMBIE.create(f.l,EntitySpawnReason.COMMAND);zombie.setPos(Vec3.atBottomCenterOf(door.west()));
            var goal=new net.minecraft.world.entity.ai.goal.BreakDoorGoal(zombie,d->true);
            field(goal,net.minecraft.world.entity.ai.goal.DoorInteractGoal.class,"doorPos",door);
            field(goal,net.minecraft.world.entity.ai.goal.BreakDoorGoal.class,"breakTime",239);
            goal.tick();
            yes(h,f.l.getBlockState(door).is(Blocks.OAK_DOOR)==denied,"Actual door-breaking tick respects claims: claimed="+claimed);
            f.set(door,Blocks.AIR);f.set(door.above(),Blocks.AIR);
            var host=claimed?f.edge.east(4):f.edge.west(4);f.set(host,Blocks.STONE);
            var silverfish=EntityTypes.SILVERFISH.create(f.l,EntitySpawnReason.COMMAND);silverfish.setPos(Vec3.atBottomCenterOf(host.west()));
            var merge=(net.minecraft.world.entity.ai.goal.Goal)inner("net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal",net.minecraft.world.entity.monster.Silverfish.class,silverfish);
            field(merge,merge.getClass(),"doMerge",true);field(merge,merge.getClass(),"selectedDirection",Direction.EAST);
            merge.start();
            yes(h,f.l.getBlockState(host).is(Blocks.STONE)==denied&&silverfish.isRemoved()!=denied,"Actual silverfish infestation respects claims: claimed="+claimed);
            f.set(host,Blocks.AIR);
        }
    });}
    static void trample(GameTestHelper h){modes(h,(f,enabled)->{
        var stranger=player(h,f.edge.west(2));f.entities.add(stranger);
        for(boolean claimed:new boolean[]{true,false}){
            var p=claimed?f.edge.east(3):f.edge.west(3);
            for(var actor:List.<Entity>of(EntityTypes.SPIDER.create(f.l,EntitySpawnReason.COMMAND),stranger,f.owner)){
                boolean denied=enabled&&claimed&&actor!=f.owner;
                f.set(p,Blocks.FARMLAND);Blocks.FARMLAND.fallOn(f.l,f.l.getBlockState(p),p,actor,2);
                yes(h,f.l.getBlockState(p).is(Blocks.FARMLAND)==denied,"Actual farmland trampling respects claims: "+actor.getType()+" claimed="+claimed);
            }
            var spider=EntityTypes.SPIDER.create(f.l,EntitySpawnReason.COMMAND);spider.setPos(Vec3.atBottomCenterOf(p));
            f.set(p.below(),Blocks.SAND);f.set(p,Blocks.TURTLE_EGG);
            for(int i=0;i<60&&f.l.getBlockState(p).is(Blocks.TURTLE_EGG);i++)Blocks.TURTLE_EGG.fallOn(f.l,f.l.getBlockState(p),p,spider,2);
            yes(h,f.l.getBlockState(p).is(Blocks.TURTLE_EGG)==(enabled&&claimed),"Actual turtle egg trampling respects claims: claimed="+claimed);
            f.set(p,Blocks.AIR);
        }
    });}
    static void boats(GameTestHelper h){
        // Like the crafter: a freshly loaded workspace chunk only shows its new entities after a server tick, so the
        // first (enabled) pass sometimes did not find the owner's boat on tick 0; a 5-tick delay was not enough either.
        warmEntityModes(h,(f,enabled)->{
        var water=f.edge.east(3);f.set(water.below(),Blocks.STONE);f.set(water,Blocks.WATER);
        var stranger=player(h,f.edge.west(8));f.entities.add(stranger);f.owner.setPos(Vec3.atBottomCenterOf(f.edge.west(8)));
        for(var actor:List.of(stranger,f.owner)){
            actor.setPos(Vec3.atBottomCenterOf(water.above(2)));actor.setXRot(90);actor.setYRot(0);
            actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_BOAT));
            Items.OAK_BOAT.use(f.l,actor,InteractionHand.MAIN_HAND);
            var boats=f.l.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class,new AABB(water).inflate(3));
            boolean allowed=!enabled||actor==f.owner;
            yes(h,boats.isEmpty()!=allowed&&actor.getMainHandItem().isEmpty()==allowed,"Actual boat placement respects claims: owner="+(actor==f.owner)+" enabled="+enabled);
            boats.forEach(Entity::discard);actor.setPos(Vec3.atBottomCenterOf(f.edge.west(8)));// never block the next actor's view
        }
    });}
    @SuppressWarnings({"rawtypes","unchecked"})
    static void composter(GameTestHelper h){modes(h,(f,enabled)->{
        var hopperPos=f.edge.east(3);var hopperState=Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN);
        f.set(hopperPos,hopperState);f.set(hopperPos.above(),Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL,8));
        var be=f.l.getBlockEntity(hopperPos);var ticker=(BlockEntityTicker<BlockEntity>)(BlockEntityTicker)((EntityBlock)Blocks.HOPPER).getTicker(f.l,hopperState,be.getType());
        for(int t=0;t<16;t++)ticker.tick(f.l,hopperPos,hopperState,be);
        yes(h,((Container)be).countItem(Items.BONE_MEAL)==1,"Same-owner hopper empties a composter (positioned wrapper resolved)");
        ((Container)be).clearContent();
        var border=f.edge.west();var sideState=Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST);
        f.set(border,sideState);f.set(f.edge,Blocks.COMPOSTER.defaultBlockState());
        var side=f.l.getBlockEntity(border);((Container)side).setItem(0,new ItemStack(Items.WHEAT_SEEDS,4));
        var sideTicker=(BlockEntityTicker<BlockEntity>)(BlockEntityTicker)((EntityBlock)Blocks.HOPPER).getTicker(f.l,sideState,side.getType());
        for(int t=0;t<16;t++)sideTicker.tick(f.l,border,sideState,side);
        int seeds=((Container)side).countItem(Items.WHEAT_SEEDS);
        // Vanilla composters only accept items from above, and claims are full chunk columns, so a wilderness hopper can never
        // feed a claimed composter; the side hopper stays full in both modes (the claim adds nothing, takes nothing away).
        yes(h,seeds==4,"Composters only accept items from above, a border hopper never feeds one: enabled="+enabled+", seeds="+seeds+", level="+f.l.getBlockState(f.edge));
        ((Container)side).clearContent();
    });}
}

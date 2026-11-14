package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.portal.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
/** PLAN40 simpledimensions-4: follow-up checks of the audit fixes (kept apart from DimensionTests). */
public final class DimensionFollowUpTests {
 /** The click throttle belongs to a real portal frame: right after lighting one, a click elsewhere is Vanilla's business (PASS, not FAIL). */
 public static void igniterThrottleOnlyForFrames(GameTestHelper h){
  var c=DimensionPortalConfig.defaultSkyblock();var cell=DimensionTests.build(h,c,1,Direction.Axis.X);
  h.getLevel().setBlock(cell.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);
  var p=DimensionTests.player(h,cell,Items.FLINT_AND_STEEL);
  h.assertTrue(DimensionTests.ignite(p,cell)==InteractionResult.SUCCESS,"A real frame lights");
  var elsewhere=cell.offset(0,0,3);h.getLevel().setBlock(elsewhere.below(),Blocks.STONE.defaultBlockState(),3);
  h.assertTrue(DimensionTests.ignite(p,elsewhere)==InteractionResult.PASS,"A click that hits no frame is not swallowed by the throttle");
  h.succeed();
 }
 private DimensionFollowUpTests(){}
}

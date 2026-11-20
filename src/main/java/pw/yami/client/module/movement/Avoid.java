package pw.yami.client.module.movement;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.movement.ClimbingHookEvent;
import pw.yami.client.event.render.GetCollisionShapeEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.world.CollisionUtils;
import pw.yami.eventbus.annotation.EventListener;

public class Avoid extends ClientModule {
   Setting voidSetting = this.registerSetting(new BooleanSetting("Void", "Prevents player from falling into the void", true));
   Setting fire = this.registerSetting(new BooleanSetting("Fire", "Prevents player from walking into fire", false));
   Setting berryBush = this.registerSetting(new BooleanSetting("BerryBush", "Prevents player from walking into sweet berry bushes", false));
   Setting cactus = this.registerSetting(new BooleanSetting("Cactus", "Prevents player from walking into cacti", false));
   Setting unloaded = this.registerSetting(new BooleanSetting("Unloaded", "Prevents player from entering chunks that haven't been loaded", false));
   Setting climb = this.registerSetting(new BooleanSetting("Climb", "Prevents player from climbing up blocks", false));
   public static boolean bd;

   public Avoid() {
      super("Avoid", "Prevents player from entering harmful areas", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() == EventPhase.PRE
         && (Boolean)this.voidSetting.atx()
         && !client.player.isSpectator()
         && client.player.getY() < client.world.getBottomY()) {
         Managers.playerStateManager.rJ(0.0);
      }
   }

   @EventListener
   public void onGetCollisionShape(GetCollisionShapeEvent getCollisionShapeEvent) {
      BlockPos pos = getCollisionShapeEvent.getPos();
      if ((Boolean)this.fire.atx() && getCollisionShapeEvent.asa() == Blocks.FIRE && client.player.getY() < pos.getY() + 1.0
         || (Boolean)this.cactus.atx() && getCollisionShapeEvent.asa() == Blocks.CACTUS
         || (Boolean)this.berryBush.atx() && getCollisionShapeEvent.asa() == Blocks.SWEET_BERRY_BUSH
         || (Boolean)this.unloaded.atx() && !CollisionUtils.aix(pos.getX(), pos.getZ())) {
         getCollisionShapeEvent.aim();
         getCollisionShapeEvent.setVoxelShape(VoxelShapes.fullCube());
      }
   }

   @EventListener
   public void pP(ClimbingHookEvent climbingHookEvent) {
      if ((Boolean)this.climb.atx()) {
         climbingHookEvent.aim();
      }
   }
}

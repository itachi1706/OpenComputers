package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.init.Blocks
import net.minecraftforge.common.util.ForgeDirection

abstract class UpgradePiston(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("piston").
    withConnector().
    create()

  def pushDirection(args: Arguments, index: Int): ForgeDirection

  def pushOrigin(side: ForgeDirection) = BlockPosition(host)

  @Callback(doc = """function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val side = pushDirection(args, 0)
    val hostPos = pushOrigin(side)
    val blockPos = hostPos.offset(side)
    if (!host.world.isAirBlock(blockPos) && node.tryChangeBuffer(-Settings.get.pistonCost) && Blocks.piston.tryExtend(host.world, hostPos.x, hostPos.y, hostPos.z, side.ordinal)) {
      host.world.setBlockToAir(blockPos)
      host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, "tile.piston.out", 0.5f, host.world.rand.nextFloat() * 0.25f + 0.6f)
      context.pause(0.5)
      result(true)
    }
    else result(false)
  }
}

object UpgradePiston {

  class Drone(drone: internal.Drone) extends UpgradePiston(drone) {
    override def pushDirection(args: Arguments, index: Int) = args.optSideAny(index, ForgeDirection.SOUTH)
  }

  class Tablet(tablet: internal.Tablet) extends Rotatable(tablet) {
    override def pushOrigin(side: ForgeDirection) =
      if (side == ForgeDirection.DOWN && tablet.player.getEyeHeight > 1) super.pushOrigin(side).offset(ForgeDirection.DOWN)
      else super.pushOrigin(side)
  }

  class Rotatable(val rotatable: internal.Rotatable with EnvironmentHost) extends UpgradePiston(rotatable) {
    override def pushDirection(args: Arguments, index: Int) = rotatable.toGlobal(args.optSideForAction(index, ForgeDirection.SOUTH))
  }

}
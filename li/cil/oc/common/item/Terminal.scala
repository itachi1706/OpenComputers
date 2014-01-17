package li.cil.oc.common.item

import cpw.mods.fml.relauncher.{SideOnly, Side}
import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class Terminal(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Terminal"

  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "server")) {
      val server = stack.getTagCompound.getString(Settings.namespace + "server")
      tooltip.add("§8" + server.substring(0, 13) + "...§7")
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":terminal")
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getBlockTileEntity(x, y, z) match {
      case rack: tileentity.Rack if side == rack.facing.ordinal() =>
        val l = 2 / 16.0
        val h = 14 / 16.0
        val s = (((1 - hitY) - l) / (h - l) * 4).toInt
        if (s >= 0 && s <= 3 && rack.items(s).isDefined) {
          if (!world.isRemote) {
            rack.servers(s) match {
              case Some(server) =>
                if (!stack.hasTagCompound) {
                  stack.setTagCompound(new NBTTagCompound("tag"))
                }
                stack.getTagCompound.setString(Settings.namespace + "server", server.machine.address)
                player.inventory.onInventoryChanged()
              case _ => // Huh?
            }
          }
          true
        }
        else false
      case _ => super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking && stack.hasTagCompound) {
      val address = stack.getTagCompound.getString(Settings.namespace + "server")
      if (address != null && !address.isEmpty) {
        if (world.isRemote) {
          player.openGui(OpenComputers, GuiType.Terminal.id, world, 0, 0, 0)
        }
        player.swingItem()
      }
    }
    super.onItemRightClick(stack, world, player)
  }
}

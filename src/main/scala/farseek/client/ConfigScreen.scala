package farseek.client

import cpw.mods.fml.client._
import cpw.mods.fml.relauncher.Side._
import cpw.mods.fml.relauncher.SideOnly
import farseek.config._
import farseek.util.ImplicitConversions._
import farseek.util._
import java.lang.System._
import net.minecraft.client.gui._
import net.minecraft.client.resources.I18n
import scala.collection.mutable
import scala.math._

/** A [[GuiScreen]] that displays configuration options for a
  * [[ConfigCategory]].
  * @author
  *   delvr
  */
@SideOnly(CLIENT)
abstract class ConfigScreen(parent: GuiScreen) extends GuiScreen {

  protected def category: ConfigCategory

  private var lastMouseX = 0
  private var lastMouseY = 0
  private var mouseStillTime = 0L

  private val buttons: mutable.Buffer[GuiButton] = buttonList
  private val Done = -1
  private val h = 20

  /** Adds the category's elements to the screen as buttons in two rows. */
  override def initGui() {
    category.elements.zipWithIndex.foreach { case (element, i) =>
      val x = if (i % 2 == 0) 20 else 10 + width / 2
      val y = 30 + i / 2 * 30
      val w = width / 2 - 30
      val control = element match {
        case category: ConfigCategory =>
          Some(
            new CategoryButton(
              category,
              i,
              x,
              y,
              w,
              h,
              s"${category.caption}..."
            )
          )
        case setting: BooleanSetting =>
          Some(new OnOffSettingButton(setting, i, x, y, w, h))
        case setting: MultiChoiceSetting =>
          Some(new MultiChoiceSettingButton(setting, i, x, y, w, h))
        case setting: NumericSetting =>
          Some(new SettingSlider(setting, i, x, y, w, h))
        case _ => None
      }
      control.foreach(buttons += _)
    }
    buttons += new GuiButton(
      Done,
      width / 4,
      height - 38,
      width / 2,
      h,
      I18n.format("gui.done")
    )
  }

  /** Draws the screen's buttons and sets up their tooltips. */
  override def drawScreen(x: Int, y: Int, renderPartialTicks: Float) {
    drawDefaultBackground()
    drawCenteredString(
      fontRendererObj,
      category.caption,
      width / 2,
      10,
      0xffffff
    )
    super.drawScreen(x, y, renderPartialTicks)
    if (abs(x - lastMouseX) <= 5 && abs(y - lastMouseY) <= 5) {
      if (currentTimeMillis >= mouseStillTime + 700) {
        buttons.find(_.func_146115_a).foreach {
          case control: SettingButton[_] =>
            val x1 = 20
            val tmp = height / 6 - 5
            val y1 = if (y <= tmp + 98) tmp + 105 else tmp
            drawGradientRect(
              x1,
              y1,
              width - x1,
              y1 + 94,
              -536870912,
              -536870912
            )
            wordWrap(control.tooltip, (width / 5.5).toInt - 2).zipWithIndex
              .foreach { case (line, index) =>
                fontRendererObj.drawStringWithShadow(
                  line,
                  x1 + 5,
                  y1 + 5 + index * 11,
                  14540253
                )
              }
          case _ =>
        }
      }
    } else {
      lastMouseX = x
      lastMouseY = y
      mouseStillTime = currentTimeMillis
    }
  }

  /** Handles clicks on buttons for settings, subcategories and "Done". */
  override protected def actionPerformed(clicked: GuiButton) {
    clicked match {
      case button: ToggleSettingButton[_] => button.clicked()
      case button: CategoryButton =>
        mc.displayGuiScreen(new CategoryConfigScreen(this, button.category))
      case button: GuiButton if button.id == Done => close()
      case _                                      =>
    }
  }

  protected def close() {
    FMLClientHandler.instance.showGuiScreen(parent)
  }
}

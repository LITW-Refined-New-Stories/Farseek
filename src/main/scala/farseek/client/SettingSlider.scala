package farseek.client

import farseek.config.NumericSetting
import net.minecraft.client.Minecraft
import net.minecraft.util.MathHelper._
import org.lwjgl.opengl.GL11._

/** A slider for a [[NumericSetting]].
  * @author
  *   delvr
  */
class SettingSlider(
    setting: NumericSetting,
    index: Int,
    _xPosition: Int,
    _yPosition: Int,
    _width: Int,
    _height: Int
) extends SettingButton(
      setting,
      index,
      _xPosition,
      _yPosition,
      _width,
      _height
    ) {

  private var mouseIsPressed = false

  private def setValueFromPosition(x: Int) {
    setValue(
      setting.snapToStep(
        setting.denormalized(
          clamp_double(
            (x - (xPosition + 4)).toDouble / (width - 8).toDouble,
            0f,
            1f
          )
        )
      )
    )
  }

  override protected def getHoverState(unused: Boolean) = 0

  // Misnomer: actually called as part of drawButton()
  override protected def mouseDragged(game: Minecraft, x: Int, y: Int) {
    if (mouseIsPressed)
      setValueFromPosition(x)
    val normalizedValue = setting.normalized(setting.value)
    glColor4f(1f, 1f, 1f, 1f)
    drawTexturedModalRect(
      xPosition + (normalizedValue * (width - 8).toDouble).toInt,
      yPosition,
      0,
      66,
      4,
      20
    )
    drawTexturedModalRect(
      xPosition + (normalizedValue * (width - 8).toDouble).toInt + 4,
      yPosition,
      196,
      66,
      4,
      20
    )
  }

  // Misnomer: means that cursor is over this, checked when screen clicked
  override def mousePressed(game: Minecraft, x: Int, y: Int) = {
    if (super.mousePressed(game, x, y)) {
      mouseIsPressed = true
      setValueFromPosition(x)
      true
    } else false
  }

  override def mouseReleased(x: Int, y: Int) {
    mouseIsPressed = false
  }
}

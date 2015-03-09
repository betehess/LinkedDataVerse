package LinkedDataVerse;

import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSName("TWEEN")
object Tween extends js.Object {
  val Tween: Tween = js.native
  def update(): Boolean = js.native
  def Easing: Easing = js.native
}

@JSName("TWEEN.Tween")
class Tween extends js.Object {
  def this(obj: js.Object) = this()
  def to(properties: js.Object = js.native, duration: js.UndefOr[Double] = js.native): Tween = js.native
  def start(): Tween = js.native
  def onUpdate(callback: js.Function1[Double, Unit]): Tween = js.native
  def onComplete(callback: js.Function0[Unit]): Tween = js.native
  def easing(easing: EasingFunc): Tween = js.native
}

trait Easing extends js.Object {
  def Linear: Linear = js.native
  def Sinusoidal: Sinusoidal = js.native
}
trait EasingFunc extends js.Function1[Double, Double]

trait Linear extends js.Object {
  def None: EasingFunc = js.native
}

trait Sinusoidal extends js.Object {
  def In: EasingFunc = js.native
  def Out: EasingFunc = js.native
  def InOut: EasingFunc = js.native
}



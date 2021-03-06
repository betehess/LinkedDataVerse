package LinkedDataVerse.world

import scala.scalajs.js
import org.denigma.threejs._

import org.scalajs.dom.html
import org.scalajs.dom
import dom.document

object TextPlane {

  def wrapText(context: js.Dynamic, text: String, x: Int, y: Int, maxWidth: Int, lineHeight: Int ) {

    val words:List[String] = text.split(" ").toList
      .foldLeft(List():List[String])( (a, b) => {
        if (b.length > 35) {
          a :+ b.take(16) + "..." + b.takeRight(16)
        } else {
          a :+ b
        }

      })

    var line = ""
    var yo = y
    var xo = x + 10

    Range(0, words.length).map(n =>  {

      val testLine = line + words(n) + " "
      val metrics = context.measureText(testLine)
      val testWidth = metrics.width.asInstanceOf[Double]

      if (testWidth > maxWidth && n > 0) {
        context.fillText(line, xo, yo);
        line = words(n) + " ";
        yo = yo + lineHeight;
      } else {
        line = testLine;
      }
    })

    context.fillText(line, xo, yo);

  }

  def apply (
    text: String,
    cssBackColor: String = "#eeeeee",
    cssForeColor: String = "#000000",
    width: Int = 300,
    height: Int = 128): Mesh = {

    val canvas = document.createElement("canvas").asInstanceOf[html.Canvas]
    val ctx = canvas.getContext("2d")
    canvas.width = width
    canvas.height = height

    ctx.fillStyle = cssBackColor
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.font = "12pt Helvetica"
    ctx.fillStyle = cssForeColor
    wrapText(ctx, text, 0, 30, canvas.width, 22)

    val texture = new Texture(canvas)
    texture.needsUpdate = true

    val canMaterial = new MeshBasicMaterial(js.Dynamic.literal(
      map = texture,
      transparent = true,
      side = 2 /*THREE.DoubleSide*/
    ).asInstanceOf[MeshBasicMaterialParameters])

    val canGeometry = new BoxGeometry(canvas.width, canvas.height, 10)
    val planeMesh = new Mesh(canGeometry, canMaterial)
    planeMesh.scale.set(0.01, 0.01, 0.01)

    planeMesh

  }

}
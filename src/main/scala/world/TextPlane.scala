package LinkedDataVerse.world

import scala.scalajs.js
import org.denigma.threejs._

import org.scalajs.dom.html
import org.scalajs.dom
import dom.document

object TextPlane {

  def wrapText(context: js.Dynamic, text: String, x: Int, y: Int, maxWidth: Int, lineHeight: Int ) {

    val words = text.split(" ")
    var line = ""
    var yo = y

    Range(0, words.length).map(n =>  {

      val testLine = line + words(n) + " "
      val metrics = context.measureText(testLine)
      val testWidth = metrics.width.asInstanceOf[Double]

      if (testWidth > maxWidth && n > 0) {
        context.fillText(line, x, yo);
        line = words(n) + " ";
        yo = yo + lineHeight;
      } else {
        line = testLine;
      }
    })

    context.fillText(line, x, yo);

  }

  def apply (text: String, cssBackColor: String = "#eeeeee", cssForeColor: String = "#000000"): Mesh = {

    val canvas = document.createElement("canvas").asInstanceOf[html.Canvas]
    val ctx = canvas.getContext("2d")
    ctx.width = 256
    ctx.height = 128
    //ctx.textAlign = "center"
    ctx.fillStyle = cssBackColor
    ctx.fillRect(0, 0, 256, 256)
    ctx.font = "12pt Helvetica"
    ctx.fillStyle = cssForeColor
    wrapText(ctx, text, 0, 30, 256, 22)

    val texture = new Texture(canvas)
    texture.needsUpdate = true

    val canMaterial = new MeshBasicMaterial(js.Dynamic.literal(
      map = texture,
      transparent = true,
      side = 2 /*THREE.DoubleSied*/
    ).asInstanceOf[MeshBasicMaterialParameters]);

    val canGeometry = new PlaneGeometry(canvas.width, canvas.height, 1, 1);
    val planeMesh = new Mesh(canGeometry, canMaterial);
    planeMesh.scale.set(0.01, 0.01, 0.01);

    planeMesh

  }

}
package LinkedDataVerse.world

import scala.scalajs.js
import org.denigma.threejs._

object ImgUrMesh {

  def apply (imgName: String): Mesh = {

    ImageUtils.crossOrigin = "Anonymous"

    val url = if (imgName.startsWith("http")) imgName else "http://i.imgur.com/" + imgName

    val texture = ImageUtils.loadTexture(url)
    println("Textureded:", texture)

    val material = new MeshBasicMaterial(js.Dynamic.literal(
      map = texture,
      side = 2 /*THREE.DoubleSide (throwing runtime weirdness*/,
      transparent = true
    ).asInstanceOf[MeshBasicMaterialParameters])

    val geom = new PlaneGeometry(1, 1, 4, 4)

    new Mesh(geom, material)

  }

}

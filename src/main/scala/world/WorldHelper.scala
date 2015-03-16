package LinkedDataVerse.world

import scala.scalajs.js
import org.denigma.threejs._

import org.scalajs.dom.html
import org.scalajs.dom
import dom.document

object WorldHelper {

  def addLights (scene: Scene): Unit = {

    // Some lights
    val dirLight1 = new DirectionalLight(0xffffff, 0.9)
    dirLight1.position.set(1, 1, 1.5).normalize()
    scene.add(dirLight1)

    val dirLight2 = new DirectionalLight(0x9999ff, 0.5)
    dirLight2.position.set(-1, -1, -1).normalize()
    scene.add(dirLight2)

    val ambLight = new AmbientLight(0x434343)
    scene.add(ambLight);

    scene.fog = new Fog(0x444444, 20, 75);

  }

}
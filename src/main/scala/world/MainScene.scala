package LinkedDataVerse.world

import scala.scalajs.js

import org.denigma.threejs._
import LinkedDataVerse.controls._
import LinkedDataVerse.scene.Container3D
import LinkedDataVerse.Tween

import org.scalajs.dom.raw.HTMLElement
import scala.util.Random

import org.scalajs.dom.html
import org.scalajs.dom
import dom.document

class MainScene(
  val container:HTMLElement,
  var width:Double,
  var height:Double,
  load: String => Unit) extends Container3D {

  override def distance = 15

  private def randPos() = {
    val dist = 40
    val half = dist / 2
    new Vector3(
      Random.nextInt(dist) - half,
      Random.nextInt(dist) - half,
      -half - Random.nextInt(dist))
  }

  def addAText(text: String, backColor: String, foreColor: String) = {
    val testText = TextPlane(text, backColor, foreColor)
    testText.position.copy(randPos())
    scene.add(testText)
    testText
  }

  def addAUrl(url: String, text: String, backColor: String, foreColor: String) = {
    val testText = TextPlane(text, backColor, foreColor)
    testText.position.copy(randPos())
    val td = testText.asInstanceOf[js.Dynamic]
    td._data = url;
    scene.add(testText)
    testText
  }

  def addImage(url: String) = {
    val img = ImgUrMesh(url)
    img.position.copy(randPos())
    scene.add(img)
    img
  }

  WorldHelper.addLights(scene);

  val boxGeom = new BoxGeometry(1, 1, 1)

  val plainMaterial = new MeshLambertMaterial(js.Dynamic.literal(
    color = new Color().setHex(0xBF8415)
  ).asInstanceOf[MeshLambertMaterialParameters])

  val meshes:Seq[Mesh] = Range(0, 6).map(i => {

    val mesh = new Mesh(boxGeom, plainMaterial)
    mesh.position.copy(randPos())
    scene.add(mesh)
    mesh

  })

  // Derp, scala.js is forcing me to used LineDashed...
  // https://github.com/antonkulaga/scala-js-facades/issues/2
  val lineMaterial = new LineDashedMaterial(js.Dynamic.literal(
    color = new Color().setHex(0xdddddd)
  ).asInstanceOf[LineDashedMaterialParameters])

  val lineGeo = new Geometry()
  meshes.drop(meshes.length / 2).foldLeft (meshes(0)) { (ac, el) =>
    lineGeo.vertices.push(el.position.clone())
    el
  }
  val lines = new Line(lineGeo, lineMaterial)
  lines.name = "lines"
  scene.add(lines);

  val img = ImgUrMesh("dAvWkN8.jpg")
  img.position.set(2, 2, -5)
  scene.add(img)

  tweenTo(img.position)

  val projector = new Projector()
  val raycaster = new Raycaster()

  def findIntersections(x:Double, y:Double) = {

    val vector = new Vector3(x, y, 1)
    projector.unprojectVector(vector, camera)

    raycaster
      .set(camera.position, vector.sub(camera.position).normalize())

    raycaster
      .intersectObjects(scene.children)
      .sortWith((a, b) => a.point.distanceTo(vector) < b.point.distanceTo(vector))
      .toList
  }

  def onCursorMove(clientX:Double, clientY:Double, width:Double, height:Double) = {

    val mouseX = ( clientX / width) * 2 - 1
    val mouseY = - ( clientY / height ) * 2 + 1

    val intersections = findIntersections(mouseX, mouseY)
    val underMouse = intersections.filter(i => i.`object`.name != "lines").groupBy(_.`object`).toMap

    underMouse

  }

  def tweenTo(pos: Vector3) = {

    val origCenter = controls.center.clone()

    val endPos = new Vector3()
      .subVectors(camera.position, pos)
      .normalize()
      .multiplyScalar(4)
      .add(pos);

    new Tween(camera.position)
      .to(endPos, 1000)
      .easing(Tween.Easing.Sinusoidal.InOut)
      .onUpdate((v:Double) => {
        camera.lookAt(pos)
        controls.reCenter(origCenter.lerp(pos, v))
      })
      .onComplete(() => {
        controls.scale = 1.0
        camera.position.copy(endPos)


      }:Unit)
      .start()
  }

  var selectedItem = new Object3D() // tmp, tracking clicked item.

  override def onEnterFrame() {

    super.onEnterFrame()

    meshes(1).rotation.y += 0.01;
    meshes(2).rotation.y += 0.015;

    val hits = onCursorMove(
      controls.lastMousePos._1,
      controls.lastMousePos._2,
      width,
      height);

    if (!hits.isEmpty && controls.clicked) {

      val ob = hits.head._1

      if (ob == selectedItem) {
        val sd = selectedItem.asInstanceOf[js.Dynamic]
        //println(sd._data)
        load(sd._data.asInstanceOf[String])
      } else {
        tweenTo(hits.head._1.position)
        selectedItem = hits.head._1
      }

    }

    controls.clicked = false

    Tween.update()

  }

  def onResize () = {
    width = dom.window.innerWidth
    camera.aspect = dom.window.innerWidth / height
    camera.updateProjectionMatrix()
    renderer.setSize(dom.window.innerWidth, height);
    width
  }
  dom.window.addEventListener("resize", (e:dom.Event) => onResize(), false)
  onResize()

}

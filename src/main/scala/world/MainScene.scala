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
  val initialUri: String,
  val container:HTMLElement,
  var width:Double,
  var height:Double,
  load: (String, Option[Object3D]) => Unit) extends Container3D {

  override def distance = 15

  private def randPos() = {
    val dist = 40
    val half = dist / 2
    new Vector3(
      Random.nextInt(dist) - half,
      Random.nextInt(dist) - half,
      -half - Random.nextInt(dist))
  }

  def localToWorld(ob: Object3D) = {
    var pos = new Vector3()
    pos.setFromMatrixPosition(ob.matrixWorld)
  }

def createTextBox(pos: Vector3, text: String, backColor: String, foreColor: String) = {
    val mesh = TextPlane(text, backColor, foreColor)
    mesh.position.copy(pos)
    mesh
  }

  def addTextBox(pos: Vector3, text: String, backColor: String, foreColor: String) = {
    val mesh = createTextBox(pos, text, backColor, foreColor)
    scene.add(mesh)
    mesh
  }

  def createAUri(pos: Vector3, url: String, text: String, backColor: String, foreColor: String) = {
    val uriText = TextPlane(text, backColor, foreColor)
    uriText.position.copy(pos)
    uriText.userData = js.Dynamic.literal(
      url = url
    )
    uriText
  }

  def addAUri(pos: Vector3, url: String, text: String, backColor: String, foreColor: String) = {
    val uriText = createAUri(pos, url, text, backColor, foreColor)
    scene.add(uriText)
    uriText
  }

  def createImage(pos: Vector3 = randPos(), url: String) = {
    val img = ImgUrMesh(url)
    img.position.copy(pos)
    img
  }

  def addImage(pos: Vector3 = randPos(), url: String) = {
    val img = createImage(pos, url)
    scene.add(img)
    img
  }

  def addABox(pos: Vector3 = randPos()) = {
    val mesh = new Mesh(boxGeom, plainMaterial)
    mesh.position.copy(pos)
    scene.add(mesh)
    mesh
  }

  def addASphere(pos: Vector3 = randPos(), useOffColor: Boolean = false) = {
    val mesh = new Mesh(sphereGeom, if (useOffColor) offMaterial else plainMaterial)
    mesh.position.copy(pos)
    scene.add(mesh)
    mesh
  }

  def createLine(a: Vector3, b: Vector3) = {

    // Derp, scala.js is forcing me to used LineDashed...
    // https://github.com/antonkulaga/scala-js-facades/issues/2
    val lineMaterial = new LineDashedMaterial(js.Dynamic.literal(
      lineWidth = 5, // not working through facade?
      color = new Color().setHex(0x6699dd)
    ).asInstanceOf[LineDashedMaterialParameters])

    val lineGeo = new Geometry()
    lineGeo.vertices.push(a.clone())
    lineGeo.vertices.push(b.clone())

    val lines = new Line(lineGeo, lineMaterial)
    lines.name = "lines"

    lines

  }

  def addLine(a: Vector3, b: Vector3) = {

    val lines = createLine(a, b)
    scene.add(lines);

    lines

  }

  def createLabel(point: Vector3, msg: String) = {
    val mesh = TextPlane(msg, "#000000", "#ffffff", 256, 45)
    mesh.position.copy(point)
    mesh
  }

  def addConnector(obj: String, predicate: String, head: Object3D, nodePos: Vector3) = {

    val headPos = localToWorld(head)
    val endPos = nodePos.clone()

    nodePos.add(new Vector3(0, 0, 5))

    head.add(createLine(headPos, nodePos))
    head.add(createLine(nodePos, endPos))

    val dir = nodePos.clone().sub(headPos)
    var len = dir.length()
    val mid = dir.normalize().multiplyScalar(len * 0.75)
    val fin = headPos.clone().add(mid)
    head.add(
      createLabel(endPos.clone().add(new Vector3(0, 0, 2.5)), predicate)
    )


  }

  WorldHelper.addLights(scene);

  val boxGeom = new BoxGeometry(1, 1, 1)
  val sphereGeom = new SphereGeometry(1, 10, 10)

  val plainMaterial = new MeshLambertMaterial(js.Dynamic.literal(
    color = new Color().setHex(0xBF8415)
  ).asInstanceOf[MeshLambertMaterialParameters])

  val offMaterial = new MeshLambertMaterial(js.Dynamic.literal(
    color = new Color().setHex(0x15BF84)
  ).asInstanceOf[MeshLambertMaterialParameters])


  val img = ImgUrMesh("http://www.w3.org/DesignIssues/diagrams/lod/597992118v2_350x350_Back.jpg")
  img.position.set(2, 2, -5)
  val imgd = img.asInstanceOf[js.Dynamic]
  //imgd.userData.url = "http://www.w3.org/People/Berners-Lee/card#i"
  imgd.userData.url = initialUri
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
      .intersectObjects(scene.children, true)
      .sortWith((a, b) => b.point.distanceTo(vector) < a.point.distanceTo(vector))
      .toList
  }

  def onCursorMove(clientX:Double, clientY:Double, width:Double, height:Double) = {

    val mouseX = ( clientX / width) * 2 - 1
    val mouseY = - ( clientY / height ) * 2 + 1

    val intersections = findIntersections(mouseX, mouseY)
    val underMouse = intersections.filter(i => {
      i.`object`.name != "lines" && i.`object`.visible == true
    }).groupBy(_.`object`).toMap

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

    val hits = onCursorMove(
      controls.lastMousePos._1,
      controls.lastMousePos._2,
      width,
      height);

    if (!hits.isEmpty && controls.clicked) {

      val ob = hits.head._1

      if (ob == selectedItem) {
        val sd = selectedItem.asInstanceOf[js.Dynamic]
        if (!js.isUndefined(sd.userData.url)) {
          load(sd.userData.url.asInstanceOf[String], Some(ob))
        } else {
          // Toggle the children
          ob.traverse((c: Object3D) => if (c != ob) {
            c.visible = !c.visible
          })
        }
      } else {
        var pos = new Vector3()
        pos.setFromMatrixPosition(ob.matrixWorld)
        tweenTo(pos)
        selectedItem = hits.head._1
      }

    }

    controls.clicked = false

    Tween.update()

  }

  def onResize () = {
    width = dom.window.innerWidth
    height = dom.window.innerHeight
    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setSize(width, height)
    (width, height)
  }
  dom.window.addEventListener("resize", (e:dom.Event) => onResize(), false)
  onResize()

}

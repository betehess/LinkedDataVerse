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

  var heads: List[Object3D] = List()

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

  def addUri(pos: Vector3, url: String, text: String, backColor: String, foreColor: String) = {
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

  def addBox(pos: Vector3 = randPos()) = {
    val mesh = new Mesh(boxGeom, plainMaterial)
    mesh.position.copy(pos)
    scene.add(mesh)
    mesh
  }

  def addSphere(pos: Vector3 = randPos(), useOffColor: Boolean = false) = {
    val mesh = new Mesh(sphereGeom, if (useOffColor) offMaterial else plainMaterial)
    mesh.position.copy(pos)
    scene.add(mesh)
    mesh
  }

  def createLine(a: Vector3, b: Vector3) = {
    // Derp, scala.js is forcing me to used LineDashed...
    // https://github.com/antonkulaga/scala-js-facades/issues/2
    val lineMaterial = new LineDashedMaterial(js.Dynamic.literal(
      linewidth = 3,
      opacity = 0.45,
      transparent = true,
      color = new Color().setHex(0x8899aa)
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
    scene.add(lines)
    lines
  }

  def createLabel(point: Vector3, msg: String) = {
    val mesh = TextPlane(msg, "#000000", "#ffffff", 290, 45)
    mesh.position.copy(point)
    mesh
  }

  def toggleNode(ob: Object3D) {
    ob.traverse((c: Object3D) => if (c != ob) {
      c.visible = !c.visible
    })
  }

  def colorObject(ob: Object3D, color: Int) {
    // Have to cast because material.color is val in facade.
    val d = ob.asInstanceOf[js.Dynamic]
    d.material.color = new org.denigma.threejs.Color().setHex(color)
  }

  def addConnector(obj: String, predicate: String, head: Object3D, nodePos: Vector3) = {

    val headPos = localToWorld(head)
    val endPos = nodePos.clone()

    nodePos.add(new Vector3(0, 0, 5))

    head.add(createLine(nodePos, endPos))
    head.add(createLine(nodePos, new Vector3(headPos.x, nodePos.y, nodePos.z)))

    val dir = nodePos.clone().sub(headPos)
    var len = dir.length()
    val mid = dir.normalize().multiplyScalar(len * 0.75)
    val fin = headPos.clone().add(mid)

    head.add(
      createLabel(endPos.clone().add(new Vector3(0, -0.3, 1.5)), predicate)
    )

  }

  WorldHelper.addLights(scene);

  val boxGeom = new BoxGeometry(1, 1, 1)
  val sphereGeom = new SphereGeometry(0.4, 20, 20)

  val plainMaterial = new MeshLambertMaterial(js.Dynamic.literal(
    color = new Color().setHex(0xBF8415)
  ).asInstanceOf[MeshLambertMaterialParameters])

  val offMaterial = new MeshLambertMaterial(js.Dynamic.literal(
    color = new Color().setHex(0xB4BFB4)
  ).asInstanceOf[MeshLambertMaterialParameters])

  val initObj = TextPlane(initialUri, "#eeeeee", "#333333", 200, 45)
  initObj.position.set(0, 0, 0)
  val initObjD = initObj.asInstanceOf[js.Dynamic]
  initObjD.userData.url = initialUri
  scene.add(initObj)

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

  var selectedItem = new Object3D() // tmp: tracking last clicked item.

  override def onEnterFrame() {

    super.onEnterFrame()

    val hits = onCursorMove(
      controls.lastMousePos._1,
      controls.lastMousePos._2,
      width,
      height);

    if (!hits.isEmpty && (controls.clicked || controls.rightClicked)) {

      val ob = hits.head._1
      if (ob == selectedItem) {
        val sd = selectedItem.asInstanceOf[js.Dynamic]
        if (!js.isUndefined(sd.userData.url)) {
          // Fond a URL - so load it.
          load(sd.userData.url.asInstanceOf[String], Some(ob))
        } else {
          // Toggle the children
          toggleNode(ob)
        }
      } else {
        if (!controls.rightClicked) {
          var pos = new Vector3().setFromMatrixPosition(ob.matrixWorld)
          tweenTo(pos)
        }
        selectedItem = hits.head._1
      }

    }

    controls.clicked = false
    controls.rightClicked = false

    if (controls.headPointer > 0 && controls.headPointer <= heads.size) {
      tweenTo(heads(controls.headPointer - 1).position)
      controls.headPointer = 0
    }

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

  // Let's dance
  camera.position.y = 6
  tweenTo(initObj.position)

}

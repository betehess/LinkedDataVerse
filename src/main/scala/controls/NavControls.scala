package LinkedDataVerse.controls

import org.denigma.threejs.{Vector2, Vector3, Camera}
import org.denigma.threejs.extensions.controls._
import org.scalajs.dom
import NavControls.HoverState
import org.scalajs.dom.raw.{Event, KeyboardEvent, MouseEvent, HTMLElement}

import scala.scalajs.js

object NavControls {

  class HoverState
  case object Zoom extends HoverState
  case object Rotate extends HoverState
  case object Pan extends HoverState
  case object Calm extends HoverState

}

class NavControls(camera:Camera, element:HTMLElement) extends CameraControls {

  var enabled = true
  var center:Vector3 = new Vector3()

  var userZoom = true
  var userZoomSpeed = 1.0

  var userRotate = true
  var userRotateSpeed = 1.0

  var userPan = true
  var userPanSpeed = 0.23

  var autoRotate = false
  var autoRotateSpeed = 2.0 // 30 seconds per round when fps is 60

  var minPolarAngle = 0 // radians
  var maxPolarAngle = Math.PI // radians

  var minDistance = 0
  var maxDistance =  Double.MaxValue //infinity?

  var headPointer = 0 //tmp: hack to tween to node

  object Keys {

    val LEFT = 37
    val UP = 38
    val RIGHT = 39
    val BOTTOM = 40
    val ROTATE = 65
    val ZOOM = 83
    val PAN = 68

    var left = false
    var right = false
    var up = false
    var down = false
    var in = false
    var out = false

  }

  protected var EPS = 0.000001
  protected var PIXELS_PER_ROUND = 1800

  protected var rotateStart = new Vector2()
  protected var rotateEnd = new Vector2()
  protected var rotateDelta = new Vector2()

  protected var zoomStart = new Vector2()
  protected var zoomEnd = new Vector2()
  protected var zoomDelta = new Vector2()

  protected var phiDelta:Double = 0
  protected var thetaDelta:Double = 0
  var scale: Double = 1.0

  var lastPosition = new Vector3()
  var lastMousePos = (0d, 0d)
  var mouseDownAt = 0l
  var clicked = false
  var rightClicked = false

  def autoRotationAngle(): Double =  2 * Math.PI / 60 / 60 * autoRotateSpeed

  def zoomScale(): Double = Math.pow( 0.95, userZoomSpeed )

  var state:HoverState = NavControls.Calm

  def rotateLeft( angle:Double = autoRotationAngle() ): Unit =   thetaDelta -= angle
  def rotateRight( angle:Double = autoRotationAngle() ): Unit =   thetaDelta += angle
  def rotateUp( angle:Double = autoRotationAngle() ): Unit =   phiDelta -= angle
  def rotateDown( angle:Double = autoRotationAngle() ): Unit =   phiDelta += angle
  def zoomIn(zScale:Double = this.zoomScale()): Unit = scale /= zScale
  def zoomOut(zScale:Double = this.zoomScale()): Unit = scale *= zScale

  def pan(distance:Vector3): Vector3 = {
    distance.transformDirection( this.camera.matrix )
    distance.multiplyScalar( userPanSpeed )
    camera.position.add( distance )
    this.center.add( distance )
  }

  def reCenter(newCenter: Vector3):Unit = {
    center.copy(newCenter)
    scale = 1.0
    phiDelta = 0
    thetaDelta = 0
  }

  def update() =  {

    var offset = camera.position.clone().sub( this.center )

    // angle from z-axis around y-axis

    var theta = Math.atan2( offset.x, offset.z )

    // angle from y-axis

    var phi = Math.atan2( Math.sqrt( offset.x * offset.x + offset.z * offset.z ), offset.y )

    if ( this.autoRotate ) {

      //this.rotateLeft( getAutoRotationAngle() )

    }

    theta = theta + thetaDelta
    phi = phi + phiDelta

    // restrict phi to be between desired limits
    phi = Math.max( this.minPolarAngle, Math.min( this.maxPolarAngle, phi ) )

    // restrict phi to be betwee EPS and PI-EPS
    phi = Math.max( EPS, Math.min( Math.PI - EPS, phi ) ) //TODO: change

    var radius = offset.length() * scale

    // restrict radius to be between desired limits
    radius = Math.max( this.minDistance, Math.min( this.maxDistance, radius ) )

    offset.x = radius * Math.sin( phi ) * Math.sin( theta )
    offset.y = radius * Math.cos( phi )
    offset.z = radius * Math.sin( phi ) * Math.cos( theta )

    camera.position.copy( this.center ).add( offset )
    camera.lookAt( this.center )

    thetaDelta = 0
    phiDelta = 0
    scale = 1

    if ( lastPosition.distanceTo( this.camera.position ) > 0 ) {

      lastPosition.copy( camera.position )

    }

    if (Keys.left == true) pan(new Vector3(-1, 0, 0))
    if (Keys.up == true) pan(new Vector3(0, 1, 0))
    if (Keys.right == true) pan(new Vector3(1, 0, 0))
    if (Keys.down == true) pan(new Vector3(0, -1, 0))
    if (Keys.in == true) pan(new Vector3(0, 0, -1)) //newzoomIn()
    if (Keys.out == true) pan(new Vector3(0, 0, 1)) //zoomOut()


  }


  def onMouseDown( event:MouseEvent ) = if(userRotate && enabled) {

    mouseDownAt = java.lang.System.currentTimeMillis()

    val e = event.asInstanceOf[js.Dynamic]
    val button = if (!js.isUndefined(e.which)) {
      e.which.asInstanceOf[Int] - 1
    } else {
      event.button
    }

    val finalButton = if (event.shiftKey) 2 else button

    //event.preventDefault()
    if(state == NavControls.Calm) {
      state = finalButton match
      {
        case 0=>NavControls.Rotate
        case 1=>NavControls.Zoom
        case 2=>NavControls.Pan
      }
    }

    state match {
      case NavControls.Rotate=>
        rotateStart.set(event.clientX,event.clientY)

      case NavControls.Zoom=>zoomStart.set(event.clientX,event.clientY)

      case NavControls.Pan=> //nothing

      case _=> //nothing

    }


  }

  def mouseRotate(clientX:Double,clientY:Double) = {

    rotateEnd.set( clientX, clientY )
    rotateDelta.subVectors( rotateEnd, rotateStart )

    val rLeft = 2 * Math.PI * rotateDelta.x / PIXELS_PER_ROUND * userRotateSpeed
    val rUp = 2 * Math.PI * rotateDelta.y / PIXELS_PER_ROUND * userRotateSpeed

    rotateLeft( rLeft)
    rotateUp( rUp )

    rotateStart.copy( rotateEnd )

  }

  def rotateOnMove(event:MouseEvent) = if(enabled && userRotate){
    state match {
      case NavControls.Rotate=>

        mouseRotate( event.clientX, event.clientY )

      case NavControls.Zoom=>

        zoomEnd.set( event.clientX, event.clientY )
        zoomDelta.subVectors( zoomEnd, zoomStart )

        if ( zoomDelta.y > 0 ) zoomIn()  else zoomOut()

        zoomStart.copy( zoomEnd )
      case NavControls.Pan=>
        val evd = event.asInstanceOf[js.Dynamic]
        val movementX:Double = evd.movementX.asInstanceOf[Double]
        val movementY:Double = evd.movementY.asInstanceOf[Double]
        this.pan(new Vector3(-movementX,movementY,0))
      //TODO: test how it works
      //var movementX = event.movementX || event.mozMovementX || event.webkitMovementX || 0;
      //var movementY = event.movementY || event.mozMovementY || event.webkitMovementY || 0;
      //this.pan( new Vector3( - movementX, movementY, 0 ) )
      case _=> //do nothing
    }
  }

  def onMouseMove(event:MouseEvent) = {
    rotateOnMove(event)
    lastMousePos = (event.clientX, event.clientY)
  }

  def onContextMenu(event:Event):js.Any = {
    event.preventDefault()
    false
  }

  def onMouseUp( event:MouseEvent ):Unit = if(enabled && userRotate){
    state = NavControls.Calm
    if (java.lang.System.currentTimeMillis() - mouseDownAt < 300) {
      val e = event.asInstanceOf[js.Dynamic]
      if (!js.isUndefined(e.which)) {
        if (e.which.asInstanceOf[Int] == 3) {
          rightClicked = true
        } else {
          clicked = true
        }
      } else if (!js.isUndefined(e.button)) {
        if (e.button.asInstanceOf[Int] == 2) {
          rightClicked = true
        } else {
          clicked = true
        }
      } else {
        clicked = true
      }
    }
  }

  def onKeyDown(e: KeyboardEvent): Unit = {
    e.keyCode match {
      case 65 => Keys.left = true //new Vector3(-1, 0, 0)
      case 87 => Keys.up = true //new Vector3(0, 1, 0)
      case 68 => Keys.right = true //new Vector3(1, 0, 0)
      case 83 => Keys.down = true //new Vector3(0, -1, 0)
      case 81 => Keys.in = true //new Vector3(0, -1, 0)
      case 69 => Keys.out = true //new Vector3(0, -1, 0)
      case _ => {}
    }
    //pan(vec)

    headPointer = e.keyCode match {
      case 49 => 1
      case 50 => 2
      case 51 => 3
      case _ => 0
    }

  }
  def onKeyUp(e: KeyboardEvent): Unit = {
    e.keyCode match {
      case 65 => Keys.left = false //new Vector3(-1, 0, 0)
      case 87 => Keys.up = false //new Vector3(0, 1, 0)
      case 68 => Keys.right = false //new Vector3(1, 0, 0)
      case 83 => Keys.down = false //new Vector3(0, -1, 0)
      case 81 => Keys.in = false //new Vector3(0, -1, 0)
      case 69 => Keys.out = false //new Vector3(0, -1, 0)
      case _ => {}
    }
  }

  def onMouseWheel(event:dom.MouseEvent) = if(enabled && userZoom) {

    event.preventDefault()

    var delta:Int = 0
    val wheel = event.asInstanceOf[js.Dynamic]
    if (!js.isUndefined(wheel.wheelDelta)){
      delta = wheel.wheelDelta.asInstanceOf[Int]
    }
    else if (!js.isUndefined(wheel.detail)) { // Firefox
      delta = - event.detail
    }

    if (delta != 0) {
      if (delta > 0) {
        zoomOut()
      } else {
        zoomIn()
      }
    }

  }

  def attach(el:HTMLElement) = {

    //js.Dynamic.global.TWEEN.Easing.Sinusoidal.InOut(1).asInstanceOf[Double]
    /*
      @JSName("TWEEN.Easing.Sinusoidal")extends js.Object
      object Sinusoidal {
        def InOut(x: Double): Double = js.naati
      }
    */

    el.addEventListener( "mousedown", this.onMouseDown _, false )
    el.addEventListener( "mousemove", this.onMouseMove _, false )
    el.addEventListener( "mouseup", this.onMouseUp _, false )
    /*el.addEventListener( "click", (e: MouseEvent) => {
      println("click")
    }, false )*/
    el.addEventListener( "mousewheel", this.onMouseWheel _, false)
    el.addEventListener( "DOMMouseScroll", this.onMouseWheel _, false ) // firefox
    el.addEventListener( "contextmenu", this.onContextMenu _, false )

    dom.document.addEventListener( "keydown", this.onKeyDown _, false )
    dom.document.addEventListener( "keyup", this.onKeyUp _, false )

  }

  this.attach(element)
}



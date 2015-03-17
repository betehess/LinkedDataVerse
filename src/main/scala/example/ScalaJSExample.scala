package example

import scala.scalajs.js

import js.annotation.JSExport
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

import org.w3.banana._, io._
import scala.concurrent.Future

import dom.document
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.html

import org.denigma.threejs._
import LinkedDataVerse.world._

import scala.scalajs.js.annotation._

class ScalaJSExample[Rdf <: RDF](implicit
  ops: RDFOps[Rdf],
  ldclient: LinkedDataClient[Rdf]
) extends js.JSApp {

  import ops._

  lazy val el:HTMLElement = dom.document.getElementById("board").asInstanceOf[HTMLElement]
  lazy val world = new MainScene("http://bertails.org/alex#me", el, 640, 480, load _)

  class Node(subject: RDF#Node, val allTriples: Iterable[Rdf#Triple], val pos: Vector3, isBNode: Boolean) {

    // tmp: Crappy grid layout
    val xgap = 4.6
    val ygap = 2.8

    val head = world.addSphere(pos, !isBNode)

    // tmp: Tracking heads for shortcut keys
    if (!isBNode) {
      world.heads = world.heads :+ head
    }

    val triples = allTriples.filter { case Triple(s, _, _) => s == subject }

    def add (scene: Scene): Object3D = {

      var xo = 0d
      var yo = 0d
      val columns = if (triples.size <= 12) 4 else 6
      val xo2 = - Math.min(columns, triples.size) - (xgap / 2)

      triples.foreach {

        case Triple(s, p, o) =>

          val nodePos = new Vector3(xo + xo2, yo, -10)
          val predicate = p.toString

          val (subject, mesh) = o.fold (
            { case URI(uriS) =>
              val linkMesh = world.createAUri(nodePos, uriS, uriS, "#268C3F", "#000000")
              (uriS, linkMesh)
            },
            { case bnode@BNode(label) =>
              val nextNode = new Node(bnode, allTriples, nodePos, true)
              nextNode.add(world.scene)
              val bnodeConnector = world.createLine(new Vector3(0, 0, 0), new Vector3(0, 0, -5))
              nextNode.head.add(bnodeConnector)

              (label, nextNode.head)
            },
            { case Literal(lexicalForm, URI(uriType), langOpt) =>

              val textBox = world.createTextBox(
                  new Vector3(xo + xo2, yo, -10),
                  lexicalForm.substring(0, 200),
                  "#253759",
                  "#fff00f")
              (lexicalForm, textBox)
            }
          )

        head.add(mesh)
        world.addConnector(subject, predicate, head, nodePos)

        // tmp: Crappy grid layout
        xo += xgap
        if (xo >= xgap * columns) {
          xo = 0
          yo += ygap
        }
      }

      // Connect to head
      val bottom = new Vector3(0, 0, -5)
      head.add(world.createLine(bottom, new Vector3(0, yo, -5)))
      head.add(world.createLine(bottom, new Vector3(0, 0, 0)))

      head
    }
  }

  var loaded: List[String] = List() // tmp: don't double load
  var worldPos = new Vector3(0, 0, -10) // tmp: move forward as loading

  def load(uri: String, start: Option[Object3D]):Unit = {

    if (!loaded.contains(uri)) {

      println("Loading:", uri)

      val newPos = start match {
        case None => worldPos
        case Some(ob) => world.localToWorld(ob)
      }

      val kb = KB.empty[Rdf]
      val kbRes = kb.point(URI(uri))

      kbRes map { res =>

        res match {

          case Image =>
            val img = world.createImage(newPos.clone().add(new Vector3(0, 0, 0)), uri)
            img.scale.set(3, 3, 3)
            start.map { o =>
              val parent = o.parent
              img.position.copy(o.position)
              parent.add(img)
              parent.remove(o)
            }.getOrElse(world.scene.add(img))

          case LDPointedGraph(pg) =>
            val triples = KB.cbd(pg)
            if (!triples.isEmpty) {

              // Move forward
              worldPos.copy(newPos).add(new Vector3(0, 0, -10))
              val node = new Node(URI(uri), triples, worldPos, false)
              node.add(world.scene)

              // Tween to new head
              world.tweenTo(node.head.position.clone().add(new Vector3(0, 0, 3)))

              start.map { o =>
                world.addLine(node.head.position, newPos)
                world.colorObject(o, 0xff88ff)
              }
            }

          case _ =>
            println("Unknown type")
            start.map { o =>
              world.colorObject(o, 0x0cc00c)
            }
        }
      }

      loaded ::= uri
    }
  }

  def reset(): Unit = {
    loaded = List()
    worldPos.z -= 15
    world.heads = List()
    val uri = dom.window.prompt("Base URI:", "http://www.w3.org/People/Berners-Lee/card#i")
    if (uri != null) {
      load(uri, None)
    }
  }

  def main(): Unit = {

    dom.window.addEventListener("keyup", (e:dom.KeyboardEvent) => {
      if (e.keyCode == 13) {
        reset()
      }
    }, false)

    world.render()

  }

}

import org.w3.banana.n3js._

object Implicits {

  implicit val turtleParser = new n3js.io.N3jsTurtleParser[N3js]

  implicit val jsonLdParser = new jsonldjs.io.JsonLdJsParser[N3js]

  implicit val ldclient = LinkedDataClient[N3js]

}

import Implicits._

object ScalaJSExample extends ScalaJSExample[N3js]

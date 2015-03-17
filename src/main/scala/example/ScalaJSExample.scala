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

class ScalaJSExample[Rdf <: RDF](implicit
  ops: RDFOps[Rdf],
  ldclient: LinkedDataClient[Rdf]
) extends js.JSApp {

  import ops._

  lazy val el:HTMLElement = dom.document.getElementById("board").asInstanceOf[HTMLElement]
  lazy val world = new MainScene("http://bertails.org/alex#me", el, 640, 480, load _)

  var worldPos = new Vector3(0, 0, -10)


  // TEMP: Just testing adding some things from the data.
  var loaded: List[String] = List()


  class Node(subject: RDF#Node, val triples: Iterable[Rdf#Triple], val pg: PointedGraph[Rdf], val pos: Vector3, val idx:Int = 0, isBNode: Boolean) {

    val head = world.addASphere(pos, !isBNode)
    if (isBNode) {
      //head.rotation.y = List(45, 25, 25, 45)(idx) * (Math.PI/180)
    } else {
      world.heads = world.heads :+ head
    }

    def add (scene: Scene): Object3D = {

      // Crappy grid layout
      val xgap = 4.6
      val ygap = 2.8

      val t = triples.filter { case Triple(s, _, _) => s == subject }

      val columns = if (t.size <= 12) 4 else 6

      var xo = 0d
      val colm = Math.min(columns, triples.size)
      val xo2 = -colm - (xgap / 2)
      var yo = 0d
      var boxesAdded = 0

      t.foreach {

        // Testing various types
        case Triple(s, p, o) =>

          val nodePos = new Vector3(xo + xo2, yo, -10)
          val col = (xo / xgap).toInt

          o.fold (
            { case URI(uriS) =>
              val linkMesh = world.createAUri(nodePos, uriS, uriS, "#268C3F", "#000000")
              head.add(linkMesh)
              world.addConnector(uriS, p.toString(), head, nodePos)
              boxesAdded += 1
              //println("URI:", uriS)
            },
            { case bnode@BNode(label) =>
              val node2 = new Node(bnode, triples, pg, nodePos, col, true)
              node2.add(world.scene)
              head.add(node2.head)

              // Line connectos
              world.addConnector(label, p.toString, head, nodePos)
              val bnodeConnector = world.createLine(new Vector3(0, 0, 0), new Vector3(0, 0, -5))

              node2.head.add(bnodeConnector)
              boxesAdded += 1
              //println("BNODE:", label)
            },
            { case Literal(lexicalForm, URI(uriType), langOpt) =>
              val predicate = p.toString()
              head.add(world.createTextBox(
                new Vector3(xo + xo2, yo, -10),
                lexicalForm.substring(0, 200), "#253759", "#ffffff"))

              world.addConnector(lexicalForm, p.toString, head, nodePos)
              boxesAdded += 1
              //println("Literal", lexicalForm)
            }
          )

        // Crappy grid layout
        xo += xgap
        if (xo >= xgap * columns) {
          xo = 0
          yo += ygap
        }
      }

      val rows = (yo / ygap).toInt
      val bot = new Vector3(0, 0, -5)
      val top = new Vector3(0, rows * ygap, -5)
      head.add(world.createLine(bot, top))

      if (rows > 0) {
        head.add(world.createLine(bot, new Vector3(0, 0, 0)))
      }

      head
    }
  }

  def load(uri: String, start: Option[Object3D]):Unit = {

    println("Loading:", uri)

    if (!loaded.contains(uri)) {

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
            img.scale.set(2, 2, 2)
            start.map { o =>
              val parent = o.parent
              img.position.copy(o.position)
              parent.add(img)
              parent.remove(o)
            }.getOrElse(world.scene.add(img))

          case LDPointedGraph(pg) =>
            val triples = KB.cbd(pg)
            if (!triples.isEmpty) {

              worldPos.copy(newPos).add(new Vector3(0, 0, -10))
              val node = new Node(URI(uri), triples, pg, worldPos, 0, false)
              node.add(world.scene)
              val focusPoint = node.head.position.clone().add(new Vector3(0, 0, 3))
              world.tweenTo(focusPoint)

              start.map { o =>
                world.addLine(node.head.position, newPos)
                // Have to cast because material.color is val in facade.
                val d = o.asInstanceOf[js.Dynamic]
                d.material.color = new org.denigma.threejs.Color().setHex(0xff88ff)
              }
            }

          case _ =>
            println("Unknown type")
            start.map { o =>
              // Have to cast because material.color is val in facade.
              val d = o.asInstanceOf[js.Dynamic]
              d.material.color = new org.denigma.threejs.Color().setHex(0x0cc00c)
            }
        }
      }

      loaded ::= uri
    }
  }

  def repoint (uri: String, pg: PointedGraph[Rdf]):Unit = {

  }

  def main(): Unit = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = "<strong>It works!</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)

    world.render()

    //load("http://www.w3.org/People/Berners-Lee/card#i")

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

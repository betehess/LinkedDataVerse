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


  class Node(val triples: Iterable[Rdf#Triple], val pg: PointedGraph[Rdf], val pos: Vector3, isBNode: Boolean) {

    println("Adding node with " + triples.size +" triples")

    val head = world.addASphere(pos, isBNode)
    head.scale.set(0.75, 0.75, 0.75)

    def add (scene: Scene): Object3D = {

      // Crappy grid layout
      val xgap = 4.6
      val ygap = 2.8
      val columns = 4

      var xo = 0d
      val colm = Math.min(columns, triples.size)
      val xo2 = -colm - (xgap / 2)
      var yo = 0d
      var boxesAdded = 0

      triples.foreach {
        // Testing various types
        case Triple(s, p, o) =>
          o.fold (
            { case URI(uriS) =>

              val nodePos = new Vector3(xo + xo2, yo, -10)
              head.add(world.createAUri(nodePos, uriS, uriS, "#268C3F", "#000000"))
              world.addConnector(uriS, p.toString(), head, nodePos)

              // Crappy grid layout
              xo += xgap
              if (xo >= xgap * columns) {
                xo = 0
                yo += ygap
              }
              println("URI:", uriS)
              uriS
            },
            { case bnode@BNode(label) =>
              val t = triples.filter { case Triple(s, _, _) => s == bnode }
              //worldPos.z -= 9

              println("BNODE:", label)
              val node2 = new Node(t, pg, new Vector3(xo + xo2, yo, -10), true)
              node2.add(world.scene)
              head.add(node2.head)

              world.addConnector(label, p.toString, head, new Vector3(xo + xo2, yo, -10))


              xo += xgap
              if (xo >= xgap * columns) {
                xo = 0
                yo += ygap
              }
            },
            { case Literal(lexicalForm, URI(uriType), langOpt) =>

                val predicate = p.toString()
                //val shortForm = if (predicate.contains("#")) predicate.split("#").last + ": " else predicate.split("/").last + ": "
                head.add(world.createTextBox(
                  new Vector3(xo + xo2, yo, -10),
                  lexicalForm.substring(0, 200), "#253759", "#ffffff"))

                world.addConnector(lexicalForm, p.toString, head, new Vector3(xo + xo2, yo, -10))

                // Crappy grid layout, again
                xo += xgap
                if (xo >= xgap * columns) {
                  xo = 0
                  yo += ygap
                }
                println("Literal", lexicalForm)
            }
          )
      }

      head
    }
  }

  def load(uri: String, start: Option[Object3D]):Unit = {

    println("Loading:", uri, start.isEmpty)

    if (!loaded.contains(uri)) {

      val newPos = start match {
        case None => worldPos
        case Some(ob) => world.localToWorld(ob)
      }

      val kb = KB.empty[Rdf]
      val kbRes = kb.point(URI(uri))

      kbRes map { res =>
        res match {

          case Image => world.addImage(newPos.clone().add(new Vector3(0, 0, 1)), uri)

          case LDPointedGraph(pg) =>
            val triples = KB.cbd(pg)
            if (!triples.isEmpty) {

              worldPos.copy(newPos).add(new Vector3(0, 0, -10))
              val node = new Node(triples, pg, worldPos, false)
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

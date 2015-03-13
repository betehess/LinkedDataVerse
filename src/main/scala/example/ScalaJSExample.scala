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
  lazy val world = new MainScene(el, 640, 480, load _)

  var worldPos = new Vector3(0, 0, -10)

  // TEMP: Just testing adding some things from the data.
  var loaded: List[String] = List()

  // how to deconstruct a node
  def printNode(node: Rdf#Node): String = node.fold(
    { case URI(uriS) => {
      if (!loaded.contains(uriS)) {
        world.addAText(uriS, "#268C3F", "#000000")
        loaded ::= uriS
      }
      uriS
    }},
    { case BNode(label) => "_:" + label },
    { case Literal(lexicalForm, URI(uriType), langOpt) => {
      langOpt match {
        case Some("en") => {
          world.addAText(lexicalForm.substring(0, 200), "#253759", "#ffffff")
          lexicalForm
        }
        case _ => ""
      }
      //lexicalForm + langOpt.map(l => " <- lang:"+l).getOrElse("")
    }}
  )

  def addTripleMesh(pos: Vector3, triples: Iterable[Rdf#Triple]): Unit = {
    var xo = 0d
    var yo = 0d
    val xgap = 3.6
    val ygap = 1.8
    val columns = 4
    var boxesAdded = 0

    triples.foreach {
        // Testing various types
        case Triple(s, "http://www.w3.org/2002/07/owl#sameAs", o) => {}
        //case Triple(s, "http://dbpedia.org/ontology/thumbnail", o) => {
        //  world.addImage("http://commons.wikimedia.org/wiki/Special:FilePath/White_Wine_Glas.jpg")//o.toString())
        //}
        case Triple(s, p, o) => {
          //println(p)
          o.fold (
            { case URI(uriS) => {
              //if (!loaded.contains(uriS)) {

                world.addAUrl(
                  new Vector3(pos.x + xo, pos.y + yo, pos.z),
                  uriS,
                  p.toString + " " + uriS,
                  "#268C3F", "#000000")

                xo += xgap
                if (xo >= xgap * columns) {
                  xo = 0
                  yo += ygap
                }
                //loaded ::= uriS
              //}
              uriS
            }},
            { case BNode(label) => println("_:" + label); "_:" + label },
            { case Literal(lexicalForm, URI(uriType), langOpt) => {
              langOpt match {
                case Some("en") => {
                  val predicate = p.toString()
                  val deets = if (predicate.contains("#")) predicate.split("#").last + ": " else predicate.split("/").last + ": "
                  world.addAText(deets + lexicalForm.substring(0, 200), "#253759", "#ffffff")
                  lexicalForm
                }
                case _ => ""
              }
            }}
          )

        }
    }
  }

  def load(uri: String):Unit = {

    println("LOADING!", uri)

    if (!loaded.contains(uri)) {

      val kb = KB.empty[Rdf]
      val kbRes = kb.point(URI(uri))

      kbRes map { res =>
        res match {
          case Image => {
            val pos = world.camera.position.clone().add(new Vector3(0, 0, -3))//, vector.sub(camera.position).normalize()
            world.addImage(pos, uri)
          }

          case LDPointedGraph(pg) => {
            val triples = KB.cbd(pg)
            if (!triples.isEmpty) {
              worldPos.z = worldPos.z - 15.0
              addTripleMesh(worldPos, triples)
              world.tweenTo(worldPos.add(new Vector3(0, 0, 5)))
            }
          }

          case _ => println("Unknown type")
        }
      }

      /*for {
        LDPointedGraph(pg) <- kbRes
      } {
        //KB.cbd(pg).foreach(println)
        val triples = KB.cbd(pg)
        if (!triples.isEmpty) {
          worldPos.z = worldPos.z - 15.0
          println(worldPos.x, worldPos.y, worldPos.z)

          addTripleMesh(worldPos, triples)
          world.tweenTo(worldPos.add(new Vector3(0, 0, 5)))
        }
      }*/
      loaded ::= uri
    }
  }

  def main(): Unit = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = "<strong>It works!</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)

    world.render()

    load("http://www.w3.org/People/Berners-Lee/card#i")

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

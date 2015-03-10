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

import LinkedDataVerse.world._

object LinkedDataClient {

  implicit def apply[Rdf <: RDF](implicit
    ops: RDFOps[Rdf],
    n3Parser: n3js.io.N3jsTurtleParser[Rdf],
    jsonLdParser: jsonldjs.io.JsonLdJsParser[Rdf]
  ): LinkedDataClient[Rdf] = new LinkedDataClient[Rdf]

  /** to be used for content negotiation with Linked Data */
  val headers = Map(
    "Accept" -> "text/turtle,application/ld+json;q=0.7,application/json;q=0.7,application/xhtml+xml;q=0.6,application/xml;q=0.6,*/*;q=0.5"
  )

}

sealed trait LDResult[+Rdf <: RDF]
case class LDGraph[Rdf <: RDF](graph: Rdf#Graph) extends LDResult[Rdf]
case object Image                                extends LDResult[Nothing]
case object Unknown                              extends LDResult[Nothing]

class LinkedDataClient[Rdf <: RDF](implicit
  ops: RDFOps[Rdf],
  n3Parser: RDFReader[Rdf, Future, Turtle],
  jsonLdParser: RDFReader[Rdf, Future, JsonLd]
) {

  def get(url: String): Future[LDResult[Rdf]] = {
    Ajax.get(url, headers = LinkedDataClient.headers).flatMap { xhr =>
      def input = new java.io.StringReader(xhr.responseText)
      val contentType = xhr.getResponseHeader("Content-Type").split(";").head
      contentType match {
        case "text/turtle" =>
          n3Parser.read(input, url).map(graph => LDGraph(graph))
        case "application/ld+json" | "application/json" =>
          jsonLdParser.read(input, url).map(graph => LDGraph(graph))
        case "image/gif" | "image/jpeg" | "image/pjpeg" | "image/png" | "image/svg+xml" | "image/tiff" =>
          Future.successful(Image)
        case _ => {
          println("Unhandled content type:", contentType)
          Future.successful(Unknown)
        }
      }
    }
  }

}



class ScalaJSExample[Rdf <: RDF](implicit
  ops: RDFOps[Rdf],
  ldclient: LinkedDataClient[Rdf]
) extends js.JSApp {

  import ops._

  lazy val el:HTMLElement = dom.document.getElementById("board").asInstanceOf[HTMLElement]
  lazy val world = new MainScene(el, 640, 480)

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

  def addTripleMesh(triples: Iterable[Rdf#Triple]): Unit = {
    triples.foreach {
        // Testing various types
        case Triple(s, "http://www.w3.org/2002/07/owl#sameAs", o) => {}
        case Triple(s, "http://dbpedia.org/property/hasPhotoCollection", o) => {
          println("photos: ", o)
        }
        case Triple(s, "http://dbpedia.org/ontology/thumbnail", o) => {
          println("adding one.")
          world.addImage("http://commons.wikimedia.org/wiki/Special:FilePath/White_Wine_Glas.jpg")//o.toString())
        }
        case Triple(s, p, o) => {
          o.fold (
            { case URI(uriS) => {
              if (!loaded.contains(uriS)) {
                world.addAText(p.toString + " " + uriS, "#268C3F", "#000000")
                println(p, o)
                loaded ::= uriS
              }
              uriS
            }},
            { case BNode(label) => "_:" + label },
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

  def main(): Unit = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = "<strong>It works!</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)

    world.render()

    val f = ldclient.get("http://dbpedia.org/resource/Wine")
    f.onSuccess {
      case LDGraph(graph) => {
        addTripleMesh(graph.triples)
        /*graph.triples.foreach { case Triple(s, p, o) => {
          if (p!= "http://www.w3.org/2002/07/owl#sameAs") println(printNode(o))
        } }*/
      }
    }
    f.onFailure { case e: Exception =>
      e.printStackTrace
    }

  }


}

import org.w3.banana.n3js._

object Implicits {

  implicit val n3Parser = new n3js.io.N3jsTurtleParser[N3js]

  implicit val jsonLdParser = new jsonldjs.io.JsonLdJsParser[N3js]

  implicit val ldclient = LinkedDataClient[N3js]

}

import Implicits._

object ScalaJSExample extends ScalaJSExample[N3js]

package example

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

import org.w3.banana._, io._
import scala.concurrent.Future
import org.scalajs.dom.ext._

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
      }
    }
  }

}

object ScalaJSExample extends js.JSApp {

  import org.w3.banana.n3js._

  implicit val n3Parser = new n3js.io.N3jsTurtleParser[N3js]

  implicit val jsonLdParser = new jsonldjs.io.JsonLdJsParser[N3js]

  val ldclient = LinkedDataClient[N3js]

  def main(): Unit = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = "<strong>It works!</strong>"
    dom.document.getElementById("playground").appendChild(paragraph)

    val f = ldclient.get("http://dbpedia.org/resource/Wine")
    f.onSuccess { case LDGraph(graph) =>
      graph.triples.foreach(println)
    }
    f.onFailure { case e: Exception =>
      e.printStackTrace
    }


  }


}

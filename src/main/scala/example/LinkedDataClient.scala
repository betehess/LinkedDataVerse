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

class LinkedDataClient[Rdf <: RDF](implicit
  ops: RDFOps[Rdf],
  turtleParser: RDFReader[Rdf, Future, Turtle],
  jsonLdParser: RDFReader[Rdf, Future, JsonLd]
) {

  import ops._

  def get(url: String): Future[LDResult[Rdf]] = {
    Ajax.get(url, headers = LinkedDataClient.headers).flatMap { xhr =>
      def input = new java.io.StringReader(xhr.responseText)
      val contentType = xhr.getResponseHeader("Content-Type").split(";").head
      contentType match {
        case "text/turtle" =>
          turtleParser.read(input, url).map(graph => LDPointer(PointedGraph(URI(url), graph)))
        case "application/ld+json" | "application/json" =>
          jsonLdParser.read(input, url).map(graph => LDPointer(PointedGraph(URI(url), graph)))
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

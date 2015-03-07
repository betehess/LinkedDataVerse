package example

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

import org.w3.banana._
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

class LinkedDataClient[Rdf <: RDF](implicit
  ops: RDFOps[Rdf],
  n3Parser: n3js.io.N3jsTurtleParser[Rdf],
  jsonLdParser: jsonldjs.io.JsonLdJsParser[Rdf]
) {

  def get(url: String): Future[Rdf#Graph] = {
    Ajax.get(url, headers = LinkedDataClient.headers).flatMap { xhr =>
      val body = xhr.responseText
      val input = new java.io.StringReader(body)
//      println(body)
      val contentType = xhr.getResponseHeader("Content-Type").split(";").head
      contentType match {
        case "text/turtle"                              => n3Parser.read(input, url)
        case "application/ld+json" | "application/json" => jsonLdParser.read(input, url)
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

    val turtle = new java.io.StringReader("""
@prefix c: <http://example.org/cartoons#>.
c:Tom a c:Cat. 
c:Jerry a c:Mouse;
        c:smarterThan c:Tom.
""")

    n3Parser.read(turtle, "http://example.com").foreach { graph =>
      graph.triples.foreach(println)
    }

    val jsonLd = new java.io.StringReader("""{
  "http://schema.org/name": "Manu Sporny",
  "http://schema.org/url": {"@id": "http://manu.sporny.org/"},
  "http://schema.org/image": {"@id": "http://manu.sporny.org/images/manu.png"}
}""")

    jsonLdParser.read(jsonLd, "http://example.com").foreach { graph =>
      graph.triples.foreach(println)
    }

    val f = ldclient.get("http://dbpedia.org/resource/Wine")
    f.onSuccess { case graph =>
      graph.triples.foreach(println)
    }
    f.onFailure { case e: Exception =>
      e.printStackTrace
    }


  }


}

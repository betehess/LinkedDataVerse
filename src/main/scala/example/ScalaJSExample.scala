package example

import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

object ScalaJSExample extends js.JSApp {

  import org.w3.banana._, plantain._

  val n3Parser = new n3js.io.N3jsTurtleParser[Plantain]

  val jsonLdParser = new jsonldjs.io.JsonLdJsParser[Plantain]

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


  }


}

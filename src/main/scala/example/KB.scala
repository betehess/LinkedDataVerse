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

object KB {

  def empty[Rdf <: RDF]: KB[Rdf] = KB(Map.empty)

}

case class KB[Rdf <: RDF](
  resources: Map[String, CachedResult[Rdf]]
) extends AnyVal {

  def point(url: Rdf#URI)(implicit ops: RDFOps[Rdf], ldclient: LinkedDataClient[Rdf]): Future[(LDResult[Rdf], KB[Rdf])] = {
    import ops._
    val URI(urlS) = url
    // we only call hashless URLs
    val hashlessUrl = urlS.replaceAll("#[^#]*$", "")
    resources.get(hashlessUrl) match {
      case None =>
        ldclient.get(hashlessUrl).map {
          case ldgraph @ LDGraph(graph)  =>
            val ldresult = LDPointedGraph(PointedGraph(url, graph))
            val newKB = KB(resources + (hashlessUrl -> ldgraph))
            (ldresult, newKB)

          case Image =>
            val newKB = KB(resources + (hashlessUrl -> Image))
            (Image, newKB)

          case Unknown =>
            val newKB = KB(resources + (hashlessUrl -> Unknown))
            (Unknown, newKB)
        }

      case Some(LDGraph(graph)) =>
        val ldresult = LDPointedGraph(PointedGraph(url, graph))
        Future.successful((ldresult, this))

      case Some(Image) =>
        Future.successful((Image, this))

      case Some(Unknown) =>
        Future.successful((Unknown, this))

    }
  }

  def unfoldForward(pg: PointedGraph[Rdf])(implicit ops: RDFOps[Rdf]): Iterable[Rdf#Triple] = {
    import ops._
    val PointedGraph(pointer, graph) = pg
    // returns the bnodes in the object position
    def getBNodes(it: Iterable[Rdf#Triple]): Iterable[Rdf#BNode] = it.flatMap { case Triple(_, _, o) =>
      o.fold(
        uri   => None,
        bnode => Some(bnode),
        lit   => None
      )
    }
    // visits all the bnodes that can be found
    @annotation.tailrec
    def loop(toVisit: List[Rdf#BNode], seen: Set[Rdf#BNode], acc: Vector[Rdf#Triple]): Vector[Rdf#Triple] = toVisit match {
      case Nil => acc
      case bnode :: bnodes =>
        val triples = ops.find(graph, bnode, ANY, ANY).toList
        val newBNodes = getBNodes(triples).toList
        loop(bnodes ++ newBNodes, seen ++ newBNodes, acc ++ triples)
    }
    val immediateTriples = ops.find(graph, pointer, ANY, ANY).toVector
    val initialBNodes = getBNodes(immediateTriples)
    loop(initialBNodes.toList, initialBNodes.toSet, immediateTriples)
  }


}

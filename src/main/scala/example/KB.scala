package example

import org.w3.banana._
import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

object KB {

  /** An empty Knowledge Base */
  def empty[Rdf <: RDF]: KB[Rdf] = new KB(Map.empty)

  /** @returns the [Concise Bounded Description](http://www.w3.org/Submission/CBD/) of the pointer in the given pointed graph
    * 
    * In layman's terms, it returns all the triples having the pointer
    * in subject position, and "follows the blank nodes"
    */
  def cbd[Rdf <: RDF](pg: PointedGraph[Rdf])(implicit ops: RDFOps[Rdf]): Iterable[Rdf#Triple] = {
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

/** A Knowledge Base that knows how to point to node on the Web
  * 
  * This implementation caches the results.
  */
class KB[Rdf <: RDF](
  private var resources: Map[String, CachedResult[Rdf]]
) {

  def get(hashlessUrl: Rdf#URI)(implicit ops: RDFOps[Rdf], ldclient: LinkedDataClient[Rdf]): Future[CachedResult[Rdf]] = {
    import ops._
    val URI(urlS) = hashlessUrl
    resources.get(urlS) match {
      case None =>
        ldclient.get(urlS).map { result =>
          resources += (urlS -> result)
          result
        }

      case Some(result) =>
        Future.successful(result)
    }

  }

  /** points to the given node on the Web */
  def point(url: Rdf#URI)(implicit ops: RDFOps[Rdf], ldclient: LinkedDataClient[Rdf]): Future[LDResult[Rdf]] = {
    import ops._
    val hashlessUrl = {
      val URI(urlS) = url
      URI(urlS.replaceAll("#[^#]*$", ""))
    }
    this.get(hashlessUrl).map {
      case LDGraph(graph) => LDPointedGraph(PointedGraph(url, graph))
      case Image          => Image
      case Unknown        => Unknown
    }

  }

}


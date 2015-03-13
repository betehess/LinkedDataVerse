package example

import org.w3.banana._

sealed trait CachedResult[+Rdf <: RDF]

case class LDGraph[Rdf <: RDF](graph: Rdf#Graph) extends CachedResult[Rdf]

sealed trait LDResult[+Rdf <: RDF]

case class LDPointedGraph[Rdf <: RDF](pg: PointedGraph[Rdf]) extends LDResult[Rdf]

case object Image extends LDResult[Nothing] with CachedResult[Nothing]

case object Unknown extends LDResult[Nothing] with CachedResult[Nothing]

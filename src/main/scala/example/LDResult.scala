package example

import org.w3.banana._

sealed trait LDResult[+Rdf <: RDF]
case class LDPointer[Rdf <: RDF](pg: PointedGraph[Rdf]) extends LDResult[Rdf]
case object Image                                       extends LDResult[Nothing]
case object Unknown                                     extends LDResult[Nothing]

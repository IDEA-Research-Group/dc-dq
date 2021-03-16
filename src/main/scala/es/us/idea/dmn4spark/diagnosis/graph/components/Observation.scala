package es.us.idea.dmn4spark.diagnosis.graph.components
import java.util.UUID.randomUUID

import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{AndVertex, Vertex}

class Observation(id: String) extends AndVertex {
  def id(): String = id

  override def toString: String = s"Observation@$id[]"

}

object Observation{

  def apply(id: String): Observation = new Observation(id)
  def apply(): Observation = new Observation(randomUUID().toString)
  def apply(brdvs: List[BRDV]): Observation = new Observation(
    s"Observation[${brdvs.sorted.mkString(":")}]".hashCode.toHexString
  )

}

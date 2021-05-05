package es.us.idea.dmn4spark.diagnosis.graph.structure

case class DMN4DQStructure(dimensionToBrdv: Map[String, Set[String]], brdvToAttributes: Map[String, Set[String]])

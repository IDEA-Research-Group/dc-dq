package es.us.idea.dmn4spark.diagnosis.graph

import es.us.idea.dmn4spark.analysis.{DMNAnalysisHelpers}
import es.us.idea.dmn4spark.analysis.extended.ExtendedDecisionDiagram
import es.us.idea.dmn4spark.diagnosis.graph.components.{Assessment, BRDV, DimensionMeasurement, Measurement, Observation}
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{DirectedEdge, Vertex}

class DMN4DQTree(vertices: List[Vertex], edges: List[DirectedEdge]) extends Tree(vertices, edges) {

  def M(assessmentValue: String): List[Measurement] = {
    edges.filter(edge => {
      edge.source() match {
        case assessment: Assessment => assessment.value() == assessmentValue
        case _ => false
      }
    }).map(edge => {
      edge.target() match {
        case measurement: Measurement => Some(measurement)
        case _ => None
      }
    }).filter(_.isDefined).map(_.get)
  }

  def D(dimensionName: String): List[DimensionMeasurement] = ???
  def O(dimensionName: String, measuredValue: String) = ???

  def pruneFromDimensionMeasurement(): DMN4DQTree = {
    import DMN4DQTree.implicits._

    val toPrune = vertices.filter(v => v match {
      case dimensionMeasurement: DimensionMeasurement => true
      case _ => false
    }).distinct

    pruneDescendants(toPrune)
  }

  override def toString: String = {
    val assessment = vertices().filter(_.isInstanceOf[Assessment])
    val dimensionMeasurements = vertices().filter(_.isInstanceOf[DimensionMeasurement])
    val brdvs = vertices().filter(_.isInstanceOf[BRDV])
    s"Assessment: $assessment\nDimensionMeasurements: $dimensionMeasurements\nBRDVS: $brdvs"

  }

}

object DMN4DQTree{

  def apply(vertices: List[Vertex], edges: List[DirectedEdge]): DMN4DQTree = new DMN4DQTree(vertices, edges)

  def apply(extendedDecisionDiagram: ExtendedDecisionDiagram): DMN4DQTree = {
    import DMNAnalysisHelpers._

    var vertices: Set[Vertex] = Set()
    var edges: Set[DirectedEdge] = Set()

    val dqa = extendedDecisionDiagram.getDQA() // Get ExtendedRules for DQA
    val dqm = extendedDecisionDiagram.getDQM() // Get ExtendedRules for DQM

    // Group by assessment output (assessmentName => measurementCandidates)
    val assessmentValuesAndRules = dqa.map(er => (er.outputs().find(_.name == "DQA")
      .getOrElse(throw new IllegalArgumentException("DQA outout expected in Assessment table")).value, er.conditions()))
      .groupBy(_._1)
    // Group by measurement value (dimension + value) => observationCandidates
    val dqmByMeasurementOutput = dqm.flatMap(er => er.outputs().map(o => ((o.name.clean(), o.value.clean()), er.conditions())))
      .groupBy(_._1)

    assessmentValuesAndRules.foreach(assessmentCandidate => {
      val assessmentValue = assessmentCandidate._1.clean()
      val measurementCandidates = assessmentCandidate._2
      val assessment = Assessment(assessmentValue) // Instantiate vertex

      vertices = vertices + assessment // Add assessment to the list of vertices

      measurementCandidates.foreach(measurementCandidate => { // Measurement candidate = each row of the DQA table

        val dimensionMeasurementCandidates = measurementCandidate._2 // Contains <dimension, value>

        val dimensionMeasurements = dimensionMeasurementCandidates.map(dimensionMeasurementCandidate => {
          val dimensionName = dimensionMeasurementCandidate.name.clean()
          val measuredValue = dimensionMeasurementCandidate.value.clean()
          val dimensionMeasurement = DimensionMeasurement(dimensionName, measuredValue)
          vertices = vertices + dimensionMeasurement
          dimensionMeasurement
        })

        val measurement = Measurement(dimensionMeasurements)
        vertices = vertices + measurement
        edges = edges + DirectedEdge(assessment, measurement)

        dimensionMeasurements.foreach(dimensionMeasurement => {
          edges = edges + DirectedEdge(measurement, dimensionMeasurement)
          // now, take the observationcandidates from dqmByMeasurementOutput (grouped by Value = grouped by
          // dimension measurement
          val observationCandidates = dqmByMeasurementOutput.getOrElse((dimensionMeasurement.dimensionName(), dimensionMeasurement.measuredValue()), List())
          observationCandidates.foreach(observationCandidate => {

            val brdvCandidates = observationCandidate._2 // obtenemos las brdvs
            val brdvs: List[BRDV] = brdvCandidates.map(brdvCandidate => {
              val name = brdvCandidate.name.clean()
              val value = brdvCandidate.value.clean()
              val brdv = BRDV(name, value)
              vertices = vertices + brdv
              brdv
            })

            val observation = Observation(brdvs)
            vertices = vertices + observation
            edges = edges + DirectedEdge(dimensionMeasurement, observation)
            // add edge between brdv and observation. brdvs were previously added to vertices
            brdvs.foreach(brdv => edges = edges + DirectedEdge(observation, brdv))

          })
        })
      })
    })

    new DMN4DQTree(vertices.toList, edges.toList)
  }

  object implicits {
    implicit def tree2dmn4dqTree(tree: Tree): DMN4DQTree = DMN4DQTree(tree.vertices(), tree.edges())
    implicit def trees2dmn4dqTrees(trees: List[Tree]): List[DMN4DQTree] = trees.map(tree2dmn4dqTree)
  }


}
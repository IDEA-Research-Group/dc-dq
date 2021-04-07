package es.us.idea.dmn4spark.diagnosis

import es.us.idea.dmn4spark.diagnosis.Diagnosis.calculateDeviation
import es.us.idea.dmn4spark.diagnosis.graph.{DMN4DQTree, Tree}
import es.us.idea.dmn4spark.diagnosis.graph.components.{Assessment, BRDV, DimensionMeasurement, Measurement}

class Diagnosis(targetBranch: DMN4DQTree, observedBranch: DMN4DQTree, assessmentDiagnosis: (String, String),
                dimensionMeasurementDiagnosis: Map[String, (String, String)],
                brdvDiagnosis: Map[String, (String, String)]) {

  def targetBranch(): DMN4DQTree = targetBranch
  def observedBranch(): DMN4DQTree = observedBranch
  def assessmentDiagnosis(): (String, String) = assessmentDiagnosis
  def dimensionMeasurementDiagnosis(): Map[String, (String, String)] = dimensionMeasurementDiagnosis
  def brdvDiagnosis(): Map[String, (String, String)] = brdvDiagnosis

  def deviation(): Int = calculateDeviation(brdvDiagnosis)
  def measurementDeviation(): Int = calculateDeviation(dimensionMeasurementDiagnosis)
  def assessmentDeviation(): Int = if(assessmentDiagnosis._1 != assessmentDiagnosis._2) 1 else 0

}

object Diagnosis {
  // Requirements:
  // 1. Only one Assessment
  def apply(targetBranch: DMN4DQTree, observedBranch: DMN4DQTree): Diagnosis = {
    val targetAssessment = targetBranch.getRoots().head.asInstanceOf[Assessment]
    val observedAssessment = observedBranch.getRoots().head.asInstanceOf[Assessment]

    val assessmentDiagnosis = (targetAssessment.value(), observedAssessment.value())

    val targetDimensionMeasurements = targetAssessment.getChildren(targetBranch).flatMap(_.getChildren(targetBranch)).map(_.asInstanceOf[DimensionMeasurement])
    val observedDimensionMeasurements = observedAssessment.getChildren(observedBranch).flatMap(_.getChildren(observedBranch)).map(_.asInstanceOf[DimensionMeasurement])

    val dimensionMeasurementDiagnosis = targetDimensionMeasurements.union(observedDimensionMeasurements).map(_.dimensionName()).distinct.map(dimensionName => {
      (dimensionName,
        (targetDimensionMeasurements.find(_.dimensionName() == dimensionName).get.measuredValue(),
          observedDimensionMeasurements.find(_.dimensionName() == dimensionName).get.measuredValue()))
    }).filter(t => t._2._1 != t._2._2).toMap

    val targetBRDVs = targetDimensionMeasurements.flatMap(_.getChildren(targetBranch)).flatMap(_.getChildren(targetBranch)).map(_.asInstanceOf[BRDV])
    val observedBRDVs = observedDimensionMeasurements.flatMap(_.getChildren(observedBranch)).flatMap(_.getChildren(observedBranch)).map(_.asInstanceOf[BRDV])

    val brdvsDiagnosis = targetBRDVs.union(observedBRDVs).map(_.name).distinct.map(brdvName => {
      (brdvName, (targetBRDVs.find(_.name == brdvName).get.value, observedBRDVs.find(_.name == brdvName).get.value))
    }).filter(t => t._2._1 != t._2._2).toMap


    new Diagnosis(targetBranch, observedBranch, assessmentDiagnosis, dimensionMeasurementDiagnosis, brdvsDiagnosis)
  }

  private def calculateDeviation(m: Map[String, (String, String)]): Int =
    m.values.count(t => t._1 != t._2)

}
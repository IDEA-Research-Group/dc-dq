package es.us.idea.dmn4spark.diagnosis.graph

import es.us.idea.dmn4spark.analysis.{DMNAnalysisHelpers, Utils}
import es.us.idea.dmn4spark.analysis.extended.ExtendedDecisionDiagram
import es.us.idea.dmn4spark.diagnosis.graph.adapters.JGraphtAdapter
import es.us.idea.dmn4spark.diagnosis.graph.components.{Assessment, BRDV, DimensionMeasurement, Measurement, Observation}
import es.us.idea.dmn4spark.diagnosis.graph.components.basic.{DirectedEdge, Vertex}
import play.api.libs.json._

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.util.Try

class DMN4DQTree(vertices: List[Vertex], edges: List[DirectedEdge], measurementToBrdvOpt: Option[Map[String, List[String]]] = None)
  extends Tree(vertices, edges) with Serializable {

  lazy val measurementToBrdv: Map[String, List[String]] = {
    measurementToBrdvOpt match {
      case Some(map) => map
      case _ => calculateBrdvDependencies()
    }
  }

  private def calculateBrdvDependencies(): Map[String, List[String]] = {
    edges().map(edge => edge.target() match {
      case brdv:BRDV => Some(edge.source(), brdv.name)
      case _ => None
    }).filter(_.isDefined).map(x => (x.get._1.getParents, x.get._2)).flatMap(x => x._1.map{
      case dimensionMeasurement: DimensionMeasurement => Some(dimensionMeasurement.dimensionName(), x._2)
      case _ => None
    }).filter(_.isDefined).map(_.get).distinct.groupBy(_._1).map(x => (x._1, x._2.map(_._2)))
  }

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
  //def O(dimensionMeasurement: DimensionMeasurement): List[Observation] = dimensionMeasurement.getChildren = ???
  def getEvidenceNamesOfDimensionMeasurement(dimensionMeasurement: DimensionMeasurement): List[String] =
    dimensionMeasurement.getChildren.headOption match {
      case Some(observation: Observation) =>observation.getChildren.map {
        case brdv: BRDV => Some(brdv.name)
        case _ => None
      }.filter(_.isDefined).map(_.get)
      case _ => List()
    }

  def pruneFromDimensionMeasurement(): DMN4DQTree = {
    import DMN4DQTree.implicits._

    val toPrune = vertices.filter(v => v match {
      case dimensionMeasurement: DimensionMeasurement => true
      case _ => false
    }).distinct

    pruneDescendants(toPrune)
  }

  // The map must represent a branch in a DMN4DQ branch
  def getBranch(map: Map[String, AnyRef]): DMN4DQTree = {
    var assessmentList = List[Assessment]()
    var dqmList = List[DimensionMeasurement]()
    var brdvList = List[BRDV]()

    var branchVertices = Set[Vertex]()
    var branchEdges = Set[DirectedEdge]()

    map.keys.foreach {
      case dqa if dqa == "DQA" => assessmentList = assessmentList :+ Assessment(map(dqa).toString)
      case dqm if "DQM\\(.+\\)".r.pattern.matcher(dqm).matches() => {
        dqmList = dqmList :+ DimensionMeasurement(dqm.replace("DQM(", "").replace(")", ""), map(dqm).toString)
      }
      case br if "^BR.*".r.pattern.matcher(br).matches() => brdvList = brdvList :+ BRDV(br, map(br).toString)
      case _ =>
    }

    if(assessmentList.size == 1) {
      val assessment = assessmentList.head
      val measurement = Measurement(dqmList)
      branchVertices = (branchVertices + assessment) + measurement
      branchEdges = branchEdges + DirectedEdge(assessment, measurement)

      dqmList.foreach(measurementDimension => {
        branchVertices = branchVertices + measurementDimension
        branchEdges = branchEdges + DirectedEdge(measurement, measurementDimension)
        measurementToBrdv.get(measurementDimension.dimensionName()) match {
          case Some(x) => {
            val candidateBrdvs = x.map(brdvName => brdvList.find(_.name == brdvName)).filter(_.isDefined).map(_.get)
            val observation = Observation(candidateBrdvs)
            branchVertices = branchVertices + observation
            branchEdges = branchEdges + DirectedEdge(measurementDimension, observation)
            candidateBrdvs.foreach(brdv => {
              branchVertices = branchVertices + brdv
              branchEdges = branchEdges + DirectedEdge(observation, brdv)
            })
          }
          case _ =>
        }
      })
    }
    val r = new DMN4DQTree(branchVertices.toList, branchEdges.toList, Some(this.measurementToBrdv))
    r
  }

  override def toString: String = {
    val assessment = vertices().filter(_.isInstanceOf[Assessment])
    val measurements = vertices().filter(_.isInstanceOf[Measurement])
    val dimensionMeasurements = vertices().filter(_.isInstanceOf[DimensionMeasurement])
    val observations = vertices().filter(_.isInstanceOf[Observation])
    val brdvs = vertices().filter(_.isInstanceOf[BRDV])
    s"Assessment: $assessment\nMeasurements: $measurements\nDimensionMeasurements: $dimensionMeasurements\nObservations: $observations\nBRDVS: $brdvs\nEdges: $edges()"
  }

}

object DMN4DQTree{

  def deserializeJson(jsObject: JsObject) = {
    val underlyingMap = jsObject.value

    def getVertexFromJsObject(possibleVertex: JsObject): Option[Vertex] = {
      Try({
        val uMap = possibleVertex.value
        uMap("type").as[String] match {
          case "Assessment" => Assessment.deserializeJson(possibleVertex)
          case "Measurement" => Measurement.deserializeJson(possibleVertex)
          case "DimensionMeasurement" => DimensionMeasurement.deserializeJson(possibleVertex)
          case "Observation" => Observation.deserializeJson(possibleVertex)
          case "BRDV" => BRDV.deserializeJson(possibleVertex)
        }
      }).toOption
    }

    // extract vertices
    val vertices = underlyingMap.get("vertices") match {
      case Some(vertices) => vertices match {
        case jsArray: JsArray => jsArray.value.map(value =>
          Try(value.as[JsObject]).toOption match {
            case Some(vertexJs) => getVertexFromJsObject(vertexJs)
            case _ => None
          }
        ).filter(_.isDefined).map(_.get)
        case _ => List()
      }
      case _ => List()
    }

    val edges = underlyingMap.get("edges") match {
      case Some(edges) => edges match {
        case jsArray: JsArray => jsArray.value.map(value =>
          Try(value.as[JsObject]).toOption match {
            case Some(edgeJs) => Try({
              val srcId = edgeJs.value("source").as[String]
              val tgId = edgeJs.value("target").as[String]
              val src = vertices.find(_.id() == srcId).head
              val tg = vertices.find(_.id() == tgId).head
              new DirectedEdge(src, tg)
            }).toOption
            case _ => None
          }
        ).filter(_.isDefined).map(_.get)
        case _ => List()
      }
      case _ => List()
    }

    new DMN4DQTree(vertices.toList, edges.toList)

  }

  def apply(vertices: List[Vertex], edges: List[DirectedEdge]): DMN4DQTree = new DMN4DQTree(vertices, edges)

  def apply(path: String): DMN4DQTree = apply(ExtendedDecisionDiagram(path, true))

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

    // extract relationships from brdvs names to dimension measurement name
    val t = dqm.flatMap(x =>
      x.conditions().map(_.name).map(y => (y, x.outputs().map(_.name))).flatMap(y => y._2.map(z => (y._1, z)))
    ).distinct.groupBy(_._2).map(x => (x._1, x._2.map(_._1))).toMap

    new DMN4DQTree(vertices.toList, edges.toList, Some(t))
  }

  object implicits {
    implicit def tree2dmn4dqTree(tree: Tree): DMN4DQTree = DMN4DQTree(tree.vertices(), tree.edges())
    implicit def trees2dmn4dqTrees(trees: List[Tree]): List[DMN4DQTree] = trees.map(tree2dmn4dqTree)
  }


}
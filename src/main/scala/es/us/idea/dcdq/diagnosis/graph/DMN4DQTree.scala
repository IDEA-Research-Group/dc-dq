package es.us.idea.dcdq.diagnosis.graph

import es.us.idea.dcdq.analysis.{DMNAnalysisHelpers, Utils}
import es.us.idea.dcdq.analysis.extended.ExtendedDecisionDiagram
import es.us.idea.dcdq.diagnosis.graph.components.{Assessment, Attribute, BRDV, Decision, DimensionMeasurement, Measurement, Observation}
import es.us.idea.dcdq.diagnosis.graph.components.basic.{AndVertex, DirectedEdge, Vertex}
import es.us.idea.dcdq.diagnosis.graph.structure.DMN4DQStructure
import es.us.idea.dcdq.diagnosis.utils.{RankCoefficientSort, SortableItem}
import es.us.idea.dmn4spark.dmn.executor.DMNExecutor
import org.camunda.bpm.dmn.feel.impl.FeelEngine
import play.api.libs.json._

import java.io.InputStream
import scala.reflect.ClassTag
import scala.util.Try

class DMN4DQTree(vertices: Set[Vertex], edges: Set[DirectedEdge], structureOpt: Option[DMN4DQStructure] = None)
  extends Tree(vertices, edges) with Serializable {

  lazy val branchStructure: DMN4DQStructure = {
    structureOpt match {
      case Some(map) => map
      case _ => inferStructure()
    }
  }

  def structureOpt(): Option[DMN4DQStructure] = structureOpt

  override def getRoots(): Set[Decision] = super.getRoots().filter(_.isInstanceOf[Decision]).map(_.asInstanceOf[Decision])

  private def calculateBrdvDependencies(): Map[String, List[String]] = {
    edges().map(edge => edge.target() match {
      case brdv:BRDV => Some(edge.source(), brdv.name)
      case _ => None
    }).filter(_.isDefined).map(x => (x.get._1.getParents, x.get._2)).flatMap(x => x._1.map{
      case dimensionMeasurement: DimensionMeasurement => Some(dimensionMeasurement.dimensionName(), x._2)
      case _ => None
    }).filter(_.isDefined).map(_.get).groupBy(_._1).map(x => (x._1, x._2.map(_._2).toList))
  }

  private def inferStructure(): DMN4DQStructure = {
    val dimensionsToBrdv = getDimensionMeasurements()
      .map(dm => (dm.dimensionName(), dm.getChildren.flatMap(_.getChildren.map(_.name)))).toList.toMap
    val brdvToAttribute = getBRDVs().map(brdv => (brdv.name, brdv.getChildren.map(_.name))).toList.toMap
    DMN4DQStructure(dimensionsToBrdv, brdvToAttribute)
  }

  def getDimensionMeasurements(): Set[DimensionMeasurement] =
    vertices().flatMap {
      case x: DimensionMeasurement => Some(x)
      case _ => None
    }

  def getBRDVs(dimensionName: String): Set[BRDV] =
    getDimensionMeasurements().filter(_.dimensionName() == dimensionName).flatMap(_.getChildren.flatMap(_.getChildren))

  def getBRDVs(): Set[BRDV] =
    vertices().flatMap {
      case x: BRDV => Some(x)
      case _ => None
    }

  def getAttributes: Set[Attribute] =
    vertices().flatMap {
      case x: Attribute => Some(x)
      case _ => None
    }

  def getObservations: Set[Observation] =
    vertices().flatMap {
      case x: Observation => Some(x)
      case _ => None
    }

  def getMeasurements: Set[Measurement] =
    vertices().flatMap {
      case x: Measurement => Some(x)
      case _ => None
    }

  def getAssessments: Set[Assessment] =
    vertices().flatMap {
      case x: Assessment => Some(x)
      case _ => None
    }

  def getDecision: Set[Decision] =
    vertices().flatMap {
      case x: Decision => Some(x)
      case _ => None
    }

  @deprecated
  def M(assessmentValue: String): Set[Measurement] = {
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
  @deprecated
  def D(dimensionName: String): Set[DimensionMeasurement] = ???
  @deprecated
  def O(dimensionName: String, measuredValue: String) = ???


  @deprecated
  def getEvidenceNamesOfDimensionMeasurement(dimensionMeasurement: DimensionMeasurement): Set[String] =
    dimensionMeasurement.getChildren.headOption match {
      case Some(observation: Observation) =>observation.getChildren.map {
        case brdv: BRDV => Some(brdv.name)
        case _ => None
      }.filter(_.isDefined).map(_.get)
      case _ => Set()
    }


  override def findAllBranches(vertex: Vertex): List[UsabilityProfile] =
    super.findAllBranches(vertex).map(t => UsabilityProfile(t))


  def union(trees: List[DMN4DQTree]): DMN4DQTree = {
    val (vertices, edges) = super.union(trees).verticesAndEdges()
    DMN4DQTree(vertices, edges)
  }

//  def pruneFromDimensionMeasurement(): DMN4DQTree = {
//    import DMN4DQTree.implicits._
//
//    val toPrune = vertices.filter(v => v match {
//      case dimensionMeasurement: DimensionMeasurement => true
//      case _ => false
//    }).distinct
//
//    pruneDescendants(toPrune)
//  }

  // The map must represent a branch in a DMN4DQ branch
  def getBranch(map: Map[String, AnyRef]): DMN4DQTree = {
    var decisionList = Set[Decision]()
    var assessmentList = Set[Assessment]()
    var dqmList = Set[DimensionMeasurement]()
    var brdvList = Set[BRDV]()

    var branchVertices = Set[Vertex]()
    var branchEdges = Set[DirectedEdge]()

    map.keys.foreach {
      case dud if dud == "BRDUD" => decisionList = decisionList + Decision(map(dud).toString)
      case dqa if dqa == "BRDQA" => assessmentList = assessmentList + Assessment(map(dqa).toString)
      case dqm if """^(BRDQM).*$""".r.pattern.matcher(dqm).matches() => {
        //dqmList = dqmList + DimensionMeasurement(dqm.replace("DQM(", "").replace(")", ""), map(dqm).toString)
        dqmList = dqmList + DimensionMeasurement(dqm, map(dqm).toString)
      }
      case br if """^(BRDV).*$""".r.pattern.matcher(br).matches() => brdvList = brdvList + BRDV(br, map(br).toString)
      case _ =>
    }

    if(decisionList.size == 1) {
      val decision = decisionList.head
      branchVertices = branchVertices + decision

      if(assessmentList.size == 1) {
        val assessment = assessmentList.head
        branchEdges = branchEdges + DirectedEdge(decision, assessment)

        val measurement = Measurement(dqmList)
        branchVertices = (branchVertices + assessment) + measurement
        branchEdges = branchEdges + DirectedEdge(assessment, measurement)

        dqmList.foreach(measurementDimension => {
          branchVertices = branchVertices + measurementDimension
          branchEdges = branchEdges + DirectedEdge(measurement, measurementDimension)
          branchStructure.dimensionToBrdv.get(measurementDimension.dimensionName()) match {
            case Some(x) => {
              val candidateBrdvs = x.map(brdvName => brdvList.find(_.name == brdvName)).filter(_.isDefined).map(_.get)
              val observation = Observation(candidateBrdvs)
              branchVertices = branchVertices + observation
              branchEdges = branchEdges + DirectedEdge(measurementDimension, observation)
              candidateBrdvs.foreach(brdv => {
                branchVertices = branchVertices + brdv
                branchEdges = branchEdges + DirectedEdge(observation, brdv)
                branchStructure.brdvToAttributes.get(brdv.name).foreach(attSet => attSet.foreach(attStr => {
                  val att = Attribute(attStr)
                  branchVertices = branchVertices + att
                  branchEdges = branchEdges + DirectedEdge(brdv, att)
                }))
              })
            }
            case _ =>
          }
        })
      }
    }


    val r = new DMN4DQTree(branchVertices, branchEdges, Some(this.branchStructure))
    r
  }

  def dimensionMeasurementRanking(assessmentRanking: List[Assessment]): Map[String, List[(Int, DimensionMeasurement)]] = {
    def baseCaseAssessment(v: Vertex): Boolean = v.isInstanceOf[Assessment]

    val assessmentRankingWithIndex = assessmentRanking.zipWithIndex.map(x => (x._1, assessmentRanking.size - x._2)).toMap

    getDimensionMeasurements().groupBy(_.dimensionName()).map(x => {
      val dimensionName = x._1
      val dimensionValues = x._2
      // Next, we process the values related to this dimension
      val dimensionValuesAndRankings = dimensionValues.flatMap(dv => {
        val allPathsToDimensionValue = allPathsToVertex(dv, baseCase = baseCaseAssessment)

        // For each assessment in the ranking, calculate a coefficient
        assessmentRankingWithIndex.map(ar => {
          val assessment = ar._1
          val assessmentRanking = ar._2
          (assessmentRanking, dv, allPathsToDimensionValue.count(_.contains(assessment)).toDouble/allPathsToDimensionValue.size)
        })
      }).toList
      val s = RankCoefficientSort(dimensionValuesAndRankings.map(x => SortableItem(x._2, x._1, x._3))).sort()
      (dimensionName, s)
    })
  }

  def brdvValuesRanking(assessmentRanking: List[Assessment]): Map[String, List[(Int, BRDV)]] = {
    def baseCaseDimensionMeasurement(v: Vertex): Boolean = v.isInstanceOf[DimensionMeasurement]

    val dmr = dimensionMeasurementRanking(assessmentRanking)

    dmr.flatMap(dmTuple => {
      // We're inside of a dimension
      val dimensionName = dmTuple._1
      val dmRanking = dmTuple._2
      val dmRankingWithIndex = dmRanking.zipWithIndex.map(x => (x._1._2, dmRanking.size - x._2)).toMap

      val pathsToBrdv = getBRDVs(dimensionName).map(brdv => {
        (brdv, allPathsToVertex(brdv, baseCase = baseCaseDimensionMeasurement))
      }).toList

      dmRankingWithIndex.flatMap(dmValue => {
        // Calculate coefficients for this value
        pathsToBrdv.map(ptbrdv =>
          (dmValue._2, ptbrdv._1, ptbrdv._2.count(_.contains(dmValue._1)).toDouble/ptbrdv._2.size)
        )
      }).groupBy(_._2.name).map(brdvNameCoefficient => {
        // Estamos procesando cada BRDV name
        val brdvName = brdvNameCoefficient._1
        val rankingBrdvAndCoeff = brdvNameCoefficient._2.toList
        val s = RankCoefficientSort(rankingBrdvAndCoeff.map(x => SortableItem(x._2, x._1, x._3))).sort()
        (brdvName, s)
      } )
    })
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
          case "Decision" => Decision.deserializeJson(possibleVertex)
          case "Assessment" => Assessment.deserializeJson(possibleVertex)
          case "Measurement" => Measurement.deserializeJson(possibleVertex)
          case "DimensionMeasurement" => DimensionMeasurement.deserializeJson(possibleVertex)
          case "Observation" => Observation.deserializeJson(possibleVertex)
          case "BRDV" => BRDV.deserializeJson(possibleVertex)
          case "Attribute" => Attribute.deserializeJson(possibleVertex)
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
        ).filter(_.isDefined).map(_.get).toSet
        case _ => List()
      }
      case _ => List()
    }

    new DMN4DQTree(vertices.toSet, edges.toSet)

  }

  def apply(vertices: Set[Vertex], edges: Set[DirectedEdge]): DMN4DQTree = new DMN4DQTree(vertices, edges)

  def apply(path: String): DMN4DQTree = apply(ExtendedDecisionDiagram(path, true))

  def apply(arrBytes: Array[Byte]): DMN4DQTree = apply(ExtendedDecisionDiagram(arrBytes, true, None))

  def apply(dmnExecutor: DMNExecutor): DMN4DQTree = apply(ExtendedDecisionDiagram(dmnExecutor, true, None))

  def apply(arrBytes: Array[Byte], feelEngine: FeelEngine): DMN4DQTree = apply(ExtendedDecisionDiagram(arrBytes, true, Some(feelEngine)))

  def apply(dmnExecutor: DMNExecutor, feelEngine: FeelEngine): DMN4DQTree = apply(ExtendedDecisionDiagram(dmnExecutor, true, Some(feelEngine)))

  def apply(extendedDecisionDiagram: ExtendedDecisionDiagram): DMN4DQTree = {
    import DMNAnalysisHelpers._

    var vertices: Set[Vertex] = Set()
    var edges: Set[DirectedEdge] = Set()

    val dud = extendedDecisionDiagram.getDUD() // Get ExtendedRules for DUD
    val dqa = extendedDecisionDiagram.getDQA() // Get ExtendedRules for DQA
    val dqm = extendedDecisionDiagram.getDQM() // Get ExtendedRules for DQM
    val leafTables = extendedDecisionDiagram.getLeafExtendedDMNTables() // Get ExtendedRules for BRDVS (only useful to get input attrs)

    // Group by dud output (dud => assessmentCandidates)
    val dudValuesAndRules = dud.map(er => (er.outputs().find(_.name == "BRDUD")
      .getOrElse(throw new IllegalArgumentException("BRDUD outout expected in DUD table")).value, er.conditions()))
      .groupBy(_._1).map(x => (x._1.clean(), x._2.flatMap(_._2)))

    // Group by assessment output (assessmentName => measurementCandidates)
    val assessmentValuesAndRules = dqa.map(er => (er.outputs().find(_.name == "BRDQA")
      .getOrElse(throw new IllegalArgumentException("BRDQA outout expected in Assessment table")).value, er.conditions()))
      .groupBy(_._1).map(x => (x._1.clean(), x._2))

    // Group by measurement value (dimension + value) => observationCandidates
    val dqmByMeasurementOutput = dqm.flatMap(er => er.outputs().map(o => ((o.name.clean(), o.value.clean()), er.conditions())))
      .groupBy(_._1)

    dudValuesAndRules.foreach(dudCandidate => {
      val dudValue = dudCandidate._1.clean()
      val assessmentCandidates = dudCandidate._2
      val dud = Decision(dudValue)

      vertices = vertices + dud

      assessmentCandidates.foreach(possibleAssessmentCandidate => {
        val assessmentValue = possibleAssessmentCandidate.value.clean()
        assessmentValuesAndRules.filter(_._1 == assessmentValue).foreach(assessmentCandidate => {
          //val measurementCandidates = assessmentCandidate._2
          val assessment = Assessment(assessmentValue) // Instantiate vertex
          vertices = vertices + assessment // Add assessment to the list of vertices
          edges = edges + DirectedEdge(dud, assessment)

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
              }).toSet

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
                  val brdvs: Set[BRDV] = brdvCandidates.map(brdvCandidate => {
                    val name = brdvCandidate.name.clean()
                    val value = brdvCandidate.value.clean()
                    val brdv = BRDV(name, value)
                    vertices = vertices + brdv

                    // find table(s) where there is at least one output attribute whose name == this brdv name
                    leafTables.filter(_.dmnTableSummary().outputValues().exists(_.name == name) ).flatMap(_.dmnTableSummary().inputAttributes()).foreach(a => {
                      val attName = a.name
                      val attribute = Attribute(attName)
                      vertices = vertices + attribute
                      edges = edges + DirectedEdge(brdv, attribute)
                    })

                    brdv
                  }).toSet

                  val observation = Observation(brdvs)
                  vertices = vertices + observation
                  edges = edges + DirectedEdge(dimensionMeasurement, observation)
                  // add edge between brdv and observation. brdvs were previously added to vertices
                  brdvs.foreach(brdv => edges = edges + DirectedEdge(observation, brdv))

                })
              })
            })
          })


        })
      })





    })



    // extract relationships from brdvs names to dimension measurement name
    // TODO temporalmente desactivado. brdvsToAttributes no es trivial de inferir (asegurarse de que las leafsTables empiezan por BR...)
    // val measurementsToBrdv = dqm.flatMap(x =>
    //   x.conditions().map(_.name).map(y => (y, x.outputs().map(_.name))).flatMap(y => y._2.map(z => (y._1, z)))
    // ).distinct.groupBy(_._2).map(x => (x._1, x._2.map(_._1).toSet )).toMap
    // val brdvsToAttributes =

    new DMN4DQTree(vertices, edges, None)
  }

  object implicits {
    implicit def tree2dmn4dqTree(tree: Tree): DMN4DQTree = DMN4DQTree(tree.vertices(), tree.edges())
    implicit def trees2dmn4dqTrees(trees: List[Tree]): List[DMN4DQTree] = trees.map(tree2dmn4dqTree)
  }


}
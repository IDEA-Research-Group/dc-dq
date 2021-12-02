package es.us.idea.dcdq.streaming.utils

import es.us.idea.dcdq.diagnosis.graph.DMN4DQTree
import es.us.idea.dcdq.streaming.exception.MessageFormatException
import es.us.idea.dmn4spark.dmn.exception.DMN4SparkException
import es.us.idea.dmn4spark.dmn.executor.{DMNExecutor, DecisionResult}
import org.json4s.jackson.Serialization.write

object StreamingEvaluationUtils {

  //val json = "{\"dmnSource\": \"file\", \"dmn\": \"models/dmn4dq.dmn\", \"data\": {\"_c0\":\"1000011\",\"EffectiveDate\":\"31/10/17\",\"PricePerUnit\":\"8.446\",\"LeaseContractLength\":\"3yr\",\"PurchaseOption\":\"No Upfront\",\"OfferingClass\":\"standard\",\"Location\":\"Asia Pacific (Sydney)\",\"InstanceType\":\"x1e.4xlarge\",\"CurrentGeneration\":\"Yes\",\"InstanceFamily\":\"Memory optimized\",\"vCPU\":\"16.0\",\"ClockSpeed\":\"2.3 GHz\",\"Memory\":\"488 GiB\",\"Storage\":\"1 x 480 SSD\",\"NetworkPerformance\":\"Up to 10 Gigabit\",\"OperatingSystem\":\"Windows\",\"Tenancy\":\"Dedicated\",\"usageType\":\"APS2-DedicatedUsage:x1e.4xlarge\",\"DedicatedEBSThroughput\":\"1750 Mbps\",\"NormalizationSizeFactor\":\"32.0\",\"ProcessorFeatures\":\"Intel AVX, Intel AVX2\"}}"

  //val req = "{\"typeOfSource\":\"file\",\"dmnSource\":\"models/dmn4dq.dmn\",\"data\":\"{\\\"_c0\\\":\\\"1000066\\\",\\\"EffectiveDate\\\":\\\"28/10/18\\\",\\\"PricePerUnit\\\":\\\"0.201\\\",\\\"LeaseContractLength\\\":\\\"1yr\\\",\\\"PurchaseOption\\\":\\\"Partial Upfront\\\",\\\"OfferingClass\\\":\\\"standard\\\",\\\"Location\\\":\\\"US East (N. Virginia)\\\",\\\"InstanceType\\\":\\\"r5a.xlarge\\\",\\\"CurrentGeneration\\\":\\\"Yes\\\",\\\"InstanceFamily\\\":\\\"Memory optimized\\\",\\\"vCPU\\\":\\\"4.0\\\",\\\"ClockSpeed\\\":\\\"2.5 GHz\\\",\\\"Memory\\\":\\\"32 GiB\\\",\\\"Storage\\\":\\\"EBS only\\\",\\\"NetworkPerformance\\\":\\\"10 Gigabit\\\",\\\"OperatingSystem\\\":\\\"Windows\\\",\\\"Tenancy\\\":\\\"Dedicated\\\",\\\"usageType\\\":\\\"DedicatedUsage:r5a.xlarge\\\",\\\"DedicatedEBSThroughput\\\":\\\"Upto 2120 Mbps\\\",\\\"NormalizationSizeFactor\\\":\\\"8.0\\\",\\\"ProcessorFeatures\\\":\\\"AVX, AVX2, AMD Turbo\\\"}\"}"
  //val eva = doEvaluation(req)
  //println(eva)

  import StreamingUtils._

  def doEvaluation(json: String): String = {
    val jsonDeserialized = transformJsonStrIntoMap(json)
    val dmnExecutor = extractDmnExecutor(jsonDeserialized)
    val dataToEval = extractData(jsonDeserialized)
    val evaluationResult = evaluate(dmnExecutor, dataToEval)
    val usabilityProfile = buildUsabilityProfile(dmnExecutor, evaluationResult)
    evaluationResultToJsonString(evaluationResult, usabilityProfile)
  }

  /**
   *
   * @param mapEithr  Map[String, AnyRef] with a "data" key of which value represents a literal string with a serialized object
   * @return Map[String, Anyref] with the parsed json string
   */
  def extractData(mapEithr: Either[DMN4SparkException, Map[String, AnyRef]]): Either[DMN4SparkException, Map[String, AnyRef]] = {

    mapEithr.map(_.get("data").map {
      case str: String => Right(transformJsonStrIntoMap(str)).joinRight
      case any@(a: Map[String, AnyRef]) => Right(any)
      case _ => Left(MessageFormatException("Failed to parse data attribute.", new IllegalArgumentException()))
    }).map {
      case Some(d) => Right(d match {
        case Right(v) => Right(v)
        case Left(v) => Left(MessageFormatException("Failed to parse data attribute.", v))
      })
      case _ => Left(MessageFormatException("data not specified", new NoSuchElementException()))
    }.joinRight.joinRight
  }

  /**
   *
   * @param dmnExecutor
   * @param data
   * @return The result of the evaluation of the data with the DMNExecutor
   */
  def evaluate(dmnExecutor: Either[DMN4SparkException, DMNExecutor], data: Either[DMN4SparkException, Map[String, AnyRef]]): Either[DMN4SparkException, Seq[Option[DecisionResult]]] = {
    val evalRes: Either[DMN4SparkException, Seq[Option[DecisionResult]]] = data.map(scalaMap => {
      val a = dmnExecutor.map(_.getDecisionsResults(scalaMap))
      a
    }).joinRight
    evalRes
  }

  def buildUsabilityProfile(dmnExecutor: Either[DMN4SparkException, DMNExecutor], decisionResult: Either[DMN4SparkException, Seq[Option[DecisionResult]]]): Either[DMN4SparkException, (String, String)] = {

    dmnExecutor.map(ex => {
      val tree = DMN4DQTree.apply(ex)
      decisionResult.map(seq => {
        val drMap = seq.flatten.map(t => (t.decisionName, t.result)).toMap
        val up = tree.getBranch(drMap)
        val upDot = up.dotRepresentation
        val upJson = up.convert2json.toString()
        (upDot, upJson)
      })
    }).joinRight

  }

  /**
   *
   * @param evalRes Seq[DecisionResult] with the result of the evaluation
   * @param usabilityProfileRepresentations (String, String), the first being the dot representation of the UP, and
   *                                        the second the json representation of the UP
   * @return A Json string with the result of the evaluation serialized. IMPORTANT:
   *         All fields are represented as escaped strings
   */
  def evaluationResultToJsonString(evalRes: Either[DMN4SparkException, Seq[Option[DecisionResult]]],
                                   usabilityProfileRepresentations: Either[DMN4SparkException, (String, String)]): String = {
    evalRes match {
      case Right(evalValue) => {
        usabilityProfileRepresentations match {
          case Right(upValue) => write(
            Map(
              "evaluationResult" -> write(evalValue.flatten.map(dr =>  (dr.decisionName, dr.result)).toMap), // Represented as escaped json string
              "usabilityProfileDot" -> upValue._1,
              "usabilityProfileJson" -> upValue._2
            )
          )
          case Left(e) => write(Map("error" -> e.getMessage, "errorCausedBy" -> e.getCause.getMessage))
        }
      }
      case Left(e) => write(Map("error" -> e.getMessage, "errorCausedBy" -> e.getCause.getMessage))
    }
  }

}

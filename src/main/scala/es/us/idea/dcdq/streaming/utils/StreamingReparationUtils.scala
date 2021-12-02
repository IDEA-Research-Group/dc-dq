package es.us.idea.dcdq.streaming.utils

import es.us.idea.dcdq.diagnosis.cost.BRDVBasedCostModel
import es.us.idea.dcdq.diagnosis.cost.csp.{Solution, UsabilityProfileTransformation}
import es.us.idea.dcdq.diagnosis.graph.components.Decision
import es.us.idea.dcdq.diagnosis.graph.{DMN4DQTree, UsabilityProfile}
import es.us.idea.dcdq.streaming.exception.{MessageFormatException, ReparationException}
import es.us.idea.dmn4spark.dmn.exception.DMN4SparkException
import es.us.idea.dmn4spark.dmn.executor.DMNExecutor
import org.json4s.jackson.Serialization.write
import play.api.libs.json.{JsObject, Json}

import scala.util.Try

object StreamingReparationUtils {

  import StreamingUtils._

  //val up = "{\"id\":\"c360d4f6\",\"vertices\":[{\"type\":\"BRDV\",\"name\":\"BRDV04\",\"value\":\"false\",\"id\":\"14ddb0f\"},{\"type\":\"BRDV\",\"name\":\"BRDV05\",\"value\":\"2\",\"id\":\"32a89bdf\"},{\"type\":\"Attribute\",\"name\":\"ClockSpeed\",\"id\":\"b71f68f9\"},{\"type\":\"Assessment\",\"value\":\"bad quality\",\"id\":\"9d1203e4\"},{\"type\":\"Observation\",\"id\":\"3d93d2b7\"},{\"type\":\"Attribute\",\"name\":\"PricePerUnit\",\"id\":\"74830ed8\"},{\"type\":\"Measurement\",\"id\":\"ccf35ca8\"},{\"type\":\"Observation\",\"id\":\"b5977c3f\"},{\"type\":\"Decision\",\"decision\":\"do not use\",\"id\":\"314c4825\"},{\"type\":\"Attribute\",\"name\":\"Location\",\"id\":\"752a03d5\"},{\"type\":\"Observation\",\"id\":\"115f18ad\"},{\"type\":\"Attribute\",\"name\":\"Memory\",\"id\":\"8927a921\"},{\"type\":\"Attribute\",\"name\":\"Storage\",\"id\":\"f2e8da5b\"},{\"type\":\"BRDV\",\"name\":\"BRDV11\",\"value\":\"appropriate\",\"id\":\"5fab3f31\"},{\"type\":\"BRDV\",\"name\":\"BRDV13\",\"value\":\"realistic\",\"id\":\"55da82d0\"},{\"type\":\"BRDV\",\"name\":\"BRDV09\",\"value\":\"3\",\"id\":\"32a8aae4\"},{\"type\":\"BRDV\",\"name\":\"BRDV10\",\"value\":\"2\",\"id\":\"32a8fd79\"},{\"type\":\"DimensionMeasurement\",\"dimensionName\":\"BRDQMAccuracy\",\"measuredValue\":\"50\",\"id\":\"740978c\"},{\"type\":\"Attribute\",\"name\":\"InstanceFamily\",\"id\":\"9e1c2479\"},{\"type\":\"BRDV\",\"name\":\"BRDV07\",\"value\":\"2\",\"id\":\"32a8a361\"},{\"type\":\"BRDV\",\"name\":\"BRDV03\",\"value\":\"2\",\"id\":\"32a8945d\"},{\"type\":\"BRDV\",\"name\":\"BRDV01\",\"value\":\"2\",\"id\":\"32a88cdb\"},{\"type\":\"BRDV\",\"name\":\"BRDV08\",\"value\":\"appropriate\",\"id\":\"362a319\"},{\"type\":\"BRDV\",\"name\":\"BRDV02\",\"value\":\"appropriate\",\"id\":\"6c507c13\"},{\"type\":\"BRDV\",\"name\":\"BRDV12\",\"value\":\"2\",\"id\":\"32a904fb\"},{\"type\":\"DimensionMeasurement\",\"dimensionName\":\"BRDQMConsistency\",\"measuredValue\":\"consistent\",\"id\":\"2fad3464\"},{\"type\":\"BRDV\",\"name\":\"BRDV06\",\"value\":\"true\",\"id\":\"35077760\"},{\"type\":\"DimensionMeasurement\",\"dimensionName\":\"BRDQMCompleteness\",\"measuredValue\":\"adequately complete\",\"id\":\"be320026\"},{\"type\":\"Attribute\",\"name\":\"OperatingSystem\",\"id\":\"7e7036b0\"}],\"edges\":[{\"source\":\"5fab3f31\",\"target\":\"7e7036b0\"},{\"source\":\"115f18ad\",\"target\":\"362a319\"},{\"source\":\"9d1203e4\",\"target\":\"ccf35ca8\"},{\"source\":\"32a8945d\",\"target\":\"b71f68f9\"},{\"source\":\"b5977c3f\",\"target\":\"32a8945d\"},{\"source\":\"b5977c3f\",\"target\":\"32a89bdf\"},{\"source\":\"b5977c3f\",\"target\":\"32a8a361\"},{\"source\":\"32a904fb\",\"target\":\"74830ed8\"},{\"source\":\"32a8fd79\",\"target\":\"7e7036b0\"},{\"source\":\"14ddb0f\",\"target\":\"b71f68f9\"},{\"source\":\"32a8aae4\",\"target\":\"9e1c2479\"},{\"source\":\"b5977c3f\",\"target\":\"32a88cdb\"},{\"source\":\"314c4825\",\"target\":\"9d1203e4\"},{\"source\":\"2fad3464\",\"target\":\"3d93d2b7\"},{\"source\":\"115f18ad\",\"target\":\"5fab3f31\"},{\"source\":\"32a89bdf\",\"target\":\"8927a921\"},{\"source\":\"b5977c3f\",\"target\":\"32a8fd79\"},{\"source\":\"115f18ad\",\"target\":\"14ddb0f\"},{\"source\":\"740978c\",\"target\":\"115f18ad\"},{\"source\":\"b5977c3f\",\"target\":\"32a904fb\"},{\"source\":\"6c507c13\",\"target\":\"752a03d5\"},{\"source\":\"32a8aae4\",\"target\":\"8927a921\"},{\"source\":\"ccf35ca8\",\"target\":\"be320026\"},{\"source\":\"115f18ad\",\"target\":\"55da82d0\"},{\"source\":\"32a8aae4\",\"target\":\"f2e8da5b\"},{\"source\":\"ccf35ca8\",\"target\":\"2fad3464\"},{\"source\":\"be320026\",\"target\":\"b5977c3f\"},{\"source\":\"32a88cdb\",\"target\":\"752a03d5\"},{\"source\":\"32a8a361\",\"target\":\"9e1c2479\"},{\"source\":\"32a8aae4\",\"target\":\"b71f68f9\"},{\"source\":\"55da82d0\",\"target\":\"74830ed8\"},{\"source\":\"35077760\",\"target\":\"8927a921\"},{\"source\":\"115f18ad\",\"target\":\"35077760\"},{\"source\":\"ccf35ca8\",\"target\":\"740978c\"},{\"source\":\"3d93d2b7\",\"target\":\"32a8aae4\"},{\"source\":\"115f18ad\",\"target\":\"6c507c13\"},{\"source\":\"362a319\",\"target\":\"9e1c2479\"}]}"
  //val setca = "{\"default\":1,\"brdvCosts\":[{\"name\":\"BRDV02\",\"costs\":[{\"observedValue\":\"appropriate enough\",\"targetValue\":\"appropriate\",\"cost\":4,\"single\":true},{\"observedValue\":\"inappropriate\",\"targetValue\":\"appropriate\",\"cost\":8,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV03\",\"costs\":[{\"observedValue\":\"0\",\"targetValue\":\"2\",\"cost\":10,\"single\":true},{\"observedValue\":\"1\",\"targetValue\":\"2\",\"cost\":10,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV06\",\"costs\":[{\"observedValue\":\"false\",\"targetValue\":\"true\",\"cost\":5,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV11\",\"costs\":[{\"observedValue\":\"inappropriate\",\"targetValue\":\"appropriate\",\"cost\":5,\"single\":true},{\"observedValue\":\"appropriate enough\",\"targetValue\":\"appropriate\",\"cost\":4,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV09\",\"costs\":[{\"observedValue\":\"1\",\"targetValue\":\"3\",\"cost\":1,\"single\":true},{\"observedValue\":\"2\",\"targetValue\":\"3\",\"cost\":1,\"single\":true},{\"observedValue\":\"0\",\"targetValue\":\"3\",\"cost\":1,\"single\":true}],\"default\":1,\"single\":true},{\"name\":\"BRDV13\",\"costs\":[{\"observedValue\":\"unrealistic\",\"targetValue\":\"exaggerated\",\"cost\":2,\"single\":true},{\"observedValue\":\"unrealistic\",\"targetValue\":\"realistic\",\"cost\":6,\"single\":true},{\"observedValue\":\"exaggerated\",\"targetValue\":\"realistic\",\"cost\":5,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV04\",\"costs\":[{\"observedValue\":\"false\",\"targetValue\":\"true\",\"cost\":5,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV07\",\"costs\":[{\"observedValue\":\"0\",\"targetValue\":\"2\",\"cost\":6,\"single\":true},{\"observedValue\":\"1\",\"targetValue\":\"2\",\"cost\":6,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV10\",\"costs\":[{\"observedValue\":\"0\",\"targetValue\":\"2\",\"cost\":10,\"single\":true},{\"observedValue\":\"1\",\"targetValue\":\"2\",\"cost\":10,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV01\",\"costs\":[{\"observedValue\":\"0\",\"targetValue\":\"2\",\"cost\":10,\"single\":true},{\"observedValue\":\"1\",\"targetValue\":\"2\",\"cost\":10,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV08\",\"costs\":[{\"observedValue\":\"appropriate enough\",\"targetValue\":\"appropriate\",\"cost\":3,\"single\":true},{\"observedValue\":\"inappropriate\",\"targetValue\":\"appropriate\",\"cost\":7,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV12\",\"costs\":[{\"observedValue\":\"0\",\"targetValue\":\"1\",\"cost\":10,\"single\":true},{\"observedValue\":\"0\",\"targetValue\":\"2\",\"cost\":10,\"single\":true},{\"observedValue\":\"1\",\"targetValue\":\"2\",\"cost\":10,\"single\":true}],\"default\":10,\"single\":true},{\"name\":\"BRDV05\",\"costs\":[{\"observedValue\":\"1\",\"targetValue\":\"2\",\"cost\":10,\"single\":true},{\"observedValue\":\"0\",\"targetValue\":\"2\",\"cost\":10,\"single\":true}],\"default\":10,\"single\":true}]}"
  //val target = "use"

  def doReparation(json:String): String = {
    // deserialize json
    val jsonDeserialized = transformJsonStrIntoMap(json)
    // get dmnExecutor
    val dmnExecutor = extractDmnExecutor(jsonDeserialized)
    // get observed usability profile
    val observedUsabilityProfile = extractObservedUsabilityProfile(jsonDeserialized)
    // get set of corrective actions
    val setCorrectiveActions = extractCorrectiveActions(jsonDeserialized)
    // get target decision
    val targetDecision = extractTargetDecision(jsonDeserialized)
    // run COP
    val solution = repair(dmnExecutor, observedUsabilityProfile, setCorrectiveActions, targetDecision)
    // build json
    val s = solutionToJsonString(solution: Either[DMN4SparkException, Solution])
    println(s)
    s
  }

  def extractTargetDecision(mapEithr: Either[DMN4SparkException, Map[String, AnyRef]]): Either[DMN4SparkException, String] = {
    val observedUsabilityProfileField = mapEithr.map(_.get("targetDecision"))
    observedUsabilityProfileField.map{
      case Some(value) => Right(value.toString)
      case _ => Left(MessageFormatException("targetDecision not found", new NoSuchElementException()))
    }.joinRight
  }

  def extractCorrectiveActions(mapEithr: Either[DMN4SparkException, Map[String, AnyRef]]): Either[DMN4SparkException, BRDVBasedCostModel] = {
    val observedUsabilityProfileField = mapEithr.map(_.get("setCorrectiveActions"))
    observedUsabilityProfileField.map{
      case Some(value) => value match {
        case jsonStr: String => Right(loadSetOfCorrectiveActions(jsonStr))
        case any@(a: Map[String, AnyRef]) => Right(loadSetOfCorrectiveActions(write(a)))
        case _ => Left(MessageFormatException("loadSetOfCorrectiveActions field invalid format", new IllegalArgumentException()))
      }
      case _ => Left(MessageFormatException("loadSetOfCorrectiveActions not found", new NoSuchElementException()))
    }.joinRight.joinRight
  }

  def extractObservedUsabilityProfile(mapEithr: Either[DMN4SparkException, Map[String, AnyRef]]): Either[DMN4SparkException, UsabilityProfile] = {
    val observedUsabilityProfileField = mapEithr.map(_.get("usabilityProfileJson"))
    observedUsabilityProfileField.map{
      case Some(value) => value match {
        case jsonStr: String => Right(loadUsabilityProfile(jsonStr))
        case any@(a: Map[String, AnyRef]) => Right(loadUsabilityProfile(write(a)))
        case _ => Left(MessageFormatException("usabilityProfileJson field invalid format", new IllegalArgumentException()))
      }
      case _ => Left(MessageFormatException("usabilityProfileJson not found", new NoSuchElementException()))
    }.joinRight.joinRight
  }

  def repair(
              dmnExecutor: Either[DMN4SparkException, DMNExecutor],
              observedUsabilityProfile: Either[DMN4SparkException, UsabilityProfile],
              setCorrectiveActions: Either[DMN4SparkException, BRDVBasedCostModel],
              targetDecision: Either[DMN4SparkException, String]): Either[DMN4SparkException, Solution] = {
    targetDecision.map(td => {
      observedUsabilityProfile.map(oup => {
        setCorrectiveActions.map(setCa => {
          dmnExecutor.map(exec => {
            Try(DMN4DQTree(exec).findAllBranches(Decision(td)))
              .toEither
              .left
              .map(e => ReparationException("Failed to find usability profiles associated to the specified target decision", e))
              .map(tds => {
                Try(UsabilityProfileTransformation.findOptimalTargetUsabilityProfiles(List(oup), tds, setCa).head)
                  .toEither
                  .left
                  .map(e => ReparationException("Failed to execute the COP.", e))
              })
          })
        })
      })
    }).joinRight.joinRight.joinRight.joinRight.joinRight
  }

  def solutionToJsonString(solution: Either[DMN4SparkException, Solution]): String =
    solution match {
      case Right(value) => Json.toJson(value).toString
      case Left(e) => write(Map("error" -> e.getMessage, "errorCausedBy" -> e.getCause.getMessage))
    }

  private def loadUsabilityProfile(jsonUp: String): Either[DMN4SparkException, UsabilityProfile] =
    Try(UsabilityProfile.deserializeJson(Json.parse(jsonUp).asInstanceOf[JsObject])).toEither
      .left
      .map(t => MessageFormatException("The format of the usability profile could not be recognized", t))

  private def loadSetOfCorrectiveActions(jsonSetCA: String): Either[DMN4SparkException, BRDVBasedCostModel] =
    Try(BRDVBasedCostModel.deserializeJsonString(jsonSetCA)).toEither
      .left
      .map(t => MessageFormatException("Incorrect format for the set of corrective actions", t))

}

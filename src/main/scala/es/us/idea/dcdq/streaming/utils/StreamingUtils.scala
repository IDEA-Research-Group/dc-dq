package es.us.idea.dcdq.streaming.utils

import es.us.idea.dcdq.streaming.exception.MessageFormatException
import es.us.idea.dmn4spark.dmn.exception.DMN4SparkException
import es.us.idea.dmn4spark.dmn.executor.DMNExecutor
import es.us.idea.dmn4spark.dmn.loader.{InputStreamLoader, PathLoader, URLLoader}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.io.ByteArrayInputStream
import scala.util.Try

object StreamingUtils {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  /**
   *
   * @param jsonStr Input string with json format
   * @return A Map[String, AnyRef] with the parsed json string
   */
  def transformJsonStrIntoMap(jsonStr: String): Either[DMN4SparkException, Map[String, AnyRef]] =
    Try(parse(jsonStr).extract[Map[String, AnyRef]])
      .toEither
      .left
      .map(t => MessageFormatException(s"Cannot parse received message: ${t.getMessage}", t))

  /**
   *
   * @param mapEithr A Map[String, AnyRef] with the following keys: "typeOfSource" and "dmnSource". "dmnSource" must be
   *                 file, url or is. "dmn" must contain the XML definition of a DMN model (if dmnSource is is) or
   *                 a path to a DMN model
   * @return A DMNExecutor
   */
  def extractDmnExecutor(mapEithr: Either[DMN4SparkException, Map[String, AnyRef]]): Either[DMN4SparkException, DMNExecutor] = {
    val dmnSourceField = mapEithr.map(_.get("typeOfSource").map(_.toString))
    val dmnField = mapEithr.map(_.get("dmnSource").map(_.toString))

    val loadedDmn: Either[DMN4SparkException, DMNExecutor] = dmnSourceField.map {
      case Some(sourceType) => Right(dmnField.map {
        case Some(dmnCandidate) => Right(loadDmn(sourceType, dmnCandidate))
        case _ => Left(MessageFormatException("dmn not specified", new NoSuchElementException()))
      })
      case _ => Left(MessageFormatException("dmnSource not specified", new NoSuchElementException()))
    }.joinRight.joinRight.joinRight.joinRight

    loadedDmn
  }

  /**
   *
   * @param sourceType type of source
   * @param dmnCandidate
   * @return
   */
  private def loadDmn(sourceType: String, dmnCandidate: String): Either[DMN4SparkException, DMNExecutor] = {
    sourceType match {
      case "file" => new PathLoader(dmnCandidate).load()
      case "url" => new URLLoader(dmnCandidate).load()
      case "is" => new InputStreamLoader(new ByteArrayInputStream(dmnCandidate.getBytes())).load()
      case _ => Left(MessageFormatException("dmnSources not valid", new IllegalArgumentException()))
    }
  }

}

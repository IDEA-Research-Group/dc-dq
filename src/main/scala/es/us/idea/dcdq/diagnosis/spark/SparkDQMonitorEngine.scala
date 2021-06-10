package es.us.idea.dcdq.diagnosis.spark

import es.us.idea.dcdq.diagnosis.graph.DMN4DQTree
import es.us.idea.dcdq.dmn.DMNSparkEngine
import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.DataFrame

import java.io.{FileInputStream, InputStream}
import java.net.{MalformedURLException, URI}
import scala.io.Source


/**
 * TODO: add configuration paratemers so that:
 * - Include branch json serialization (default: no)
 * - Include branch dot representation (default: no)
 * */
class SparkDQMonitorEngine(df: DataFrame) extends DMNSparkEngine(df) {

  //def setDecisions(decisions: String*): DMNSparkEngine = new DMNSparkEngine(df, decisions)

  override def loadFromLocalPath(path:String) = dmnSparkEnine.load(IOUtils.toByteArray(new FileInputStream(path)))

  def loadFromHDFS(uri: String, configuration: Configuration = new Configuration()) = {

    val hdfsUrlPattern = "((hdfs?)(:\\/\\/)(.*?)(^:[0-9]*$)?\\/)".r

    val firstPart = hdfsUrlPattern.findFirstIn(uri) match {
      case Some(s) => s
      case _ => throw new MalformedURLException(s"The provided HDFS URI is not valid: $uri")
    }

    val uriParts = uri.split(firstPart)
    if(uriParts.length != 2) throw new MalformedURLException(s"The provided HDFS URI is not valid. Path not found: $uri")

    val path = uriParts.lastOption match {
      case Some(s) => s
      case _ => throw new MalformedURLException(s"The provided HDFS URI is not valid. Path not found: $uri")
    }

    val fs = FileSystem.get(new URI(firstPart), configuration)
    val filePath = if(!new Path(path).isAbsolute) new Path(s"/$path") else new Path(path)

    val fsDataInputStream = fs.open(filePath);

//    execute(IOUtils.toByteArray(fsDataInputStream.getWrappedStream))
  }

  def loadFromURL(url: String) = ???

//  def loadFromInputStream(is: InputStream) = execute(IOUtils.toByteArray(is))

  private def generateDMN4DQTree(is: InputStream): DMN4DQTree = DMN4DQTree.apply(is)

  private def execute(is: InputStream): DataFrameDQMonitor = {


    val dmn4dqTree = generateDMN4DQTree(is)

    val df = super.execute()

    // for each tuple, calculate trees, add their id and json representation to the df
    // then, distinct by tree id -> get json representation
    // create auxiliar df/broadcast dictionary (id, json representation) and save that in DataframeDQMMonitor
    // drop auxiliary columns (e.g., tree serialization)


  }

}

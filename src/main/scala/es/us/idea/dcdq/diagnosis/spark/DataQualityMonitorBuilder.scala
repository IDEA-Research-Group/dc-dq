package es.us.idea.dcdq.diagnosis.spark

import es.us.idea.dcdq.diagnosis.NewDiagnosis
import es.us.idea.dcdq.diagnosis.graph.DMN4DQTree
import es.us.idea.dcdq.diagnosis.graph.adapters.JGraphtAdapter
import es.us.idea.dcdq.spark.{SparkRowToJavaTypesConversor, Utils}
import es.us.idea.dmn4spark.dmn.executor.DMNExecutor
import es.us.idea.dmn4spark.spark.engine.DMNSparkDFEngine
import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.api.java.UDF1
import org.apache.spark.sql.functions.{array, col, count, explode, first, struct, sum, udf}
import org.apache.spark.sql.functions
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.storage.StorageLevel
import play.api.libs.json.{JsObject, Json}
import shapeless.HList.ListCompat.::

import java.io.{ByteArrayInputStream, FileInputStream, InputStream}
import java.net.{MalformedURLException, URI}
import scala.::
import scala.collection.immutable.Nil.:::
import scala.io.Source
import scala.jdk.CollectionConverters.{mapAsScalaMapConverter, setAsJavaSetConverter}


/**
 * TODO: add configuration paratemers so that:
 * - Include branch json serialization (default: no)
 * - Include branch dot representation (default: no)
 *
 * - Refactorize to use DMNSparkEngine methods
 * */
class DataQualityMonitorBuilder(df: DataFrame) /*extends DMNSparkDFEngine(df, separateDMNColumns = true)*/ {

  //def setDecisions(decisions: String*): DMNSparkEngine = new DMNSparkEngine(df, decisions)

  def loadDmnFromLocalPath(path:String) = this.monitor(IOUtils.toByteArray(new FileInputStream(path)))

  def loadDmnFromHDFS(uri: String, configuration: Configuration = new Configuration()) = {

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

    this.monitor(IOUtils.toByteArray(fsDataInputStream.getWrappedStream))
  }
//
//  override def loadFromURL(url: String) = {
//    val content = Source.fromURL(url)
//    val bytes = content.mkString.getBytes
//    execute(IOUtils.toByteArray(new ByteArrayInputStream(bytes)))
//  }
//
//  override def loadFromInputStream(is: InputStream): DataQualityMonitor = execute(IOUtils.toByteArray(is))


  private def generateDMN4DQTree(dmnBytes: Array[Byte]): DMN4DQTree = DMN4DQTree.apply(dmnBytes)
//
  // private def execute(dmnInputStream: InputStream): DataQualityMonitor = {
  def monitor(dmnBytes: Array[Byte]): DataQualityMonitor = {

    val dmn4dqTree = generateDMN4DQTree(dmnBytes)

    // TODO it should be extracted from super, super should store the dmnExecutor!
    val dmnExecutor = new DMNExecutor(dmnBytes)
    val decisions = dmnExecutor.decisionKeys
    val brdvDecisions = decisions.filter(_.contains("BR"))
    val noBrdvDecisions = decisions.filter( !brdvDecisions.contains(_) )

    val spark = df.sparkSession

    val rowToJson = udf((r: Row) => r.json.hashCode.toHexString)

    val dmnSparkDFengine = new DMNSparkDFEngine(df, dmnExecutor, brdvDecisions, dmnOutputColumnName = Some("dmnOutput"))

    //val firstEvaluation = super.execute(dmnBytes, brdvDecisions)
    val firstEvaluation = dmnSparkDFengine.execute()
    val schema = firstEvaluation.schema
    val firstEvaluationRdd = firstEvaluation.rdd.persist(StorageLevel.MEMORY_AND_DISK)

    val dfWithBrdvDmnOutput = spark.createDataFrame(firstEvaluationRdd, schema)
      .withColumn("brdvRowHash", rowToJson(
        struct(brdvDecisions.map(c => col(s"dmnOutput.$c").as(c)): _*)
      ))
      //.withColumn("brdvRowHash", rowToJson(col("dmnOutput")))

    // agrupar por brdvRowHash -> cada fila tendrá el count y todas las columnas dmnOutput
    val groupedByBrdvs = dfWithBrdvDmnOutput.select("dmnOutput", "brdvRowHash").groupBy("brdvRowHash")
      .agg(
        struct(
          count("brdvRowHash").as("count"),
          first(struct("dmnOutput")).as("f"),
          sum("dmnOutput.runTime").as("brdvEvaluationTime")
        ).as("t")
      ).select(
        (col("brdvRowHash") :: col("t.count").as("count") :: col("t.brdvEvaluationTime").as("brdvEvaluationTime") :: Nil) ++
          brdvDecisions.map(c => col(s"t.f.dmnOutput.$c").as(c)): _*
    )

    //groupedByBrdvs.printSchema()
    //groupedByBrdvs.show(false)

    // pasar dmn4spark para obtener el resto de columnas
    //val fullDmnEvaluations =
    //  new DMNSparkDFEngine(groupedByBrdvs, dmnOutputColumnName = None).execute(DMNAnalysisUtils.pruneDmnTables(dmnBytes, brdvDecisions), noBrdvDecisions).cache()
    val dmnExecutorWithoutBrdvTables = new DMNExecutor(DMNAnalysisUtils.pruneDmnTables(dmnBytes, brdvDecisions))
    val fullDmnEvaluations = new DMNSparkDFEngine(groupedByBrdvs, dmnExecutorWithoutBrdvTables).appendOutput().evaluateDecisions(noBrdvDecisions:_*).execute().cache()

    //val beforeUp = brdvDecisions.foldLeft(fullDmnEvaluations)((acc, d) => acc.withColumnRenamed(d, struct))

    val beforeUp = fullDmnEvaluations
      .withColumn("DMN", struct(
        brdvDecisions.map(col) ++ noBrdvDecisions.map(col) : _*
      )).drop(brdvDecisions ++ noBrdvDecisions : _*).withColumnRenamed("runTime", "nonBrdvEvaluationTime")
    // STEP 1. for each tuple, calculate trees, add their id and json representation to the df

    // we'll add two columns: (1) tree (2) tree id
    val newColumns = Seq("BranchJson", "BranchId", "DotRepresentation")

    val func = new UDF1[Row, Row] {
      override def call(t1: Row): Row = {
        // TODO minimize type conversions, make it safe!
        val map = SparkRowToJavaTypesConversor.spark2javamap(t1).get("DMN").asInstanceOf[java.util.HashMap[String, AnyRef]].asScala.toMap
        // call function which returns tree object plus id

        val branch = dmn4dqTree.getBranch(map)
        val result = Seq(branch.convert2json.toString(), branch.getId,
          JGraphtAdapter.printDot(branch.vertices().asJava, branch.edges().asJava))
        Row.apply(result: _*)
      }
    }

    // get branches from metadata
    val metadata = applyUDF(func, newColumns, beforeUp)
    val metadataStruct = metadata.schema
    val metadataRdd = metadata.rdd.cache()
    val metadataDf = spark.createDataFrame(metadataRdd, metadataStruct)
    //fullDmnEvaluations.unpersist()

    //val metadataDf = metadataWithBranches.drop("brdvRowHash")
    //metadataWithBranches.unpersist()
    val dataDf = dfWithBrdvDmnOutput.join(metadataDf.select("brdvRowHash", "BranchId").hint("broadcast"), "brdvRowHash").drop("brdvRowHash", "dmnOutput")
    //dfWithBrdvDmnOutput.unpersist()


    val treeDf = metadataDf.sparkSession.createDataFrame(
      metadataDf.sparkSession.sparkContext.parallelize(Seq(Row(dmn4dqTree.convert2json.toString()))),
      StructType(List(StructField("Tree", StringType, true)))
    )

    new DataQualityMonitor(System.currentTimeMillis().toHexString, dataDf, metadataDf, treeDf, dmn4dqTree)
  }

  private def applyUDF(udfToApply: UDF1[Row, Row], newColumns: Seq[String], df: DataFrame): DataFrame = {
    val originalColumns = df.columns.map(col).toSeq
    val tempColumn = s"__${System.currentTimeMillis().toHexString}"
    val names = newColumns.map(d => (s"$tempColumn.$d", d))
    val dmn4dqTreeUdf = udf(udfToApply, Utils.createStructType(newColumns))
    val transformedDF = df.withColumn(tempColumn, explode(array(dmn4dqTreeUdf(struct(originalColumns: _*)))))
    names.foldLeft(transformedDF)((acc, n) => acc.withColumn(n._2, col(n._1))).drop(col(tempColumn))
  }

}

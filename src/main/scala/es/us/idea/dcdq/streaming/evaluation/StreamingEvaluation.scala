package es.us.idea.dcdq.streaming.evaluation

import es.us.idea.dcdq.streaming.StreamingConfig
import es.us.idea.dcdq.streaming.utils.StreamingEvaluationUtils
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.DataTypes
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import scala.io.Source
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

object StreamingEvaluation extends App {

  val sourceYaml = Source.fromResource("streaming-config.yaml").mkString
  val yaml = new Yaml(new Constructor(classOf[StreamingConfig]))
  val streamingConfig = yaml.load(sourceYaml).asInstanceOf[StreamingConfig]

  val spark = SparkSession.builder()
    .master("local")
    .appName("DMN4DQ Streaming - Single UP Evaluation").getOrCreate()

  import spark.implicits._

  implicit val formats = org.json4s.DefaultFormats

  val dfRecords = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", streamingConfig.bootstrapServers.asScala.mkString(", "))
    .option("subscribe", streamingConfig.evaluationRequestTopic)
    .option("startingOffsets", streamingConfig.startingOffsets)
    .load()


  val doEvaluation = dfRecords.select($"key".cast(DataTypes.StringType), $"value".cast(DataTypes.StringType))
    .map(r => {
      val id = r.getString(0)
      val value = r.getString(1)
      (id, StreamingEvaluationUtils.doEvaluation(value))
    }).toDF("key", "value")

  val writeEvaluation = doEvaluation.writeStream
    .format("kafka")
    .option("kafka.bootstrap.servers", streamingConfig.bootstrapServers.asScala.mkString(", "))
    .option("topic", streamingConfig.evaluationResultTopic)
    .option("checkpointLocation",streamingConfig.checkpointEvaluationLocation)
    .start()

  writeEvaluation.awaitTermination()
}



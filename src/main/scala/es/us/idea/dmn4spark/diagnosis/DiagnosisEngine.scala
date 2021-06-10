package es.us.idea.dmn4spark.diagnosis

import es.us.idea.dmn4spark.diagnosis.cost.OldCostModel
import es.us.idea.dmn4spark.diagnosis.graph.DMN4DQTree
import es.us.idea.dmn4spark.diagnosis.graph.adapters.JGraphtAdapter
import es.us.idea.dmn4spark.spark.{SparkDataConversor, Utils}
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions.{array, col, explode, struct, udf}
import org.apache.spark.sql.api.java.UDF1
import play.api.libs.json.{JsObject, Json}

import scala.collection.JavaConverters._

class DiagnosisEngine(df: DataFrame, dmn4dqTree: DMN4DQTree) extends Serializable {

  def profiles(): DataFrame = {
    // we'll add two columns: (1) tree (2) tree id
    val newColumns = Seq("BranchId")

    val func = new UDF1[Row, Row] {
      override def call(t1: Row): Row = {
        val map = SparkDataConversor.spark2javamap(t1).asScala.toMap
        // call function which returns tree object plus id
        val branch = dmn4dqTree.getBranch(map)
        val branchJson = branch.convert2json.toString()
        val branchId = branch.getId
        val result = {
          Seq(branch.getId)
        }

        Row.apply(result: _*)
      }
    }
    applyUDF(func, newColumns)
  }

  def branches(dotRepresentation: Boolean = false): DataFrame = {

    // we'll add two columns: (1) tree (2) tree id
    val newColumns = if(dotRepresentation) Seq("Branch", "BranchId", "dotRepresentation") else Seq("Branch", "BranchId")

    val func = new UDF1[Row, Row] {
      override def call(t1: Row): Row = {
        val map = SparkDataConversor.spark2javamap(t1).asScala.toMap
        // call function which returns tree object plus id
        val branch = dmn4dqTree.getBranch(map)
        val result = {
          if(dotRepresentation) Seq(branch.convert2json.toString(), branch.getId,
            JGraphtAdapter.printDot(branch.vertices().asJava, branch.edges().asJava))
          else Seq(branch.convert2json.toString(), branch.getId)
        }

        Row.apply(result: _*)
      }
    }
    applyUDF(func, newColumns)
  }

  def minimumCostToTarget(targetBranches: List[DMN4DQTree], criteria: Map[String, String], costModel: OldCostModel): DataFrame = {
    assert(df.columns.contains("Branch"))

    val newColumns = Seq("TargetBranch", "TargetBranchId", "cost", "targetBranchDotRepresentation")

    val func = new UDF1[Row, Row] {
      override def call(t1: Row): Row = {
        val map = SparkDataConversor.spark2javamap(t1).asScala.toMap

        val result = if(criteria.map(x => map(x._1) == x._2).forall(_ == true)) {
          val branch = DMN4DQTree.deserializeJson(Json.parse(map("Branch").toString).as[JsObject])
          val selectedDiagnosis = targetBranches.map(t => (t, new NewDiagnosis(t, branch).getCost(costModel))).minBy(x => x._2)
          Seq(selectedDiagnosis._1.convert2json.toString, selectedDiagnosis._1.getId, selectedDiagnosis._2.toString,
            JGraphtAdapter.printDot(selectedDiagnosis._1.vertices().asJava, selectedDiagnosis._1.edges().asJava))
        } else Seq("", "", "0", "")
        // call function which returns tree object plus id
        val branch = dmn4dqTree.getBranch(map)

        Row.apply(result: _*)
      }
    }
    applyUDF(func, newColumns)
  }

  private def applyUDF(udfToApply: UDF1[Row, Row], newColumns: Seq[String]): DataFrame = {
    val originalColumns = df.columns.map(col).toSeq
    val tempColumn = s"__${System.currentTimeMillis().toHexString}"
    val names = newColumns.map(d => (s"$tempColumn.$d", d))
    val dmn4dqTreeUdf = udf(udfToApply, Utils.createStructType(newColumns))
    val transformedDF = df.withColumn(tempColumn, explode(array(dmn4dqTreeUdf(struct(originalColumns: _*)))))
    names.foldLeft(transformedDF)((acc, n) => acc.withColumn(n._2, col(n._1))).drop(col(tempColumn))
  }


}

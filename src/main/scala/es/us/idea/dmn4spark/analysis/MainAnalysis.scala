package es.us.idea.dmn4spark.analysis

import java.io.FileInputStream

import es.us.idea.dmn4spark.analysis.extended.ExtendedDecisionDiagram
import es.us.idea.dmn4spark.dmn.DMNExecutor
import es.us.idea.dmn4spark.dmn.engine.SafeCamundaFeelEngineFactory
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object MainAnalysis {
  def main(args: Array[String]): Unit = {
    val path = "models/basic-diagram-3.dmn"

    val extended = ExtendedDecisionDiagram(path, true)

    extended.extendedDMNTables().foreach(extendedDMNTable => {
      println(s"------------- Extended rules for table ${extendedDMNTable.dmnTableSummary().name()} ------------")
      extendedDMNTable.extendedRules().foreach(extendedRule => {
        println(extendedRule)
      })

    })

    println("*** printing table summary ****")

    //println(extended.summary().toString)

  }
}

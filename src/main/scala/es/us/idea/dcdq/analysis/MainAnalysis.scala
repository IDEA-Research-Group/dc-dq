package es.us.idea.dcdq.analysis

import java.io.FileInputStream
import es.us.idea.dcdq.analysis.extended.ExtendedDecisionDiagram
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object MainAnalysis {
  def main(args: Array[String]): Unit = {
    //val path = "models/dmn4dq.dmn"
    val path = "models/basic-diagram-3.dmn"

    val extended = ExtendedDecisionDiagram(path, true)

    extended.extendedDMNTables().foreach(extendedDMNTable => {
      println(s"------------- Extended rules for table ${extendedDMNTable.dmnTableSummary().name()} w/ inputs ${extendedDMNTable.dmnTableSummary().inputAttributes()} ------------")
      extendedDMNTable.extendedRules().foreach(extendedRule => {
        println(extendedRule)
      })

    })

    println("*** printing table summary ****")

    //println(extended.summary().toString)

  }
}

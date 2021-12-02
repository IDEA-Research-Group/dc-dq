package es.us.idea.dcdq.diagnosis.spark

import es.us.idea.dcdq.junk.TestReadXML.bytes

import java.io.ByteArrayInputStream
import scala.xml.Elem

object DMNAnalysisUtils {

  def pruneDmnTables(dmnBytes: Array[Byte], tableNames: Seq[String]): Array[Byte] = {
    val xml = scala.xml.XML.load(new ByteArrayInputStream(dmnBytes))

    val decisions = xml.child.filterNot(node => {
      if(node.label == "decision")
        node.attribute("name").exists(attr => {
          attr.exists(x => tableNames.contains(x.toString()))
        })
      else false
    })

    val decisionFilteredIR = decisions.map(d => {
      if(d.label == "decision") {
        val decisionElem = d.asInstanceOf[Elem]
        val informationRequirements = decisionElem.child.filterNot(node => {
          if(node.label == "informationRequirement") {
            node.asInstanceOf[Elem].child.exists(rd => rd.label == "requiredDecision" && rd.attribute("href").exists(_.exists(x => tableNames.contains(x.toString().replace("#", "")))))
          } else false
        })
        decisionElem.copy(child = informationRequirements)
      } else d
    })

    val newXml = xml.copy(child = decisionFilteredIR)
    newXml.toString.getBytes
  }

}

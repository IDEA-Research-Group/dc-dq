package es.us.idea.dcdq.analysis

import es.us.idea.dcdq.analysis.extended.{ExtendedDecisionDiagram, ExtendedRule}

import scala.util.matching.Regex

object DMNAnalysisHelpers {

  implicit class GetDQComponents(extendedDecisionDiagram: ExtendedDecisionDiagram) {

    private def getExtendedRules(predicate: String => Boolean): List[ExtendedRule] =
      extendedDecisionDiagram.extendedDMNTables().filter(extTable => predicate(extTable.dmnTableSummary().name()))
        .flatMap(_.extendedRules())

    def getDUD(dudIdentifier: String = "DUD"): List[ExtendedRule] =
      getExtendedRules(x => x == dudIdentifier)

    def getDQA(dqaIdentifier: String = "DQA"): List[ExtendedRule] =
      getExtendedRules(x => x == dqaIdentifier)

    def getDQM(measurementPattern: Regex = "DQM\\(.+\\)".r): List[ExtendedRule] =
      getExtendedRules(x => measurementPattern.pattern.matcher(x).matches())

    def getBRDVs(brdvPattern: Regex = "^BR.*".r): List[ExtendedRule] =
      getExtendedRules(x => brdvPattern.pattern.matcher(x).matches())

  }

  implicit class CleanString(str: String) {
    def clean(): String = str.replaceAll("^\"|\"$", "").trim
  }

}

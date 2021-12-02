package es.us.idea.dcdq.analysis

import es.us.idea.dmn4spark.dmn.engine.SafeCamundaFeelEngineFactory
import es.us.idea.dmn4spark.dmn.executor.DMNExecutor

import java.io.FileInputStream
import org.apache.commons.io.IOUtils
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl
import org.camunda.bpm.dmn.engine.impl.spi.`type`.DmnTypeDefinition
import org.camunda.bpm.engine.variable.impl.context.SingleVariableContext
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl._

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object MainDMNAnalysis {
  def combinations[T](toCombine: List[List[T]]): List[List[T]] = {
    toCombine match {
      case Nil => List(Nil) // nil = list with zero elements in it
      case x :: y => for (elementX <- x; elementY <- combinations(y)) yield elementX :: elementY
    }
  }

  //case class DMNTableInfo(name: String, rules: List[Rule], outputs: List[String], outputValues: List[OutputValue], isLeafTable: Boolean)
  case class DMNTableInfo(name: String, rules: List[Rule], outputs: Outputs, isLeafTable: Boolean)

  case class Outputs(names: List[String], expressions: List[String], dmnType: DmnTypeDefinition)

  case class Rule(conditions: List[Condition], output: String, priority: Int)

  case class Condition(input: String, value: String) // input field

  var dmnTableInfos: List[DMNTableInfo] = List()
  // def recursiveInspection(decisions: Iterable[DmnDecision]): Unit = {
  //   decisions.map(x => {
  //
  //   })
  // }

  def main(args: Array[String]): Unit = {
    val path = "models/basic-diagram-3.dmn"
    //val path = "models/business-rules-2.dmn"

    //val outputMap


    val dmnExecutor = new DMNExecutor(IOUtils.toByteArray(new FileInputStream(path)))

    val modelInstasnce = dmnExecutor.dmnModelInstance
    val dmnEngine = dmnExecutor.dmnEngine

    val graph = dmnEngine.parseDecisionRequirementsGraph(modelInstasnce)

    val decisions = graph.getDecisions.asScala

    val fef = new SafeCamundaFeelEngineFactory().createInstance()

    // Examinar cada tabla
    for (decision <- decisions) {
      val decisionName = decision.getName
      val requiredDecisions = decision.getRequiredDecisions.asScala

      val decisionLogic = decision.getDecisionLogic
      val decisionLogicImpl = decisionLogic.asInstanceOf[DmnDecisionTableImpl] //aqui la tabla dmn
      val rules = decisionLogicImpl.getRules.asScala

      val inputs = decisionLogicImpl.getInputs.asScala.map(x => x.getName)

      // Los nombres de los outputs
      val outputNames = decisionLogicImpl.getOutputs.asScala.map(x => x.getName).toList
      // Los valores (expressions) de los outputs de esta tabla
      val outputExpressions = rules.flatMap(r => r.getConclusions.asScala).map(_.getExpression).toList
      val outputTypeDefinition = decisionLogicImpl.getOutputs.asScala.map(x => x.getTypeDefinition).head

      val tableOutputs = Outputs(outputNames, outputExpressions, outputTypeDefinition)


      //val outputs = decisionLogicImpl.getOutputs.asScala.map(_.getOutputName).mkString(", ") // cadena que representa todos los outputs separados por comas
      //println(tableName)
      println(s"Processing table $decisionName")
      println(s"With inputs: ${inputs.mkString(", ")}")
      println(s"Outouts: ${tableOutputs.expressions.mkString(", ")}")

      // comprobar si alguno de los inputs de esta tabla depende de algún output de jerarquía inferior
      val isLeafTable = inputs.flatMap(x => dmnTableInfos.flatMap(y => y.outputs.names).filter(y => y.contains(x))).isEmpty

      println(s"Is Leaf table? $isLeafTable")

      //val dmnTableInfo = DMNTableInfo(decisionName, List(), tableOutputs, isLeafTable)
      //dmnTableInfos = dmnTableInfo :: dmnTableInfos

      //if(!isLeafTable && decisionName == "DQAssessment"){

      // aqui vamos a guardar las reglas validas
      var validRules = List[Rule]()
      if (!isLeafTable) {
        println("Conditions for this table: ")

        // Vamos a examinar regla por regla
        for (ruleAndPriority <- rules.zipWithIndex) {
          val rule = ruleAndPriority._1
          val priority = ruleAndPriority._2

          val conditions = rule.getConditions.asScala.map(x => x.getExpression)
          val zippedConditions = inputs.zip(conditions) // (INPUT name, condition)
          println(zippedConditions)
          val conclusionExpression = rule.getConclusions.asScala.map(_.getExpression).head

          // aqui almacenamos las condiciones que satisfacen la regla
          //var validConditions = List[Condition]()
          // Para almacenar todas las condiciones que dan lugar a esta conclusión
          var listConditionsToConclusion = List[List[Condition]]()
          for (zippedCondition <- zippedConditions) {
            val inputName = zippedCondition._1
            val conditionExpression = zippedCondition._2
            println(s"CONDITION TO TEST: $inputName ---> $conditionExpression")

            // tomar todos los posibles valores para este input (todos los expressions existentes)
            val tableAssociatedToInputName = dmnTableInfos.filter(t => t.name == inputName).head
            val possibleInputExpressions = tableAssociatedToInputName.outputs.expressions
            // tomar datatype
            val possibleInputDataType = tableAssociatedToInputName.outputs.dmnType
            println(s"THESE ARE THE POSSIBLE INPUTS ${possibleInputExpressions.mkString(", ")}")

            var multiConditions = List[Condition]() // condiciones asociadas al mismo input
            // Se evalua cada posible input para esta condición
            if (conditionExpression == null) {
              for (inputValue <- possibleInputExpressions)
                multiConditions = Condition(inputName, inputValue) :: multiConditions
            } else {
              for (inputValue <- possibleInputExpressions) {
                // analizar el tipo de inputvalue
                // TODO todo esto se puede hacer antes
                val value = inputValue.replaceAll("^\"|\"$", "")

                println(possibleInputDataType)
                println(value)

                // TODO Check datatypes before
                val contextValue = possibleInputDataType.getTypeName match {
                  case "string" => new StringValueImpl(value)
                  case "boolean" => new BooleanValueImpl(value.toBoolean)
                  case "number" => new NumberValueImpl(value.toDouble)
                  case "double" => new DoubleValueImpl(value.toDouble)
                  case "integer" => new IntegerValueImpl(value.toInt)
                  case "long" => new LongValueImpl(value.toLong)
                  case _ => throw new Exception(s"Data type not supported. Error when analysing output datatype " +
                    s"${possibleInputDataType.getTypeName} with value $value.")
                }

                val fef = new SafeCamundaFeelEngineFactory().createInstance()
                //val context = SingleVariableContext.singleVariable(inputName, new StringValueImpl(inputValue.replaceAll("^\"|\"$", "")))
                val context = SingleVariableContext.singleVariable(inputName, contextValue)
                val evaluation = fef.evaluateSimpleUnaryTests(conditionExpression, inputName, context)
                println(s"Input => $inputValue, evaluation: $evaluation")
                if (evaluation) {
                  multiConditions = Condition(inputName, inputValue) :: multiConditions
                }

              }
            }

            // todo add multiconditions to the set of rules
            listConditionsToConclusion = multiConditions :: listConditionsToConclusion
            // println(evaluations)
            println("*** NEXT CONDITION ***")
          }
          val allRulesCombined = combinations(listConditionsToConclusion)
          validRules = allRulesCombined.map(x => Rule(x, conclusionExpression, priority)) ::: validRules

          //validRules = Rule(validConditions, conclusionExpression) :: validRules

          //println(validRules.mkString("\n"))
        }
      } // Fin ifLeafTable
      val dmnTableInfo = DMNTableInfo(decisionName, validRules, tableOutputs, isLeafTable)
      dmnTableInfos = dmnTableInfo :: dmnTableInfos

      //for(rule<-rules){
      //  val conditions = rule.getConditions.asScala.map(x => x.getExpression).mkString(", ")
      //  println(conditions)
      //  val conclusions = rule.getConclusions.asScala.map(x => x.getExpression).mkString(", ")
      //  println(conclusions)
      //  if(decisionName == "DQAssessment") {
      //    val expression = rule.getConditions.get(0).getExpression
      //    val fef = new SafeCamundaFeelEngineFactory().createInstance()
      //    val context = SingleVariableContext.singleVariable("Completeness", new StringValueImpl("complete"))
      //    //val evaluation = fef.evaluateSimpleUnaryTests(compiledDecision, context)
      //    val evaluation = fef.evaluateSimpleUnaryTests(expression, "Completeness", context)
      //    println(s"$expression - $evaluation")
      //  }
      //}

      //println(decisionLogicImpl)
      println("************ END TABLE ***********")
    }

    //println(dmnTableInfos.mkString("\n"))

    for (dmnTableInfo <- dmnTableInfos) {

      println(s"***** Table: ${dmnTableInfo.name}*****")
      println("rules: ")
      println(dmnTableInfo.rules.mkString("\n"))


    }


    println("finished")


  }

}

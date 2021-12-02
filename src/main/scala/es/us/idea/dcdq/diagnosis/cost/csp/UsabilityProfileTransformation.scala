package es.us.idea.dcdq.diagnosis.cost.csp

import es.us.idea.dcdq.diagnosis.cost.BRDVBasedCostModel
import es.us.idea.dcdq.diagnosis.graph.UsabilityProfile

import scala.jdk.CollectionConverters.seqAsJavaListConverter

object UsabilityProfileTransformation {
  case class UsabilityProfileCost(usabilityProfile: UsabilityProfile, cost: Int)

  case class KeyToDomain(key: String, domain: List[(String, Int)])

//  def findOptimalTargetUsabilityProfiles(
//                                          observedUsabilityProfile: UsabilityProfile,
//                                          targetUsabilityProfiles: List[UsabilityProfile],
//                                          brdvBasedCostModel: BRDVBasedCostModel
//                                        )/*: List[UsabilityProfileCost]*/ = {
//
//    // Solo se mapean los valores que estén en el dominio de este problema (observada + targets)
//    val dict = (targetUsabilityProfiles :+ observedUsabilityProfile)
//      .flatMap(_.getBRDVs())
//      .groupBy(_.name)
//      .map(x => (x._1, x._2.map(_.value).distinct.sorted.zipWithIndex))
//      .toList
//      .sortBy(_._1).map(kd => KeyToDomain(kd._1, kd._2))
//
//    println("Imprimiendo el observado: ")
//    println(observedUsabilityProfile.getBRDVs().toList.sortBy(_.name))
//
//    println("Imprimiendo todos los validos: ")
//    println(targetUsabilityProfiles.map(_.getBRDVs().toList.sortBy(_.name)).mkString(" --- "))
//
//    //observedUsabilityProfile.getBRDVs().toList.sortBy(_.name)
//
//    val adapter = COPModelAdapter(dict)
//
//    val domainBounding = adapter.domainBounding()
//
//
//    println(dict)
//    val observedUPCodified = adapter.codify(observedUsabilityProfile)
//    val targetsUPCodified = adapter.codify(targetUsabilityProfiles)
//    val costModelCodified = adapter.codifyCostModel(brdvBasedCostModel)
//    //println(targetsUPCodified.decodificationMap().get(targetsUPCodified.codifications()(0).hashCode()))
//    //println(costModelCodified.toList)
//    //println(observedUPCodified.toList)
//    //println(targetsUPCodified.codifications().map(_.toList).mkString(" ---- "))
//    //println(domainBounding.toList)
//
//    val codifiedSolution = COPFinal.runCop(observedUPCodified, targetsUPCodified.codifications(), costModelCodified, domainBounding)
//
//    val selectedUP = targetsUPCodified.decodify(codifiedSolution.getBrdv)
//    val cost = codifiedSolution.getCost
//
//    println(s"Selected UP: ${selectedUP.get.getBRDVs().toList.sortBy(_.name)}")
//    println(s"Cost: $cost")
//
//  }

  def findOptimalTargetUsabilityProfiles(
                                          observedUsabilityProfiles: List[UsabilityProfile],
                                          targetUsabilityProfiles: List[UsabilityProfile],
                                          brdvBasedCostModel: BRDVBasedCostModel
                                        ): List[Solution] /*(Long, Int)*/ /*: List[UsabilityProfileCost]*/ = {

    // Solo se mapean los valores que estén en el dominio de este problema (observada + targets)
    val dict = (targetUsabilityProfiles ::: observedUsabilityProfiles)
      .flatMap(_.getBRDVs())
      .groupBy(_.name)
      .map(x => (x._1, x._2.map(_.value).distinct.sorted.zipWithIndex))
      .toList
      .sortBy(_._1).map(kd => KeyToDomain(kd._1, kd._2))

    //println("Imprimiendo el observado: ")
    //println(observedUsabilityProfiles.getBRDVs().toList.sortBy(_.name))

    //println("Imprimiendo todos los validos: ")
    //println(targetUsabilityProfiles.map(_.getBRDVs().toList.sortBy(_.name)).mkString(" --- "))

    //observedUsabilityProfile.getBRDVs().toList.sortBy(_.name)

    val adapter = COPModelAdapter(dict)

    val domainBounding = adapter.domainBounding()


    println(dict)
    val observedUPCodified = adapter.codify(observedUsabilityProfiles)
    val targetsUPCodified = adapter.codify(targetUsabilityProfiles)
    //targetsUPCodified.codifications().map(_.toList).toList.foreach(println)
    val costModelCodified = adapter.codifyCostModel(brdvBasedCostModel)
    //println(targetsUPCodified.decodificationMap().get(targetsUPCodified.codifications()(0).hashCode()))
    //println(costModelCodified.toList)
    //println(observedUPCodified.codifications().toList.map(_.toList).mkString("\n"))
    //println(targetsUPCodified.codifications().map(_.toList).mkString(" ---- "))
    //println(domainBounding.toList)
    //println(observedUPCodified.codificationMap().map(x => (x._1.getBRDVs().toList.sortBy(_.name), x._2.toList)).mkString("\n"))
    //for(i <- observedUsabilityProfiles.indices) {
    //  val obs = observedUsabilityProfiles(i).getBRDVs().toList.sortBy(_.name)
    //  val obsCod = observedUPCodified.codifications()(i).toList
    //  println(" *************** ")
    //  println(s"OBS: $obs")
    //  println(s"COD: $obsCod")
    //  println(" *************** ")
    //}

    val R = observedUPCodified.codifications().map(x => 1)

    val start = System.currentTimeMillis()
    val codifiedSolution = COPMultiUPFinal.runCop(observedUPCodified.codifications(), targetsUPCodified.codifications(), R, costModelCodified, domainBounding)
    val totalCopTime = System.currentTimeMillis() - start

    val solUpsCodified = codifiedSolution.gettUps()

    val selectedUPs = solUpsCodified.map(x => targetsUPCodified.decodify(x.map(_.toInt)))

    val brdvCosts = codifiedSolution.getBrdvCosts
    val upCosts = codifiedSolution.getUpCosts
    val upBrdvCosts = codifiedSolution.getUpToBrdvCosts


    val solution = observedUsabilityProfiles.zipWithIndex.map{ case (obs, index) =>{
      val target = if(index < selectedUPs.length) selectedUPs(index) else None
      // val costs = if(index < upCosts.length) Some(upCosts(index)) else None

      val actions = obs.getBRDVs().flatMap(obsBrdv => {
        target.flatMap(_.getBRDVs().find(t => t.name == obsBrdv.name)) match {
          case Some(targetBrdv) => {
            brdvBasedCostModel
              .brdvCosts.find(_.name == obsBrdv.name)
              .flatMap(_.costs.find(c => c.observedValue == obsBrdv.value && c.targetValue == targetBrdv.value).map(_.cost)) match {
              case Some(cost) => Some(Action(obsBrdv.name, obsBrdv.value, targetBrdv.value, cost)) // The action is defined in the cost model
              case _ => None // The action is not defined!
            }
          }
          case _ => None // it is almost impossible
        }
      })
      Solution(observedUsabilityProfile = obs, targetUsabilityProfile = target, actions = actions.toList, totalCopTime)
    }}


/*
    for(i <- selectedUPs.indices) {
      val obs = observedUsabilityProfiles(i)
      val sel = selectedUPs(i).get

      println(" *************** ")
      println(s"OBS: ${obs.getBRDVs().toList.sortBy(_.name)}")
      println(s"SEL: ${sel.getBRDVs().toList.sortBy(_.name)}")
      println(s"OBS: ${observedUPCodified.codifications().map(_.toList).toList(i)}")
      println(s"SEL: ${solUpsCodified.map(_.toList).toList(i)}")
      println(s"COST: ${upCosts(i)}")
      println(s"COST_BRDV: ${upBrdvCosts(i).mkString(", ")}")
      println(" *************** ")
    }
*/


    //val cost = codifiedSolution.getCost
//
    //println(s"Selected UPs: \n${selectedUPs.map(_.get.getBRDVs().toList.sortBy(_.name)).mkString("\n")}")
    //println(s"Selected UPs: \n${selectedUPs.map(_.get.getDecision).mkString("\n")}")
    //println(s"Cost: $cost")
    //(totalCopTime, cost)

    solution
  }



}

package es.us.idea.dcdq.diagnosis.cost.csp

import es.us.idea.dcdq.diagnosis.cost.BRDVBasedCostModel
import es.us.idea.dcdq.diagnosis.cost.csp.codified.costModel.{BRDVCost, TransitionCost}
import es.us.idea.dcdq.diagnosis.cost.csp.UsabilityProfileTransformation.KeyToDomain
import es.us.idea.dcdq.diagnosis.graph.UsabilityProfile

import collection.JavaConverters._
import scala.collection.mutable

class COPModelAdapter(dictionary: List[KeyToDomain]) {

  def domainBounding(): Array[Int] = dictionary.map(_.domain.size - 1).toArray

  def codifyCostModel(costModel: BRDVBasedCostModel): Array[BRDVCost] = {
    dictionary.map(kd =>
      costModel.brdvCosts.find(_.name == kd.key).map(brdvCost =>
        new BRDVCost(
          // We only codify the necessary ObservedToTarget (only those present in the domain for this brdv)
          brdvCost.costs.map(ottc =>
            (kd.domain.find(_._1 == ottc.observedValue).map(_._2), kd.domain.find(_._1 == ottc.targetValue).map(_._2), ottc.cost, ottc.single)
          )
            .filter(t => t._1.isDefined && t._2.isDefined)
            .map(t => new TransitionCost(t._1.get, t._2.get, t._3, t._4)).asJava,
          brdvCost.default,
          brdvCost.single
        )
      ) match {
        case Some(brdvCost) => brdvCost
        case _ => throw new NoSuchElementException(s"No cost model specified for BRDV ${kd.key}")
      }
    ).toArray
  }

  def codify(usabilityProfile: UsabilityProfile): Array[Int] = {
    val thisUP = usabilityProfile.getBRDVs().toList.sortBy(_.name)

    dictionary.map(kd =>
      kd.domain.find(v => v._1 == {
        thisUP.find(_.name == kd.key).map(_.value) match {
          case Some(str) => str
          case _ => throw new NoSuchElementException(s"Error with usability profile ${usabilityProfile.getId}. Could not map " +
            s"a valid value for the key ${kd.key}.")
        }
      }) match {
        case Some(x) => x._2
        case _ => throw new NoSuchElementException(s"Error with usability profile ${usabilityProfile.getId}. Could not map " +
          s"a valid value for the key ${kd.key}.")
      }
    ).toArray
  }

  def codify(usabilityProfiles: List[UsabilityProfile]): UsabilityProfilesCodifications = {
    UsabilityProfilesCodifications(
      mutable.LinkedHashMap(
        usabilityProfiles.map(e => (e, this.codify(e))).toArray: _*
      )
    )
  }

}

object COPModelAdapter {
  def apply(dictionary: List[KeyToDomain]): COPModelAdapter = new COPModelAdapter(dictionary)
}

/***
 *
 * @param codificationMap Original UsabilityProfile objects codified into an Int Array
 * @param decodificationMap Hashcode of the Int Arrays with the original UsabilityProfile
 */
class UsabilityProfilesCodifications(codificationMap: mutable.LinkedHashMap[UsabilityProfile, Array[Int]], decodificationMap: mutable.LinkedHashMap[Int, UsabilityProfile]) {

  def codificationMap(): mutable.LinkedHashMap[UsabilityProfile, Array[Int]] = this.codificationMap

  def decodificationMap(): mutable.LinkedHashMap[Int, UsabilityProfile] = this.decodificationMap

  def decodify(codifiedUsabilityProfile: Array[Int]) =
    decodificationMap().get(codifiedUsabilityProfile.toSeq.hashCode())

  def codifications(): Array[Array[Int]] = {
    this.codificationMap().values.toArray
  }

  def decodifications(): List[UsabilityProfile] = this.decodificationMap().values.toList

}

object UsabilityProfilesCodifications {
  def apply(codificationMap: mutable.LinkedHashMap[UsabilityProfile, Array[Int]]): UsabilityProfilesCodifications =
    new UsabilityProfilesCodifications(codificationMap, codificationMap.map(e => (e._2.toSeq.hashCode(), e._1)))
}
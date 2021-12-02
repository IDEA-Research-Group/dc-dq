package es.us.idea.dcdq.streaming

import scala.beans.BeanProperty

//case class StreamingConfig(
//                            bootstrapServers: List[String],
//                            startingOffsets: Option[String],
//                            checkpointEvaluationLocation: Option[String],
//                            checkpointReparationLocation: Option[String],
//                            evaluationRequestTopic: String,
//                            evaluationResultTopic: String,
//                            reparationRequestTopic: String,
//                            reparationResultTopic: String
//                          )
//

class StreamingConfig {
  @BeanProperty var bootstrapServers: java.util.ArrayList[String] = new java.util.ArrayList()
  @BeanProperty var startingOffsets: String = ""
  @BeanProperty var checkpointEvaluationLocation: String = ""
  @BeanProperty var checkpointReparationLocation: String = ""
  @BeanProperty var evaluationRequestTopic: String = ""
  @BeanProperty var evaluationResultTopic: String = ""
  @BeanProperty var reparationRequestTopic: String = ""
  @BeanProperty var reparationResultTopic: String = ""


  override def toString = s"StreamingConfig(bootstrapServers=$bootstrapServers, startingOffsets=$startingOffsets, checkpointEvaluationLocation=$checkpointEvaluationLocation, checkpointReparationLocation=$checkpointReparationLocation, evaluationRequestTopic=$evaluationRequestTopic, evaluationResultTopic=$evaluationResultTopic, reparationRequestTopic=$reparationRequestTopic, reparationResultTopic=$reparationResultTopic)"
}
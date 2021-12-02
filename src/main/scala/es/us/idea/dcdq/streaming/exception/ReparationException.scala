package es.us.idea.dcdq.streaming.exception

import es.us.idea.dmn4spark.dmn.exception.DMN4SparkException


case class ReparationException(private val message: String = "",
                               private val cause: Throwable = None.orNull)
  extends DMN4SparkException(message, cause)

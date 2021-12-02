package es.us.idea.dcdq.diagnosis.spark.persistence

import com.mongodb.spark.config.WriteConfig

case class PersistenceConfiguration(dataStorage: WriteConfig, metadataStorage: WriteConfig)
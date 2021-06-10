package es.us.idea.dcdq

import es.us.idea.dcdq.dmn.DMNSparkEngine
import org.apache.spark.sql.DataFrame

object implicits {

  implicit class DMN4Spark(df: DataFrame) {
    def dmn: DMNSparkEngine = new DMNSparkEngine(df)
  }

}

package es.us.idea.dmn4spark.analysis

object Utils {

  //def main(args: Array[String]): Unit = {
  //  val allVals = List(
  //    List(Condition("Consistency", "consistent")),
  //    List(Condition("Accuracy", "accurate"), Condition("Accuracy", "very accurate")),
  //    List(Condition("Completeness", "complete enough"), Condition("Completeness", "adequately complete"))
  //  )
  //  val res = "enoughQuality"
  //  println(combinations(allVals).mkString("\n"))
  //}

  def combinations[T](toCombine: List[List[T]]): List[List[T]] = {
    toCombine match {
      case Nil => List(Nil) // nil = list with zero elements in it
      case x :: y => for (elementX <- x; elementY <- combinations(y)) yield elementX :: elementY
    }
  }

}

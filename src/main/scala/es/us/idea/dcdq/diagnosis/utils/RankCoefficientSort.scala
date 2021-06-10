package es.us.idea.dcdq.diagnosis.utils

case class SortableItem[T](o: T, rank: Int, coefficient: Double)

class RankCoefficientSort[T](l: List[SortableItem[T]]) {

  lazy val size: Int = elements().size
  lazy val upperRank: Int = l.map(_.rank).max
  lazy val lowerRank: Int = l.map(_.rank).min

  // Return 1 if i1 > i2 in this rank
  // Return 0 if i1 == i2 in this rank
  // Return -1 if i1 < i2 in this rank
  def compareInRank(rank: Int, i1: T, i2: T) = {
    val selectedRank = l.filter(_.rank == rank)

    selectedRank.find(_.o == i1).flatMap(is1 => {
      selectedRank.find(_.o == i2).map(is2 => {
        if(is1.coefficient > is2.coefficient) 1
        else if(is1.coefficient == is2.coefficient) 0
        else -1
      })
    }).getOrElse(0)
  }

  def compare(i1: T, i2: T): Int = {
    var c = 0
    var i = upperRank
    while(c == 0 && i > lowerRank) {
      c = compareInRank(i, i1, i2)
      i = i-1
    }
    c
  }

  def elements(): List[T] = l.map(_.o).distinct

  def sort(): List[(Int, T)] = {
    val sorted = elements().sortWith((o1, o2) => {
      if(compare(o1, o2)>=0) true else false
    })

    // Now, assign

    var res: List[(Int, T)] = List()
    var i1 = 0
    var i2 = 1
    var rank = sorted.size

    while(i2 < sorted.size) {
      val o1 = sorted(i1)
      val o2 = sorted(i2)

      val c = compare(o1, o2)
      res = res :+ (rank, o1)

      if(c != 0) rank = rank - 1
      i1 = i1 + 1
      i2 = i2 + 1
    }
    res = res :+ (rank, sorted(i1))

    val a = res.map(_._1).min
    val b = res.map(_._1).max
    val c = 1
    val d = c + (b-a)

    val f = RankCoefficientSort.shiftRange((a, b), (c, d))(_)

    res.map(x => (f(x._1), x._2))
  }
}

object RankCoefficientSort {

  def apply[T](l: List[SortableItem[T]]): RankCoefficientSort[T] = new RankCoefficientSort(l)

  private def shiftRange(from: (Int, Int), to: (Int, Int))(n: Int): Int = {
    val a = from._1
    val b = from._2
    val c = to._1
    val d = to._2
    c+((d-c)/(b-a))*(n-a) // FIXME could throw excetion due to dividing by 0
  }

  def main(args: Array[String]): Unit = {
    val toSort = List(
      SortableItem("A", 3, 0.5),
      SortableItem("B", 3, 0.4),
      SortableItem("C", 3, 0.0),
      SortableItem("A", 2, 0.4),
      SortableItem("B", 2, 0.5),
      SortableItem("C", 2, 0.3),
      SortableItem("A", 1, 0.1),
      SortableItem("B", 1, 0.1),
      SortableItem("C", 1, 0.7)
    )

    val rcs = new RankCoefficientSort(toSort)

    println(rcs.compare("B", "C"))
    println(rcs.sort)


  }



}

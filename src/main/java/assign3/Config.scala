package assign3

/**
 * Object for Project 3 specific configurations
 */
object Config {
  // Number of workers, specified by Project 2
  val NUM_WORKERS: Int = 2

  /**
   * List of number of portfolios to analyze where each index represents a rung.
   */
  val ladder: List[Int] = List(
    1000,
    2000,
    //    4000,
    //    8000,
    //    16000,
    //    32000,
    //    64000,
    //    100000
  )
}

package assign2

import org.apache.log4j.Logger
import parascale.actor.last.{Task, Worker}
import parascale.util._

import scala.collection.parallel.CollectionConverters._

/**
 * Spawns workers on the localhost.
 *
 * @author Ron.Coleman
 */
object PerfectWorker extends App {
  val LOG = Logger.getLogger(getClass)

  LOG.info("started")

  // Number of hosts in this configuration
  val nhosts = getPropertyOrElse("nhosts", 1)

  // One-port configuration
  val port1 = getPropertyOrElse("port", 8000)

  // If there is just one host, then the ports will include 9000 by default
  // Otherwise, if there are two hosts in this configuration, use just one
  // port which must be specified by VM options
  val ports = if (nhosts == 1) List(port1, 9000) else List(port1)

  // Spawn the worker(s).
  // Note: for initial testing with a single host, "ports" contains two ports.
  // When deploying on two hosts, "ports" will contain one port per host.
  for (port <- ports) {
    // Construction forks a thread which automatically runs the actor act
    // method.
    new PerfectWorker(port)
  }
}

/**
 * Template worker for finding a perfect number.
 *
 * @param port Localhost port this worker listens to
 * @author Ron.Coleman
 */
class PerfectWorker(port: Int) extends Worker(port) {

  import PerfectWorker._

  /**
   * Handles actor startup after construction.
   */
  override def act: Unit = {
    val name = getClass.getSimpleName
    LOG.info("started " + name + " (id=" + id + ")")

    // Wait for inbound messages as tasks.
    while (true) {
      receive match {
        // It gets the partition range info from the task payload then
        // spawns futures (or uses parallel collections) to analyze the
        // partition in parallel. Finally, when done, it replies
        // with the partial sum and the total estimated serial time for
        // this worker.
        case task: Task if task.payload.isInstanceOf[Partition] =>
          LOG.info("got partition = " + task + " calculating partial sum")

          // "receive" expects a Task so Result is stored in payload.
          // Must unpack and cast the payload.
          val partition: Partition = task.payload.asInstanceOf[Partition]

          val t0: Long = System.nanoTime()
          val partialSum = _parallelPartialSum(partition)
          val t1: Long = System.nanoTime()

          // Race condition, sleep avoids connection refused
          sleep(250L)

          // Send back partial sum, t0 and t1
          sender.send(Result(partialSum, t0, t1))
      }
    }
  }

  /**
   * Using parallel collections, calculates the partial sum over a partition of
   * the candidate receive from the dispatcher.
   *
   * @param partition - subset range of candidate sent by the Dispatcher
   * @return partial sum
   */
  def _parallelPartialSum(partition: Partition): Long = {
    // Calculate Partial Sum
    val RANGE = 1000000L
    val numPartitions = (partition.end / RANGE.toDouble).ceil.toInt

    // Start with a par collection which propagates through subsequent
    // calculations
    val partitions = (0L until numPartitions).par

    val ranges = for (k <- partitions) yield {
      val lower: Long = k * RANGE + partition.start
      val upper: Long = partition.end min ((k + 1) * RANGE + partition.start)
      (lower, upper)
    }

    // Ranges is a collection of 2-tuples of the lower-to-upper partition
    // bounds
    val sums = ranges.map {
      lowerUpper =>
        val (lower, upper) = lowerUpper
        _sumOfFactorsInRange(lower, upper, partition.candidate)
    }

    sums.sum
  }

  /**
   * Computes the sum of factors in a range using a loop which is robust for
   * large numbers.
   *
   * @param lower  Lower part of range
   * @param upper  Upper part of range
   * @param number Number
   * @return Sum of factors
   */
  def _sumOfFactorsInRange(lower: Long, upper: Long, number: Long): Long = {
    var index: Long = lower

    var sum = 0L

    while (index <= upper) {
      if (number % index == 0L)
        sum += index

      index += 1L
    }

    sum
  }
}

package assign3

import org.apache.log4j.Logger
import parabond.cluster.{Partition, check, checkReset, nanoSecondsToSeconds}
import parabond.util.MongoHelper
import parascale.actor.last.{Dispatcher, Task}
import parascale.util.getPropertyOrElse

import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

/**
 * Spawns a dispatcher to connect to multiple workers.
 *
 * @author Ron.Coleman
 */
object ParaDispatcher extends App {
  val LOG = Logger.getLogger(getClass)
  LOG.info("started")

  // For initial testing on a single host, use this socket.
  // When deploying on multiple hosts, use the VM argument,
  // -Dsocket=<ip address>:9000 which points to the second
  // host.
  val socket2 = getPropertyOrElse("socket", "localhost:9000")

  // This spawns a list of relay workers at the sockets
  new ParaDispatcher(List("localhost:8000", socket2))
}

/**
 * Dispatcher which dispatches partitions of groups of randomly selected
 * portfolios to price.
 *
 * @param sockets List of sockets
 * @author Alex.Badia
 */
class ParaDispatcher(sockets: List[String]) extends Dispatcher(sockets) {

  import Config._
  import ParaDispatcher._

  import java.util

  /**
   * Number of portfolios at an index in ladder.
   */
  var rungIndex: Int = 0

  /**
   * Handles actor startup after construction.
   */
  override def act: Unit = {
    LOG.info("sockets to workers = " + sockets)

    if (rungIndex == 0) {
      MongoHelper.hush()
      printReportHeader()
    }

    val startTn: Long = System.nanoTime()

    val rung: Int = ladder(rungIndex)

    // Resets the check portfolio prices
    val portfolioIds: List[Int] = checkReset(rung, 0)

    workers.head.send(Partition(rung / 2, 0))
    workers(1).send(Partition(rung / 2, rung / 2))

    // Partial runtimes from workers
    val partialT1s: ListBuffer[Long] = ListBuffer[Long]()

    // Wait for 2 replies, one from each worker
    while (partialT1s.length < 2) {
      receive match {
        case task: Task if task.payload.isInstanceOf[Result] =>
          LOG.info("received reply with result" + task)

          // "receive" expects a Task so Result is stored in payload.
          // Must unpack and cast the payload.
          val result: Result = task.payload.asInstanceOf[Result]
          partialT1s += result.partialT1
      }
    }

    // Portfolios that were found not to be priced
    val missed = check(portfolioIds)

    // If the Dispatch/Worker architecture ran correctly all portfolios
    // should've been priced.
    if (missed.nonEmpty) {
      // Logged this way purposely to avoid the Collection name when logging
      // the array.
      LOG.error(
        String.format(
          "Not all Portfolios were priced: %s",
          util.Arrays.toString(missed.toArray)))
    }

    val endTn: Long = System.nanoTime()

    // Performance statistics
    val n: Double = Runtime.getRuntime.availableProcessors()
    val t1: Double = partialT1s.sum seconds
    val tn: Double = (endTn - startTn) seconds
    val r: Double = if (t1 <= 0) 0 else t1 / tn
    val e: Double = if (t1 <= 0) 0 else r / n
    printReportRecord2(rung, missed.length, t1, tn, r, e)

    // Repeat for all rungs in the ladder
    if (rungIndex < ladder.length - 1) {
      rungIndex += 1
      this.act
    }
  }

  /**
   * Prints Project 2 Report Header to console.
   */
  def printReportHeader(): Unit = {
    println("ParaBond Analysis")
    println("By Alex Badia")
    println(LocalDateTime.now().format(
      DateTimeFormatter.ofPattern("dd-MM-yyyy. HH:mm:ss")))
    println(
      "Runtime Max Memory: "
        + Runtime.getRuntime.maxMemory() / 1_000_000 + " MB")
    println(getPropertyOrElse("node", "Basic Node"))
    println("Workers: " + NUM_WORKERS)
    println(
      "Hosts: "
        + getPropertyOrElse("host", InetAddress.getLocalHost.getHostAddress)
        + " (dispatcher), "
        + workers(0).forwardAddr + " (worker). "
        + workers(1).forwardAddr + " (worker), "
        + MongoHelper.getHost + " (mongo)")
    println("Host Cores: " + Runtime.getRuntime.availableProcessors())
    println("Mongo Cores: 16")
    printf(
      "%-7s%-12s%-12s%-12s%-12s%-12s\n",
      "N",
      "Missed",
      "T1(s)",
      "TN(s)",
      "R",
      "e")
  }

  /**
   * Prints is perfect number output for candidate to console.
   *
   * @param n      - Number if portfolios that should've been priced
   * @param missed - number of portfolios not priced
   * @param t1     - runtime on a uniprocessor
   * @param tn     - runtime on a system with N processors
   * @param r      - speedup
   * @param e      - efficiency
   */
  def printReportRecord2(n: Int, missed: Int, t1: Double, tn: Double, r: Double, e: Double): Unit = {
    printf("%-7d%-12d%-12.2f%-12.2f%-12.2f%-12.2f\n", n, missed, t1, tn, r, e)
  }
}
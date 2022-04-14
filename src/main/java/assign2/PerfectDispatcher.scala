package assign2

import org.apache.log4j.Logger
import parascale.actor.last.{Dispatcher, Task}
import parascale.future.perfect.candidates
import parascale.util._

import java.net.InetAddress
import scala.collection.mutable.ListBuffer

/**
 * Spawns a dispatcher to connect to multiple workers.
 *
 * @author Ron.Coleman
 */
object PerfectDispatcher extends App {
  val LOG = Logger.getLogger(getClass)
  LOG.info("started")

  // For initial testing on a single host, use this socket.
  // When deploying on multiple hosts, use the VM argument,
  // -Dsocket=<ip address>:9000 which points to the second
  // host.
  val socket2 = getPropertyOrElse("socket", "localhost:9000")

  // Construction forks a thread which automatically runs the actor act method.
  new PerfectDispatcher(List("localhost:8000", socket2))
}

/**
 * Template dispatcher which tests readiness of the host(s)
 *
 * @param sockets List of sockets
 * @author Ron.Coleman
 */
class PerfectDispatcher(sockets: List[String]) extends Dispatcher(sockets) {

  import PerfectDispatcher._

  /**
   * Keeps track of current candidate in the list of candidates.
   */
  var currentCandidateIndex = 0

  /**
   * Handles actor startup after construction.
   */
  def act: Unit = {
    if (currentCandidateIndex == 0) {
      diagnosticsAssign2Header()
    }

    val startTn: Long = System.nanoTime()
    val candidate: Long = candidates(this.currentCandidateIndex)

    LOG.info("sockets to workers = " + sockets)

    // Create the partition info and put it in two separate messages, one for
    // each worker. Hard coded for sake of demonstration...
    LOG.info("sending message to worker " + workers.head.id)
    workers.head.send(Partition(1, candidate / 2, candidate))

    LOG.info("sending message to worker " + workers.last.id)
    workers.last.send(Partition(candidate / 2 + 1, candidate, candidate))

    // Partial sums for from workers
    val sums: ListBuffer[Long] = ListBuffer[Long]()

    // Partial runtime from workers
    val partialRuntimes: ListBuffer[Long] = ListBuffer[Long]()

    // Wait for 2 replies, one from each worker
    while (sums.length < 2) {
      // This while loop waits for two replies, one from each worker.
      // The result, that is, the partial sum and the elapsed times are
      // in the payload as a Result class.
      receive match {
        case task: Task if task.payload.isInstanceOf[Result] =>
          LOG.info("received reply with result" + task)

          // "receive" expects a Task so Result is stored in payload.
          // Must unpack and cast the payload.
          val result: Result = task.payload.asInstanceOf[Result]
          sums += result.sum
          partialRuntimes += result.t1 - result.t0
      }
    }

    // Answer if candidate is perfect
    val sumPartialSums = sums.sum
    val isPerfect: Boolean = 2L * candidate == sumPartialSums

    val endTn: Long = System.nanoTime()

    // Runtime calculations and diagnostics
    val n: Double = Runtime.getRuntime.availableProcessors()
    val t1: Double = partialRuntimes.sum / 1_000_000_000
    val tn: Double = ((endTn - startTn) / 1_000_000_000).toDouble
    val r: Double = if (t1 <= 0) 0 else t1 / tn
    val e: Double = if (t1 <= 0) 0 else r / n
    diagnosticAssign2Record(candidate, isPerfect, t1, tn, r, e)

    // Repeat for all candidates
    if (this.currentCandidateIndex < candidates.length - 1) {
      this.currentCandidateIndex += 1
      this.act
    }
  }

  /**
   * Prints Project 2 Report Header to console.
   */
  def diagnosticsAssign2Header(): Unit = {
    println("PNF Using Parallel Collections")
    println("by Alex Badia")
    println(java.time.LocalDate.now)
    println("Max Memory: " + Runtime.getRuntime.maxMemory() / 1_000_000 + " MB")
    println("Cores: " + Runtime.getRuntime.availableProcessors())
    println("Dispatcher: " + getPropertyOrElse("host", InetAddress.getLocalHost.getHostAddress))
    println("Workers: " + workers.head.forwardAddr + ", " + workers.last.forwardAddr)
    printf("%-15s%-12s%-12s%-12s%-12s%-12s\n", "Candidate", "Perfect", "T1(s)", "TN(s)", "R", "e")
  }

  /**
   * Prints is perfect number output for candidate to console.
   *
   * @param candidate - number to see if it is perfect or not
   * @param isPerfect - true if the candidate is perfect
   * @param t1        - runtime on a uniprocessor
   * @param tn        - runtime on a system with N processors
   * @param r         - speedup
   * @param e         - efficiency
   */
  def diagnosticAssign2Record(candidate: Long, isPerfect: Boolean, t1: Double, tn: Double, r: Double, e: Double): Unit = {
    printf("%-15d%-12b%-12.2f%-12.2f%-12.2f%-12.2f\n", candidate, isPerfect, t1, tn, r, e)
  }
}


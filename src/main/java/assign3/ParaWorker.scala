package assign3

import org.apache.log4j.Logger
import parabond.cluster.{Analysis, BasicNode, CoarseGrainedNode, FineGrainedNode, MemoryBoundNode, Node, Partition}
import parascale.actor.last.{Task, Worker}
import parascale.util.getPropertyOrElse

import scala.language.postfixOps

/**
 * Spawns workers on the localhost.
 *
 * @author Ron.Coleman
 */
object ParaWorker extends App {
  val LOG = Logger.getLogger(getClass)
  LOG.info("started")

  //a. If worker running on a single host, spawn two workers
  //   else spawn one worker.
  val nhosts = getPropertyOrElse("nhosts", 1)

  //Set the node, default to basic node
  val prop =
    getPropertyOrElse("node", "parabond.cluster.BasicNode")
  val clazz = Class.forName(prop)

  // One-port configuration
  val port1 = getPropertyOrElse("port", 8000)

  // If there is 1 host, then ports include 9000 by default
  // Otherwise, if there are two hosts in this configuration,
  // use just one port which must be specified by VM options
  val ports =
  if (nhosts == 1) List(port1, 9000) else List(port1)

  // Spawn the worker(s).
  // Note: for initial testing with a single host, "ports"
  // contains two ports. When deploying on two hosts, "ports"
  // will contain one port per host.
  for (port <- ports) {
    // Start a new worker.
    new ParaWorker(port)
  }
}

/**
 * Worker for finding pricing portfolios.
 *
 * @param port Localhost port this worker listens to
 * @author Alex.badis
 */
class ParaWorker(port: Int) extends Worker(port) {

  import ParaWorker._

  /**
   * Handles actor startup after construction.
   */
  override def act: Unit = {
    while (true) {
      receive match {
        case task: Task if task.payload.isInstanceOf[Partition] =>
          LOG.info("received reply with result" + task)
          val partition: Partition = task.payload.asInstanceOf[Partition]
          val analysis: Analysis = getNode(partition).analyze()
          val partialT1 = analysis.results.foldLeft(0L) { (dt, job) =>
            // Maintain Long, converting to seconds too early looses too
            // much data.
            dt + (job.result.t1 - job.result.t0)
          }
          sender.send(Result(partialT1))
      }
    }
  }

  /**
   * ParaBond's Node DOES NOT have analyze method that accepts a Partition as
   * a paramter. Looking at the source code, the Node constructer accepts the
   * Partition as an argument instead.
   *
   * @param partition - a subset of a randomly select "rung" number of nodes
   * @return BasicNode | CoarseGrainedNode | FineGrainedNode
   */
  def getNode(partition: Partition): Node = {
    val node: String = getPropertyOrElse("node", "parabond.cluster.BasicNode")
    if (node.equals("parabond.cluster.BasicNode")) {
      new BasicNode(partition)
    } else if (node.equals("parabond.cluster.MemoryBoundNode")) {
      new MemoryBoundNode(partition)
    } else if (node.equals("parabond.cluster.CoarseGrainNode")) {
      new CoarseGrainedNode(partition)
    } else if (node.equals("parabond.cluster.FineGrainNode")) {
      new FineGrainedNode(partition)
    } else {
      new BasicNode(partition)
    }
  }
}

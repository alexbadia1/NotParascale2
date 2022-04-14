package assign2

/**
 * This is the message sent from Worker to the Dispatcher
 * It MUST be serializable to be transmitted as a POJO.
 *
 * @author A. Badia
 * @param sum  sum of the partial range
 * @param t0  start time
 * @param t1 end time
 */
case class Result(sum: Long, t0: Long, t1: Long) extends Serializable

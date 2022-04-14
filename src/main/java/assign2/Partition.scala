package assign2

/**
 * This is the message sent from Dispatcher to the Worker
 * It MUST be serializable to be transmitted as a POJO.
 *
 * @author A. Badia
 * @param start  Lower part of range
 * @param end  Upper part of range
 * @param candidate Number
 */
case class Partition(start: Long, end: Long, candidate: Long)
  extends Serializable
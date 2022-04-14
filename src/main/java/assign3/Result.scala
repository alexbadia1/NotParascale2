package assign3

/**
 * This is the message sent from Worker to the Dispatcher
 * It MUST be serializable to be transmitted as a POJO.
 *
 * @author A. Badia
 * @param partialT1 end time
 */
case class Result(partialT1: Long) extends Serializable

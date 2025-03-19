package lukekafka.producer

import kyo.*
import org.apache.kafka.clients.producer.RecordMetadata

// TODO Result or Abort?
case class BrokerAck(recordMetadata: Chunk.Indexed[RecordMetadata] < (Async & Abort[Throwable]))

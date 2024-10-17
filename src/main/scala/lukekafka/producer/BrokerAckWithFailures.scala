package lukekafka.producer

import kyo.*
import org.apache.kafka.clients.producer.RecordMetadata

case class BrokerAckWithFailures(recordMetadata: Chunk.Indexed[Result[Throwable, RecordMetadata]] < Async)

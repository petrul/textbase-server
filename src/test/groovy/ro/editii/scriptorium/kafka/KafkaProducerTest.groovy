package ro.editii.scriptorium.kafka

import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult

import java.util.concurrent.CompletableFuture

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class KafkaProducerTest {

    @Test
    void waitsForKafkaToAcknowledgeTheMessage() {
        def template = mock(KafkaTemplate)
        def result = mock(SendResult)
        def metadata = mock(RecordMetadata)
        when(result.recordMetadata).thenReturn(metadata)
        when(template.send('topic', 'payload'))
                .thenReturn(CompletableFuture.completedFuture(result))

        new KafkaProducer(template).send('topic', 'payload')

        verify(template).send('topic', 'payload')
    }

    @Test
    void reportsKafkaDeliveryFailure() {
        def template = mock(KafkaTemplate)
        def failed = new CompletableFuture<SendResult<String, String>>()
        failed.completeExceptionally(new RuntimeException('broker unavailable'))
        when(template.send('topic', 'payload')).thenReturn(failed)

        def error = assertThrows(IllegalStateException) {
            new KafkaProducer(template).send('topic', 'payload')
        }

        assertEquals('Could not send Kafka message to topic topic', error.message)
    }

    @Test
    void declaresDistinctTopicsForImportsAndReimports() {
        def props = new KafkaProps()
        props.newOpusImportedTopicName = 'new-topic'
        props.opusReimportedTopicName = 'reimport-topic'
        def configuration = new KafkaConfiguration(kafkaProps: props)

        assertEquals('new-topic', configuration.newOpus().name())
        assertEquals('reimport-topic', configuration.reimportOpus().name())
    }
}

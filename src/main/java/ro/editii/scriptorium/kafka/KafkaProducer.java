package ro.editii.scriptorium.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Component
@Log4j2
public class KafkaProducer {

    private static final long SEND_TIMEOUT_SECONDS = 30;

    final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String topic, String data) {
        try {
            final var result = this.kafkaTemplate.send(topic, data)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Kafka message sent to topic {}, partition {}, offset {}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending Kafka message to topic " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not send Kafka message to topic " + topic, e);
        }
    }

    public void sendAsJson(String topic, Object object) {
        final var om = new JsonMapper();
        final var json = om.writeValueAsString(object);
        this.send(topic, json);
    }

}

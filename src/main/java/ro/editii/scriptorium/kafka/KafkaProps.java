package ro.editii.scriptorium.kafka;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class KafkaProps {

    @Value("${kafka.newOpusImportedTopic:textbase_newOpusImportedTopic}")
    String newOpusImportedTopicName;

    @Value("${kafka.opusReimportedTopic:textbase_opusReimportedTopic}")
    String opusReimportedTopicName;

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    String bootstrapServers;
}

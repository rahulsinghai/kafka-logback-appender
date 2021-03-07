package io.github.rahulsinghai;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Properties;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class KafkaLogbackAppender extends KafkaLogbackConfigBase<ILoggingEvent> {

    protected Producer<byte[], byte[]> producer = null;

    public void start() {
        super.start();
        Properties props = getProducerProperties();
        this.producer = createKafkaProducer(props);
        addInfo("Kafka producer connected to " + brokerList);
        addInfo("Logging for topic: " + topic);
    }

    @Override
    public void stop() {
        super.stop();
        if (producer != null) {
            producer.close();
        }
    }

    protected void append(ILoggingEvent event) {
        byte[] message = null;
        if (encoder != null) {
            encoder.setContext(getContext());
            message = encoder.encode(event);
        } else {
            message = event.getMessage().getBytes();
        }

        final ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, message);
        Future<RecordMetadata> response = producer.send(record);

        /*try {
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        addError("Error waiting for Kafka response", exception);
                    }
                }
            });
        } catch (BufferExhaustedException | TimeoutException e) {
            addError("Error waiting for Kafka response", e);
        }*/

        if (syncSend) {
            try {
                response.get();
            } catch (Exception ex) {
                addError("Error waiting for Kafka response", ex);
            }
        }
    }

    protected Producer<byte[], byte[]> createKafkaProducer(Properties props) {
        return new KafkaProducer<>(props);
    }
}

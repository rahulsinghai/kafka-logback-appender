package io.github.rahulsinghai.helper;

import io.github.rahulsinghai.KafkaLogbackAppender;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.utils.Crc32;

import java.util.*;
import java.util.concurrent.*;

public class InspectableKafkaLogbackAppender extends KafkaLogbackAppender {

    private Properties mockProperities = null;

    public InspectableKafkaLogbackAppender(Properties props) {
        mockProperities = props;
    }

    @Override
    protected Properties getProducerProperties() {
        // Set mandatory properties for mock implementations
        String topic = "Test";
        setBrokerList("localhost:9092");

        Properties props = super.getProducerProperties();
        if (mockProperities != null) {
            props.putAll(mockProperities);

            if (mockProperities.get(topic) != null) {
                setTopic((String)mockProperities.get(topic));
            }
        }

        return props;
    }

    @Override
    protected Producer createKafkaProducer(Properties props) {
        return new MockKafkaProducer();
    }

    /**
     * Accessor to the mock implementation of the Producer for verifying actions.
     *
     * @return Mock producer
     */
    public MockKafkaProducer getMockProducer() {
        return (MockKafkaProducer) producer;
    }



    /**
     * Mock implementation of a Kafka Producer which allows controlling and
     * manipulating its state and behavior.
     */
    public class MockKafkaProducer<K, V> implements Producer<byte[], byte[]> {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // Queue with the values sent to Kafka since the last reset
        private List queue = null;

        // Delay until a sent entry is acknowledged
        private long ackDelay = 0;

        public MockKafkaProducer() {
            reset();
        }

        @Override
        public void initTransactions() {

        }

        @Override
        public void beginTransaction() throws ProducerFencedException {

        }

        @Override
        public void sendOffsetsToTransaction(
            Map<TopicPartition, OffsetAndMetadata> map,
            String s) throws ProducerFencedException {

        }

        @Override
        public void commitTransaction() throws ProducerFencedException {

        }

        @Override
        public void abortTransaction() throws ProducerFencedException {

        }

        @Override
        public Future<RecordMetadata> send(final ProducerRecord<byte[], byte[]> record) {

            return executorService.submit(new Callable<RecordMetadata>() {
                public RecordMetadata call() throws Exception {
                    if (ackDelay > 0) {
                        try {
                            Thread.sleep(ackDelay);
                        } catch (InterruptedException ex) {
                            // continue
                        }
                    }
                    queue.add(record);

                    return new RecordMetadata(new TopicPartition(getTopic(), 0), queue.size(), queue.size(),
                            System.currentTimeMillis(), Crc32.crc32(record.value()), record.key().length, record.value().length);
                }
            });
        }

        public Future<RecordMetadata> send(ProducerRecord producerRecord, Callback callback) {
            return null;
        }

        // Non-implemented interface methods
        public void close(long l, TimeUnit timeUnit) { }
        public void close() { }
        public void flush() { }
        public List<PartitionInfo> partitionsFor(String s) { return null; }
        public Map<MetricName, ? extends Metric> metrics() { return null; }

        // Mock methods
        public void reset() {
            ackDelay = 0;
            queue = Collections.synchronizedList(new LinkedList());
        }

        public void setAckDelay(long delay) {
            this.ackDelay = delay;
        }

        public String getTopic() { return topic; }

        /**
         * Return a copy of the queue with the currently sent Producer records
         * @return list of ProducerRecords
         */
        public List getQueue() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // continue
            }
            LinkedList listSnapshot = new LinkedList();
            Iterator it=queue.iterator();
            while (it.hasNext()){
                listSnapshot.add(it.next());
            }
            return listSnapshot;
        }
    }
}

package io.github.rahulsinghai;

import static ch.qos.logback.core.CoreConstants.CODES_URL;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_KERBEROS_SERVICE_NAME;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_TYPE_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import java.util.Properties;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.serialization.ByteArraySerializer;

/**
 * Base class holding all configuration parameters of the appender (the underlying KafkaProducer
 * and the appender itself).
 */
public abstract class KafkaLogbackConfigBase<I extends DeferredProcessingAware> extends UnsynchronizedAppenderBase<ILoggingEvent> {
    protected String brokerList = null;
    protected String topic = null;
    protected String compressionType = null;
    protected String securityProtocol = null;
    protected String sslTruststoreLocation = null;
    protected String sslTruststorePassword = null;
    protected String sslKeystoreType = null;
    protected String sslKeystoreLocation = null;
    protected String sslKeystorePassword = null;
    protected String saslKerberosServiceName = null;
    protected String saslMechanism;
    protected String clientJaasConfPath = null;
    protected String clientJaasConf;
    protected String kerb5ConfPath = null;
    protected String maxBlockMs = null;
    protected String keySerializerClass = null;
    protected String valueSerializerClass = null;

    protected int retries = Integer.MAX_VALUE;
    protected int requiredNumAcks = 1;
    protected int deliveryTimeoutMs = 120000;
    protected int lingerMs = 0;
    protected int batchSize = 16384;
    protected boolean ignoreExceptions = true;
    protected boolean syncSend = false;

    protected Encoder<ILoggingEvent> encoder;

    /**
     * Return the configuration properties for the KafkaProducer.
     * @return Properties with configured producer params
     */
    protected Properties getProducerProperties() {
        // check for config parameter validity
        Properties props = new Properties();
        if (brokerList != null)
            props.put(BOOTSTRAP_SERVERS_CONFIG, brokerList);
        if (props.isEmpty())
            throw new ConfigException("The bootstrap servers property should be specified");
        if (topic == null)
            throw new ConfigException("Topic must be specified by the Kafka Logback appender");
        if (compressionType != null)
            props.put(COMPRESSION_TYPE_CONFIG, compressionType);
        props.put(ACKS_CONFIG, Integer.toString(requiredNumAcks));
        props.put(RETRIES_CONFIG, retries);
        props.put(DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);

        if (securityProtocol != null) {
            props.put(SECURITY_PROTOCOL_CONFIG, securityProtocol);
        }

        if (securityProtocol != null && securityProtocol.contains("SSL") && sslTruststoreLocation != null && sslTruststorePassword != null) {
            props.put(SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            props.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);

            if (sslKeystoreType != null && sslKeystoreLocation != null && sslKeystorePassword != null) {
                props.put(SSL_KEYSTORE_TYPE_CONFIG, sslKeystoreType);
                props.put(SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
                props.put(SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
            }
        }
        if (securityProtocol != null && securityProtocol.contains("SASL") && saslKerberosServiceName != null && clientJaasConfPath != null) {
            props.put(SASL_KERBEROS_SERVICE_NAME, saslKerberosServiceName);
            System.setProperty("java.security.auth.login.config", clientJaasConfPath);
        }
        if (kerb5ConfPath != null) {
            System.setProperty("java.security.krb5.conf", kerb5ConfPath);
        }
        if (saslMechanism != null) {
            props.put(SASL_MECHANISM, saslMechanism);
        }
        if (clientJaasConf != null) {
            props.put(SASL_JAAS_CONFIG, clientJaasConf);
        }
        if (maxBlockMs != null) {
            props.put(MAX_BLOCK_MS_CONFIG, maxBlockMs);
        }

        if (keySerializerClass != null) {
            props.put(KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
        } else {
            props.put(KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        }
        if (valueSerializerClass != null) {
            props.put(VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
        } else {
            props.put(VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        }

        return props;
    }

    // Producer parameters

    public void setBrokerList(String brokerList) {
        this.brokerList = brokerList;
    }

    public void setRequiredNumAcks(int requiredNumAcks) {
        this.requiredNumAcks = requiredNumAcks;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setMaxBlockMs(String maxBlockMs) {
        this.maxBlockMs = maxBlockMs;
    }

    public void setSyncSend(boolean syncSend) {
        this.syncSend = syncSend;
    }

    public void setKeySerializerClass(String clazz) { this.keySerializerClass = clazz; }

    public void setValueSerializerClass(String clazz) { this.valueSerializerClass = clazz; }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public void setSslTruststoreLocation(String sslTruststoreLocation) {
        this.sslTruststoreLocation = sslTruststoreLocation;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public void setSslKeystoreType(String sslKeystoreType) {
        this.sslKeystoreType = sslKeystoreType;
    }

    public void setSslKeystoreLocation(String sslKeystoreLocation) {
        this.sslKeystoreLocation = sslKeystoreLocation;
    }

    public void setSaslKerberosServiceName(String saslKerberosServiceName) {
        this.saslKerberosServiceName = saslKerberosServiceName;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public void setClientJaasConf(final String clientJaasConf) {
        this.clientJaasConf = clientJaasConf;
    }

    public void setClientJaasConfPath(String clientJaasConfPath) {
        this.clientJaasConfPath = clientJaasConfPath;
    }

    public void setKerb5ConfPath(String kerb5ConfPath) {
        this.kerb5ConfPath = kerb5ConfPath;
    }

    // Appender configuration parameters

    public void setLayout(Layout<ILoggingEvent> layout) {
        addWarn("This appender no longer admits a layout as a sub-component, set an encoder instead.");
        addWarn("To ensure compatibility, wrapping your layout in LayoutWrappingEncoder.");
        addWarn("See also " + CODES_URL + "#layoutInsteadOfEncoder for details");
        LayoutWrappingEncoder<ILoggingEvent> lwe = new LayoutWrappingEncoder<ILoggingEvent>();
        lwe.setLayout(layout);
        lwe.setContext(context);
        this.encoder = lwe;
    }

    public void setDeliveryTimeoutMs(int deliveryTimeoutMs) {
        this.deliveryTimeoutMs = deliveryTimeoutMs;
    }

    public void setLingerMs(int lingerMs) {
        this.lingerMs = lingerMs;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) { this.encoder = encoder; }
}

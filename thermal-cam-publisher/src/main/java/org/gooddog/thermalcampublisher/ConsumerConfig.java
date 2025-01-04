package org.gooddog.thermalcampublisher;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.gooddog.thermal_cam.ThermalCam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
@EnableKafka
public class ConsumerConfig {
  @Bean("myListenerFactory")
  ConcurrentKafkaListenerContainerFactory<String, ThermalCam.ThermalCamFrame>
      kafkaListenerContainerFactory(
          ConsumerFactory<String, ThermalCam.ThermalCamFrame> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, ThermalCam.ThermalCamFrame> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
  }

  @Bean
  public ConsumerFactory<String, ThermalCam.ThermalCamFrame> consumerFactory(
      @Value("${spring.application.kafka.bootstrap_servers}") String kafkaBootstrapServers,
      @Value("${spring.application.kafka.schema_registry_url}") String schemaRegistry,
      @Value("${spring.application.kafka.thermal_cam.consumer_group}") String consumerGroupId,
      DefaultKafkaConsumerFactoryCustomizer customizer) {
    DefaultKafkaConsumerFactory<String, ThermalCam.ThermalCamFrame> consumerFactory =
        new DefaultKafkaConsumerFactory<>(
            consumerProps(kafkaBootstrapServers, schemaRegistry, consumerGroupId));
    customizer.customize(consumerFactory);
    return consumerFactory;
  }

  private Map<String, Object> consumerProps(
      String kafkaBootstrapServers, String schemaRegistry, String consumerGroupId) {
    Map<String, Object> props = new HashMap<>();

    // see
    // https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-protobuf.html
    props.put(
        org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        kafkaBootstrapServers);
    props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
    props.put(
        org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringDeserializer.class);
    props.put(
        org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer.class);
    props.put(SPECIFIC_PROTOBUF_VALUE_TYPE, ThermalCam.ThermalCamFrame.class);
    props.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);
    props.put(
        org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return props;
  }
}

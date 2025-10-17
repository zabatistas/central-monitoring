# Message Integration Guide: RabbitMQ and Kafka

This guide provides comprehensive instructions for applications to connect to the Central Monitoring System's RabbitMQ and Kafka messaging infrastructure.

## Overview

The Central Monitoring System publishes metrics data to both RabbitMQ and Kafka when applications are monitored. External applications can connect as consumers to receive these metrics in real-time.

## Infrastructure Configuration

### RabbitMQ Configuration
- **Host**: `82.223.13.241` (or `rabbitmq` within Docker network)
- **Port**: `5672`
- **Management UI**: `15672`
- **Username**: `guest`
- **Password**: `guest`
- **Exchange**: `metrics.exchange` (TopicExchange)
- **Queue**: `metrics.queue`
- **Routing Key Pattern**: `metrics.#` (all messages with routing keys starting with "metrics.")

### Kafka Configuration
- **Bootstrap Servers**: `kafka:9092` (or `82.223.13.241:9092` from host)
- **Topic**: `metrics-topic`
- **Consumer Group**: `metrics-group`
- **Partitions**: 3
- **Replication Factor**: 1

---

## Integration Methods

### 1. Spring Boot Applications

#### A. RabbitMQ Integration

##### Dependencies (Maven)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

##### Application Configuration (`application.yml`)
```yaml
spring:
  rabbitmq:
    host: 82.223.13.241  # or rabbitmq if running in Docker
    port: 5672
    username: guest
    password: guest
```

##### Consumer Configuration Class
```java
@Configuration
@EnableRabbit
public class RabbitConsumerConfig {

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> 
           rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = 
            new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }
}
```

##### Message Consumer Service
```java
@Service
@Slf4j
public class MetricsConsumerService {

    @RabbitListener(queues = "metrics.queue")
    public void receiveMetrics(String message, 
                              @Header Map<String, Object> headers,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Received metrics from RabbitMQ: {}", message);
            
            // Parse the JSON message
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MetricsData metrics = objectMapper.readValue(message, MetricsData.class);
            
            // Process the metrics data
            processMetrics(metrics);
            
            // Acknowledge message
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Error processing metrics message: {}", e.getMessage(), e);
            try {
                // Reject message and requeue
                channel.basicReject(deliveryTag, true);
            } catch (IOException ioException) {
                log.error("Error rejecting message: {}", ioException.getMessage());
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "app-specific-queue", durable = "true"),
        exchange = @Exchange(value = "metrics.exchange", type = ExchangeTypes.TOPIC),
        key = "metrics.${app.id}"  // Listen only to your app's metrics
    ))
    public void receiveAppSpecificMetrics(String message) {
        log.info("Received app-specific metrics: {}", message);
        // Process app-specific metrics
    }

    private void processMetrics(MetricsData metrics) {
        // Implement your business logic here
        log.info("Processing metrics for application: {}", metrics.getApplicationId());
        log.info("Metrics timestamp: {}", metrics.getTimestamp());
        log.info("Metrics data: {}", metrics.getMetrics());
    }
}
```

#### B. Kafka Integration

##### Dependencies (Maven)
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

##### Application Configuration (`application.yml`)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092  # or 82.223.13.241:9092 from host
    consumer:
      group-id: my-app-consumer-group  # Use unique group ID
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      enable-auto-commit: false  # For manual acknowledgment
      max-poll-records: 10
    listener:
      ack-mode: manual_immediate
```

##### Kafka Consumer Configuration
```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-app-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> 
           kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
```

##### Kafka Message Consumer Service
```java
@Service
@Slf4j
public class KafkaMetricsConsumerService {

    @KafkaListener(topics = "metrics-topic")
    public void consumeMetrics(String message, 
                              Acknowledgment acknowledgment,
                              ConsumerRecord<String, String> record) {
        try {
            log.info("Received metrics from Kafka: partition={}, offset={}, message={}", 
                    record.partition(), record.offset(), message);
            
            // Parse the JSON message
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MetricsData metrics = objectMapper.readValue(message, MetricsData.class);
            
            // Process the metrics data
            processMetrics(metrics);
            
            // Manual acknowledgment
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", e.getMessage(), e);
            // Don't acknowledge on error - message will be redelivered
        }
    }

    @KafkaListener(topics = "metrics-topic", 
                   groupId = "specific-app-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeWithFilter(String message, Acknowledgment acknowledgment) {
        try {
            // Filter messages based on content
            if (message.contains("\"applicationId\":\"my-app-id\"")) {
                log.info("Processing message for my application: {}", message);
                processMessage(message);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error in filtered consumer: {}", e.getMessage(), e);
        }
    }

    private void processMetrics(MetricsData metrics) {
        // Implement your business logic
        log.info("Processing metrics for application: {}", metrics.getApplicationId());
    }
}
```

### 2. Data Model Classes

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsData {
    private String applicationId;
    private LocalDateTime timestamp;
    private Map<String, Object> metrics;
}
```

---

## 2. Non-Spring Applications

### A. RabbitMQ with Java (Plain AMQP)

##### Dependencies (Maven)
```xml
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.19.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
</dependency>
```

##### RabbitMQ Consumer Implementation
```java
public class RabbitMQMetricsConsumer {
    private static final String QUEUE_NAME = "metrics.queue";
    private static final String HOST = "82.223.13.241"; // or "rabbitmq" in Docker
    private static final int PORT = 5672;
    private static final String USERNAME = "guest";
    private static final String PASSWORD = "guest";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Ensure queue exists
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Received metrics: " + message);
            
            try {
                // Process the message
                processMetricsMessage(message);
                
                // Acknowledge message
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
                // Reject and requeue
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
            }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
        
        System.out.println("Waiting for messages. Press CTRL+C to exit.");
        // Keep running
        Thread.currentThread().join();
    }

    private static void processMetricsMessage(String message) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        JsonNode jsonNode = objectMapper.readTree(message);
        String applicationId = jsonNode.get("applicationId").asText();
        String timestamp = jsonNode.get("timestamp").asText();
        
        System.out.println("Processing metrics for app: " + applicationId);
        System.out.println("Timestamp: " + timestamp);
        
        // Add your processing logic here
    }
}
```

### B. Kafka with Java (Plain Kafka Client)

##### Dependencies (Maven)
```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.6.1</version>
</dependency>
```

##### Kafka Consumer Implementation
```java
public class KafkaMetricsConsumer {
    private static final String TOPIC_NAME = "metrics-topic";
    private static final String BOOTSTRAP_SERVERS = "kafka:9092"; // or 82.223.13.241:9092
    private static final String GROUP_ID = "my-app-consumer-group";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(TOPIC_NAME));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Received message: partition=%d, offset=%d, key=%s, value=%s%n",
                            record.partition(), record.offset(), record.key(), record.value());
                    
                    try {
                        processMetricsMessage(record.value());
                        
                        // Manual commit after successful processing
                        consumer.commitSync();
                    } catch (Exception e) {
                        System.err.println("Error processing message: " + e.getMessage());
                        // Don't commit, will be reprocessed
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }

    private static void processMetricsMessage(String message) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        JsonNode jsonNode = objectMapper.readTree(message);
        String applicationId = jsonNode.get("applicationId").asText();
        
        System.out.println("Processing metrics for app: " + applicationId);
        // Add your processing logic here
    }
}
```

---

## 3. Python Applications

### A. RabbitMQ with Python (Pika)

##### Installation
```bash
pip install pika
```

##### Python RabbitMQ Consumer
```python
import pika
import json
import sys
from datetime import datetime

def process_metrics(ch, method, properties, body):
    try:
        message = body.decode('utf-8')
        print(f"Received metrics: {message}")
        
        # Parse JSON
        metrics_data = json.loads(message)
        application_id = metrics_data.get('applicationId')
        timestamp = metrics_data.get('timestamp')
        metrics = metrics_data.get('metrics')
        
        print(f"Processing metrics for app: {application_id}")
        print(f"Timestamp: {timestamp}")
        
        # Add your processing logic here
        
        # Acknowledge message
        ch.basic_ack(delivery_tag=method.delivery_tag)
        
    except Exception as e:
        print(f"Error processing message: {e}")
        # Reject and requeue
        ch.basic_reject(delivery_tag=method.delivery_tag, requeue=True)

def main():
    # Connection parameters
    credentials = pika.PlainCredentials('guest', 'guest')
    parameters = pika.ConnectionParameters(
        host='82.223.13.241',  # or 'rabbitmq' in Docker
        port=5672,
        credentials=credentials
    )
    
    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()
    
    # Ensure queue exists
    channel.queue_declare(queue='metrics.queue', durable=True)
    
    # Set up consumer
    channel.basic_qos(prefetch_count=1)  # Process one message at a time
    channel.basic_consume(queue='metrics.queue', on_message_callback=process_metrics)
    
    print('Waiting for messages. To exit press CTRL+C')
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        channel.stop_consuming()
        connection.close()
        sys.exit(0)

if __name__ == '__main__':
    main()
```

### B. Kafka with Python

##### Installation
```bash
pip install kafka-python
```

##### Python Kafka Consumer
```python
from kafka import KafkaConsumer
import json
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def process_metrics_message(message_value):
    """Process the received metrics message"""
    try:
        metrics_data = json.loads(message_value)
        application_id = metrics_data.get('applicationId')
        timestamp = metrics_data.get('timestamp')
        metrics = metrics_data.get('metrics')
        
        logger.info(f"Processing metrics for app: {application_id}")
        logger.info(f"Timestamp: {timestamp}")
        
        # Add your processing logic here
        
    except Exception as e:
        logger.error(f"Error processing message: {e}")
        raise

def main():
    consumer = KafkaConsumer(
        'metrics-topic',
        bootstrap_servers=['kafka:9092'],  # or ['82.223.13.241:9092']
        group_id='my-python-consumer-group',
        auto_offset_reset='earliest',
        enable_auto_commit=False,
        value_deserializer=lambda m: m.decode('utf-8')
    )

    try:
        for message in consumer:
            logger.info(f"Received message: partition={message.partition}, "
                       f"offset={message.offset}, value={message.value}")
            
            try:
                process_metrics_message(message.value)
                
                # Manual commit after successful processing
                consumer.commit()
                
            except Exception as e:
                logger.error(f"Error processing message: {e}")
                # Don't commit, message will be reprocessed
                
    except KeyboardInterrupt:
        logger.info("Shutting down consumer...")
    finally:
        consumer.close()

if __name__ == '__main__':
    main()
```

---

## 4. Node.js Applications

### A. RabbitMQ with Node.js

##### Installation
```bash
npm install amqplib
```

##### Node.js RabbitMQ Consumer
```javascript
const amqp = require('amqplib');

async function consumeMetrics() {
    try {
        // Connect to RabbitMQ
        const connection = await amqp.connect({
            hostname: '82.223.13.241', // or 'rabbitmq' in Docker
            port: 5672,
            username: 'guest',
            password: 'guest'
        });
        
        const channel = await connection.createChannel();
        
        // Ensure queue exists
        await channel.assertQueue('metrics.queue', { durable: true });
        
        // Set prefetch to process one message at a time
        await channel.prefetch(1);
        
        console.log('Waiting for messages. To exit press CTRL+C');
        
        // Consume messages
        await channel.consume('metrics.queue', async (message) => {
            if (message !== null) {
                try {
                    const content = message.content.toString();
                    console.log('Received metrics:', content);
                    
                    // Parse and process the message
                    const metricsData = JSON.parse(content);
                    await processMetrics(metricsData);
                    
                    // Acknowledge message
                    channel.ack(message);
                    
                } catch (error) {
                    console.error('Error processing message:', error);
                    // Reject and requeue
                    channel.reject(message, true);
                }
            }
        });
        
    } catch (error) {
        console.error('Error connecting to RabbitMQ:', error);
    }
}

async function processMetrics(metricsData) {
    console.log('Processing metrics for app:', metricsData.applicationId);
    console.log('Timestamp:', metricsData.timestamp);
    
    // Add your processing logic here
}

// Start consuming
consumeMetrics();
```

### B. Kafka with Node.js

##### Installation
```bash
npm install kafkajs
```

##### Node.js Kafka Consumer
```javascript
const { Kafka } = require('kafkajs');

const kafka = Kafka({
    clientId: 'my-nodejs-app',
    brokers: ['kafka:9092'] // or ['82.223.13.241:9092']
});

const consumer = kafka.consumer({ groupId: 'my-nodejs-consumer-group' });

async function consumeMessages() {
    try {
        await consumer.connect();
        await consumer.subscribe({ topic: 'metrics-topic' });
        
        console.log('Connected to Kafka. Waiting for messages...');
        
        await consumer.run({
            eachMessage: async ({ topic, partition, message }) => {
                try {
                    const value = message.value.toString();
                    console.log(`Received message: partition=${partition}, offset=${message.offset}, value=${value}`);
                    
                    // Parse and process the message
                    const metricsData = JSON.parse(value);
                    await processMetrics(metricsData);
                    
                } catch (error) {
                    console.error('Error processing message:', error);
                    // Message will be reprocessed due to error
                }
            },
        });
        
    } catch (error) {
        console.error('Error connecting to Kafka:', error);
    }
}

async function processMetrics(metricsData) {
    console.log('Processing metrics for app:', metricsData.applicationId);
    console.log('Timestamp:', metricsData.timestamp);
    
    // Add your processing logic here
}

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('Shutting down consumer...');
    await consumer.disconnect();
    process.exit(0);
});

// Start consuming
consumeMessages();
```

---

## 5. Docker Environment Connection

When running your application in the same Docker network as the monitoring system:

### Docker Compose Configuration
```yaml
version: '3.9'

services:
  your-app:
    image: your-app:latest
    depends_on:
      - rabbitmq
      - kafka
    networks:
      - keycloak-net  # Same network as monitoring system
    environment:
      RABBITMQ_HOST: rabbitmq
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092

networks:
  keycloak-net:
    external: true  # Use existing network
```

### Environment Variables
```bash
# RabbitMQ
RABBITMQ_HOST=82.223.13.241
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Kafka
KAFKA_BOOTSTRAP_SERVERS=82.223.13.241:9092
KAFKA_CONSUMER_GROUP=my-app-group
```

---

## 6. Message Format

The messages published by the Central Monitoring System follow this JSON format:

```json
{
  "applicationId": "user-service",
  "timestamp": "2023-10-17T14:30:00",
  "metrics": {
    "metrics": [
      {
        "metric": {
          "__name__": "cpu_usage",
          "application_id": "user-service",
          "instance": "82.223.13.241:8080"
        },
        "timestamp": 1697548200,
        "value": "75.5"
      }
    ],
    "total_count": 25,
    "returned_count": 10
  }
}
```

---

## 7. Testing and Troubleshooting

### Testing RabbitMQ Connection
```bash
# Check if RabbitMQ is accessible
curl -u guest:guest http://82.223.13.241:15672/api/overview

# List queues
curl -u guest:guest http://82.223.13.241:15672/api/queues

# Check queue messages
curl -u guest:guest http://82.223.13.241:15672/api/queues/%2F/metrics.queue
```

### Testing Kafka Connection
```bash
# List topics
docker exec -it kafka kafka-topics --bootstrap-server kafka:9092 --list

# Consume messages from command line
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic metrics-topic \
  --from-beginning
```

### Common Issues and Solutions

1. **Connection refused**: Ensure the services are running and accessible
2. **Queue not found**: Check if the queue is declared correctly
3. **Authentication failed**: Verify credentials
4. **Messages not being consumed**: Check consumer group configuration
5. **JSON parsing errors**: Verify message format compatibility

---

## 8. Best Practices

### Error Handling
- Always implement proper error handling
- Use dead letter queues for failed messages
- Log errors appropriately
- Implement retry mechanisms with exponential backoff

### Performance
- Use appropriate consumer configurations (prefetch for RabbitMQ, max.poll.records for Kafka)
- Consider message acknowledgment strategies
- Monitor consumer lag
- Implement health checks

### Security
- Use proper authentication mechanisms
- Consider using SSL/TLS for production
- Implement proper access controls
- Rotate credentials regularly

---

## 9. Monitoring Integration

To register your application for monitoring, make a POST request to:

```bash
curl -X POST http://82.223.13.241:8081/metrics/add-application \
  -H "Content-Type: application/json" \
  -d '"your-app-id"'
```

Once registered, your application's metrics will be automatically published to both RabbitMQ and Kafka every 60 seconds.

---

This guide provides everything needed to connect external applications to your monitoring system's messaging infrastructure. Choose the appropriate integration method based on your application's technology stack and requirements.
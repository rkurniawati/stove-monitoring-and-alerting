# Stove Monitoring and Alerting Project

This is the source code for the Stove Monitoring Project. More information can be found in this [presentation](https://docs.google.com/presentation/d/e/2PACX-1vQIFKavVQjKR7zRES9MspUE8__-Zxw_jsz_CQLSpXys-Jc3RGGJDyXw5U4xZ6W9045wMJYRZfb8WsIG/pub?start=false&loop=false&delayms=3000).

## Requirements

To run the Java services, you will need to have the following software installed on your machine:
- Java (17 or later)
- Gradle (7.2 or later)
- Docker (20.10.8 or later) to run the services (Prometheus, Kafka, and others) needed 

## Running the Required Services

To start Prometheus, Grafana, Alert Manager, Kafka Broker, and Confluent Schema Registry, use the following command:

```bash
docker compose up -d
```

## Building and Running the Thermal Cam Prometheus Publisher Service

Open the directory `thermal-cam-publisher` in a Java IDE and run the program. To view the metrics being published to Prometheus, open this URL:
    
```
http://localhost:8080/actuator/prometheus
```

## Building and Running the Thermal Cam Visualizer Service

Open the directory `thermal-cam-viewer` in a Java IDE and run the program. To view the "image" from the thermal camera, open this URL:

```
http://localhost:8888/
```

## Running the Thermal Cam Collection Script

The thermal camera script should requires the following:
- It has to be run on a Raspberry Pi with the MLX90640 thermal camera wired to correct IC2 GPIO pins
- The Raspberry Pi should have:
  - a reasonable recent `python3` 
  - [Circuit Python library](https://learn.adafruit.com/adafruit-mlx90640-ir-thermal-camera/python-circuitpython) libraries to read data from the thermal camera
  - [Confluent Kafka client library](https://github.com/confluentinc/confluent-kafka-python/blob/master/INSTALL.md) 
  - Google ProtoBuf `protoc` compiler

To collect data from the thermal camera attached to the raspberry pi and publish it to a Kafka broker (topic thermal.cam.topic) and Schema Registry, you can use the following command below (note that my-kitchen is a location identifier to allow for multiple cameras):

```bash
python3 thermal_cam_kafka.py -b broker:9092 -s http://schema-registry:8081 -t thermal.cam.topic -c my-kitchen
```

Note that you will need to adjust the script to point to the correct Kafka broker and schema registry service endpoints. You can run these service on a separate machine.

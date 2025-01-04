import time
import board
import busio
import adafruit_mlx90640

import protobuf.thermal_cam_pb2 as thermal_cam_pb2;
from confluent_kafka import Producer
from confluent_kafka.serialization import StringSerializer, SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.protobuf import ProtobufSerializer
import argparse

def delivery_report(err, msg):
    """
    Reports the failure or success of a message delivery.

    Args:
        err (KafkaError): The error that occurred on None on success.
        msg (Message): The message that was produced or failed.
    """

    if err is not None:
        print("Delivery failed for thermal cam record {}: {}".format(msg.key(), err))
        return
    print('Thermal cam record {} successfully produced to {} [partition {}] at offset {}'.format(
        msg.key(), msg.topic(), msg.partition(), msg.offset()))

def main(args):
    topic = args.topic

    schema_registry_conf = {'url': args.schema_registry}
    schema_registry_client = SchemaRegistryClient(schema_registry_conf)

    string_serializer = StringSerializer('utf8')
    protobuf_serializer = ProtobufSerializer(thermal_cam_pb2.ThermalCamFrame,
                                             schema_registry_client,
                                             {'use.deprecated.format': False})

    producer_conf = {'bootstrap.servers': args.bootstrap_servers}

    producer = Producer(producer_conf)

    cid = args.cid

    print("Producing user records to topic {}. ^C to exit.".format(topic))
    print("cid {}".format(cid))

    # Setup I2C sensor
    i2c = busio.I2C(board.SCL, board.SDA, frequency=800000)

    mlx = adafruit_mlx90640.MLX90640(i2c)
    print("MLX addr detected on I2C", [hex(i) for i in mlx.serial_number])

    # if using higher refresh rates yields a 'too many retries' exception,
    # try decreasing this value to work with certain pi/camera combinations
    mlx.refresh_rate = adafruit_mlx90640.RefreshRate.REFRESH_2_HZ

    serial = ":".join([hex(i) for i in mlx.serial_number])
    frame = [0] * 768
    while True:
        try:
            mlx.getFrame(frame)
        except ValueError:
            # these happen, no biggie - retry
            continue

        tc_frame = thermal_cam_pb2.ThermalCamFrame(frame_data = frame)

        producer.produce(topic=topic, partition=0,
                         key=string_serializer(cid+":"+serial),
                         value=protobuf_serializer(tc_frame,
                                                   SerializationContext(topic, MessageField.VALUE)),
                         on_delivery=delivery_report)

        print("Published {}:{}: {} ...".format(cid, serial, tc_frame.frame_data[:2]))
        time.sleep(20)

if __name__ == '__main__':

    parser = argparse.ArgumentParser(description="Test publishing thermal cam data to kafka")
    parser.add_argument('-b', dest="bootstrap_servers", required=True,
                        help="Bootstrap broker(s) (host[:port])")
    parser.add_argument('-s', dest="schema_registry", required=True,
                        help="Schema Registry (http(s)://host[:port]")
    parser.add_argument('-t', dest="topic", default="test_mlx",
                        help="Topic name")
    parser.add_argument('-c', dest="cid", default="bertie",
                        help="Customer identifier")

    # python3 test_cam_kafka.py -b broker:9092 -s http://schema-registry:8081 -t thermal.cam.test -c bertie
    #
    main(parser.parse_args())

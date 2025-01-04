package org.gooddog.thermalcampublisher;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.gooddog.thermal_cam.ThermalCam;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ImageController {
  private final ThermalCamFramePublisherService imageService;
  private final ReadWriteLock lock;

  public ImageController(ThermalCamFramePublisherService imageService) {
    this.imageService = imageService;
    this.lock = new ReentrantReadWriteLock();
  }

  @KafkaListener(
      topics = "${spring.application.kafka.thermal_cam.topic}",
      groupId = "${spring.application.kafka.thermal_cam.consumer_group}",
      containerFactory = "myListenerFactory")
  public void myListener(ConsumerRecord<String, ThermalCam.ThermalCamFrame> record) {
    log.debug(record.value().toString());
    log.info("Received thermal cam frame");
    try {
      lock.writeLock().lock();
      ThermalCam.ThermalCamFrame currentFrame = record.value();
      imageService.publishFrame(currentFrame);
    } finally {
      lock.writeLock().unlock();
    }
  }
}

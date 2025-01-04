package org.gooddog.thermalcamviewer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.gooddog.thermal_cam.ThermalCam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ImageController {
  private final ImageService imageService;
  private ReadWriteLock lock;
  private ThermalCam.ThermalCamFrame currentFrame;

  public ImageController(ImageService imageService) {
    this.imageService = imageService;
    this.lock = new ReentrantReadWriteLock();
  }

  @GetMapping(value = "/heatmap", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<BufferedImage> getHeatMap() throws IOException {
    if (currentFrame == null) {
      return null;
    }
    // temporary lock to ensure that the currentFrame is not being updated while we are generating
    // the image
    ThermalCam.ThermalCamFrame copy;
    try {
      lock.readLock().lock();
      copy = ThermalCam.ThermalCamFrame.parseFrom(currentFrame.toByteArray());
    } catch (InvalidProtocolBufferException e) {
      log.error("Error copying current frame", e);
      return null;
    } finally {
      lock.readLock().unlock();
    }
    // create a temporary image file
    File imageFile = File.createTempFile("thermal_cam_frame", ".png");
    writeToFile(copy, imageFile);
    BufferedImage image = ImageIO.read(imageFile);
    imageFile.delete();
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
  }

  private File writeToFile(ThermalCam.ThermalCamFrame frame, File imageFile) throws IOException {
    imageService.generateImage(frame, imageFile);
    return imageFile;
  }

  @KafkaListener(
      topics = "${spring.application.kafka.thermal_cam.topic}",
      groupId = "${spring.application.kafka.thermal_cam.consumer_group}",
      containerFactory = "myListenerFactory")
  public void myListener(ConsumerRecord<String, ThermalCam.ThermalCamFrame> record)
      throws IOException {
    log.debug(record.value().toString());
    log.info("Received thermal cam frame");
    try {
      lock.writeLock().lock();
      currentFrame = record.value();
      //            File imageFile = new File(String.format("images/thermal_cam_frame-%s-%d.png",
      // record.key(), record.offset()));
      //            writeToFile(currentFrame, imageFile);
      //        String data = currentFrame.getFrameDataList().subList(0,
      // 10).stream().map(Object::toString).collect(Collectors.joining(", "));
      //        saveProto(String.format("thermal_cam_frame-%s-%d.json", record.key(),
      // record.offset()), frame, true);
      //        log.info("First 10 elements of frame data: {}", data);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public static <T extends MessageOrBuilder> void saveProto(
      String outputFile, T proto, boolean isJson) {
    log.info("*** Writing proto to file: {}", outputFile);
    try (final OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(outputFile))) {
      if (isJson) {
        writer.write(JsonFormat.printer().print(proto));
      } else {
        writer.write(TextFormat.printer().printToString(proto));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

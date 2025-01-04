package org.gooddog.thermalcampublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gooddog.thermalcampublisher.ThermalCamFramePublisherService.HEATMAP_FRAME_SUMMARY;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.gooddog.thermal_cam.ThermalCam;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Slf4j
@SpringBootTest(
    classes = {ThermalCamFramePublisherService.class, TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ApplicationTests {

  @Autowired private ThermalCamFramePublisherService publisherService;

  @Autowired private MeterRegistry meterRegistry;

  static Stream<ThermalCam.ThermalCamFrame> generateFrames() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Resource stateFile = new ClassPathResource("data/2024-10-28T232503.200.json");
    File file = stateFile.getFile();
    List<JsonNode> frames = objectMapper.readValue(file, new TypeReference<List<JsonNode>>() {});
    log.info("Read {} frames", frames.size());

    List<ThermalCam.ThermalCamFrame> thermalCamFrames =
        frames.stream()
            .map(
                frame -> {
                  ThermalCam.ThermalCamFrame.Builder builder =
                      ThermalCam.ThermalCamFrame.newBuilder();
                  frame
                      .get("frameData")
                      .elements()
                      .forEachRemaining(
                          pixel -> {
                            builder.addFrameData(pixel.floatValue());
                          });
                  return builder.build();
                })
            .toList();
    log.info("Converted {} frames", thermalCamFrames.size());
    return thermalCamFrames.stream();
  }

  @ParameterizedTest
  @MethodSource("generateFrames")
  void getFrames(ThermalCam.ThermalCamFrame param) {
    publisherService.publishFrame(param);
    DistributionSummary summary = meterRegistry.get(HEATMAP_FRAME_SUMMARY).summary();
    assertThat(summary.count()).isEqualTo(param.getFrameDataList().size());
  }
}

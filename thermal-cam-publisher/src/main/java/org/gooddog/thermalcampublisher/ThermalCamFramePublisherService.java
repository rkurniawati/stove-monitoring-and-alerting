package org.gooddog.thermalcampublisher;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.stream.IntStream;

import org.gooddog.thermal_cam.ThermalCam;
import org.springframework.stereotype.Service;

@Service
public class ThermalCamFramePublisherService {

  public static final String HEATMAP_FRAME_SUMMARY = "thermalcam.heatmap.frameSummary";
  private final MeterRegistry meterRegistry;
  private DistributionSummary frameSummary;

  public ThermalCamFramePublisherService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void publishFrame(ThermalCam.ThermalCamFrame frame) {
    // remove previous frame summary
    if (frameSummary != null) {
      meterRegistry.remove(frameSummary);
    }
    frameSummary =
        DistributionSummary.builder(HEATMAP_FRAME_SUMMARY)
            .description("Summary of frame data")
            .minimumExpectedValue(20.0 * 1000)
            .maximumExpectedValue(35.0 * 1000)
            .scale(1000)
            .serviceLevelObjectives(
                IntStream.range(0, 15).mapToDouble(i -> i * 1000 + 20_000.0).toArray())
            .baseUnit("pixels")
            .register(meterRegistry);

    // publish frame
    for (float pixel : frame.getFrameDataList()) {
      frameSummary.record(pixel);
    }
  }
}

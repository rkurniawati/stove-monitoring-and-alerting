package org.gooddog.thermalcamviewer;

import com.google.protobuf.util.JsonFormat;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.gooddog.thermal_cam.ThermalCam;

@Service
public class ImageService {

  @AllArgsConstructor
  static class ColorPoint {
    float r, g, b, heatIndex;
  }

  // low range of the sensor (this will be blue on the screen)
  private static final float MIN_TEMP = 20;

  // The high range of the sensor (this will be red on the screen).
  // The high range is 35C as a compromise, otherwise at room temp
  // we will have an almost all blue heatmap.
  private static final float MAX_TEMP = 35;

  private final List<ColorPoint> colorPoints;

  public ImageService() {
    // create default heat map gradient
    colorPoints =
        List.of(
            new ColorPoint(0, 0, 1, 0), // blue
            new ColorPoint(0, 1, 1, 0.25f), // cyan
            new ColorPoint(0, 1, 0, 0.5f), // green
            new ColorPoint(1, 1, 0, 0.75f), // yellow
            new ColorPoint(1, 0, 0, 1) // red
            );
  }

  public void generateImage(File jsonFrameFile, File imageFile) throws IOException {
    // read jsonFrameData
    String jsonFrameDataString = new String(Files.readAllBytes(jsonFrameFile.toPath()));
    ThermalCam.ThermalCamFrame.Builder frameBuilder = ThermalCam.ThermalCamFrame.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(jsonFrameDataString, frameBuilder);
    generateImage(frameBuilder.build(), imageFile);
  }

  public void generateImage(ThermalCam.ThermalCamFrame frame, File imageFile) throws IOException {

    // generate image
    byte[] imageBytes = generateImageRgbBytes(frame);

    // write image to imageFile
    writePngFromRgb(imageBytes, imageFile);
  }

  private void writePngFromRgb(byte[] aByteArray, File imageFile) throws IOException {
    // write imageBytes to imageFile
    // 3 bytes per pixel: red, green, blue
    int width = 32;
    int height = 24;
    DataBuffer buffer = new DataBufferByte(aByteArray, aByteArray.length);

    // 3 bytes per pixel: red, green, blue
    WritableRaster raster =
        Raster.createInterleavedRaster(
            buffer, width, height, 3 * width, 3, new int[] {0, 1, 2}, (Point) null);
    ColorModel cm =
        new ComponentColorModel(
            ColorModel.getRGBdefault().getColorSpace(),
            false,
            true,
            Transparency.OPAQUE,
            DataBuffer.TYPE_BYTE);
    BufferedImage image = new BufferedImage(cm, raster, true, null);

    ImageIO.write(image, "png", imageFile);
  }

  private byte[] generateImageRgbBytes(ThermalCam.ThermalCamFrame frame) {
    List<Float> frameDataList = frame.getFrameDataList();

    // convert frameDataList to imageBytes
    byte[] imageBytes = new byte[frameDataList.size() * 3];

    for (int i = 0; i < frameDataList.size(); i++) {
      float temperature = frameDataList.get(i);
      byte[] rgb = temperatureToRgb(tempToHeatIndex(temperature));
      imageBytes[i * 3] = rgb[0];
      imageBytes[i * 3 + 1] = rgb[1];
      imageBytes[i * 3 + 2] = rgb[2];
    }
    return imageBytes;
  }

  private float tempToHeatIndex(float temperature) {
    return (temperature - MIN_TEMP) / (MAX_TEMP - MIN_TEMP);
  }

  private byte[] temperatureToRgb(float heatIndex) {
    // convert temperature to rgb
    // using http://www.andrewnoske.com/wiki/Code_-_heatmaps_and_color_gradients
    float red = 1, green = 0, blue = 0;
    for (int i = 0; i < colorPoints.size(); i++) {
      ColorPoint currC = colorPoints.get(i);
      if (heatIndex < currC.heatIndex) {
        ColorPoint prevC = colorPoints.get(i > 0 ? i - 1 : i);
        float valueDiff = (prevC.heatIndex - currC.heatIndex);
        float fractBetween = (valueDiff == 0) ? 0 : (heatIndex - currC.heatIndex) / valueDiff;
        red = (prevC.r - currC.r) * fractBetween + currC.r;
        green = (prevC.g - currC.g) * fractBetween + currC.g;
        blue = (prevC.b - currC.b) * fractBetween + currC.b;
        break;
      }
    }
    return new byte[] {toByte(red), toByte(green), toByte(blue)};
  }

  private byte toByte(float red) {
    return (byte) (red * 255);
  }
}

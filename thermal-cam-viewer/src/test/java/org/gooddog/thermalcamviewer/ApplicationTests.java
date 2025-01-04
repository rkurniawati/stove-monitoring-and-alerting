package org.gooddog.thermalcamviewer;

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
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@SpringBootTest(classes = ImageService.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Slf4j
class ApplicationTests {
  @Autowired private ImageService imageService;

  @Test
  void testImageIo() throws IOException {
    byte[] aByteArray = {0xa, 0x2, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    int width = 1;
    int height = 2;

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

    ImageIO.write(image, "png", new File("image.png"));
  }

  @Test
  void testEnumerateFiles() throws IOException {
    // get all filenames in resources/cam-data directory
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    Resource[] resources = resolver.getResources("classpath*:/**/thermal_cam_frame-*.json");
    for (Resource resource : resources) {
      log.info("Resource {}:", resource.getURL());
      imageService.generateImage(
          resource.getFile(), new File(resource.getFilename().replace(".json", ".png")));
    }
  }
}

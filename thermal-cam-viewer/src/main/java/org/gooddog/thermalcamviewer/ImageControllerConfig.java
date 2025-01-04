package org.gooddog.thermalcamviewer;

import java.awt.image.BufferedImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
public class ImageControllerConfig {
  @Bean
  public HttpMessageConverter<BufferedImage> bufferedImageHttpMessageConverter() {
    return new BufferedImageHttpMessageConverter();
  }
}

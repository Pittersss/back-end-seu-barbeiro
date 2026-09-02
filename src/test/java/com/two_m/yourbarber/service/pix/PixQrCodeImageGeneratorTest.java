package com.two_m.yourbarber.service.pix;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PixQrCodeImageGeneratorTest {

    @Test
    void toPngBase64_validContent_producesDecodablePngImage() throws Exception {
        String base64 = PixQrCodeImageGenerator.toPngBase64("00020101021226...6304ABCD");

        byte[] bytes = Base64.getDecoder().decode(base64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(320);
        assertThat(image.getHeight()).isEqualTo(320);
    }

    @Test
    void toPngBase64_customSize_respectsRequestedDimensions() throws Exception {
        String base64 = PixQrCodeImageGenerator.toPngBase64("some pix payload", 200);

        byte[] bytes = Base64.getDecoder().decode(base64);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        assertThat(image.getWidth()).isEqualTo(200);
        assertThat(image.getHeight()).isEqualTo(200);
    }
}

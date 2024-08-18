package org.eu.polarexpress.conductor.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class ImageUtil {

    public static byte[] compressImage(InputStream inputStream, String type) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        var processedImage = new ByteArrayOutputStream(1024);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(type);
        ImageWriter writer = writers.next();

        ImageOutputStream outputStream = ImageIO.createImageOutputStream(processedImage);
        writer.setOutput(outputStream);

        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(0.5f);

        writer.write(null, new IIOImage(bufferedImage, null, null), params);
        outputStream.close();
        writer.dispose();
        return processedImage.toByteArray();
    }

}

package com.example.airecruitment.parser;

import com.example.airecruitment.config.AppProperties;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeParserService {
    private final Tika tika = new Tika();
    private final AppProperties properties;

    public ResumeParserService(AppProperties properties) {
        this.properties = properties;
    }

    public ParsedDocument parse(MultipartFile file) {
        String contentType = detectContentType(file);
        String text = contentType.startsWith("image/") ? parseImage(file) : parseDocument(file);
        TextBlock block = new TextBlock(text.strip(), 1, 0, 0, 1, 1);
        return new ParsedDocument(text.strip(), List.of(block));
    }

    private String detectContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String detected = tika.detect(inputStream, new Metadata());
            return detected == null ? "application/octet-stream" : detected;
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法识别文件类型", ex);
        }
    }

    private String parseDocument(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.parseToString(inputStream);
        } catch (Exception ex) {
            throw new IllegalArgumentException("文档解析失败", ex);
        }
    }

    private String parseImage(MultipartFile file) {
        if (!properties.getOcr().isEnabled()) {
            throw new IllegalStateException("图片简历需要启用 OCR：app.ocr.enabled=true 并配置 Tesseract 语言包");
        }
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setLanguage(properties.getOcr().getLanguage());
            if (!properties.getOcr().getDataPath().isBlank()) {
                tesseract.setDatapath(properties.getOcr().getDataPath());
            }
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("无法读取图片内容");
            }
            return tesseract.doOCR(image);
        } catch (Exception ex) {
            throw new IllegalArgumentException("OCR 解析失败", ex);
        }
    }
}

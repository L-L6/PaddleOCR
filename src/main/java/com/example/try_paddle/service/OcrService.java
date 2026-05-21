package com.example.try_paddle.service;

import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.ocr.config.ParamConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OcrService {

    @Autowired
    private InferenceEngine inferenceEngine;

    @Value("${file.upload.dir}")
    private String uploadDir;

    public String recognizeText(MultipartFile file) throws IOException {
        // 1. 确保上传目录存在
        ensureUploadDirectoryExists();

        // 2. 生成唯一文件名，避免冲突
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = "ocr_" + UUID.randomUUID() + extension;

        // 3. 创建临时文件路径
        Path tempFilePath = Paths.get(uploadDir, fileName);

        try {
            // 4. 保存上传的文件到临时位置
            Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件已保存到临时位置: {}", tempFilePath.toAbsolutePath());

            // 5. 配置OCR参数
            ParamConfig paramConfig = ParamConfig.getDefaultConfig();
            paramConfig.setDoAngle(true);  // 开启方向检测
            paramConfig.setBoxScoreThresh(0.3f);  // 降低检测阈值，提高识别率
            paramConfig.setBoxThresh(0.4f);

            // 6. 使用文件路径进行OCR识别
            OcrResult ocrResult = inferenceEngine.runOcr(tempFilePath.toAbsolutePath().toString(), paramConfig);

            // 7. 返回识别结果
            String result = ocrResult.getStrRes().trim();
            log.info("OCR识别结果: {}", result);

            return result;

        } finally {
            // 8. 清理临时文件
            try {
                Files.deleteIfExists(tempFilePath);
                log.info("已清理临时文件: {}", tempFilePath.toAbsolutePath());
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 新增方法：识别截图
     * @param screenshot 截图图片
     * @return 识别结果
     */
    public String recognizeScreenshot(BufferedImage screenshot) throws IOException {
        // 确保临时目录存在
        ensureUploadDirectoryExists();

        // 生成临时文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        String timestamp = sdf.format(new Date());
        String fileName = "screenshot_" + timestamp + ".png";
        Path tempFilePath = Paths.get(uploadDir, fileName);

        try {
            // 保存截图到临时文件
            File tempFile = tempFilePath.toFile();
            ImageIO.write(screenshot, "png", tempFile);
            log.info("截图已保存到临时位置: {}", tempFilePath.toAbsolutePath());

            // 配置OCR参数
            ParamConfig paramConfig = ParamConfig.getDefaultConfig();
            paramConfig.setDoAngle(true);
            paramConfig.setBoxScoreThresh(0.3f);
            paramConfig.setBoxThresh(0.4f);

            // 执行OCR识别
            OcrResult ocrResult = inferenceEngine.runOcr(tempFilePath.toAbsolutePath().toString(), paramConfig);

            String result = ocrResult.getStrRes().trim();
            log.info("截图OCR识别结果: {}", result);

            return result;

        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempFilePath);
                log.info("已清理临时截图文件: {}", tempFilePath.toAbsolutePath());
            } catch (IOException e) {
                log.warn("清理临时截图文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 确保上传目录存在
     */
    private void ensureUploadDirectoryExists() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("创建上传目录: {}", uploadPath.toAbsolutePath());
        }
    }

    /**
     * 只提取数字
     */
    public String recognizeDigitsOnly(MultipartFile file) throws IOException {
        String text = recognizeText(file);
        return text.replaceAll("[^0-9]", "");
    }

    /**
     * 新增方法：从截图只提取数字
     */
    public String recognizeDigitsFromScreenshot(BufferedImage screenshot) throws IOException {
        String text = recognizeScreenshot(screenshot);
        return text.replaceAll("[^0-9]", "");
    }
}

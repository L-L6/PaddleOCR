package com.example.try_paddle.controller;

import com.example.try_paddle.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/text")
    public ResponseEntity<?> extractText(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("请上传有效的图片文件");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null ||
                    !(contentType.startsWith("image/jpeg") ||
                            contentType.startsWith("image/png") ||
                            contentType.startsWith("image/jpg"))) {
                return ResponseEntity.badRequest().body("仅支持 JPG/PNG 格式图片");
            }

            String result = ocrService.recognizeText(file);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/digits")
    public ResponseEntity<?> extractDigits(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("请上传有效的图片文件");
            }

            String result = ocrService.recognizeDigitsOnly(file);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("数字提取失败: " + e.getMessage());
        }
    }
}

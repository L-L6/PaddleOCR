package com.example.try_paddle.config;

import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrConfig {

    @Bean
    public InferenceEngine inferenceEngine(){
        // 使用 PP-OCRv4 模型，您也可以根据需求选择 Model.ONNX_PPOCR_V3
        InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
        // 设置推理线程数，建议根据 CPU 核心数调整
        //engine.setNumThread(Runtime.getRuntime().availableProcessors());
        return engine;
    }
}

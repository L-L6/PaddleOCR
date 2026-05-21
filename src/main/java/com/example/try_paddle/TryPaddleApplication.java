package com.example.try_paddle;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class TryPaddleApplication implements CommandLineRunner {

	public static void main(String[] args) {

		System.setProperty("java.awt.headless", "false");

		ConfigurableApplicationContext context = SpringApplication.run(TryPaddleApplication.class, args);

		// 启动GUI截图工具
		startScreenshotGui(context);
	}

	@Override
	public void run(String... args) {
		System.out.println("PaddleOCR应用已启动！");
		System.out.println("功能：");
		System.out.println("1. Web API: POST /api/ocr/text 上传图片识别");
		System.out.println("2. 桌面截图: 运行后弹出截图窗口，可截图识别");
		System.out.println("=========================================");
	}

	private static void startScreenshotGui(ConfigurableApplicationContext context) {
		// 在新线程中启动GUI，避免阻塞Spring Boot
		SwingUtilities.invokeLater(() -> {
			try {
				// 启动截图GUI
				com.example.try_paddle.gui.ScreenshotGui gui =
						new com.example.try_paddle.gui.ScreenshotGui(context);
				gui.setVisible(true);

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

}

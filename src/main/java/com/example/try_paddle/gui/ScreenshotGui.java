package com.example.try_paddle.gui;

import com.example.try_paddle.service.OcrService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ScreenshotGui extends JFrame{
    private OcrService ocrService;
    private JButton captureButton;
    private JButton regionButton;
    private JTextArea resultArea;
    private JLabel statusLabel;



    public ScreenshotGui(ConfigurableApplicationContext context) {
        // 从Spring容器获取OcrService
        this.ocrService = context.getBean(OcrService.class);
        initUI();
    }

    private void initUI() {
        setTitle("PaddleOCR 截图识别工具");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 只关闭窗口，不退出应用

        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//关闭窗口就退出应用
        setLocationRelativeTo(null);

        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 标题
        JLabel titleLabel = new JLabel("截图识别工具", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("截图方式"));

        captureButton = new JButton("全屏截图");
        captureButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        captureButton.addActionListener(e -> captureFullScreen());

        regionButton = new JButton("区域截图");
        regionButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        regionButton.addActionListener(e -> startRegionCapture());

        buttonPanel.add(captureButton);
        buttonPanel.add(regionButton);
        mainPanel.add(buttonPanel);

        // 结果区域
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("识别结果"));

        resultArea = new JTextArea(10,20);
        resultArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(true);

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(100, 150));
        resultPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(resultPanel);

        // 状态栏
        statusLabel = new JLabel("就绪", SwingConstants.LEFT);
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        mainPanel.add(statusLabel);


        getContentPane().add(mainPanel);

        // 快捷键
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "capture");
        getRootPane().getActionMap().put("capture", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureFullScreen();
            }
        });
    }

    private void captureFullScreen() {
        try {
            // 隐藏窗口
            setState(Frame.ICONIFIED);
            Thread.sleep(500);

            // 截取全屏
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect);

            // 恢复窗口
            setState(Frame.NORMAL);

            // 识别图片
            recognizeScreenshot(screenshot, "全屏截图");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("截图失败: " + e.getMessage());
        }
    }

    private void startRegionCapture() {
        // 最小化窗口
        setState(Frame.ICONIFIED);

        // 1. 创建一个自定义的JDialog，用于绘制选区
        JDialog regionDialog = new JDialog() {
            private RegionSelector selectorRef; // 用于存储selector引用

            public void setSelector(RegionSelector selector) {
                this.selectorRef = selector;
            }

            @Override
            public void paint(Graphics g) {
                // 先调用父类的paint方法绘制背景
                super.paint(g);

                // 然后绘制选区边框
                if (selectorRef != null) {
                    selectorRef.paintSelection(g);
                }
            }
        };

        regionDialog.setUndecorated(true);
        regionDialog.setOpacity(0.3f);
        regionDialog.setBackground(Color.LIGHT_GRAY);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        regionDialog.setSize(screenSize);
        regionDialog.setLocation(0, 0);

        // 2. 创建RegionSelector
        RegionSelector selector = new RegionSelector(regionDialog);

        // 3. 将selector传递给dialog
        if (regionDialog instanceof JDialog) {
            // 调用自定义的setSelector方法
            try {
                regionDialog.getClass().getMethod("setSelector", RegionSelector.class)
                        .invoke(regionDialog, selector);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 4. 添加鼠标监听器
        regionDialog.addMouseListener(selector);
        regionDialog.addMouseMotionListener(selector);

        // 5. 显示对话框
        regionDialog.setVisible(true);
    }

    private void recognizeScreenshot(BufferedImage image, String source) {
        statusLabel.setText("正在识别中...");

        // 在新线程中处理识别
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    // 调用OCR服务的新方法
                    return ocrService.recognizeScreenshot(image);
                } catch (Exception e) {
                    throw new RuntimeException("识别失败: " + e.getMessage(), e);
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();

                    // 在控制台输出
                    System.out.println("\n========== 截图识别结果 ==========");
                    System.out.println("截图来源: " + source);
                    System.out.println("识别结果: " + result);
                    System.out.println("=================================\n");

                    // 更新GUI,将文本显示在文本框上
                    resultArea.setText(result);
                    statusLabel.setText("识别完成");

                } catch (Exception e) {
                    String errorMsg = "识别失败: " + e.getMessage();
                    resultArea.setText(errorMsg);
                    statusLabel.setText(errorMsg);
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    /**
     * 区域选择器
     */
    class RegionSelector extends MouseAdapter {
        private JDialog dialog;
        private Point startPoint = null;
        private Point endPoint = null;
        private BufferedImage screenImage = null;
        private Rectangle selectionRect = null;

        // 使用更明显的颜色
        private static final Color BORDER_COLOR = Color.RED;
        private static final int BORDER_THICKNESS = 2;
        private static final BasicStroke BORDER_STROKE = new BasicStroke(BORDER_THICKNESS);

        public RegionSelector(JDialog dialog) {
            this.dialog = dialog;
            try {
                Robot robot = new Robot();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                screenImage = robot.createScreenCapture(new Rectangle(screenSize));
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            startPoint = e.getPoint();
            selectionRect = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            endPoint = e.getPoint();

            if (startPoint != null) {
                int x = Math.min(startPoint.x, endPoint.x);
                int y = Math.min(startPoint.y, endPoint.y);
                int width = Math.abs(endPoint.x - startPoint.x);
                int height = Math.abs(endPoint.y - startPoint.y);

                selectionRect = new Rectangle(x, y, width, height);

                // 关键：强制对话框重绘
                dialog.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            endPoint = e.getPoint();
            if (startPoint != null && endPoint != null) {
                int x = Math.min(startPoint.x, endPoint.x);
                int y = Math.min(startPoint.y, endPoint.y);
                int width = Math.abs(endPoint.x - startPoint.x);
                int height = Math.abs(endPoint.y - startPoint.y);

                if (width > 10 && height > 10) {
                    try {
                        // 1. 保持选区显示（不立即清除）
                        selectionRect = new Rectangle(x, y, width, height);

                        // 2. 短暂延迟，让用户能看到选区
                        Timer timer = new Timer(300, evt -> {
                            try {
                                BufferedImage regionImage = screenImage.getSubimage(x, y, width, height);
                                dialog.dispose();
                                setState(Frame.NORMAL);
                                recognizeScreenshot(regionImage, "区域截图");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                dialog.dispose();
                                setState(Frame.NORMAL);
                                statusLabel.setText("区域截图失败: " + ex.getMessage());
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        dialog.dispose();
                        setState(Frame.NORMAL);
                        statusLabel.setText("区域截图失败: " + ex.getMessage());
                    }
                } else {
                    // 区域太小，取消选择
                    dialog.dispose();
                    setState(Frame.NORMAL);
                }
            } else {
                dialog.dispose();
                setState(Frame.NORMAL);
            }
        }

        /**
         * 自定义绘制选区边框
         */
        public void paintSelection(Graphics g) {
            if (selectionRect != null && selectionRect.width > 0 && selectionRect.height > 0) {
                Graphics2D g2d = (Graphics2D) g.create();

                // 设置抗锯齿，使边框更平滑
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制边框
                g2d.setColor(BORDER_COLOR);
                g2d.setStroke(BORDER_STROKE);
                g2d.drawRect(selectionRect.x, selectionRect.y,
                        selectionRect.width, selectionRect.height);

                // 可选：添加尺寸显示
                String sizeText = String.format("%d × %d", selectionRect.width, selectionRect.height);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(sizeText);

                // 在选区上方显示尺寸
                int textX = selectionRect.x + (selectionRect.width - textWidth) / 2;
                int textY = selectionRect.y - 5;

                // 绘制文字背景
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(textX - 3, textY - fm.getAscent() + 3,
                        textWidth + 6, fm.getHeight());

                // 绘制文字
                g2d.setColor(Color.WHITE);
                g2d.drawString(sizeText, textX, textY);

                g2d.dispose();
            }
        }
    }


}

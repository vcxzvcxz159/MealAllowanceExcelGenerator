package com.taxiexcel;

import com.taxiexcel.ui.ExcelGeneratorUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.swing.*;

@SpringBootApplication(scanBasePackages = "com.taxiexcel") // AppLauncher 포함 패키지
public class MainApplication {
    public static void main(String[] args) {
        // Spring Context 생성
        SpringApplication app = new SpringApplication(MainApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // 웹 서버 없이 실행
        app.setHeadless(false); // headless 끄기
        ApplicationContext context = app.run(args);

        // AppLauncher Bean 가져오기
        com.taxiexcel.AppLauncher launcher = context.getBean(com.taxiexcel.AppLauncher.class);

        // UI 실행
        SwingUtilities.invokeLater(() -> {
            ExcelGeneratorUI ui = new ExcelGeneratorUI(launcher);
            ui.initUI();
        });
    }
}

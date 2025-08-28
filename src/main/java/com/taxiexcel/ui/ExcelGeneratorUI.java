package com.taxiexcel.ui;

import com.taxiexcel.AppLauncher;
import com.taxiexcel.dto.ExcelValueDTO;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Properties;

@Component
public class ExcelGeneratorUI extends JFrame {

    private final AppLauncher appLauncher;
    private final ExcelValueDTO excelValueDTO;

    private final File configFile;

    public ExcelGeneratorUI(AppLauncher appLauncher) {
        this.appLauncher = appLauncher;

        // JAR 위치 기준 config 폴더 설정
        String jarDir = new File(".").getAbsolutePath(); // 현재 작업 디렉토리
        File configDir = new File(jarDir, "config");

        if (!configDir.exists()) configDir.mkdirs();

        configFile = new File(configDir, "excel_value_config.properties");

        this.excelValueDTO = new ExcelValueDTO();
        loadSettings();
    }

    public void initUI() {
        setTitle("야근식대 엑셀 생성기 ver 1.0");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 200);
        setLocationRelativeTo(null);

        // 메인 패널
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 20, 0, 20)); // 전체 여백
        setContentPane(mainPanel);

        // 위쪽
        JPanel infoPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        infoPanel.add(new JLabel("1. 설정 버튼을 클릭하여 설정값을 입력합니다."));
        infoPanel.add(new JLabel("2. 파일 생성 버튼을 클릭하여 엑셀파일을 생성합니다."));
        infoPanel.setBorder(new EmptyBorder(0, 10, 20, 10));
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // 버튼 + 상태창 패널 (프로그레스바 바로 위)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
        centerPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        centerPanel.setBorder(new EmptyBorder(0, 10, 0, 10));

        JButton generateButton = new JButton("파일 생성");
        JLabel statusLabel = new JLabel("준비 완료");
        JButton optionButton = new JButton("설정");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        centerPanel.add(generateButton);
        centerPanel.add(Box.createHorizontalGlue()); // 버튼과 상태창 사이 공간
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createHorizontalGlue());
        centerPanel.add(optionButton);

        mainPanel.add(centerPanel);
        mainPanel.add(Box.createVerticalStrut(10)); // 버튼과 프로그레스바 사이 약간의 공간

        // 프로그레스바
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(false);
        progressBar.setBorder(new EmptyBorder(0, 10, 0, 10));
        progressBar.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        mainPanel.add(progressBar);

        mainPanel.add(Box.createVerticalStrut(10));

        // 제작자 라벨 (프로그레스바 아래 우측)
        JLabel authorLabel = new JLabel("제작자: 전창현");
        JPanel authorPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        authorPanel.add(authorLabel);
        mainPanel.add(authorPanel, java.awt.BorderLayout.SOUTH);

        // 설정 버튼 클릭 이벤트
        optionButton.addActionListener(e -> showSettingsDialog());

        // 생성 버튼 클릭 이벤트
        generateButton.addActionListener(e -> {
            generateButton.setEnabled(false);
            statusLabel.setText("생성 중...");
            progressBar.setIndeterminate(true);

            new Thread(() -> {
                try {
                    appLauncher.launch(excelValueDTO, this);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("완료!");
                        progressBar.setIndeterminate(false);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {

                        statusLabel.setText("오류 발생");
                        progressBar.setIndeterminate(true);
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> generateButton.setEnabled(true));
                }
            }).start();
        });

        setVisible(true);
    }

    // 설정 다이얼로그
    private void showSettingsDialog() {
        // ===== 필드 정의 =====
        JTextField imageFileDirPathField = new JTextField(excelValueDTO.getImageFileDirPath(), 20);
        imageFileDirPathField.setEnabled(false); // 직접 입력 못하게 막기
        JButton imageFileDirPathBtn = new JButton("폴더 선택");

        JTextField excelFileDirPathField = new JTextField(excelValueDTO.getExcelFileDirPath(), 20);
        excelFileDirPathField.setEnabled(false);
        JButton excelFileDirPathBtn = new JButton("폴더 선택");

        JTextField authorField = new JTextField(excelValueDTO.getAuthor(), 20);
        JTextField positionField = new JTextField(excelValueDTO.getPosition(), 20);
        JTextField departmentField = new JTextField(excelValueDTO.getDepartment(), 20);
        JTextField teamLeaderField = new JTextField(excelValueDTO.getTeamLeader(), 20);
        JTextField proposerField = new JTextField(excelValueDTO.getProposer(), 20);

        // ===== 버튼 액션 =====
        imageFileDirPathBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                imageFileDirPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        excelFileDirPathBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                excelFileDirPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // ===== 레이아웃 세팅 (GridBagLayout) =====
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 여백
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // 작성자명
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("작성자명"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        panel.add(authorField, gbc);

        // 직급
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("직급"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        panel.add(positionField, gbc);

        // 부서
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("부서"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        panel.add(departmentField, gbc);

        // 팀장명
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("팀장명"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        panel.add(teamLeaderField, gbc);

        // 입안자명
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("입안자명"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        panel.add(proposerField, gbc);

        // 이미지 폴더 경로
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("이미지 폴더 경로"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        JPanel imgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imgPanel.add(imageFileDirPathField);
        imgPanel.add(imageFileDirPathBtn);
        panel.add(imgPanel, gbc);

        // 엑셀파일 폴더 경로
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("엑셀파일 폴더 경로"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        JPanel excelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        excelPanel.add(excelFileDirPathField);
        excelPanel.add(excelFileDirPathBtn);
        panel.add(excelPanel, gbc);

        // ===== 다이얼로그 출력 =====
        int result = JOptionPane.showConfirmDialog(this, panel, "엑셀 생성 설정",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            excelValueDTO.setAuthor(authorField.getText());
            excelValueDTO.setPosition(positionField.getText());
            excelValueDTO.setDepartment(departmentField.getText());
            excelValueDTO.setTeamLeader(teamLeaderField.getText());
            excelValueDTO.setProposer(proposerField.getText());
            excelValueDTO.setImageFileDirPath(imageFileDirPathField.getText());
            excelValueDTO.setExcelFileDirPath(excelFileDirPathField.getText());

            saveSettings();
        }
    }

    public void showFailedFiles(List<String> failedFiles) {
        if (failedFiles == null || failedFiles.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (String fileName : failedFiles) {
            sb.append(fileName).append("\n");
        }

        JOptionPane.showMessageDialog(this,
                sb.toString(),
                "OCR 실패 이미지",
                JOptionPane.WARNING_MESSAGE);
    }


    // 설정 읽기
    private void loadSettings() {
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                Properties prop = new Properties();
                prop.load(in);
                excelValueDTO.setAuthor(prop.getProperty("author", ""));
                excelValueDTO.setPosition(prop.getProperty("position", ""));
                excelValueDTO.setDepartment(prop.getProperty("department", ""));
                excelValueDTO.setTeamLeader(prop.getProperty("teamLeader", ""));
                excelValueDTO.setProposer(prop.getProperty("proposer", ""));

                excelValueDTO.setImageFileDirPath(prop.getProperty("imageFileDirPath",  ""));
                excelValueDTO.setExcelFileDirPath(prop.getProperty("excelFileDirPath", ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 설정 저장
    private void saveSettings() {
        try (OutputStream out = new FileOutputStream(configFile)) {
            Properties prop = new Properties();
            prop.setProperty("author", excelValueDTO.getAuthor());
            prop.setProperty("position", excelValueDTO.getPosition());
            prop.setProperty("department", excelValueDTO.getDepartment());
            prop.setProperty("teamLeader", excelValueDTO.getTeamLeader());
            prop.setProperty("proposer", excelValueDTO.getProposer());
            prop.setProperty("imageFileDirPath", excelValueDTO.getImageFileDirPath());
            prop.setProperty("excelFileDirPath", excelValueDTO.getExcelFileDirPath());

            prop.store(out, "Excel Generator Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

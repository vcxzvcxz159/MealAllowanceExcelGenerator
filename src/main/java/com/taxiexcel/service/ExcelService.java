package com.taxiexcel.service;

import com.taxiexcel.dto.ExcelValueDTO;
import com.taxiexcel.dto.TexiInfoDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ExcelService {

    private static final String TEMPLATE_PATH = "template/야근식대양식.xlsx";

    public void createExcelFromTemplate(Map<String, List<TexiInfoDTO>> texiInfoListMap, ExcelValueDTO excelValueDTO) throws Exception {
        for (Map.Entry<String, List<TexiInfoDTO>> entry : texiInfoListMap.entrySet()) {
            String yearStr = entry.getKey().split("/")[0];
            int year = Integer.parseInt(yearStr);

            String monthStr = entry.getKey().split("/")[1];
            int month = Integer.parseInt(monthStr);

            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet firstSheet = workbook.getSheetAt(0);
                this.setFirstSheetData(firstSheet, LocalDate.of(year, month, 1), excelValueDTO);

                Sheet secondSheet = workbook.getSheetAt(1);
                this.setSecondSheetData(secondSheet, LocalDate.of(year, month, 1), excelValueDTO, entry.getValue());

                Sheet thirdSheet = workbook.getSheetAt(2);
                this.setThirdSheetData(thirdSheet, entry.getValue());

                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

                String outputFileName = "야근식양식_" + yearStr + "년_" + monthStr + "월_" + excelValueDTO.getAuthor() + ".xlsx";
                File outputFile = new File(excelValueDTO.getExcelFileDirPath() + "/" + outputFileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    workbook.write(fos);
                }
            }
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row;
    }

    private Cell getOrCreateCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        return cell;
    }

    private void setCellValue(Sheet sheet, int rowIndex, int colIndex, String value) {
        Row row = getOrCreateRow(sheet, rowIndex);
        Cell cell = getOrCreateCell(row, colIndex);
        cell.setCellValue(value);
    }

    private void setCellValue(Sheet sheet, int rowIndex, int colIndex, double value) {
        Row row = getOrCreateRow(sheet, rowIndex);
        Cell cell = getOrCreateCell(row, colIndex);
        cell.setCellValue(value);
    }

    private void setFirstSheetData(Sheet firstSheet, LocalDate localDate, ExcelValueDTO excelValueDTO) {
        Row row = getOrCreateRow(firstSheet, 2);
        getOrCreateCell(row, 2).setCellValue(excelValueDTO.getProposer());
        getOrCreateCell(row, 3).setCellValue(excelValueDTO.getTeamLeader());

        row = getOrCreateRow(firstSheet, 4);
        Cell cell = getOrCreateCell(row, 0);
        String currentValue = cell.getStringCellValue();
        cell.setCellValue(currentValue + excelValueDTO.getAuthor());

        int lengthOfMonth = localDate.lengthOfMonth();
        for (int day = 0; day < lengthOfMonth; day++) {
            setCellValue(firstSheet, 8 + day, 0,
                    localDate.getYear() + "년 " + localDate.getMonthValue() + "월 " + (day + 1) + "일");
        }

        setCellValue(firstSheet, 42, 1,
                localDate.getYear() + "-" + String.format("%02d", localDate.getMonthValue()) + "-" + localDate.lengthOfMonth());
    }

    private void setSecondSheetData(Sheet secondSheet, LocalDate localDate,
                                    ExcelValueDTO excelValueDTO, List<TexiInfoDTO> texiInfoDTOList) {

        setCellValue(secondSheet, 1, 5, excelValueDTO.getDepartment());
        setCellValue(secondSheet, 2, 5, excelValueDTO.getPosition());
        setCellValue(secondSheet, 2, 6, excelValueDTO.getAuthor());

        for (int rowIdx = 3; rowIdx < 65; rowIdx++) {

            System.out.println("rowIdx : " + rowIdx);

            LocalDate finalLocalDate = localDate;
            Optional<TexiInfoDTO> texiInfoDTOOptional = texiInfoDTOList.stream()
                    .filter(texiInfo -> finalLocalDate.getDayOfMonth() == texiInfo.getPaymentDttm().getDayOfMonth())
                    .findAny();

            System.out.println("isPresents : " + texiInfoDTOOptional.isPresent());

            if (rowIdx % 2 == 0) { // 짝수 행 → 택시

                if (texiInfoDTOOptional.isPresent()) {

                    System.out.println("택시 row");

                    TexiInfoDTO texiInfoDTO = texiInfoDTOOptional.get();

                    setCellValue(secondSheet, rowIdx, 5,
                            texiInfoDTO.getPaymentDttm().format(DateTimeFormatter.ofPattern("HH:mm")));
                    setCellValue(secondSheet, rowIdx, 6,
                            Double.parseDouble(texiInfoDTO.getPaidAmount().replaceAll("[^0-9]", "")));
                    setCellValue(secondSheet, rowIdx, 7, "야근");

                }

                localDate = localDate.plusDays(1);
                System.out.println("localDate plusDays : " + localDate.getDayOfMonth());
            } else { // 홀수 행 → 식대
                if (texiInfoDTOOptional.isPresent()) {
                    System.out.println("식대 row");

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E", Locale.KOREAN);
                    setCellValue(secondSheet, rowIdx, 0, localDate.format(formatter));

                    // texiInfoDTO의 paymentDttm의 시간이 00:00 ~ 09:00 사이라면 해당 row가 아닌 rowIdx -2
                    // 1일은 해당날의 식대에 입력
                    int dinnerRowIdx = rowIdx;
                    int day = texiInfoDTOOptional.get().getPaymentDttm().getDayOfMonth();
                    int hour = texiInfoDTOOptional.get().getPaymentDttm().getHour();
                    if (hour < 9 && day != 1) {
                        dinnerRowIdx = rowIdx - 2;
                    }

                    setCellValue(secondSheet, dinnerRowIdx, 5, "18:00");
                    setCellValue(secondSheet, dinnerRowIdx, 6, 11000);
                    setCellValue(secondSheet, dinnerRowIdx, 7, "식대");
                }
            }
        }
    }

    private void setThirdSheetData(Sheet thirdSheet, List<TexiInfoDTO> texiInfoDTOList) throws IOException {
        Workbook workbook = thirdSheet.getWorkbook();
        Drawing<?> drawing = thirdSheet.createDrawingPatriarch();

        int columnCell = 7;  // 셀 간격
        int rowCell = 60;    // 행 간격

        int colIndex = 1;
        int rowIndex = 5;

        int maxCols = colIndex + (3 * columnCell); // 한 줄에 최대 3개

        int maxWidth = 1500;  // 픽셀
        int maxHeight = 1000; // 픽셀

        CreationHelper helper = workbook.getCreationHelper();

        for (TexiInfoDTO dto : texiInfoDTOList) {
            File imgFile = dto.getImageFile();
            if (imgFile == null || !imgFile.exists()) continue;

            // 이미지 읽기 및 비율 유지 리사이즈
            BufferedImage original = ImageIO.read(imgFile);
            int width = original.getWidth();
            int height = original.getHeight();
            double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
            if (scale < 1.0) {
                width = (int) (width * scale);
                height = (int) (height * scale);
            }
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, width, height, null);
            g.dispose();

            // 바이트 배열 변환 및 타입 결정
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String lowerName = imgFile.getName().toLowerCase();
            int pictureType;

            if (lowerName.endsWith(".png")) {
                pictureType = Workbook.PICTURE_TYPE_PNG;
                ImageIO.write(resized, "png", baos);
            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                pictureType = Workbook.PICTURE_TYPE_JPEG;
                ImageIO.write(resized, "jpg", baos);
            } else if (lowerName.endsWith(".gif")) {
                // Excel은 GIF 직접 지원 안됨 → PNG로 변환
                pictureType = Workbook.PICTURE_TYPE_PNG;
                ImageIO.write(resized, "png", baos);
            } else {
                // 지원하지 않는 형식이면 건너뜀
                continue;
            }

            byte[] bytes = baos.toByteArray();
            int pictureIdx = workbook.addPicture(bytes, pictureType);

            // 이미지 위에 텍스트 넣기
            int labelRowIndex = rowIndex - 2;
            if (labelRowIndex < 0) labelRowIndex = 0;
            Row labelRow = thirdSheet.getRow(labelRowIndex);
            if (labelRow == null) labelRow = thirdSheet.createRow(labelRowIndex);

            Cell labelCell = labelRow.getCell(colIndex);
            if (labelCell == null) labelCell = labelRow.createCell(colIndex);

            String label = String.format("<%02d월 %02d일 택시비 영수증>",
                    dto.getPaymentDttm().getMonthValue(),
                    dto.getPaymentDttm().getDayOfMonth());
            labelCell.setCellValue(label);

            // 이미지 넣기
            Row row = thirdSheet.getRow(rowIndex);
            if (row == null) row = thirdSheet.createRow(rowIndex);

            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(colIndex);
            anchor.setRow1(rowIndex);

            Picture pict = drawing.createPicture(anchor, pictureIdx);
            pict.resize(); // 이미 리사이즈 되어 있으므로 비율 유지

            // 다음 위치 계산
            colIndex += columnCell;
            if (colIndex >= maxCols) {
                colIndex = 1;
                rowIndex += rowCell;
            }
        }
    }
}

package com.taxiexcel;

import com.taxiexcel.dto.ExcelValueDTO;
import com.taxiexcel.dto.TexiInfoDTO;
import com.taxiexcel.service.ExcelService;
import com.taxiexcel.service.TaxiOcrService;
import com.taxiexcel.ui.ExcelGeneratorUI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.*;

@Component
@RequiredArgsConstructor
public class AppLauncher {

    private final TaxiOcrService taxiOcrService;
    private final ExcelService excelService;

    public void launch(ExcelValueDTO excelValueDTO, ExcelGeneratorUI ui) throws Exception {
        // 1. 이미지 읽기
        File imageFolder = new File(excelValueDTO.getImageFileDirPath());

        if (!imageFolder.isDirectory()) {
            throw new Exception("해당 경로는 디렉토리가 아닙니다.");
        }

        File[] fileImages = imageFolder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".gif");
        });

        if (fileImages == null || fileImages.length == 0) {
            throw new Exception("해당 디렉토리에 이미지 파일이 존재하지 않습니다.");
        }

        // 이미지 정보 가져오기
        List<String> failedFiles = new ArrayList<>();
        Map<String, List<TexiInfoDTO>> texiInfoListMap = new HashMap<>();
        for (File fileImage : List.of(fileImages)) {
            TexiInfoDTO texiInfoDTO = taxiOcrService.ocrKkaoTexiImage(fileImage, failedFiles);

            if (texiInfoDTO != null) {
                // 해당 DTO의 결재일자를 조회하여 해당 년, 월에 맞는 Map의 List에 add
                String key = texiInfoDTO.getPaymentDttm().getYear() + "/" + texiInfoDTO.getPaymentDttm().getMonthValue();

                texiInfoListMap
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(texiInfoDTO);
            }
        }

        // Map에 각각의 List 결재일자 오름차순으로 sort
        for (List<TexiInfoDTO> list : texiInfoListMap.values()) {
            list.sort(Comparator.comparing(TexiInfoDTO::getPaymentDttm));
        }

        System.out.println("Map<TexiInfoDTO> : " + texiInfoListMap);

        // 엑셀파일
        excelService.createExcelFromTemplate(texiInfoListMap, excelValueDTO);

        // 실패 이미지 UI에 전달
        if (!failedFiles.isEmpty()) {
            ui.showFailedFiles(failedFiles);
        }
    }
}

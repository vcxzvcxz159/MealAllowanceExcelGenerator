package com.taxiexcel.service;


import com.taxiexcel.dto.TexiInfoDTO;
import com.taxiexcel.mapper.KeyMapper;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TaxiOcrService {

    private final KeyMapper keyMapper;
    private final Tesseract tesseract;
    public TaxiOcrService(KeyMapper keyMapper) throws IOException {

        // Tesseract 초기화
        Tesseract tesseract = new Tesseract();

        // resources/tessdata 폴더를 임시 폴더로 복사
        File tempTessDataDir = Files.createTempDirectory("tessdata").toFile();
        tempTessDataDir.deleteOnExit(); // 프로그램 종료 시 삭제

        // kor.traineddata 등 파일 복사
        String[] languages = {"kor.traineddata", "eng.traineddata"}; // 필요에 따라 추가
        for (String langFile : languages) {
            ClassPathResource res = new ClassPathResource("tessdata/" + langFile);
            File targetFile = new File(tempTessDataDir, langFile);
            Files.copy(res.getInputStream(), targetFile.toPath());
        }

        // Tesseract datapath와 언어 설정
        tesseract.setDatapath(tempTessDataDir.getAbsolutePath());
        tesseract.setLanguage("kor");

        this.tesseract = tesseract;
        this.keyMapper = keyMapper;
    }

    public TexiInfoDTO ocrKkaoTexiImage(File imageFile, List<String> failedFiles) throws Exception {

        int MAX_RETRY = 3;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                BufferedImage image = ImageIO.read(imageFile);

                Map<String, Object> texiInfoMap = this.ocrSection(tesseract, image, keyMapper);

                TexiInfoDTO texiInfoDTO = this.mapToInfo(texiInfoMap);

                // 필수값 검증
                if (isValid(texiInfoDTO)) {
                    // 결제일자 세팅
                    texiInfoDTO.setPaymentDttm(
                            LocalDateTime.parse(texiInfoDTO.getPaymentDate().split("/")[0].trim(),
                                    DateTimeFormatter.ofPattern("yy.MM.dd HH:mm"))
                    );

                    // 이미지 세팅
                    texiInfoDTO.setImageFile(imageFile);

                    // 결제 금액 세팅
                    texiInfoDTO.setPaidAmount(calculatePaidAmountSum(texiInfoDTO.getPaidAmount()));

                    return texiInfoDTO;
                } else {
                    System.out.println("Validation failed for " + imageFile.getName() + ", attempt " + attempt);
                }

            } catch (Exception e) {
                System.out.println("OCR error on " + imageFile.getName() + ", attempt " + attempt + " : " + e.getMessage());
            }
        }

        // 3회 실패 후 실패 리스트에 저장
        failedFiles.add(imageFile.getName());
        return null;
    }

    private boolean isValid(TexiInfoDTO texiInfoDTO) {

        if (texiInfoDTO.getPaymentDate() == null || texiInfoDTO.getPaymentDate().isEmpty()) {
            return false;
        }

        if (texiInfoDTO.getPaidAmount() == null || texiInfoDTO.getPaidAmount().isEmpty()) {
            return false;
        }

        return true;
    }
    // 문자열로 된 금액들을 합산
    private String calculatePaidAmountSum(String paidAmountStr) {

        return String.valueOf(Arrays.stream(paidAmountStr.split("/"))
                .map(String::trim)
                .mapToInt(s -> Integer.parseInt(s.replaceAll("[^0-9]", "")))
                .sum());

    }
    private Map<String, Object> ocrSection(Tesseract tesseract, BufferedImage image, KeyMapper mapper) throws TesseractException {

        String text = tesseract.doOCR(image);

        Map<String, Object> map = new LinkedHashMap<>();
        for (String line : text.split("\\r?\\n")) {
            String[] parts = line.split("\\s{2,}|\\s*:\\s*", 2); // 공백 또는 ':' 기준으로 split
            if (parts.length == 2) {
                String key = mapper.getEnglishKey(parts[0].replaceAll("\\p{Z}", "").replaceAll("\\s", "").trim());
                String value = parts[1].trim();

                if (map.containsKey(key)) {
                    Object existing = map.get(key);
                    List<String> list;
                    if (existing instanceof List) {
                        list = (List<String>) existing;
                    } else {
                        list = new ArrayList<>();
                        list.add(existing.toString());
                    }
                    list.add(value);
                    map.put(key, list);
                } else {
                    map.put(key, value);
                }
            }
        }
        return map;
    }


    @SafeVarargs
    private TexiInfoDTO mapToInfo(Map<String, Object>... maps) {
        TexiInfoDTO texiInfoDTO = new TexiInfoDTO();

        for (Map<String, Object> map : maps) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                try {
                    Field field = TexiInfoDTO.class.getDeclaredField(entry.getKey());
                    field.setAccessible(true);

                    Object value = entry.getValue();

                    // 만약 List라면 콤마로 합쳐서 하나의 문자열로 변환
                    if (value instanceof List<?> list) {
                        value = String.join("/ ", list.stream().map(Object::toString).toList());
                        field.set(texiInfoDTO, value);
                    } else if (value instanceof String str) {
                        field.set(texiInfoDTO, str);
                    }

                } catch (NoSuchFieldException ignored) {
                    // DTO에 없는 key는 무시
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return texiInfoDTO;
    }
}

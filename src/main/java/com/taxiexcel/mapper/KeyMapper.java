package com.taxiexcel.mapper;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class KeyMapper {

    private static final String KEY_MAPPING_PROPERTIES = "/dictionary/key_mapping.properties";
    private final Properties prop = new Properties();

    public KeyMapper() throws IOException {
        // "/"로 시작 → classpath 루트 기준
        try (InputStream is = getClass().getResourceAsStream(KEY_MAPPING_PROPERTIES)) {
            if (is != null) {
                prop.load(new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
            } else {
                throw new RuntimeException("key_mapping.properties 파일을 찾을 수 없습니다.");
            }
        }
    }

    public String getEnglishKey(String koreanKey) {
        return prop.getProperty(koreanKey, koreanKey); // 매핑 없으면 그대로
    }
}

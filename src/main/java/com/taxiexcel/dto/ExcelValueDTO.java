package com.taxiexcel.dto;

import lombok.Data;

@Data
public class ExcelValueDTO {

    private String proposer;
    private String teamLeader;
    private String author;
    private String position;
    private String department;

    private String imageFileDirPath;
    private String excelFileDirPath;
}

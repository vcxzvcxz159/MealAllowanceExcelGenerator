package com.taxiexcel.dto;

import lombok.Data;

import java.io.File;
import java.time.LocalDateTime;

@Data
public class TexiInfoDTO {

    private String departure;
    private String destination;
    private String driveTime;
    private String callOption;
    private String carNum;
    private String driverNm;
    private String brand;
    private String driveFee;
    private String paidAmount;
    private String paymentMethod;
    private String paymentDate;

    private LocalDateTime paymentDttm;
    private File imageFile;
}

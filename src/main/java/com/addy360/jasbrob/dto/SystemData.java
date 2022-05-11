package com.addy360.jasbrob.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemData {
    private int availableProcessor;
    private long freeMemory;
    private long maxMemory;
    private long totalMemory;
    private Date time;

}

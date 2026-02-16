package com.megan.dataproject.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportResponse {
    private String fileName;
    private String contentType;
    private String data;
}

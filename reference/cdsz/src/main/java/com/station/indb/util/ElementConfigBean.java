package com.station.indb.util;

import lombok.Data;

@Data
public class ElementConfigBean {
    private String elementName;
    private String elementCnName;
    private float scale;
    private float offset;
    private String unit;
}

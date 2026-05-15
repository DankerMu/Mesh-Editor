package com.constants;

/**
 * @author renzhitong
 * @PROJECT_NAME: decode
 * @Package org.decode.util
 * @Description: java类作用描述
 * @date 2022/10/24 14:50
 * @Version: 1.0
 */
public enum DataTypeEnum {
    ECMF("ecmf"),
    ECMWF("ecmwf"),
    T1279("t1279"),
    KWBC("kwbc"),
    GRAPES("grapes"),
    QQZQ("qqzq"),
    SODA("soda"),
    HJC("hjc"),
    HT("ht"),
    EC("ec"),
    ED("ed"),
    KW("kw"),
    RJ("rj"),
    ERA5("era5"),
    WW3("ww3"),
    KT("kt"),
    SWC9KM("swc9km"),
    SWC3KM("swc3km"),
    SCMOC("scmoc"),
    CMPA("cmpa"),
    CLDAS("cldas"),
    PRODUCT("product"),
    EXCEL("excel"),
    
    ECMFNC("ecmfnc"),
    STATION("station"),
    RAIN("rain"),
    DEEP("deep"),
    PTYPE("ptype"),
    MICAPS("micaps"),
    MICAPSHIS("micapshis"),
    FY4B("fy4b"),

    DEFAULT("default");

    private String dataType;
    private DataTypeEnum(String dataType) {
        this.dataType = dataType;
    }

    public String getDataType()
    {
        return this.dataType;
    }
}

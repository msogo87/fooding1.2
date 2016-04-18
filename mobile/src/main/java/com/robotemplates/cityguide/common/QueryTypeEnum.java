package com.robotemplates.cityguide.common;

/**
 * Created by msogolovsky on 13/04/2016.
 */
public enum QueryTypeEnum {
    QUERY_LIST(0),
    QUERY_MAP(1) ,
    QUERY_POI(2) ;

    private final int value;
    private QueryTypeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

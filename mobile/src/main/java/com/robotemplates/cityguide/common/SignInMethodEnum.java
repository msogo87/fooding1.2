package com.robotemplates.cityguide.common;

/**
 * Created by msogolovsky on 13/04/2016.
 */
public enum SignInMethodEnum {
    SIGN_IN_METHOD_NONE      (0),
    SIGN_IN_METHOD_FACEBOOK  (1),
    SIGN_IN_METHOD_GOOGLE    (2);

    private final int value;
    private SignInMethodEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

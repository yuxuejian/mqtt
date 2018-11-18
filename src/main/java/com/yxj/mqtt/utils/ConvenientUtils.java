package com.yxj.mqtt.utils;

public class ConvenientUtils {
    public static final String OS_NAME = System.getProperty("os.name");
    private static boolean isLinuxPlatform = false;
    private static boolean isWindowsPlatform = false;

    static {
        if (OS_NAME != null && OS_NAME.equalsIgnoreCase("linux")) {
            isLinuxPlatform = true;
        }
        if (OS_NAME != null && OS_NAME.equalsIgnoreCase("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static  boolean isWindowsPlatform() {
        return isWindowsPlatform;
    }
}

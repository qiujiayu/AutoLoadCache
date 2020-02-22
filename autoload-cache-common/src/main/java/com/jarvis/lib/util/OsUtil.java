package com.jarvis.lib.util;

/**
 * @author: jiayu.qiu
 */
public class OsUtil {

    private static OsUtil instance = new OsUtil();

    private static boolean isLinux;

    static {
        String os = System.getProperty("os.name");
        String linux = "LINUX";
        if ((os != null) && (os.toUpperCase().indexOf(linux) > -1)) {
            isLinux = true;
        } else {
            isLinux = false;
        }
    }

    public static OsUtil getInstance() {
        return instance;
    }

    public boolean isLinux() {
        return isLinux;
    }
}

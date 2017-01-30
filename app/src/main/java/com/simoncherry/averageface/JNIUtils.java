package com.simoncherry.averageface;

/**
 * Created by Simon on 2017/1/21.
 */

public class JNIUtils {

    static {
        System.loadLibrary("jsoncpp");
        System.loadLibrary("JNI_APP");
    }

    public static native int[] doGrayScale(int[] buf, int w, int h);

    public static native int[] doEdgeDetection(int[] buf, int w, int h);

    public static native int[] doBinaryzation(int[] buf, int w, int h);

    public static native void testParseJson(String str);

    public static native void testReadFile(String path);

    public static native String averageFaceTest(String path);
}

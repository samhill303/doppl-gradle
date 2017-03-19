package co.touchlab.doppl.utils;
/**
 * Created by kgalligan on 5/10/16.
 */
public class PlatformUtils
{
    public static boolean isJ2objc()
    {
        return System.getProperty("java.vendor").contains("J2ObjC");
    }
}

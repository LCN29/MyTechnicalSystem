package com.lcn29.spring2.util;

/**
 * <pre>
 *  对象工具类
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 19:10
 */
public class ObjectUtils {

    public static String getIdentityHexString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }
}

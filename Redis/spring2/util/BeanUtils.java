package com.lcn29.spring2.util;

import java.lang.reflect.Constructor;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 17:11
 */
public class BeanUtils {

    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo)  {
        return (T) instantiateClass(clazz);
    }

    public static <T> T instantiateClass(Class<T> clazz){

        if (clazz.isInterface()) {
            throw new RuntimeException("Specified class is an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws Exception {

        try {
            return ctor.newInstance(args);
        } catch (Exception ex) {
            throw new Exception("Is it an abstract class?", ex);
        }
    }

}

package com.lcn29.spring2.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 18:22
 */
public class ResourcePatternUtils {
    public static boolean isUrl(String resourceLocation) {
        return (resourceLocation != null &&
                (resourceLocation.startsWith("classpath*:") ||
                        ResourceUtils.isUrl(resourceLocation)));
    }

    public static URI toURI(String location) throws URISyntaxException {
        return new URI(StringUtils.replace(location, " ", "%20"));
    }
}

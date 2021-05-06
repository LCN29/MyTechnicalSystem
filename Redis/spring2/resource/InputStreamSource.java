package com.lcn29.spring2.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:35
 */
public interface InputStreamSource {

    InputStream getInputStream() throws IOException;
}

package com.lcn29.spring2.resource;

import com.sun.istack.internal.Nullable;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-28 10:13
 */
@Data
public class EncodedResource implements InputStreamSource {

    private final Resource resource;

    private final String encoding;

    private final Charset charset;

    public EncodedResource(Resource resource) {
        this(resource, null, null);
    }

    public EncodedResource(Resource resource, String encoding) {
        this(resource, encoding, null);
    }

    public EncodedResource(Resource resource, Charset charset) {
        this(resource, null, charset);
    }

    private EncodedResource(Resource resource, @Nullable String encoding, @Nullable Charset charset) {
        super();
        this.resource = resource;
        this.encoding = encoding;
        this.charset = charset;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.resource.getInputStream();
    }


}

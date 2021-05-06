package com.lcn29.spring2.reader;

import com.lcn29.spring2.resource.Resource;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-28 11:15
 */
public class ReaderContext {

    private final Resource resource;

    private final SourceExtractor sourceExtractor;

    public ReaderContext(Resource resource, SourceExtractor sourceExtractor) {
        this.resource = resource;
        this.sourceExtractor = sourceExtractor;
    }

    public final Resource getResource() {
        return this.resource;
    }

    public Object extractSource(Object sourceCandidate) {
        return this.sourceExtractor.extractSource(sourceCandidate, this.resource);
    }
}

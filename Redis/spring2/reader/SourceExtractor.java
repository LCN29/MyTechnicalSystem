package com.lcn29.spring2.reader;

import com.lcn29.spring2.resource.Resource;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 17:28
 */
@FunctionalInterface

public interface SourceExtractor {

    Object extractSource(Object sourceCandidate, Resource definingResource);
}

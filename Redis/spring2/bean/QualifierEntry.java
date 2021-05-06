package com.lcn29.spring2.bean;

import com.lcn29.spring2.reader.ParseState;
import com.lcn29.spring2.util.StringUtils;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 16:05
 */
public class QualifierEntry implements ParseState.Entry {

    private final String typeName;

    public QualifierEntry(String typeName) {
        if (!StringUtils.hasText(typeName)) {
            throw new IllegalArgumentException("Invalid qualifier type '" + typeName + "'");
        }
        this.typeName = typeName;
    }
}

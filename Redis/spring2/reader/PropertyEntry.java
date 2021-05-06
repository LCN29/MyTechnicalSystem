package com.lcn29.spring2.reader;

import com.lcn29.spring2.util.StringUtils;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 13:53
 */
public class PropertyEntry implements ParseState.Entry{

    private final String name;

    public PropertyEntry(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Invalid property name '" + name + "'");
        }

        this.name = name;
    }
}

package com.lcn29.spring2.reader;

import lombok.Data;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 17:36
 */
@Data
public class ConstructorArgumentEntry implements ParseState.Entry {

    private final int index;

    public ConstructorArgumentEntry() {
        this.index = -1;
    }

    public ConstructorArgumentEntry(int index) {
        this.index = index;
    }

}

package com.lcn29.spring2.bean;

import com.sun.istack.internal.Nullable;
import lombok.Data;

import java.util.Properties;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 15:57
 */
@Data
public class ManagedProperties extends Properties implements Mergeable, BeanMetadataElement {

    private Object source;

    private boolean mergeEnabled;

    @Override
    public Object merge(@Nullable Object parent) {
        if (!this.mergeEnabled) {
            throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
        }
        if (parent == null) {
            return this;
        }
        if (!(parent instanceof Properties)) {
            throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
        }
        Properties merged = new ManagedProperties();
        merged.putAll((Properties) parent);
        merged.putAll(this);
        return merged;
    }
}

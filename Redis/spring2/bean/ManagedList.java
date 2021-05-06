package com.lcn29.spring2.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 15:25
 */
@Data
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement{

    private Object source;

    private String elementTypeName;

    private boolean mergeEnabled;

    public ManagedList() {
    }

    public ManagedList(int initialCapacity) {
        super(initialCapacity);
    }


    @Override
    public List<E> merge(Object parent) {
        if (!this.mergeEnabled) {
            throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
        }

        if (parent == null) {
            return this;
        }
        if (!(parent instanceof List)) {
            throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
        }

        List<E> merged = new ManagedList<>();
        merged.addAll((List<E>) parent);
        merged.addAll(this);
        return merged;
    }

}

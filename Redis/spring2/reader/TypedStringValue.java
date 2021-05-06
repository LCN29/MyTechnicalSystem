package com.lcn29.spring2.reader;

import com.lcn29.spring2.bean.BeanMetadataElement;
import lombok.Data;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 13:42
 */
@Data
public class TypedStringValue implements BeanMetadataElement {

    private String value;

    private volatile Object targetType;

    private Object source;

    private String specifiedTypeName;

    private volatile boolean dynamic;

    public TypedStringValue(String value) {
        setValue(value);
    }

    public TypedStringValue(String value, Class<?> targetType) {
        setValue(value);
        setTargetType(targetType);
    }

    public TypedStringValue(String value, String targetTypeName) {
        setValue(value);
        setTargetTypeName(targetTypeName);
    }

    public void setTargetTypeName(String targetTypeName) {
        this.targetType = targetTypeName;
    }
}

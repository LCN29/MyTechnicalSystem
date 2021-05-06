package com.lcn29.spring2.bean;

import com.lcn29.spring2.reader.AttributeAccessor;
import com.lcn29.spring2.reader.BeanMetadataAttributeAccessor;
import lombok.Data;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-05-02 13:56
 */
@Data
public class PropertyValue extends BeanMetadataAttributeAccessor {

    private final String name;

    private final Object value;

    private boolean optional = false;

    private boolean converted = false;

    private Object convertedValue;

    volatile Boolean conversionNecessary;

    transient volatile Object resolvedTokens;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public PropertyValue(PropertyValue original) {
        this.name = original.getName();
        this.value = original.getValue();
        this.optional = original.isOptional();
        this.converted = original.converted;
        this.convertedValue = original.convertedValue;
        this.conversionNecessary = original.conversionNecessary;
        this.resolvedTokens = original.resolvedTokens;
        setSource(original.getSource());
        copyAttributesFrom(original);
    }

    protected void copyAttributesFrom(AttributeAccessor source) {
        String[] attributeNames = source.attributeNames();
        for (String attributeName : attributeNames) {
            setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }
}

package com.lcn29.spring2.bean;

import com.lcn29.spring2.reader.BeanMetadataAttributeAccessor;
import com.lcn29.spring2.reader.MethodOverrides;
import com.lcn29.spring2.reader.MutablePropertyValues;
import com.lcn29.spring2.resource.Resource;
import com.sun.istack.internal.Nullable;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 19:08
 */
@Data
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor implements BeanDefinition {

    private volatile Object beanClass;

    private String factoryBeanName;

    private String factoryMethodName;

    private String description;

    private String scope = "";

    private boolean abstractFlag = false;

    private boolean lazyInit = false;

    private Resource resource;

    private int dependencyCheck = 0;

    private int autowireMode = 0;

    private String[] dependsOn;

    private boolean autowireCandidate = true;

    private boolean primary = false;


    private MethodOverrides methodOverrides = new MethodOverrides();

    private ConstructorArgumentValues constructorArgumentValues;

    private MutablePropertyValues propertyValues;

    private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();


    public Class<?> getBeanClass() throws IllegalStateException {
        Object beanClassObject = this.beanClass;
        if (beanClassObject == null) {
            throw new IllegalStateException("No bean class specified on bean definition");
        }
        if (!(beanClassObject instanceof Class)) {
            throw new IllegalStateException(
                    "Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
        }
        return (Class<?>) beanClassObject;
    }

    @Override
    public String getBeanClassName() {
        Object beanClassObject = this.beanClass;
        if (beanClassObject instanceof Class) {
            return ((Class<?>) beanClassObject).getName();
        }
        else {
            return (String) beanClassObject;
        }
    }

    public void setAbstract(boolean abstractFlag) {
        this.abstractFlag = abstractFlag;
    }

    public void addQualifier(AutowireCandidateQualifier qualifier) {
        this.qualifiers.put(qualifier.getTypeName(), qualifier);
    }

    @Override
    public void setBeanClassName(@Nullable String beanClassName) {
        this.beanClass = beanClassName;
    }

    @Override
    public boolean isAbstract() {
        return this.abstractFlag;
    }

}

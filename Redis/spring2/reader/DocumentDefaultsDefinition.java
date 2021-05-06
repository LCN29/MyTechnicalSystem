package com.lcn29.spring2.reader;

import lombok.Data;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-28 17:11
 */
@Data
public class DocumentDefaultsDefinition implements DefaultsDefinition{

    private String lazyInit;

    private String merge;

    private String autowire;

    private String autowireCandidates;

    private String initMethod;

    private String destroyMethod;

    private Object source;

}

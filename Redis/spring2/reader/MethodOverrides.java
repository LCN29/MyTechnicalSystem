package com.lcn29.spring2.reader;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 17:06
 */
public class MethodOverrides {

    private final Set<MethodOverride> overrides = new CopyOnWriteArraySet<>();

    public MethodOverrides() {
    }

    public MethodOverrides(MethodOverrides other) {
        addOverrides(other);
    }

    public void addOverrides(MethodOverrides other) {
        if (other != null) {
            this.overrides.addAll(other.overrides);
        }
    }

    public void addOverride(MethodOverride override) {
        this.overrides.add(override);
    }

}

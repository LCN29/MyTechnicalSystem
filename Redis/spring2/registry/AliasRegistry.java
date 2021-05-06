package com.lcn29.spring2.registry;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-27 20:38
 */
public interface AliasRegistry {

    /**
     * 别名注册
     *
     * @param name  bean 名
     * @param alias 别名
     */
    void registerAlias(String name, String alias);
}

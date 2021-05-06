package com.lcn29.spring2.util;

import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <pre>
 *
 * </pre>
 *
 * @author lcn29
 * @date 2021-04-29 11:09
 */
public class DomUtils {


    public static String getChildElementValueByTagName(Element ele, String childEleName) {
        Element child = getChildElementByTagName(ele, childEleName);
        return (child != null ? getTextValue(child) : null);
    }

    public static Element getChildElementByTagName(Element ele, String childEleName) {
        NodeList nl = ele.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element && nodeNameMatch(node, childEleName)) {
                return (Element) node;
            }
        }
        return null;
    }

    public static List<Element> getChildElementsByTagName(Element ele, String childEleName) {
        return getChildElementsByTagName(ele, new String[] {childEleName});
    }

    private static boolean nodeNameMatch(Node node, String desiredName) {
        return (desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName()));
    }

    private static boolean nodeNameMatch(Node node, Collection<?> desiredNames) {
        return (desiredNames.contains(node.getNodeName()) || desiredNames.contains(node.getLocalName()));
    }

    public static String getTextValue(Element valueEle) {
        StringBuilder sb = new StringBuilder();
        NodeList nl = valueEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                sb.append(item.getNodeValue());
            }
        }
        return sb.toString();
    }

    public static List<Element> getChildElementsByTagName(Element ele, String... childEleNames) {
        List<String> childEleNameList = Arrays.asList(childEleNames);
        NodeList nl = ele.getChildNodes();
        List<Element> childEles = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element && nodeNameMatch(node, childEleNameList)) {
                childEles.add((Element) node);
            }
        }
        return childEles;
    }
}

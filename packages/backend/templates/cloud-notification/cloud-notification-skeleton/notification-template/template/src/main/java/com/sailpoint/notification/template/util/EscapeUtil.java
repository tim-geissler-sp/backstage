package com.sailpoint.notification.template.util;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.tools.generic.EscapeTool;

/**
 * Extends {@link EscapeTool EscapeTool} class and overrides javascript method
 * to apply json escaping instead of JavaScript as it doesn't contain any escapeJson methods.
 */
public class EscapeUtil extends EscapeTool {

    @Override
    public String javascript(Object string) {
        return  string == null ? null : StringEscapeUtils.escapeJson(String.valueOf(string));
    }
}
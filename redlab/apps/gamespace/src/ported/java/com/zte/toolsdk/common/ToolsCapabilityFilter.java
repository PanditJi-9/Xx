package com.zte.toolsdk.common;

import java.util.List;

/* loaded from: classes2.dex */
public class ToolsCapabilityFilter {
    String function_name;
    List<String> language;
    String package_name;
    List<String> region;
    String role;

    public ToolsCapabilityFilter(String packageName) {
        this(packageName, null, null, null, null);
    }

    public ToolsCapabilityFilter(String packageName, String functionName, String role, List<String> region, List<String> language) {
        this.package_name = packageName;
        this.function_name = functionName;
        this.role = role;
        this.region = region;
        this.language = language;
    }
}

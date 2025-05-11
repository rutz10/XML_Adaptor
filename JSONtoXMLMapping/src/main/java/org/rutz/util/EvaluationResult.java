package org.rutz.util;

import com.fasterxml.jackson.databind.JsonNode;

public class EvaluationResult {
    private final boolean shouldProcessRule;
    private final String valueToTransform; // Raw string value from jPath or defaultValue
    private final JsonNode contextForChildren;
    private final boolean derivedFromDefaultValue; // Flag to indicate if valueToTransform came from defaultValue

    private EvaluationResult(boolean shouldProcess, String value, JsonNode childContext, boolean derivedFromDefault) {
        this.shouldProcessRule = shouldProcess;
        this.valueToTransform = value;
        this.contextForChildren = childContext;
        this.derivedFromDefaultValue = derivedFromDefault;
    }

    public static EvaluationResult doNotProcess() {
        return new EvaluationResult(false, null, null, false);
    }

    public static EvaluationResult processRule(String valueToTransform, JsonNode contextForChildren, boolean derivedFromDefault) {
        return new EvaluationResult(true, valueToTransform, contextForChildren, derivedFromDefault);
    }

    public boolean shouldProcess() {
        return shouldProcessRule;
    }

    public String getValueToTransform() {
        return valueToTransform;
    }

    public JsonNode getContextForChildren() {
        return contextForChildren;
    }

    public boolean isDerivedFromDefaultValue() {
        return derivedFromDefaultValue;
    }
} 
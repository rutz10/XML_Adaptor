# Refactoring Plan: Generic Conditional and Derived Field Processing

This document outlines the plan to refactor `JsonToXmlSteam.java` to support generic conditional and derived field processing based entirely on the mapping CSV configuration. The core of this refactoring involves introducing a `MappingRuleEvaluator` helper class.

## 1. Goals

*   Remove all hardcoded logic in `JsonToXmlSteam.java` that handles specific field names (e.g., "otherfullname", "Status") for conditional or derived value generation.
*   Enable the system to process any conditional/derived field logic defined declaratively in the mapping CSV via `conditionJPath`, `jPath`, and `defaultValue` columns.
*   Improve code modularity, maintainability, and testability by separating rule evaluation logic from XML generation and stream traversal.
*   Make `JsonToXmlSteam.java` cleaner and more focused on its core responsibilities.

## 2. Proposed Architecture: `MappingRuleEvaluator`

A new helper class, `MappingRuleEvaluator`, will be introduced. Its primary responsibilities will be:

*   For a given `Mapping` rule and the current `JsonNode` context:
    *   Evaluate if the rule's `conditionJPath` is met.
    *   Determine the source of the XML element's text value (from `jPath` or `defaultValue`).
    *   Identify the appropriate `JsonNode` context for processing any child mappings of the current rule.

An `EvaluationResult` class will be used to pass this information back to the caller (`JsonToXmlSteam`).

### 2.1. `EvaluationResult.java`

This class will act as a data carrier for the outcome of a rule evaluation.

```java
package org.rutz.util; // Or an appropriate package

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
```

### 2.2. `MappingRuleEvaluator.java`

This class will contain the logic for evaluating mapping rules.

```java
package org.rutz.util; // Or an appropriate package

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.rutz.mapping.Mapping; // Assuming Mapping class location
import org.rutz.steam.JsonToXmlSteam; // For convertJsonPathToJsonPointer

public class MappingRuleEvaluator {

    private final JsonNode globalJsonRoot;

    public MappingRuleEvaluator(JsonNode globalJsonRoot) {
        if (globalJsonRoot == null) {
            throw new IllegalArgumentException("Global JSON root cannot be null.");
        }
        this.globalJsonRoot = globalJsonRoot;
    }

    public EvaluationResult evaluateRule(JsonNode currentContext, Mapping rule) {
        if (rule == null) {
            return EvaluationResult.doNotProcess();
        }
        if (currentContext == null || currentContext.isMissingNode()) {
             // If current context is missing, can only proceed if jPath is absolute or rule is condition-less default value
            if (rule.getJPath() == null || rule.getJPath().isEmpty() || !rule.getJPath().startsWith("$")) {
                 if (rule.getConditionJPath() == null || rule.getConditionJPath().isEmpty()){
                     if(rule.getDefaultValue() != null){
                         return EvaluationResult.processRule(rule.getDefaultValue(), currentContext, true);
                     }
                 }
                // Cannot resolve relative jPath or conditionJPath without a valid context
                // unless it's a simple default value without conditions or absolute jPath
                 //return EvaluationResult.doNotProcess();
            }
        }


        // 1. Evaluate conditionJPath
        String conditionJPath = rule.getConditionJPath();
        if (conditionJPath != null && !conditionJPath.isEmpty()) {
            JsonNode conditionNode = resolvePath(currentContext, conditionJPath);
            if (conditionNode == null || conditionNode.isMissingNode()) {
                return EvaluationResult.doNotProcess(); // Condition not met
            }
            // Optional: Check boolean condition
            // if (conditionNode.isBoolean() && !conditionNode.asBoolean()) {
            //     return EvaluationResult.doNotProcess();
            // }
        }

        String valueToTransform = null;
        JsonNode contextForChildren = currentContext; // Default
        boolean derivedFromDefault = false;

        // 2. Determine value from jPath
        String jPath = rule.getJPath();
        if (jPath != null && !jPath.isEmpty()) {
            JsonNode dataNode = resolvePath(currentContext, jPath);

            if (dataNode != null && !dataNode.isMissingNode()) {
                if (dataNode.isValueNode()) {
                    valueToTransform = dataNode.asText();
                }
                // If dataNode is an object or array, it becomes the context for children.
                // valueToTransform remains null if this rule is just a container for children
                // using dataNode as their context.
                contextForChildren = dataNode;
            }
        }

        // 3. Apply defaultValue if no value from jPath, or if jPath was empty
        if (valueToTransform == null && (jPath == null || jPath.isEmpty() || (contextForChildren != null && contextForChildren.isNull()) )) { // check if jPath was specified but yielded null
            if (rule.getDefaultValue() != null ) { // allow empty string as default value
                valueToTransform = rule.getDefaultValue();
                derivedFromDefault = true;
                // If defaultValue is used, contextForChildren remains as determined by currentContext,
                // unless jPath actually found a (null) node, in which case contextForChildren might have changed.
                // This usually means children operate in the parent's context when defaultValue is the source.
                 if (jPath == null || jPath.isEmpty()){
                     contextForChildren = currentContext;
                 }
            }
        }


        return EvaluationResult.processRule(valueToTransform, contextForChildren, derivedFromDefault);
    }

    /**
     * Resolves a JSON path (simple field, JSONPath, or JSON Pointer segment) against a given context.
     * - Handles "." to refer to the currentContext itself.
     * - Handles paths starting with "$" as absolute paths from the globalJsonRoot.
     * - Handles other paths as relative to the currentContext.
     */
    private JsonNode resolvePath(JsonNode currentContext, String path) {
        if (path == null || path.isEmpty()) {
            return MissingNode.getInstance();
        }
        if (".".equals(path)) {
            return currentContext;
        }

        JsonNode effectiveContext = currentContext;
        String pathToEvaluate = path;

        if (path.startsWith("$")) {
            effectiveContext = this.globalJsonRoot;
            // convertJsonPathToJsonPointer expects path without '$' for root.
            pathToEvaluate = path.length() > 1 ? path.substring(1) : "";
            if (pathToEvaluate.startsWith(".")) {
                pathToEvaluate = pathToEvaluate.substring(1);
            }
        }

        if (effectiveContext == null || effectiveContext.isMissingNode()) {
             // Cannot resolve path if context is missing, unless path was supposed to be absolute and global root is also missing (which is an issue)
            if(!path.startsWith("$")) return MissingNode.getInstance();
            // if path IS absolute and global root is missing, then constructor should have handled it or it's an error state.
            // For safety, return MissingNode.
             if(this.globalJsonRoot == null || this.globalJsonRoot.isMissingNode()) return MissingNode.getInstance();
        }


        // Attempt direct field access for simple names if not an absolute path from a potentially different effectiveContext
        if (!pathToEvaluate.contains(".") && !pathToEvaluate.contains("[") && !pathToEvaluate.contains("]") && effectiveContext != null && effectiveContext.has(pathToEvaluate)) {
            return effectiveContext.get(pathToEvaluate);
        }
        
        // Fallback to JSON Pointer for more complex paths or when direct access fails
        String pointer = JsonToXmlSteam.convertJsonPathToJsonPointer(pathToEvaluate);
        return effectiveContext.at(pointer);
    }
}
```

## 3. Refactoring `JsonToXmlSteam.java`

`JsonToXmlSteam.java` will be modified to use `MappingRuleEvaluator`.

### 3.1. Initialization
*   Store the root `JsonNode` (`globalJsonRootNode`) parsed from the input JSON string as a member variable.
*   Instantiate `MappingRuleEvaluator` in `transformJsonToXml`, passing `globalJsonRootNode`.

```java
// In JsonToXmlSteam.java
private JsonNode globalJsonRootNode;
private MappingRuleEvaluator ruleEvaluator;

public void transformJsonToXml(String jsonString, List<Mapping> mappings, String outputFilePath) throws XMLStreamException, IOException {
    // ... (parse jsonString into this.globalJsonRootNode)
    this.ruleEvaluator = new MappingRuleEvaluator(this.globalJsonRootNode);
    // ... rest of the method
}
```

### 3.2. Core Logic (e.g., in `writeXmlElement` or `processChildMappings`)

The main recursive method that processes mappings will be adapted:

```java
// Conceptual changes in a recursive processing method:
// (Method signature might be `writeXmlElement(XMLStreamWriter writer, JsonNode currentProcessingContext, Mapping mappingRule)`)
// or inside `processChildMappings` loop

// For each mappingRule:
EvaluationResult evalResult = this.ruleEvaluator.evaluateRule(currentProcessingContext, mappingRule);

if (!evalResult.shouldProcess()) {
    return; // Or continue to next sibling mapping
}

String elementName = getElementNameFromXPath(mappingRule.getXPath()); // Existing helper
// Consider if elementName itself could be dynamic based on mapping in future. For now, it's from xPath.

writer.writeStartElement(elementName);

String textContentForElement = evalResult.getValueToTransform();
JsonNode contextForChildren = evalResult.getContextForChildren();

// If contextForChildren from evalResult is null or missing, default to currentProcessingContext
if (contextForChildren == null || contextForChildren.isMissingNode()) {
    contextForChildren = currentProcessingContext;
}


if (textContentForElement != null) {
    // Apply existing AttributeLevelTransformation if not derived from default OR if expressions on default values are supported
    // The current `AttributeLevelTransformation.transform` may need access to globalJsonRootNode if JEXL expressions use absolute paths.
    String transformedValue = AttributeLevelTransformation.transform(
        textContentForElement,
        mappingRule,
        this.globalJsonRootNode // Pass global root for expressions
    );
    if (transformedValue != null) {
        writer.writeCharacters(transformedValue);
    }
} else if (mappingRule.getJPath() == null || mappingRule.getJPath().isEmpty()) {
    // If no text content AND jPath was empty, this might be a structural parent element.
    // Proceed to process children.
}


// Process child mappings
if (mappingRule.getChildMappings() != null && !mappingRule.getChildMappings().isEmpty()) {
    // Ensure childMappings are sorted by order
    List<Mapping> sortedChildMappings = mappingRule.getSortedChildMappings(); // Assuming this method exists or is added

    for (Mapping childMapping : sortedChildMappings) {
        // Recursive call:
        // writeXmlElement(writer, contextForChildren, childMapping); // if this is the recursive function
        // or adapt processChildMappings to take the contextForChildren
    }
}

writer.writeEndElement();
```

### 3.3. `processChildMappings` Adaptation
If `processChildMappings` is the primary loop for children, its signature and internal logic will change:
It will need to accept the `JsonNode parentDataForChildren` (which is `evalResult.getContextForChildren()`).
Inside the loop, when calling `writeXmlElement` for each `childMapping`, it passes this `parentDataForChildren`.

```java
// Potential adaptation of processChildMappings
private void processChildMappings(XMLStreamWriter writer, JsonNode parentDataForChildren, Mapping parentMapping) throws XMLStreamException {
    if (parentMapping.getChildMappings() != null) {
        for (Mapping childMapping : parentMapping.getSortedChildMappings()) { // Ensure sorting by 'order'
            // Current 'writeXmlElement' finds its own JsonNode based on childMapping.getJPath() and a context.
            // This needs to be harmonized. The 'parentDataForChildren' is the context against which childMapping's jPath should be resolved.
            
            // This implies writeXmlElement will itself use ruleEvaluator for each child.
            writeXmlElement(writer, parentDataForChildren, childMapping);
        }
    }
}
```
The methods `processElement`, `processArrayElement`, `processObjectElement`, and `processValueNode` will likely be simplified or their roles adjusted. The primary decision-making about *what* to write and *from where* (value or children context) will be guided by `EvaluationResult`.

### 3.4. `convertJsonPathToJsonPointer(String jsonPath)` in `JsonToXmlSteam`
*   This method needs to be robust.
*   When `MappingRuleEvaluator.resolvePath` calls it:
    *   For absolute paths (like `$.field` or `$`), it should strip the `$` and convert the rest.
    *   For relative paths (like `field` or `object.subfield`), it needs to produce a pointer that `JsonNode.at()` can use correctly relative to the `currentContext` node. E.g., "fieldName" becomes "/fieldName".

### 3.5. `AttributeLevelTransformation.transform`
*   The signature might need to change to `transform(String sourceAttributeValue, Mapping mapping, JsonNode expressionContext)` if JEXL expressions require access to values from the `globalJsonRootNode` (e.g., `fmfcn:someFunc($.path.from.root)`). The `expressionContext` would be `globalJsonRootNode`.

## 4. Removal of Hardcoded Logic

All `if/else if` blocks in `JsonToXmlSteam.java` that check for specific string values of `jPath`, `xPath`, `conditionJPath`, or `defaultValue` (e.g., `"otherfullname"`, `"Status"`, `"active"`) to implement conditional logic will be removed. The new generic evaluation flow will handle these cases based on the `Mapping` object's properties.

## 5. Testing Strategy

*   **Unit Tests for `MappingRuleEvaluator`**:
    *   Test various combinations of `jPath`, `conditionJPath`, `defaultValue`.
    *   Test with different JSON structures for `currentContext` and `globalJsonRoot`.
    *   Test `resolvePath` with simple field names, nested paths, array access (if supported by `convertJsonPathToJsonPointer`), absolute paths, and the `.` path.
    *   Test edge cases: missing context, missing paths, empty values.
*   **Unit/Integration Tests for `JsonToXmlSteam`**:
    *   Verify that existing functionality remains intact with simple mappings.
    *   Test complex scenarios previously hardcoded (like the `otherfullname` issue) now driven by CSV.
    *   Test new combinations of conditional and derived field mappings.
    *   Validate XML output against expected structures for various inputs.

## 6. Potential Challenges and Considerations

*   **Complexity of `resolvePath` and `convertJsonPathToJsonPointer`**: Ensuring these correctly handle all valid path syntaxes (simple names, dot-notation relative paths, full JSONPath for absolute, JSON Pointer conversion) is critical. The current `convertJsonPathToJsonPointer` might need significant enhancements if it doesn't fully support relative path to JSON Pointer conversion suitable for `JsonNode.at()` on a sub-context.
*   **Performance**: Introducing an extra layer of evaluation for each mapping. For very large and deeply nested structures, performance impact should be considered, though clarity and correctness are primary for now.
*   **Recursive Structure of `JsonToXmlSteam`**: Carefully adapting the existing recursive calls (`writeXmlElement`, `processChildMappings`, etc.) to correctly pass and use the `contextForChildren` obtained from `EvaluationResult` is crucial.

This detailed plan aims to create a more robust, maintainable, and flexible JSON to XML transformation engine. 
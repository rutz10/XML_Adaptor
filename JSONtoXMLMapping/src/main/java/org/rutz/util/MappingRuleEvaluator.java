package org.rutz.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.rutz.Mapping;
import org.rutz.JsonToXmlSteam;
import java.util.logging.Logger;

public class MappingRuleEvaluator {

    private static final Logger LOGGER = Logger.getLogger(MappingRuleEvaluator.class.getName());
    private final JsonNode globalJsonRoot;

    public MappingRuleEvaluator(JsonNode globalJsonRoot) {
        if (globalJsonRoot == null || globalJsonRoot.isMissingNode() || globalJsonRoot.isNull()) {
            LOGGER.warning("MappingRuleEvaluator initialized with null or missing globalJsonRoot.");
            this.globalJsonRoot = MissingNode.getInstance(); 
        } else {
            this.globalJsonRoot = globalJsonRoot;
        }
    }

    public EvaluationResult evaluateRule(JsonNode currentContext, Mapping rule) {
        if (rule == null) {
            LOGGER.finer("Rule is null, skipping processing.");
            return EvaluationResult.doNotProcess();
        }

        String ruleIdentifier = "Rule(xPath: '" + rule.getXPath() + "', jPath: '" + rule.getJPath() + "', condition: '" + rule.getConditionJPath() + "')";
        LOGGER.finer("Evaluating " + ruleIdentifier + " against context: " + (currentContext != null ? currentContext.getNodeType() : "null"));

        JsonNode effectiveCurrentContext = (currentContext == null || currentContext.isMissingNode() || currentContext.isNull()) 
                                           ? MissingNode.getInstance() 
                                           : currentContext;
        if (effectiveCurrentContext.isMissingNode()){
            LOGGER.finer("Effective current context is MissingNode for " + ruleIdentifier);
        }

        // 1. Evaluate conditionJPath
        String conditionJPath = rule.getConditionJPath();
        if (conditionJPath != null && !conditionJPath.isEmpty()) {
            LOGGER.finer("Evaluating conditionJPath: '" + conditionJPath + "' for " + ruleIdentifier);
            JsonNode conditionNode = resolvePath(effectiveCurrentContext, conditionJPath, ruleIdentifier + " condition");
            if (conditionNode == null || conditionNode.isMissingNode()) {
                LOGGER.finer("ConditionJPath '" + conditionJPath + "' not met (node missing). Skipping " + ruleIdentifier);
                return EvaluationResult.doNotProcess(); 
            }
            // Optional: Check for explicit boolean false if conditionNode is boolean
            if (conditionNode.isBoolean() && !conditionNode.asBoolean()) {
                 LOGGER.finer("ConditionJPath '" + conditionJPath + "' evaluated to boolean false. Skipping " + ruleIdentifier);
                 return EvaluationResult.doNotProcess();
            }
            LOGGER.finer("ConditionJPath '" + conditionJPath + "' met for " + ruleIdentifier);
        }

        String valueToTransform = null;
        JsonNode contextForChildren = effectiveCurrentContext; 
        boolean derivedFromDefault = false;

        // 2. Determine value and context for children from jPath
        String jPath = rule.getJPath();
        if (jPath != null && !jPath.isEmpty()) {
            LOGGER.finer("Resolving jPath: '" + jPath + "' for " + ruleIdentifier);
            JsonNode dataNode = resolvePath(effectiveCurrentContext, jPath, ruleIdentifier + " data");

            if (dataNode == null || dataNode.isMissingNode()) {
                LOGGER.finer("jPath '" + jPath + "' resolved to MissingNode. Rule will not produce a value based on jPath for " + ruleIdentifier + ". No default value considered yet.");
                contextForChildren = MissingNode.getInstance();
            } else {
                LOGGER.finer("jPath '" + jPath + "' resolved to node type: " + dataNode.getNodeType() + " for " + ruleIdentifier);
                if (dataNode.isValueNode()) {
                    String tempValue = dataNode.asText(null);
                    if (tempValue != null && !tempValue.isEmpty()) {
                        valueToTransform = tempValue;
                        LOGGER.finer("Value from jPath '" + jPath + "': '" + valueToTransform + "' for " + ruleIdentifier);
                        contextForChildren = effectiveCurrentContext; 
                    } else {
                        LOGGER.finer("jPath '" + jPath + "' resolved to a value node but text is null or empty ('"+tempValue+"'). Treating as no value provided by this jPath for " + ruleIdentifier);
                        contextForChildren = MissingNode.getInstance();
                    }
                } else {
                    LOGGER.finer("jPath '" + jPath + "' resolved to a non-value node (Object/Array). Setting as contextForChildren for " + ruleIdentifier);
                    contextForChildren = dataNode;
                }
            }
        }

        // 3. Apply defaultValue if applicable
        // A defaultValue is used if: 
        //    a) No jPath was specified (valueToTransform is null), AND condition (if any) was met.
        //    b) jPath was specified but resolved to MissingNode (valueToTransform is null), AND condition (if any) was met.
        if (valueToTransform == null && rule.getDefaultValue() != null) {
             // Check if conditionJPath logic already decided to skip. If so, defaultValue is irrelevant here.
            // This check is implicitly handled if this part of code is reached after conditionJPath check passes.
            LOGGER.finer("No value from jPath for " + ruleIdentifier + ". Considering defaultValue: '" + rule.getDefaultValue() + "'.");
            valueToTransform = rule.getDefaultValue();
            derivedFromDefault = true;
            LOGGER.finer("Using defaultValue '" + valueToTransform + "' for " + ruleIdentifier);
            // If defaultValue is used, contextForChildren usually remains the effectiveCurrentContext, 
            // unless jPath already set it to something more specific (even if that jPath didn't yield a direct value).
            if (jPath == null || jPath.isEmpty() || (contextForChildren != null && contextForChildren.isMissingNode())) {
                 contextForChildren = effectiveCurrentContext;
            }
        }
        
        // 4. Final check: if no jPath and no conditionJPath+defaultValue, this rule should not process.
        // This is particularly for structural elements that might have children but no direct value themselves.
        // However, if it has children, it should still process to create the parent tag.
        // The `writeXmlElement` in `JsonToXmlSteam` handles writing the tag, and then calls for children.
        // This `evaluateRule` is more about *what value* to put in the tag and *what context* for children.

        // **Revised logic for shouldProcess:**
        // A rule should NOT process if a specific jPath was given but it yielded no data, AND no defaultValue is applicable.
        if (jPath != null && !jPath.isEmpty() && (valueToTransform == null && !derivedFromDefault)) {
             JsonNode checkDataNode = resolvePath(effectiveCurrentContext, jPath, ruleIdentifier + " final check");
             if (checkDataNode == null || checkDataNode.isMissingNode()){
                LOGGER.finer("Final check: jPath '" + jPath + "' was specified but yielded no data, and no default was used. Skipping " + ruleIdentifier);
                return EvaluationResult.doNotProcess();
             }
        }

        // If contextForChildren ended up being null/missing, default it to the parent's context for safety in recursion.
        if (contextForChildren == null || contextForChildren.isMissingNode() || contextForChildren.isNull()){
            LOGGER.finer("ContextForChildren is missing/null for " + ruleIdentifier + ". Defaulting to effectiveCurrentContext.");
            contextForChildren = effectiveCurrentContext;
        }

        // Decision point:
        // A rule should only proceed to `processRule` if it's genuinely supposed to create an element.

        // Case 1: Rule is purely structural (no jPath, no conditionJPath).
        // Such rules always process to create their tag, allowing children to be processed.
        // valueToTransform will be null, derivedFromDefault is false.
        // contextForChildren has been set to effectiveCurrentContext earlier if jPath was empty.
        if ((jPath == null || jPath.isEmpty()) && (conditionJPath == null || conditionJPath.isEmpty())) {
            LOGGER.finer("Decision: Purely structural rule (no jPath, no conditionJPath). Processing for " + ruleIdentifier);
            // Ensure contextForChildren is set if it was modified by an absent jPath attempt
            if (contextForChildren.isMissingNode() && (jPath == null || jPath.isEmpty())) {
                contextForChildren = effectiveCurrentContext;
            }
        }
        // Case 2: Rule has a jPath.
        else if (jPath != null && !jPath.isEmpty()) {
            if (valueToTransform == null && !derivedFromDefault) {
                // jPath was specified, it did not yield a usable (non-empty) value, 
                // AND no default value (from a conditionJPath) was applied.
                LOGGER.finer("Decision: jPath-based rule ('" + jPath + "') yielded no usable value and no default applied. Skipping " + ruleIdentifier);
                return EvaluationResult.doNotProcess();
            }
            // Otherwise, jPath yielded a usable value OR a default (triggered by a condition) was applied. Proceed.
            LOGGER.finer("Decision: jPath-based rule ('" + jPath + "') will proceed. valueToTransform: '" + valueToTransform + "', derivedFromDefault: " + derivedFromDefault + " for " + ruleIdentifier);
        }
        // Case 3: Rule has conditionJPath (and potentially defaultValue), but no jPath.
        else if (conditionJPath != null && !conditionJPath.isEmpty()) {
            // If we reach here, the conditionJPath was met (otherwise, we would have returned doNotProcess earlier).
            // valueToTransform will be the defaultValue (if any, which could be null if no defaultValue was specified).
            // derivedFromDefault will be true if defaultValue was used.
            LOGGER.finer("Decision: Condition-based rule ('" + conditionJPath + "') will proceed. valueToTransform: '" + valueToTransform + "' (derivedFromDefault: " + derivedFromDefault + ") for " + ruleIdentifier);
             // If only conditionJPath was present, contextForChildren should be the parent's context
            contextForChildren = effectiveCurrentContext;
        } else {
            // This state should ideally not be reached if mapping rules are well-formed 
            // (e.g., a rule must be structural, or have jPath, or have conditionJPath).
            // As a fallback, treat as skippable to prevent unexpected empty tags.
            LOGGER.warning("Decision: Rule " + ruleIdentifier + " is in an unexpected state (neither purely structural, jPath-based, nor condition-based). Skipping as a precaution.");
            return EvaluationResult.doNotProcess();
        }

        // Ensure contextForChildren is not MissingNode if we decided to process.
        // If it became MissingNode (e.g. jPath resolved to empty value), but we are proceeding (e.g. due to defaultValue),
        // children should still get a valid context, typically the parent's context.
        if (contextForChildren.isMissingNode()) {
            LOGGER.finer("contextForChildren was MissingNode for " + ruleIdentifier + " but rule is proceeding. Resetting to effectiveCurrentContext.");
            contextForChildren = effectiveCurrentContext;
        }

        LOGGER.finer("Finalizing for " + ruleIdentifier + ": valueToTransform='" + valueToTransform + "', contextForChildren nodeType: " + (contextForChildren !=null ? contextForChildren.getNodeType() : "null") + ", derivedFromDefault:" + derivedFromDefault);
        return EvaluationResult.processRule(valueToTransform, contextForChildren, derivedFromDefault);
    }

    private JsonNode resolvePath(JsonNode currentContext, String path, String ruleContextInfo) {
        LOGGER.finer("resolvePath - ruleContext: [" + ruleContextInfo + "], originalPath: '" + path + "', currentContext type: " + (currentContext != null ? currentContext.getNodeType() : "null"));

        if (path == null || path.isEmpty()) {
            LOGGER.finer("resolvePath - Path is null or empty for [" + ruleContextInfo + "]. Returning MissingNode.");
            return MissingNode.getInstance();
        }
        if (".".equals(path)) {
            LOGGER.finer("resolvePath - Path is '.' for [" + ruleContextInfo + "]. Returning currentContext.");
            return currentContext;
        }

        JsonNode effectiveContextToQuery = currentContext;
        String pathSegmentToConvert = path;
        boolean isPathRelative = true; // Default assumption

        if (path.startsWith("$")) {
            effectiveContextToQuery = this.globalJsonRoot;
            pathSegmentToConvert = path.length() > 1 && path.charAt(1) == '.' ? path.substring(2) : (path.length() > 0 ? path.substring(1) : "");
            isPathRelative = false; 
            LOGGER.finer("resolvePath - Absolute path detected for [" + ruleContextInfo + "]. Using globalJsonRoot. Segment to convert: '" + pathSegmentToConvert + "'.");
        } else {
            if (currentContext == null || currentContext.isMissingNode() || currentContext.isNull()) {
                LOGGER.finer("resolvePath - Relative path '" + path + "' for [" + ruleContextInfo + "] but currentContext is missing. Returning MissingNode.");
                return MissingNode.getInstance();
            }
            // pathSegmentToConvert remains as path for relative paths
            LOGGER.finer("resolvePath - Relative path detected for [" + ruleContextInfo + "]. Using currentContext. Segment to convert: '" + pathSegmentToConvert + "'.");
        }
        
        // Attempt direct field access for simple, non-nested relative paths before JSON Pointer conversion for performance and simplicity.
        if (isPathRelative && !pathSegmentToConvert.contains(".") && !pathSegmentToConvert.contains("[") && !pathSegmentToConvert.contains("]")) {
            if (effectiveContextToQuery != null && effectiveContextToQuery.has(pathSegmentToConvert)) {
                JsonNode directNode = effectiveContextToQuery.get(pathSegmentToConvert);
                LOGGER.finer("resolvePath - Direct field access for relative path '" + pathSegmentToConvert + "' in [" + ruleContextInfo + "] found node: " + directNode.getNodeType());
                return directNode;
            }
             LOGGER.finer("resolvePath - Direct field access for relative path '" + pathSegmentToConvert + "' in [" + ruleContextInfo + "] did NOT find field.");
        }
        
        String pointer = JsonToXmlSteam.convertJsonPathToJsonPointer(pathSegmentToConvert, isPathRelative);
        LOGGER.finer("resolvePath - Converted path segment '" + pathSegmentToConvert + "' to JSON Pointer: '" + pointer + "' for [" + ruleContextInfo + "]. Querying on context: " + effectiveContextToQuery.getNodeType());

        if (effectiveContextToQuery == null || effectiveContextToQuery.isMissingNode()){ 
            LOGGER.finer("resolvePath - Effective context to query is missing before .at(pointer) for [" + ruleContextInfo + "]. Path: '" + path + "'. Returning MissingNode.");
            return MissingNode.getInstance();
        }
        JsonNode resultNode = effectiveContextToQuery.at(pointer);
        LOGGER.finer("resolvePath - For [" + ruleContextInfo + "], path '" + path + "' (pointer '" + pointer + "') on context " + effectiveContextToQuery.getNodeType() + " yielded node: " + resultNode.getNodeType());
        return resultNode;
    }
} 
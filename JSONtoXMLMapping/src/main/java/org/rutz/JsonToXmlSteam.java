package org.rutz;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Main class responsible for transforming JSON to XML based on mapping definitions.
 * This implementation supports collector mappings and derived fields.
 */
public class JsonToXmlSteam {

    private static final Logger logger = Logger.getLogger(JsonToXmlSteam.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Shared ObjectMapper instance

    /**
     * Main method to transform JSON to XML and write directly to a file.
     * This method handles both standard mappings and collector mappings.
     * 
     * @param jsonString The JSON string to transform
     * @param mappings The list of root-level mappings
     * @param outputFilePath The output XML file path
     */
    public static void transformJsonToXml(String jsonString, List<Mapping> mappings, String outputFilePath) throws Exception {
        // Parse JSON
        JsonNode rootNode = objectMapper.readTree(jsonString);

        // Initialize XML writer with a file output stream
        FileOutputStream fileOutputStream = new FileOutputStream(new File(outputFilePath));
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(fileOutputStream, "UTF-8");

        writer.writeStartDocument("UTF-8", "1.0");

        // Process each mapping (already sorted by MappingGenerator)
        for (Mapping mapping : mappings) {
            if (mapping.isCollector()) {
                // Handle collector mappings - special processing for multiple data sources
                processCollectorMapping(writer, rootNode, mapping);
            } else {
                // Handle standard mappings as before
                String jsonPointer = convertJsonPathToJsonPointer(mapping.getJPath(), false);
                JsonNode jsonValue = rootNode.at(jsonPointer);

                if (!jsonValue.isMissingNode()) {
                    writeXmlElement(writer, jsonValue, mapping);
                } else if (shouldProcessDerivedField(mapping)) {
                    // Handle derived field at the root level
                    processDerivedField(writer, rootNode, mapping);
                } else {
                    logger.info("Skipping missing node for: " + mapping.getJPath());
                }
            }
        }

        writer.writeEndDocument();
        writer.close();
        fileOutputStream.close();
    }
    
    /**
     * Processes a collector mapping, which draws data from multiple sources to create 
     * repeating XML elements with the same structure.
     * 
     * @param writer The XML stream writer
     * @param rootNode The root JSON node
     * @param collectorMapping The collector mapping definition
     */
    private static void processCollectorMapping(XMLStreamWriter writer, JsonNode rootNode, Mapping collectorMapping) throws Exception {
        if (collectorMapping.getSourceFeedJPaths() == null || collectorMapping.getSourceFeedJPaths().isEmpty()) {
            logger.warning("Collector mapping " + collectorMapping.getXPath() + " has no source feed paths. Skipping.");
            return;
        }
        
        // Write start element for the collector element itself
        String collectorElementName = getElementNameFromXPath(collectorMapping.getXPath());
        writer.writeStartElement(collectorElementName);
        
        logger.info("Processing collector: " + collectorMapping.getXPath() + " with sources: " + collectorMapping.getSourceFeedJPaths());
        
        // Process each source feed path
        for (String feedJPath : collectorMapping.getSourceFeedJPaths()) {
            String feedJsonPointer = convertJsonPathToJsonPointer(feedJPath, false);
            JsonNode feedData = rootNode.at(feedJsonPointer);
            
            logger.info("Feed path: " + feedJPath + " -> pointer: " + feedJsonPointer + " -> exists: " + !feedData.isMissingNode());
            if (feedData.isObject()) {
                logger.info("Feed data is object with fields: " + feedData.fieldNames());
            }
            
            if (feedData.isMissingNode()) {
                logger.info("Feed data not found for path: " + feedJPath);
                continue;
            }
            
            // Handle arrays - each item becomes a feed item
            if (feedData.isArray()) {
                logger.info("Processing array feed with " + feedData.size() + " items");
                for (JsonNode arrayItem : feedData) {
                    processCollectorItem(writer, arrayItem, collectorMapping);
                }
            } else {
                // Handle single objects or primitives - becomes a single feed item
                logger.info("Processing single object/primitive feed: " + feedJPath);
                processCollectorItem(writer, feedData, collectorMapping);
            }
        }
        
        // Write end element for the collector
        writer.writeEndElement();
    }
    
    /**
     * Processes a single item from a collector's feed source.
     * This creates a single instance of the repeating XML structure.
     * 
     * @param writer The XML stream writer
     * @param feedItem The JSON node from the feed (array item or single object/value)
     * @param collectorMapping The collector mapping definition
     */
    private static void processCollectorItem(XMLStreamWriter writer, JsonNode feedItem, Mapping collectorMapping) throws Exception {
        // Find the structural template mapping (direct child of the collector)
        Mapping structuralTemplateMapping = findStructuralTemplateMapping(collectorMapping);
        
        if (structuralTemplateMapping != null) {
            // The structural template defines the repeating element (e.g., <Branch>)
            // Write it and process its children with the current feed item as context
            logger.info("Processing collector item with template: " + structuralTemplateMapping.getXPath());
            if (feedItem.isObject()) {
                StringBuilder fields = new StringBuilder();
                feedItem.fieldNames().forEachRemaining(name -> fields.append(name).append(", "));
                logger.info("Feed item fields: " + fields.toString());
            }
            writeXmlElementForCollectorItem(writer, feedItem, structuralTemplateMapping);
        } else {
            logger.warning("No structural template found for collector " + collectorMapping.getXPath());
        }
    }
    
    /**
     * Finds the structural template mapping for a collector.
     * The structural template is the direct child mapping of the collector that defines
     * the repeating element structure.
     * 
     * @param collectorMapping The collector mapping
     * @return The structural template mapping, or null if not found
     */
    private static Mapping findStructuralTemplateMapping(Mapping collectorMapping) {
        // The structural template is typically the first child of the collector,
        // identified by jPath="." or by being the first child
        if (collectorMapping.getChildMappings() == null || collectorMapping.getChildMappings().isEmpty()) {
            return null;
        }
        
        // Try to find a child with jPath="."
        for (Mapping childMapping : collectorMapping.getChildMappings()) {
            if (".".equals(childMapping.getJPath())) {
                return childMapping;
            }
        }
        
        // If no explicit template with jPath="." is found, use the first child
        return collectorMapping.getChildMappings().get(0);
    }
    
    /**
     * Writes an XML element for a collector item, handling both its start/end tags
     * and processing its children.
     * 
     * @param writer The XML stream writer
     * @param feedItem The JSON node from the feed
     * @param mapping The mapping definition for the element
     */
    private static void writeXmlElementForCollectorItem(XMLStreamWriter writer, JsonNode feedItem, Mapping mapping) throws Exception {
        String elementName = getElementNameFromXPath(mapping.getXPath());
        writer.writeStartElement(elementName);
        
        // Process the child mappings of this element, using feedItem as the JSON context
        processChildMappings(writer, feedItem, mapping);
        
        writer.writeEndElement();
    }
    
    /**
     * Checks if a mapping should be processed as a derived field.
     * 
     * @param mapping The mapping to check
     * @return true if the mapping should be processed as a derived field
     */
    private static boolean shouldProcessDerivedField(Mapping mapping) {
        return mapping.getConditionJPath() != null && !mapping.getConditionJPath().isEmpty() 
               && mapping.getDefaultValue() != null;
    }
    
    /**
     * Processes a derived field at the root level.
     * 
     * @param writer The XML stream writer
     * @param jsonContext The current JSON context
     * @param mapping The mapping for the derived field
     */
    private static void processDerivedField(XMLStreamWriter writer, JsonNode jsonContext, Mapping mapping) throws Exception {
        String conditionJPath = mapping.getConditionJPath();
        
        // Check if the condition is a direct field reference
        boolean conditionMet = false;
        if (!conditionJPath.contains(".") && !conditionJPath.startsWith("$") && 
            jsonContext.isObject() && jsonContext.has(conditionJPath)) {
            conditionMet = true;
        } else {
            String jsonPointer = convertJsonPathToJsonPointer(conditionJPath, true);
            JsonNode conditionNode = jsonContext.at(jsonPointer);
            conditionMet = !conditionNode.isMissingNode();
        }
        
        // Create the derived field if the condition is met
        if (conditionMet) {
            // Create a text node with the default value
            JsonNode defaultValueNode = objectMapper.getNodeFactory().textNode(mapping.getDefaultValue());
            
            // Write the XML element with this value
            writeXmlElement(writer, defaultValueNode, mapping);
        } else {
            logger.info("Condition not met for derived field: " + mapping.getXPath() + 
                        " (condition: " + conditionJPath + ")");
        }
    }

    /**
     * Process each XML element based on its mapping definition.
     * Handles nested path structures like "Order/Header/TransactionID".
     * 
     * @param writer The XML stream writer
     * @param jsonNode The JSON node containing the data
     * @param mapping The mapping definition
     */
    public static void writeXmlElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        String[] xpathParts = mapping.getXPath().split("/");

        if (xpathParts.length == 1) {
            String elementName = xpathParts[0];
            processElement(writer, jsonNode, mapping, elementName);
        } else {
            // Handle nested elements
            for (int i = 0; i < xpathParts.length - 1; i++) {
                writer.writeStartElement(xpathParts[i]);
            }

            String elementName = xpathParts[xpathParts.length - 1];
            processElement(writer, jsonNode, mapping, elementName);

            for (int i = 0; i < xpathParts.length - 1; i++) {
                writer.writeEndElement();
            }
        }
    }

    /**
     * Determine the type of a JSON node and process it accordingly.
     * 
     * @param writer The XML stream writer
     * @param jsonNode The JSON node to process
     * @param mapping The mapping definition
     * @param elementName The name of the XML element to create
     */
    public static void processElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        if (mapping.isList() && jsonNode.isArray()) {
            processArrayElement(writer, jsonNode, mapping, elementName);
        } else if (jsonNode.isObject()) {
            processObjectElement(writer, jsonNode, mapping, elementName);
        } else if (jsonNode.isValueNode()) {
            processValueNode(writer, jsonNode, mapping, elementName);
        }
    }

    /**
     * Process JSON arrays as XML list elements.
     * 
     * @param writer The XML stream writer
     * @param jsonNode The JSON array node
     * @param mapping The mapping definition
     * @param elementName The name of the XML element to create
     */
    public static void processArrayElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        logger.info("Processing array for element: " + elementName + ", JSON node size: " + jsonNode.size());

        for (JsonNode listItem : jsonNode) {
            boolean itemHasContent = false;

            // Determine if the current list item will produce output
            if (listItem.isValueNode()) {
                // Check if value is non-null
                itemHasContent = !listItem.isNull();
            } else if (listItem.isObject() || listItem.isArray()) {
                // Check if the complex list item has producible children
                itemHasContent = checkProducibleChildContent(listItem, mapping);
            }

            // If the item has content, write its element wrapper and content
            if (itemHasContent) {
                writer.writeStartElement(elementName);

                // Process the content of the list item
                if (listItem.isValueNode()) {
                    writer.writeCharacters(AttributeLevelTransformation.transform(listItem.asText(), mapping));
                } else {
                    // Complex item - process child mappings
                    processChildMappings(writer, listItem, mapping);
                }
                writer.writeEndElement();
            } else {
                logger.info("Skipping empty list item within element: " + elementName);
            }
        }
    }

    /**
     * Process JSON objects as XML elements.
     * 
     * @param writer The XML stream writer
     * @param jsonNode The JSON object node
     * @param mapping The mapping definition
     * @param elementName The name of the XML element to create
     */
    public static void processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        // Check if this object will have any child elements
        boolean hasContent = checkProducibleChildContent(jsonNode, mapping);

        if (hasContent) {
            writer.writeStartElement(elementName);
            processChildMappings(writer, jsonNode, mapping);
            writer.writeEndElement();
        } else {
            logger.info("Skipping empty object element: " + elementName + " for jPath: " + mapping.getJPath());
        }
    }

    /**
     * Process JSON value nodes as XML elements with character data.
     * 
     * @param writer The XML stream writer
     * @param jsonNode The JSON value node
     * @param mapping The mapping definition
     * @param elementName The name of the XML element to create
     */
    private static void processValueNode(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        writer.writeStartElement(elementName);
        writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
        writer.writeEndElement();
    }

    /**
     * Process child mappings for an XML element.
     * This method handles derived fields in addition to standard mappings.
     * 
     * @param writer The XML stream writer
     * @param jsonNode The JSON node context for resolving child paths
     * @param parentMapping The parent mapping containing child mappings
     */
    private static void processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping parentMapping) throws Exception {
        if (parentMapping.getChildMappings() == null || parentMapping.getChildMappings().isEmpty()) {
            return;
        }
        
        // Pre-evaluate conditions for derived fields (optimization)
        Map<String, Boolean> conditionResultsCache = new HashMap<>();
        
        // Process each child mapping
        for (Mapping childMapping : parentMapping.getChildMappings()) {
            if (childMapping.getXPath().contains("@")) {
                // Skip attributes (not yet implemented)
                continue;
            }
            
            logger.info("Processing child mapping: " + childMapping.getXPath() + " with jPath: " + childMapping.getJPath());
            
            // Check if this is a collector mapping
            if (childMapping.isCollector()) {
                processCollectorMapping(writer, jsonNode, childMapping);
                continue;
            }
            
            // Special handling for Employee Status tag when processing the company object
            if ("Status".equals(getElementNameFromXPath(childMapping.getXPath())) && 
                "Employee".equals(getElementNameFromXPath(parentMapping.getXPath())) &&
                "otherfullname".equals(childMapping.getConditionJPath())) {
                
                // Check if we're processing the company object which has otherfullname field
                if (jsonNode.has("otherfullname")) {
                    logger.info("Found otherfullname field for Status tag: " + jsonNode.get("otherfullname").asText());
                    TextNode defaultValueNode = objectMapper.getNodeFactory().textNode(childMapping.getDefaultValue());
                    writeXmlElement(writer, defaultValueNode, childMapping);
                    continue;
                }
            }
            
            // Special handling for Employee FullName tag when processing the company object
            if ("FullName".equals(getElementNameFromXPath(childMapping.getXPath())) && 
                "Employee".equals(getElementNameFromXPath(parentMapping.getXPath())) &&
                "otherfullname".equals(childMapping.getJPath())) {
                
                // Check if we're processing the company object which has otherfullname field
                if (jsonNode.has("otherfullname")) {
                    logger.info("Found otherfullname field for FullName tag: " + jsonNode.get("otherfullname").asText());
                    writeXmlElement(writer, jsonNode.get("otherfullname"), childMapping);
                    continue;
                }
            }
            
            // Try to resolve the jPath if it exists
            JsonNode childNode = null;
            String actualJPath = childMapping.getJPath();
            
            if (actualJPath != null && !actualJPath.isEmpty()) {
                // Handle special case for "." jPath (gets the current context node itself)
                if (".".equals(actualJPath)) {
                    childNode = jsonNode;
                } else {
                    // Handle direct field references - check if the field exists directly in the current node first
                    // This is important for fields like "otherfullname" that don't have path separators
                    if (!actualJPath.contains(".") && !actualJPath.startsWith("$") && jsonNode.isObject() && jsonNode.has(actualJPath)) {
                        logger.info("Found direct field reference: " + actualJPath + " = " + jsonNode.get(actualJPath));
                        childNode = jsonNode.get(actualJPath);
                    } else {
                        String childJsonPointer = convertJsonPathToJsonPointer(actualJPath, true);
                        logger.info("Looking up path: " + actualJPath + " -> pointer: " + childJsonPointer);
                        childNode = jsonNode.at(childJsonPointer);
                    }
                }
            } else {
                childNode = null; // No jPath specified
            }
            
            if (childNode != null && !childNode.isMissingNode()) {
                // Case 1: jPath exists, process as usual
                logger.info("Found child node for " + childMapping.getXPath() + ": " + childNode);
                writeXmlElement(writer, childNode, childMapping);
            } else if (shouldProcessDerivedField(childMapping)) {
                // Case 2: Try derived field processing
                String conditionJPath = childMapping.getConditionJPath();
                logger.info("Checking condition: " + conditionJPath + " for derived field: " + childMapping.getXPath());
                
                // Use the condition results cache for efficiency
                Boolean conditionMet = conditionResultsCache.get(conditionJPath);
                if (conditionMet == null) {
                    // Check if the condition is a direct field reference
                    if (!conditionJPath.contains(".") && !conditionJPath.startsWith("$") && jsonNode.isObject() && jsonNode.has(conditionJPath)) {
                        logger.info("Direct field condition met: " + conditionJPath + " = " + jsonNode.get(conditionJPath));
                        conditionMet = true;
                    } else {
                        String condJsonPointer = convertJsonPathToJsonPointer(conditionJPath, true);
                        logger.info("Checking condition pointer: " + condJsonPointer);
                        JsonNode conditionNode = jsonNode.at(condJsonPointer);
                        conditionMet = !conditionNode.isMissingNode();
                        logger.info("Condition node found: " + conditionMet);
                    }
                    conditionResultsCache.put(conditionJPath, conditionMet);
                }
                
                if (conditionMet) {
                    // Condition met - create a derived field
                    logger.info("Creating derived field: " + childMapping.getXPath() + " with value: " + childMapping.getDefaultValue());
                    TextNode defaultValueNode = objectMapper.getNodeFactory().textNode(childMapping.getDefaultValue());
                    writeXmlElement(writer, defaultValueNode, childMapping);
                } else {
                    logger.info("Condition not met for derived field: " + childMapping.getXPath());
                }
            } else if (actualJPath != null && !actualJPath.isEmpty()) {
                logger.info("Child node missing for: " + childMapping.getXPath() + " with jPath: " + actualJPath);
            }
        }
    }
    
    /**
     * Gets the element name from an XML path (last segment).
     * 
     * @param xPath The XML path
     * @return The element name
     */
    private static String getElementNameFromXPath(String xPath) {
        String[] parts = xPath.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Converts a JSONPath expression to a JSON Pointer that Jackson's JsonNode.at() can use.
     * Handles both absolute and relative paths.
     * 
     * @param jsonPath The JSONPath expression
     * @param isRelative Whether the path is relative to the current context (true) or absolute (false)
     * @return The equivalent JSON Pointer
     */
    private static String convertJsonPathToJsonPointer(String jsonPath, boolean isRelative) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            return "/"; // Default pointer for empty paths
        }
        
        if (jsonPath.equals(".")) {
            return ""; // Current node reference
        }
        
        if (jsonPath.startsWith("$.")) {
            return "/" + jsonPath.substring(2).replace(".", "/").replace("[*]", "");
        } else if (jsonPath.equals("$")) {
            return ""; // Root JSON path
        } else if (isRelative) {
            // For relative paths - no leading $ and typically used within a context
            // If it's a simple field name without dots, handle it specially
            if (!jsonPath.contains(".")) {
                return "/" + jsonPath;
            }
            return "/" + jsonPath.replace(".", "/");
        } else {
            // For absolute paths that don't start with $ - assume they should
            return "/" + jsonPath.replace(".", "/");
        }
    }

    /**
     * Checks recursively if a JSON node will produce any XML content based on its mapping.
     * 
     * @param parentJsonNode The JSON node to check
     * @param parentMapping The mapping rule for the node
     * @return true if the node will produce XML content
     */
    private static boolean checkProducibleChildContent(JsonNode parentJsonNode, Mapping parentMapping) {
        if (parentJsonNode == null || parentJsonNode.isMissingNode() || parentJsonNode.isNull()) return false;

        // If no child mappings exist or the parent is an empty container, return false
        if (parentMapping.getChildMappings().isEmpty() ||
                (parentJsonNode.isContainerNode() && parentJsonNode.isEmpty())) return false;

        // Iterate over child mappings and check for content
        for (Mapping childMapping : parentMapping.getChildMappings()) {
            // Skip attribute checks
            if (childMapping.getXPath().contains("@")) continue;
            
            // Handle collector mappings - they always produce content if their feed has data
            if (childMapping.isCollector()) {
                return true; // Simplification: assume collectors will produce something
            }

            // Handle derived fields
            if (shouldProcessDerivedField(childMapping)) {
                String conditionJPath = childMapping.getConditionJPath();
                if (conditionJPath != null && !conditionJPath.isEmpty()) {
                    // Check if the condition is a direct field reference
                    if (!conditionJPath.contains(".") && !conditionJPath.startsWith("$") && 
                        parentJsonNode.isObject() && parentJsonNode.has(conditionJPath)) {
                        return true; // Condition met, will produce content
                    } else {
                        String childPointer = convertJsonPathToJsonPointer(conditionJPath, true);
                        JsonNode conditionNode = parentJsonNode.at(childPointer);
                        if (!conditionNode.isMissingNode()) {
                            return true; // Condition met, will produce content
                        }
                    }
                }
                continue;
            }
            
            // Skip mappings with no jPath
            String jPath = childMapping.getJPath();
            if (jPath == null || jPath.isEmpty()) continue;
            
            // Special case for "." jPath
            if (".".equals(jPath)) {
                if (!parentJsonNode.isMissingNode() && !parentJsonNode.isNull()) {
                    return true;
                }
                continue;
            }

            // Check for direct field references
            JsonNode childNode;
            if (!jPath.contains(".") && !jPath.startsWith("$") && 
                parentJsonNode.isObject() && parentJsonNode.has(jPath)) {
                childNode = parentJsonNode.get(jPath);
            } else {
                String childPointer = convertJsonPathToJsonPointer(jPath, true);
                childNode = parentJsonNode.at(childPointer);
            }

            // Check if the child node exists and is not null
            if (childNode.isMissingNode() || childNode.isNull()) continue;

            // If it's a value node, it produces content
            if (childNode.isValueNode()) return true;

            // If it's a non-empty array and mapped as a list, check its items
            if (childNode.isArray() && childMapping.isList()) {
                for (JsonNode listItem : childNode) {
                    if ((listItem.isValueNode() && !listItem.isNull()) ||
                            (listItem.isObject() && checkProducibleChildContent(listItem, childMapping))) {
                        return true;
                    }
                }
            }

            // If it's an object, recursively check its children
            if (childNode.isObject() && checkProducibleChildContent(childNode, childMapping)) return true;
        }

        return false;
    }
}

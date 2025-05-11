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
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.rutz.Mapping;
import org.rutz.util.EvaluationResult;
import org.rutz.util.MappingRuleEvaluator;

/**
 * Main class responsible for transforming JSON to XML based on mapping definitions.
 * This implementation supports collector mappings and derived fields.
 */
public class JsonToXmlSteam {

    private static final Logger logger = Logger.getLogger(JsonToXmlSteam.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Shared ObjectMapper instance

    // Member variables for evaluator and global root
    private MappingRuleEvaluator ruleEvaluator;
    private JsonNode globalJsonRootNode;

    /**
     * Main method to transform JSON to XML and write directly to a file.
     * This method handles both standard mappings and collector mappings.
     * 
     * @param jsonString The JSON string to transform
     * @param mappings The list of root-level mappings
     * @param outputFilePath The output XML file path
     */
    public void transformJsonToXml(String jsonString, List<Mapping> mappings, String outputFilePath) throws Exception {
        this.globalJsonRootNode = objectMapper.readTree(jsonString);
        this.ruleEvaluator = new MappingRuleEvaluator(this.globalJsonRootNode);

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        try (FileOutputStream fos = new FileOutputStream(new File(outputFilePath))) {
            XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(fos, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");

            // Sort top-level mappings by order
            mappings.sort(Comparator.comparingInt(Mapping::getOrder));

            for (Mapping mapping : mappings) {
                // All processing now starts through the new refactored writeXmlElement
                this.writeXmlElement(writer, this.globalJsonRootNode, mapping);
            }

            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (Exception e) {
            logger.severe("Error during XML transformation: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Processes a collector mapping, which draws data from multiple sources to create 
     * repeating XML elements with the same structure.
     * 
     * @param writer The XML stream writer
     * @param rootNode The root JSON node
     * @param collectorMapping The collector mapping definition
     */
    private void processCollectorMapping(XMLStreamWriter writer, JsonNode currentCollectorContext, Mapping collectorMapping) throws Exception {
        if (collectorMapping.getSourceFeedJPaths() == null || collectorMapping.getSourceFeedJPaths().isEmpty()) {
            logger.warning("Collector mapping " + collectorMapping.getXPath() + " has no source feed paths. Skipping.");
            return;
        }
        
        String collectorElementName = getElementNameFromXPath(collectorMapping.getXPath());
        writer.writeStartElement(collectorElementName);
        logger.info("Processing collector: " + collectorMapping.getXPath() + " with sources: " + collectorMapping.getSourceFeedJPaths());
        
        for (String feedJPath : collectorMapping.getSourceFeedJPaths()) {
            JsonNode contextForFeedPath = this.globalJsonRootNode; // Collector feeds are always from global root.
            String pathSegment = feedJPath;

            if (feedJPath.startsWith("$")) {
                pathSegment = feedJPath.length() > 1 && feedJPath.charAt(1) == '.' ? feedJPath.substring(2) : (feedJPath.length() > 0 ? feedJPath.substring(1) : "");
            } 
            // Assuming collector sourceFeedJPaths are absolute starting with '$' as per general use case.
            // If they could be relative, more complex context determination would be needed here.

            String pointer = convertJsonPathToJsonPointer(pathSegment, false); // 'false' as path was absolute from global root
            JsonNode feedData = contextForFeedPath.at(pointer);
            
            logger.info("Collector Feed path: '" + feedJPath + "' (resolved segment: '" + pathSegment + "', pointer: '" + pointer + "') -> resolved to node type: " + (feedData != null ? feedData.getNodeType() : "null") + ", exists: " + (feedData != null && !feedData.isMissingNode()));
            
            if (feedData == null || feedData.isMissingNode()) {
                logger.info("Feed data not found for path: " + feedJPath);
                continue;
            }
            
            if (feedData.isArray()) {
                for (JsonNode arrayItem : feedData) {
                    this.processCollectorItem(writer, arrayItem, collectorMapping);
                }
            } else {
                this.processCollectorItem(writer, feedData, collectorMapping);
            }
        }
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
    private void processCollectorItem(XMLStreamWriter writer, JsonNode feedItem, Mapping collectorMapping) throws Exception {
        Mapping structuralTemplateMapping = findStructuralTemplateMapping(collectorMapping);
        
        if (structuralTemplateMapping != null) {
            logger.info("Processing collector item with template: " + structuralTemplateMapping.getXPath());
            this.writeXmlElement(writer, feedItem, structuralTemplateMapping);
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
        if (collectorMapping.getChildMappings() == null || collectorMapping.getChildMappings().isEmpty()) return null;
        for (Mapping childMapping : collectorMapping.getChildMappings()) {
            if (".".equals(childMapping.getJPath())) return childMapping;
        }
        return collectorMapping.getChildMappings().get(0); // Fallback to first child
    }
    
    /**
     * Writes an XML element for a collector item, handling both its start/end tags
     * and processing its children.
     * 
     * @param writer The XML stream writer
     * @param feedItem The JSON node from the feed
     * @param mapping The mapping definition for the element
     */
    private void writeXmlElement(XMLStreamWriter writer, JsonNode currentContext, Mapping mapping) throws Exception {
        EvaluationResult evalResult = this.ruleEvaluator.evaluateRule(currentContext, mapping);

        if (!evalResult.shouldProcess()) {
            return; 
        }

        // Handle collector mappings specifically if the rule IS a collector
        if (mapping.isCollector()) {
            processCollectorMapping(writer, currentContext, mapping); // currentContext for a collector is typically the global root or a relevant parent node
            return; 
        }

        String elementName = getElementNameFromXPath(mapping.getXPath());
        writer.writeStartElement(elementName);

        String textContentForElement = evalResult.getValueToTransform();
        JsonNode contextForChildren = evalResult.getContextForChildren();
        
        if (contextForChildren == null || contextForChildren.isMissingNode() || contextForChildren.isNull()) {
            contextForChildren = currentContext; 
             if (contextForChildren == null || contextForChildren.isMissingNode() || contextForChildren.isNull()) {
                contextForChildren = this.globalJsonRootNode; // Fallback to global root
            }
        }

        if (textContentForElement != null) {
            String transformedValue = AttributeLevelTransformation.transform(
                textContentForElement,
                mapping,
                this.globalJsonRootNode 
            );
            if (transformedValue != null) {
                writer.writeCharacters(transformedValue);
            }
        }

        // Process child mappings (if any) using the determined contextForChildren
        // This is where the old processChildMappings logic is integrated.
        if (mapping.getChildMappings() != null && !mapping.getChildMappings().isEmpty()) {
            // Use getChildMappings() as it's pre-sorted by MappingGenerator
            for (Mapping childMapping : mapping.getChildMappings()) { 
                // Recursive call to the new writeXmlElement for children
                this.writeXmlElement(writer, contextForChildren, childMapping);
            }
        }
        writer.writeEndElement();
    }
    
    /**
     * Gets the element name from an XML path (last segment).
     * 
     * @param xPath The XML path
     * @return The element name
     */
    private static String getElementNameFromXPath(String xPath) {
        if (xPath == null || xPath.isEmpty()) return "";
        String[] parts = xPath.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Converts a JSONPath expression to a JSON Pointer that Jackson's JsonNode.at() can use.
     * Handles both absolute and relative paths.
     * 
     * @param pathSegmentToConvert The JSONPath expression segment, assumed to be pre-processed for root selection (e.g. no leading "$." or "$")
     * @param isRelative Whether the path is relative (not strictly used by this method's logic currently, but kept for signature compatibility)
     * @return The equivalent JSON Pointer
     */
    public static String convertJsonPathToJsonPointer(String pathSegmentToConvert, boolean isRelative) {
        // pathSegmentToConvert is assumed to be ALREADY PROCESSED for root selection (e.g. no leading "$." or "$")
        // and represents the part of the path relative to the chosen root (current or global)
        if (pathSegmentToConvert == null) { 
            // This case should ideally be handled by the caller (resolvePath)
            // but returning "/" (root of context) is a safe fallback.
            return "/"; 
        }
        if (pathSegmentToConvert.isEmpty()) {
            // For Jackson's .at(), empty string "" means the current node.
            // This is correct if resolvePath sets pathSegmentToConvert to "" for original paths like "$" or "$."
            return ""; 
        }
        if (".".equals(pathSegmentToConvert)) {
            // "." also means the current node itself. Jackson's .at("") achieves this.
            return ""; 
        }

        String processedPath = pathSegmentToConvert;
        // Escape characters for JSON Pointer as per RFC 6901
        processedPath = processedPath.replace("~", "~0").replace("/", "~1");
        
        // Construct the JSON Pointer.
        // JSON Pointers always start with "/" for segments, unless it's an empty string for the root itself.
        return "/" + processedPath.replace(".", "/").replace("[*]", ""); // Simplified [*] to empty segment for arrays
    }
}

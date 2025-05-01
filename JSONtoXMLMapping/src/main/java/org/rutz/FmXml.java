//package org.rutz;
//
//import javax.xml.stream.XMLStreamWriter;
//import java.util.List;
//import java.util.logging.Logger;
//import com.fasterxml.jackson.databind.JsonNode;
//
//public class FmXml    {
//
//    private static final Logger logger = Logger.getLogger(JsonToXmlSteam.class.getName()); // Logger instance
//
//    // Main method to transform JSON to XML and write directly to a file
//    public  void transformJsonToXml(JsonNode rootNode,List<Mapping> mappings, XMLStreamWriter writer) throws Exception {
//        try {
//            // Parse JSON
//            JsonNode jsonValue;
//
//            // Process each mapping
//            for (Mapping mapping : mappings) {
//                String jsonPointer = convertJsonPathToJsonPointer(mapping.getJPath());
//                jsonValue = rootNode.at(jsonPointer);
//
//                if (!jsonValue.isMissingNode()) {
//                    writeXmlElement(writer, jsonValue, mapping);
//                } else {
//                    logger.info("Skipping missing node for: " + mapping.getJPath());
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//    // Process each XML element
//    public static void writeXmlElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
//        String[] xpathParts = mapping.getXPath().split("/");
//
//        if (xpathParts.length == 1) {
//            String elementName = xpathParts[0];
//            processElement(writer, jsonNode, mapping, elementName);
//        } else {
//            // Handle nested elements
//            for (int i = 0; i < xpathParts.length - 1; i++) {
//                writer.writeStartElement(xpathParts[i]);
//            }
//
//            String elementName = xpathParts[xpathParts.length - 1];
//            processElement(writer, jsonNode, mapping, elementName);
//
//            for (int i = 0; i < xpathParts.length - 1; i++) {
//                writer.writeEndElement();
//            }
//        }
//    }
//
//    // Process each element, including handling objects, lists, and value nodes
//    public static void processElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        if (mapping.isList() && jsonNode.isArray()) {
//            processArrayElement(writer, jsonNode, mapping, elementName);
//        } else if (jsonNode.isObject()) {
//            processObjectElement(writer, jsonNode, mapping, elementName);
//        } else if (jsonNode.isValueNode()) {
//            processValueNode(writer, jsonNode, mapping, elementName);
//        }
//    }
//
//    // Process JSON arrays as XML list elements
//    public static void processArrayElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        logger.info("jsonNode array count: " + jsonNode.size());
//
//        for (JsonNode listItem : jsonNode) {
//            writer.writeStartElement(elementName);
////            writeAttributes(writer, listItem, mapping);
//
//            if (listItem.isValueNode()) {
//                writer.writeCharacters(AttributeLevelTransformation.transform(listItem.asText(), mapping));
//            } else {
//                processChildMappings(writer, listItem, mapping);
//            }
//            writer.writeEndElement();
//        }
//    }
//
//    // Process JSON objects as XML elements
//    public static void processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        writer.writeStartElement(elementName);
////        writeAttributes(writer, jsonNode, mapping);
//
//        if (mapping.getChildMappings() != null && !mapping.getChildMappings().isEmpty()) {
//            processChildMappings(writer, jsonNode, mapping);
//        } else if (jsonNode.isValueNode()) {
//            writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
//        }
//        writer.writeEndElement();
//    }
//
//    // Process JSON value nodes as XML elements
//    public static void processValueNode(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        writer.writeStartElement(elementName);
////        writeAttributes(writer, jsonNode, mapping);
//        writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
//        writer.writeEndElement();
//    }
//
//
//    // Process child mappings recursively
//    private static void processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
//        if (mapping.getChildMappings() != null) {
//            for (Mapping childMapping : mapping.getChildMappings()) {
//                if (!childMapping.getXPath().contains("@")) {
//                    logger.info("========Inside processChildMappings ==== " + childMapping.getXPath());// Skip attributes
//                    String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
//                    JsonNode childNode = jsonNode.at(childPointer);
//
//                    if (!childNode.isMissingNode()) {
//                        try {
//                            writeXmlElement(writer, childNode, childMapping);
//                        } catch (Exception e) {
//                            logger.info("Error processing child node: " + childMapping.getXPath());
//                            throw new RuntimeException(e);
//                        }
//                    } else {
//                        logger.info("========Child node missing for: " + childMapping.getXPath());
//                    }
//                }
//            }
//        }
//    }
//
//    // Convert JSONPath to JSON Pointer
//    public static String convertJsonPathToJsonPointer(String jsonPath) {
//        if (jsonPath.startsWith("$.") ) {
//            return "/" + jsonPath.substring(2).replace(".", "/").replace("[*]", "");
//        } else if (jsonPath.equals("$")) {
//            return ""; // Root JSON path
//        } else {
//            throw new IllegalArgumentException("Invalid JSONPath expression: " + jsonPath);
//        }
//    }
//}
//
//package org.rutz;
//
//import com.fasterxml.jackson.databind.JsonNode;
//
//import javax.xml.stream.XMLStreamWriter;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//public class FmXml {
//    private static final Logger logger = Logger.getLogger(FmXml.class.getName());
//
//    // Cache for JSON path to pointer conversions
//    private static final Map<String, String> jsonPathCache = new ConcurrentHashMap<>();
//
//    /**
//     * Transforms JSON to XML based on provided mappings
//     *
//     * @param rootNode The root JSON node to transform
//     * @param mappings List of mappings that define the transformation
//     * @param writer XMLStreamWriter to write the XML output
//     * @throws Exception If any error occurs during transformation
//     */
//    public void transformJsonToXml(JsonNode rootNode, List<Mapping> mappings, XMLStreamWriter writer) throws Exception {
//        if (rootNode == null || mappings == null || writer == null) {
//            throw new IllegalArgumentException("Root node, mappings, and writer cannot be null");
//        }
//
//        try {
//            // Process each mapping
//            for (Mapping mapping : mappings) {
//                String jsonPointer = getJsonPointer(mapping.getJPath());
//                JsonNode jsonValue = rootNode.at(jsonPointer);
//
//                if (!jsonValue.isMissingNode()) {
//                    writeXmlElement(writer, jsonValue, mapping);
//                } else if (logger.isLoggable(Level.FINE)) {
//                    logger.fine("Skipping missing node for: " + mapping.getJPath());
//                }
//            }
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "Error transforming JSON to XML", e);
//            throw new RuntimeException("Error transforming JSON to XML", e);
//        }
//    }
//
//    /**
//     * Writes an XML element based on a JSON node and mapping
//     *
//     * @param writer XMLStreamWriter to write the XML output
//     * @param jsonNode The JSON node to transform
//     * @param mapping The mapping that defines the transformation
//     * @throws Exception If any error occurs during writing
//     */
//    public static void writeXmlElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
//        String xpath = mapping.getXPath();
//        if (xpath == null || xpath.isEmpty()) {
//            logger.warning("Empty XPath in mapping, skipping element");
//            return;
//        }
//
//        String[] xpathParts = xpath.split("/");
//
//        try {
//            if (xpathParts.length == 1) {
//                processElement(writer, jsonNode, mapping, xpathParts[0]);
//            } else {
//                // Handle nested elements
//                for (int i = 0; i < xpathParts.length - 1; i++) {
//                    if (!xpathParts[i].isEmpty()) {
//                        writer.writeStartElement(xpathParts[i]);
//                    }
//                }
//
//                String elementName = xpathParts[xpathParts.length - 1];
//                processElement(writer, jsonNode, mapping, elementName);
//
//                // Close nested elements in reverse order
//                for (int i = xpathParts.length - 2; i >= 0; i--) {
//                    if (!xpathParts[i].isEmpty()) {
//                        writer.writeEndElement();
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "Error writing XML element: " + xpath, e);
//            throw e;
//        }
//    }
//
//    /**
//     * Processes a single element based on its JSON type
//     */
//    public static void processElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        if (jsonNode == null || elementName == null || elementName.isEmpty()) {
//            return;
//        }
//
//        if (mapping.isList() && jsonNode.isArray()) {
//            processArrayElement(writer, jsonNode, mapping, elementName);
//        } else if (jsonNode.isObject()) {
//            processObjectElement(writer, jsonNode, mapping, elementName);
//        } else if (jsonNode.isValueNode()) {
//            processValueNode(writer, jsonNode, mapping, elementName);
//        }
//    }
//
//    /**
//     * Processes a JSON array as XML list elements
//     */
//    public static void processArrayElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        if (logger.isLoggable(Level.FINE)) {
//            logger.fine("Processing array with " + jsonNode.size() + " elements");
//        }
//
//        for (JsonNode listItem : jsonNode) {
//            writer.writeStartElement(elementName);
//
//            if (listItem.isValueNode()) {
//                writer.writeCharacters(AttributeLevelTransformation.transform(listItem.asText(), mapping));
//            } else {
//                processChildMappings(writer, listItem, mapping);
//            }
//            writer.writeEndElement();
//        }
//    }
//
//    /**
//     * Processes a JSON object as an XML element
//     */
//    public static void processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        writer.writeStartElement(elementName);
//
//        if (mapping.getChildMappings() != null && !mapping.getChildMappings().isEmpty()) {
//            processChildMappings(writer, jsonNode, mapping);
//        } else if (jsonNode.isValueNode()) {
//            writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
//        }
//        writer.writeEndElement();
//    }
//
//    /**
//     * Processes a JSON value node as an XML element
//     */
//    public static void processValueNode(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
//        writer.writeStartElement(elementName);
//        writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
//        writer.writeEndElement();
//    }
//
//    /**
//     * Processes child mappings recursively
//     */
//    private static void processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
//        List<Mapping> childMappings = mapping.getChildMappings();
//        if (childMappings == null || childMappings.isEmpty()) {
//            return;
//        }
//
//        for (Mapping childMapping : childMappings) {
//            // Skip attributes for now
//            if (childMapping.getXPath() != null && !childMapping.getXPath().contains("@")) {
//                if (logger.isLoggable(Level.FINE)) {
//                    logger.fine("Processing child mapping: " + childMapping.getXPath());
//                }
//
//                String childPointer = getJsonPointer(childMapping.getJPath());
//                JsonNode childNode = jsonNode.at(childPointer);
//
//                if (!childNode.isMissingNode()) {
//                    writeXmlElement(writer, childNode, childMapping);
//                } else if (logger.isLoggable(Level.FINE)) {
//                    logger.fine("Child node missing for: " + childMapping.getXPath());
//                }
//            }
//        }
//    }
//
//    /**
//     * Gets a JSON pointer from cache or converts it if not present
//     */
//    private static String getJsonPointer(String jsonPath) {
//        return jsonPathCache.computeIfAbsent(jsonPath, FmXml::convertJsonPathToJsonPointer);
//    }
//
//    /**
//     * Converts a JSONPath expression to a JSON Pointer
//     */
//    public static String convertJsonPathToJsonPointer(String jsonPath) {
//        if (jsonPath == null || jsonPath.isEmpty()) {
//            throw new IllegalArgumentException("JSON path cannot be null or empty");
//        }
//
//        if (jsonPath.startsWith("$.")) {
//            return "/" + jsonPath.substring(2).replace(".", "/").replace("[*]", "");
//        } else if (jsonPath.equals("$")) {
//            return ""; // Root JSON path
//        } else {
//            throw new IllegalArgumentException("Invalid JSONPath expression: " + jsonPath);
//        }
//    }
//}
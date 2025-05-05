package org.rutz;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Comparator; // Import Comparator
import java.util.List;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToXmlSteam    {

    private static final Logger logger = Logger.getLogger(JsonToXmlSteam.class.getName()); // Logger instance

    // Main method to transform JSON to XML and write directly to a file
    public static void transformJsonToXml(String jsonString, List<Mapping> mappings, String outputFilePath) throws Exception {
        // Parse JSON
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonString);

        // Initialize XML writer with a file output stream
        FileOutputStream fileOutputStream = new FileOutputStream(new File(outputFilePath));
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(fileOutputStream, "UTF-8");

        writer.writeStartDocument("UTF-8", "1.0");

        // Sort the root-level mappings based on their order field
//        mappings.sort(Comparator.comparingInt(Mapping::getOrder));

        // Process each mapping (now in the desired order)
        for (Mapping mapping : mappings) {
            String jsonPointer = convertJsonPathToJsonPointer(mapping.getJPath());
            JsonNode jsonValue = rootNode.at(jsonPointer);

            if (!jsonValue.isMissingNode()) {
                writeXmlElement(writer, jsonValue, mapping);
            } else {
                logger.info("Skipping missing node for: " + mapping.getJPath());
            }
        }

        writer.writeEndDocument();
        writer.close();
        fileOutputStream.close();
    }

    // Process each XML element
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

    // Process each element, including handling objects, lists, and value nodes
    public static void processElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        if (mapping.isList() && jsonNode.isArray()) {
            processArrayElement(writer, jsonNode, mapping, elementName);
        } else if (jsonNode.isObject()) {
            processObjectElement(writer, jsonNode, mapping, elementName);
        } else if (jsonNode.isValueNode()) {
            processValueNode(writer, jsonNode, mapping, elementName);
        }
    }

    // Process JSON arrays as XML list elements
    public static void processArrayElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        logger.info("Processing array for element: " + elementName + ", JSON node size: " + jsonNode.size());

        for (JsonNode listItem : jsonNode) {
            boolean itemHasContent = false;

            // Determine if the current list item will produce output
            if (listItem.isValueNode()) {
                // Check if value is non-null (add !listItem.asText().isEmpty() if needed for empty strings)
                itemHasContent = !listItem.isNull();
            } else if (listItem.isObject() || listItem.isArray()) {
                // Check if the complex list item has producible children based on the list mapping's children
                // We use 'mapping' here because its childMappings define the structure *inside* the list item.
                itemHasContent = checkProducibleChildContent(listItem, mapping);
            }

            // If the item has content, write its element wrapper and content
            if (itemHasContent) {
                writer.writeStartElement(elementName); // Start the <elementName> tag for the item
                // TODO: Handle writing attributes for the list item element if needed.
                // writeAttributes(writer, listItem, mapping);

                // Process the content of the list item
                if (listItem.isValueNode()) {
                    writer.writeCharacters(AttributeLevelTransformation.transform(listItem.asText(), mapping));
                } else {
                    // listItem is complex (Object/Array), process its children using the list mapping rules
                    processChildMappings(writer, listItem, mapping);
                }
                writer.writeEndElement(); // End the <elementName> tag for the item
            } else {
                // Log skipping the empty list item
                logger.info("Skipping empty list item within element: " + elementName);
            }
        }
    }

    // Process JSON objects as XML elements
    public static void processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        // Check if this object will have any child elements written before creating it
        boolean hasContent = checkProducibleChildContent(jsonNode, mapping);

        if (hasContent) {
            writer.writeStartElement(elementName);
            // TODO: Handle writing attributes here if they exist and should be written even if no child elements exist.
            // writeAttributes(writer, jsonNode, mapping);
            processChildMappings(writer, jsonNode, mapping); // Write the children
            writer.writeEndElement();
        } else {
            // Log skipping the empty element
            logger.info("Skipping empty object element: " + elementName + " for jPath: " + mapping.getJPath());
        }
    }

    // Process JSON value nodes as XML elements
    private static void processValueNode(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        writer.writeStartElement(elementName);
//        writeAttributes(writer, jsonNode, mapping);
        writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
        writer.writeEndElement();
    }


    // Process child mappings recursively
    private static void processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        if (mapping.getChildMappings() != null) {
            for (Mapping childMapping : mapping.getChildMappings()) {
                if (!childMapping.getXPath().contains("@")) {
                    logger.info("========Inside processChildMappings ==== " + childMapping.getXPath());// Skip attributes
                    String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
                    JsonNode childNode = jsonNode.at(childPointer);

                    if (!childNode.isMissingNode()) {
                        writeXmlElement(writer, childNode, childMapping);
                    } else {
                        logger.info("========Child node missing for: " + childMapping.getXPath());
                    }
                }
            }
        }
    }

    // Convert JSONPath to JSON Pointer
    private static String convertJsonPathToJsonPointer(String jsonPath) {
        if (jsonPath.startsWith("$.") ) {
            return "/" + jsonPath.substring(2).replace(".", "/").replace("[*]", "");
        } else if (jsonPath.equals("$")) {
            return ""; // Root JSON path
        } else {
            throw new IllegalArgumentException("Invalid JSONPath expression: " + jsonPath);
        }
    }

    /**
     * Checks recursively if a given JSON node, based on its corresponding mapping's children,
     * will produce any actual XML content (child elements or attributes).
     * This helps avoid creating empty parent elements.
     *
     * @param parentJsonNode The current JSON node (representing the potential parent element).
     * @param parentMapping The mapping rule for the potential parent element, containing child mappings.
     * @return true if at least one child mapping corresponds to existing/non-null data
     *         that will result in XML output, false otherwise.
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

            String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
            JsonNode childNode = parentJsonNode.at(childPointer);

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

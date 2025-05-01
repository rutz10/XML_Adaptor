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
        mappings.sort(Comparator.comparingInt(Mapping::getOrder));

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
        logger.info("jsonNode array count: " + jsonNode.size());

        for (JsonNode listItem : jsonNode) {
            writer.writeStartElement(elementName);
//            writeAttributes(writer, listItem, mapping);

            if (listItem.isValueNode()) {
                writer.writeCharacters(AttributeLevelTransformation.transform(listItem.asText(), mapping));
            } else {
                processChildMappings(writer, listItem, mapping);
            }
            writer.writeEndElement();
        }
    }

    // Process JSON objects as XML elements
    public static void processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        writer.writeStartElement(elementName);
//        writeAttributes(writer, jsonNode, mapping);

        if (mapping.getChildMappings() != null && !mapping.getChildMappings().isEmpty()) {
            processChildMappings(writer, jsonNode, mapping);
        } else if (jsonNode.isValueNode()) {
            writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
        }
        writer.writeEndElement();
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
}

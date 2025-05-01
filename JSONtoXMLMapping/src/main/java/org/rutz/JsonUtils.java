package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonUtils {

    // Generic function to copy an object with a specific name into a new container and return the JSON as a string
    public static String copyObjectToNewContainer(String inputFilePath, String containerName,
                                                  String objectName, String newContainerName) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Read the JSON data from the file into a String
            String jsonString = inputFilePath;

            // Parse the JSON string into a JsonNode
            JsonNode rootNode = objectMapper.readTree(jsonString);

            // Navigate to the container in the JSON (this can be an arbitrary array or object in the root)
            JsonNode containerNode = rootNode.path(containerName);

            if (containerNode.isArray()) {
                ArrayNode containerArrayNode = (ArrayNode) containerNode;

                // Create a new array to hold the new container data
                ArrayNode newContainerNode = objectMapper.createArrayNode();

                // Loop through the array and find the object with the specified name
                for (JsonNode objectNode : containerArrayNode) {
                    if (objectNode.path("departmentName").asText().equals(objectName)) {
                        // Create a deep copy of the object and add it to the new container array
                        ObjectNode copiedObjectNode = (ObjectNode) objectNode.deepCopy();
                        newContainerNode.add(copiedObjectNode);
                    }
                }

                // Add the new container to the root node
                ((ObjectNode) rootNode).set(newContainerName, newContainerNode);

            } else if (containerNode.isObject()) {
                // In case the container is an object, we can implement specific logic to traverse it
                // This part can be extended if needed for specific use cases
            }

            // Convert the modified rootNode back to a JSON string and return it
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

package org.rutz;

import java.util.*;
import java.util.Comparator; // Import Comparator

import java.io.FileReader;
import java.io.IOException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class MappingGenerator {
    public static List<Mapping> readMappingsFromCsv(String filePath) throws IOException, CsvValidationException {
        Map<String, Mapping> mappingRegistry = new HashMap<>();
        // Create a virtual root mapping (order doesn't matter for the virtual root itself)
        Mapping virtualRoot = new Mapping( "$",  "Root",  false,  "",  "", "", "", "", 0 );
        mappingRegistry.put("Root", virtualRoot);
        // List<Mapping> rootMappings = new ArrayList<>(); // This list seems unused
        // rootMappings.add(virtualRoot);

        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = new ArrayList<>();
            String[] row;

            // Read all rows from the CSV file
            boolean isHeader = true;
            while ((row = csvReader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip the header row
                    continue;
                }
                rows.add(row);
            }
            // Process mappings iteratively
            processMappingsIteratively(rows, mappingRegistry, virtualRoot);

            // Sort mappings recursively based on the 'order' field
            sortMappingsRecursively(virtualRoot);

            // Debug: Print the final mapping hierarchy (after sorting)
            System.out.println("\n--- Sorted Mapping Hierarchy ---");
            printMappingHierarchy(virtualRoot,  0);
            System.out.println("------------------------------\n");

            // Return child mappings of the virtual root (now sorted)
            return virtualRoot.getChildMappings();
        }
    }

    public static void processMappingsIteratively(List<String[]> rows, Map<String, Mapping> mappingRegistry, Mapping virtualRoot) {
        boolean changesMade;
        do {
            changesMade = false;
            for (String[] row : rows) {
                String jPath = row[0];
                String xPath = row[1];
                boolean isList = "Yes".equalsIgnoreCase(row[2]);
                String jsonType = row[3];
                String xmlType = row[4];
                String exprsn = row[5];
                String namespace = row[6];
                String parentXPath = row[7] != null ? row[7].trim() : "";
                int order = 0; // Default order
                if (row.length > 8 && row[8] != null && !row[8].trim().isEmpty()) {
                    try {
                        order = Integer.parseInt(row[8].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid order value '" + row[8] + "' for xPath '" + xPath + "'. Using default order 0.");
                        // Optionally log this error more formally
                    }
                }


                if (mappingRegistry.containsKey(xPath)) {
                    // Skip already processed mappings
                    continue;
                }
                if (parentXPath.isEmpty() || mappingRegistry.containsKey(parentXPath)) {
                    // Process the mapping if parent XPath is empty or already resolved
                    Mapping mapping = new Mapping(jPath, xPath, isList, jsonType, xmlType, exprsn, namespace, parentXPath, order); // Pass order here
                    if (parentXPath.isEmpty() ) {
                        // Link top level mappings to the virtual root
                        System.out.println("Linking top level mapping to virtual root : " + xPath);
                        virtualRoot.addChildMapping(mapping);
                    } else {
                        // Handle the case where the parent XPath is not yet resolved
                        // You might want to log a warning or store this mapping for later processing
                        Mapping parentMapping = mappingRegistry.get(parentXPath);
                        System.out.println("Adding child mapping to parent: " + xPath + " -> " + parentXPath);
                        parentMapping.addChildMapping(mapping);
                    }

                    mappingRegistry.put(xPath, mapping);
                    changesMade = true;
                } else {
                    // Handle the case where the parent XPath is not yet resolved
                    // You might want to log a warning or store this mapping for later processing
                    System.out.println("Parent XPath not resolved yet: " + parentXPath + " for " + xPath);
                }
            }
        } while (changesMade);
    }

    // Print the mapping hierarchy
    private static void printMappingHierarchy(Mapping mapping, int level) {
        String indent = " ".repeat(level * 2);
        System.out.println(indent + "Mapping: " + mapping.getXPath());
        for (Mapping child : mapping.getChildMappings()) {
            printMappingHierarchy(child,  level + 1);
        }
    }

    // Recursively sorts child mappings based on the 'order' field
    private static void sortMappingsRecursively(Mapping mapping) {
        if (mapping == null || mapping.getChildMappings() == null || mapping.getChildMappings().isEmpty()) {
            return; // Base case: no children to sort
        }

        // Sort the direct children of the current mapping
        mapping.getChildMappings().sort(Comparator.comparingInt(Mapping::getOrder));

        // Recursively sort the children of each child
        for (Mapping child : mapping.getChildMappings()) {
            sortMappingsRecursively(child);
        }
    }
}

package org.rutz;

import java.util.*;
import java.util.Comparator; // Import Comparator

import java.io.FileReader;
import java.io.IOException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class MappingGenerator {
    /**
     * Reads mappings from a CSV file, processes them into a hierarchical structure,
     * and returns the top-level mappings that serve as entry points.
     * Now supports collector mappings and derived fields.
     * 
     * @param filePath Path to the mappings CSV file
     * @return List of top-level mappings (direct children of the virtual root)
     */
    public static List<Mapping> readMappingsFromCsv(String filePath) throws IOException, CsvValidationException {
        Map<String, Mapping> mappingRegistry = new HashMap<>();
        // Create a virtual root mapping (order doesn't matter for the virtual root itself)
        Mapping virtualRoot = new Mapping("$", "Root", false, "", "", "", "", "", 0);
        mappingRegistry.put("Root", virtualRoot);

        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = new ArrayList<>();
            String[] row;

            // Read all rows from the CSV file
            boolean isHeader = true;
            String[] headers = null; // To store header column names
            while ((row = csvReader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip the header row
                    headers = row;    // Store the headers for column index lookup
                    continue;
                }
                rows.add(row);
            }
            
            // Process mappings iteratively with support for new fields
            processMappingsIteratively(rows, mappingRegistry, virtualRoot, headers);

            // Sort mappings recursively based on the 'order' field
            sortMappingsRecursively(virtualRoot);

            // Debug: Print the final mapping hierarchy (after sorting)
            System.out.println("\n--- Sorted Mapping Hierarchy ---");
            printMappingHierarchy(virtualRoot, 0);
            System.out.println("------------------------------\n");

            // Return child mappings of the virtual root (now sorted)
            return virtualRoot.getChildMappings();
        }
    }

    /**
     * Process mappings from CSV rows, building the hierarchy iteratively.
     * Supports the new collector mappings and derived fields functionality.
     * 
     * @param rows The CSV rows to process
     * @param mappingRegistry Registry of mappings by xpath for quick lookup
     * @param virtualRoot The virtual root mapping that serves as parent for top-level mappings
     * @param headers The CSV column headers to help find column positions
     */
    public static void processMappingsIteratively(List<String[]> rows, Map<String, Mapping> mappingRegistry, 
                                                  Mapping virtualRoot, String[] headers) {
        // Find column indices for the various fields (with fallbacks for backward compatibility)
        int isCollectorIdx = findColumnIndex(headers, "isCollector", -1);
        int sourceFeedJPathsIdx = findColumnIndex(headers, "sourceFeedJPaths", -1);
        int conditionJPathIdx = findColumnIndex(headers, "conditionJPath", -1);
        int defaultValueIdx = findColumnIndex(headers, "defaultValue", -1);
        
        List<String[]> unprocessedRows = new ArrayList<>(rows);
        List<String[]> processedInThisPass = new ArrayList<>();

        int previousUnprocessedCount;
        do {
            previousUnprocessedCount = unprocessedRows.size();
            processedInThisPass.clear();

            for (String[] row : unprocessedRows) {
                String jPath = row[0];
                String xPath = row[1]; // This is the local name, and also the key for this rule if it's a parent
                boolean isList = "Yes".equalsIgnoreCase(row[2]);
                String jsonType = row[3];
                String xmlType = row[4];
                String exprsn = row[5];
                String namespace = row[6];
                String parentXPath = (row.length > 7 && row[7] != null) ? row[7].trim() : "";
                
                int order = 0; // Default order
                if (row.length > 8 && row[8] != null && !row[8].trim().isEmpty()) {
                    try {
                        order = Integer.parseInt(row[8].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid order value '" + row[8] + "' for xPath '" + xPath + "'. Using default order 0.");
                    }
                }
                
                boolean isCollector = false;
                List<String> sourceFeedJPaths = new ArrayList<>();
                String conditionJPath = null;
                String defaultValue = null;
                
                if (isCollectorIdx >= 0 && row.length > isCollectorIdx && row[isCollectorIdx] != null) {
                    isCollector = "Yes".equalsIgnoreCase(row[isCollectorIdx].trim());
                }
                
                if (sourceFeedJPathsIdx >= 0 && row.length > sourceFeedJPathsIdx && 
                    row[sourceFeedJPathsIdx] != null && !row[sourceFeedJPathsIdx].trim().isEmpty()) {
                    String[] feeds = row[sourceFeedJPathsIdx].split(";");
                    for (String feed : feeds) {
                        sourceFeedJPaths.add(feed.trim());
                    }
                }
                
                if (conditionJPathIdx >= 0 && row.length > conditionJPathIdx) {
                    conditionJPath = (row[conditionJPathIdx] != null) ? row[conditionJPathIdx].trim() : null;
                    if (conditionJPath != null && conditionJPath.isEmpty()) {
                        conditionJPath = null;
                    }
                }
                
                if (defaultValueIdx >= 0 && row.length > defaultValueIdx) {
                    defaultValue = (row[defaultValueIdx] != null) ? row[defaultValueIdx] : null;
                }

                // Each row defines a mapping. We create it if its parent is available.
                // The original skip "if (mappingRegistry.containsKey(xPath))" was the bug, as xPath is local name.
                
                if (parentXPath.isEmpty() || mappingRegistry.containsKey(parentXPath)) {
                    // Parent is resolvable (or it's a root-level mapping).
                    Mapping mapping = new Mapping(jPath, xPath, isList, jsonType, xmlType, exprsn, namespace, 
                                                  parentXPath, order, isCollector, sourceFeedJPaths,
                                                  conditionJPath, defaultValue);
                    
                    if (parentXPath.isEmpty()) {
                        virtualRoot.addChildMapping(mapping);
                        System.out.println("Linking top level mapping to virtual root : " + xPath);
                    } else {
                        Mapping parentMapping = mappingRegistry.get(parentXPath);
                        // Ensure parentMapping is not null, though containsKey should guarantee it
                        if (parentMapping != null) {
                            parentMapping.addChildMapping(mapping);
                            System.out.println("Adding child mapping to parent: " + xPath + " -> " + parentXPath);
                        } else {
                             // Should not happen if logic is correct and parentXPath is valid
                            System.err.println("Error: Parent mapping object not found for parentXPath: " + parentXPath + " when processing child " + xPath);
                        }
                    }

                    // Add this mapping to the registry IF its xPath is intended to be a unique ID
                    // that other rules can reference as parentXPath.
                    // For structural elements (like "Organization", "Employee"), their xPath is unique and used as parentXPath.
                    // For leaf elements (like "Status", "FullName"), their xPath might not be unique.
                    // If we put "Status" here, it might get overwritten by another "Status" rule.
                    // However, "Status" is not used as a parentXPath in the CSV.
                    // So, we only need to ensure that structural xPaths are reliably in the map.
                    // A simple strategy: if an xPath is not yet in the map, add it. The first definition for that xPath wins.
                    // This is consistent with how parents are resolved.
                    if (!mappingRegistry.containsKey(xPath)) {
                        mappingRegistry.put(xPath, mapping);
                    }
                    
                    processedInThisPass.add(row);
                }
            }
            unprocessedRows.removeAll(processedInThisPass);
        } while (unprocessedRows.size() < previousUnprocessedCount && !unprocessedRows.isEmpty()); // Loop if progress was made

        if (!unprocessedRows.isEmpty()) {
            System.err.println("Warning: Could not resolve parent dependencies for all mapping rows. Unprocessed rows (" + unprocessedRows.size() + "):");
            for (String[] row : unprocessedRows) {
                System.err.println("  xPath: " + row[1] + ", parentXPath: " + (row.length > 7 ? row[7] : "N/A"));
            }
        }
    }
    
    /**
     * Find the index of a column in the CSV headers.
     * 
     * @param headers The CSV header row
     * @param columnName The name of the column to find
     * @param defaultVal Default value to return if not found
     * @return The index of the column, or defaultVal if not found
     */
    private static int findColumnIndex(String[] headers, String columnName, int defaultVal) {
        if (headers == null) return defaultVal;
        
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return defaultVal;
    }

    /**
     * Print the mapping hierarchy for debugging purposes.
     * 
     * @param mapping The current mapping to print
     * @param level The indentation level
     */
    private static void printMappingHierarchy(Mapping mapping, int level) {
        String indent = " ".repeat(level * 2);
        String collectorInfo = mapping.isCollector() ? " [COLLECTOR: " + 
                               String.join(", ", mapping.getSourceFeedJPaths()) + "]" : "";
        String derivedInfo = mapping.getConditionJPath() != null ? 
                              " [DERIVED: if " + mapping.getConditionJPath() + 
                              " exists, value=" + mapping.getDefaultValue() + "]" : "";
        
        System.out.println(indent + "Mapping: " + mapping.getXPath() + collectorInfo + derivedInfo);
        for (Mapping child : mapping.getChildMappings()) {
            printMappingHierarchy(child, level + 1);
        }
    }

    /**
     * Recursively sorts child mappings based on the 'order' field.
     * 
     * @param mapping The mapping whose children should be sorted
     */
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

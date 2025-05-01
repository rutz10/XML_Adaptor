package org.rutz;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingGenerator1 {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    public List<Mapping> readMappingsFromCsv(String filePath) throws IOException, CsvValidationException {
        Map<String, Mapping> mappingRegistry = new HashMap<>();
        List<Mapping> rootMappings = new ArrayList<>();
        // Create a virtual root mapping
        Mapping virtualRoot = new Mapping( "$",  "Root",  false,  "",  "", "", "", ""  );
        mappingRegistry.put("Root", virtualRoot);
        rootMappings.add(virtualRoot);

        File file = getFileContent(filePath);

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
            // Debug: Print the final mapping hierarchy
            printMappingHierarchy(virtualRoot,  0);
            // Return child mappings of the virtual root
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
                String parentXPath = row[7] != null ? row[7].trim() : ""; // No need to trim, as it might be null

                if (mappingRegistry.containsKey(xPath)) {
                    // Skip already processed mappings
                    continue;
                }
                if (parentXPath.isEmpty() || mappingRegistry.containsKey(parentXPath)) {
                    // Process the mapping if parent XPath is empty or already resolved
                    Mapping mapping = new Mapping(jPath, xPath, isList, jsonType, xmlType, exprsn, namespace, parentXPath);
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

    public File getFileContent(String filePath) {
        File file = null;
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:" + filePath);
            for (Resource resource : resources) {
                String fileName = resource.getFilename().trim();
                if (!fileName.isEmpty() && fileName.endsWith("csv")) {
                    InputStream is = null;
                    is = resource.getInputStream();
                    file = File.createTempFile(resource.getFilename(), ".tmp");
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (file != null) {
        }
        return file;
    }


    // Print the mapping hierarchy
    private static void printMappingHierarchy(Mapping mapping, int level) {
        String indent = " ".repeat(level * 2);
        System.out.println(indent + "Mapping: " + mapping.getXPath());
        for (Mapping child : mapping.getChildMappings()) {
            printMappingHierarchy(child,  level + 1);
        }
    }
}
package org.rutz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demo class to demonstrate the advanced Collector Mapping functionality
 * with a complex, multi-level JSON structure.
 */
public class CollectorMappingDemo {

    public static void main(String[] args) {
        try {
            // Define file paths
            String jsonFilePath = "src/main/resources/complex_company.json";
            String mappingsFilePath = "src/main/resources/complex_mappings.csv";
            String outputXmlPath = "complex_output.xml";
            
            System.out.println("=== Advanced Collector Mapping Demo ===");
            System.out.println("1. Using JSON data file: " + jsonFilePath);
            System.out.println("2. Using mappings file: " + mappingsFilePath);
            
            // Read the JSON content
            String jsonString = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            
            // Load and process mappings
            List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(mappingsFilePath);
            System.out.println("3. Loaded and processed mappings");
            
            // Perform the transformation
            System.out.println("4. Transforming JSON to XML...");
            JsonToXmlSteam.transformJsonToXml(jsonString, mappings, outputXmlPath);
            
            System.out.println("5. Transformation complete! Output written to: " + outputXmlPath);
            System.out.println("\nKey features demonstrated:");
            System.out.println("• Collection of data from multiple JSON paths into unified XML structures");
            System.out.println("• Derived fields based on conditions (e.g., Status fields)");
            System.out.println("• Multi-level nested XML generation");
            System.out.println("• Handling of various data types (strings, numbers, booleans, objects)");
            System.out.println("• Proper ordering of XML elements");
            
            // Print a sample of the collector mappings from the CSV for reference
            System.out.println("\nCollector Mapping Examples in the configuration:");
            System.out.println("1. Departments Collector: Gathers data from both mainDepartments and satellite offices");
            System.out.println("   sourceFeedJPaths: \"$.company.mainDepartments[*];$.company.satelliteOffices.departments[*]\"");
            System.out.println("2. Employees Collector: Combines regular staff and contractors");
            System.out.println("   sourceFeedJPaths: \"$.company.staffDirectory[*];$.company.contractStaff[*]\"");
            System.out.println("3. Projects Collector: Combines active, upcoming, and completed projects");
            System.out.println("   sourceFeedJPaths: \"$.company.projects.active[*];$.company.projects.upcoming[*];$.company.projects.completed[*]\"");
            
        } catch (Exception e) {
            System.err.println("Error in transformation process:");
            e.printStackTrace();
        }
    }
} 
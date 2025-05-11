//package org.rutz;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
///**
// * Demo class to demonstrate the collector mapping functionality.
// * This class loads example data and mappings, and executes the transformation.
// */
//public class DemoCollectorMapping {
//
//    public static void main(String[] args) {
//        try {
//            // Read the JSON data
//            String jsonFilePath = "src/main/resources/example_collector.json";
//            String jsonString = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
//            System.out.println("Loaded JSON data from: " + jsonFilePath);
//
//            // Read the mappings
//            String mappingsFilePath = "src/main/resources/mappings_collector.csv";
//            List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(mappingsFilePath);
//            System.out.println("Loaded and processed mappings from: " + mappingsFilePath);
//
//            // Generate the XML output
//            String outputFilePath = "output_collector.xml";
//            System.out.println("Generating XML...");
//            JsonToXmlSteam.transformJsonToXml(jsonString, mappings, outputFilePath);
//            System.out.println("XML generation complete. Output written to: " + outputFilePath);
//
//            // Display success message
//            System.out.println("\nTransformation completed successfully!");
//            System.out.println("The collector mapping functionality has processed data from multiple sources:");
//            System.out.println("1. Array items from $.company.branchList[*]");
//            System.out.println("2. Single object from $.company.mainBranch");
//            System.out.println("3. Primitive value from $.company.alternateBranchInfo");
//            System.out.println("\nDerived fields were created based on conditions:");
//            System.out.println("- Status: 'Active' if 'isActive' exists, 'Inactive' otherwise");
//            System.out.println("- Region: 'DefaultRegion' if 'name' exists");
//
//        } catch (Exception e) {
//            System.err.println("Error in transformation process:");
//            e.printStackTrace();
//        }
//    }
//}
package org.rutz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

/**
 * Test class for the Collector Mapping functionality.
 * Tests the ability to transform complex JSON structures to XML
 * using multiple source paths and derived fields.
 */
public class CollectorMappingTest {

    @TempDir
    Path tempDir;

    private Path jsonFilePath;
    private Path mappingsFilePath;
    private Path outputXmlPath;

    @BeforeEach
    public void setUp() throws IOException {
        // Copy test resources to temporary directory
        copyResourceToTemp("complex_test.json", "complex_test.json");
        copyResourceToTemp("test_mappings.csv", "test_mappings.csv");
        
        // Set file paths
        jsonFilePath = tempDir.resolve("complex_test.json");
        mappingsFilePath = tempDir.resolve("test_mappings.csv");
        outputXmlPath = tempDir.resolve("test_output.xml");
    }

    /**
     * Copies a resource file to the temporary test directory
     * 
     * @param resourceName The name of the resource file
     * @param targetFileName The name to give the copied file
     * @throws IOException if the resource cannot be copied
     */
    private void copyResourceToTemp(String resourceName, String targetFileName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            Files.copy(is, tempDir.resolve(targetFileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    public void testCollectorMappingTransformation() throws Exception {
        // Load JSON data
        String jsonString = Files.readString(jsonFilePath);
        
        // Load and process mappings
        List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(mappingsFilePath.toString());
        
        // Perform the transformation
        JsonToXmlSteam.transformJsonToXml(jsonString, mappings, outputXmlPath.toString());
        
        // Verify the output file exists
        File outputFile = outputXmlPath.toFile();
        assertTrue(outputFile.exists(), "Output XML file should be created");
        assertTrue(outputFile.length() > 0, "Output XML file should not be empty");
        
        // Parse the output XML for validation
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(outputFile);
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        
        // Test 1: Verify the root element and company info
        XPathExpression expr = xpath.compile("/Organization/CompanyInfo/Name");
        String companyName = expr.evaluate(doc);
        assertEquals("TechCorp Global", companyName, "Company name should match");
        
        // Test 2: Count the departments from multiple sources
        expr = xpath.compile("count(/Organization/Departments/Department)");
        double departmentCount = (double) expr.evaluate(doc, XPathConstants.NUMBER);
        assertEquals(5.0, departmentCount, "Should have 5 departments (3 from main + 2 from satellite)");
        
        // Test 3: Verify derived fields based on conditions
        expr = xpath.compile("/Organization/Departments/Department[DepartmentName='Engineering']/Status");
        String engineeringStatus = expr.evaluate(doc);
        assertEquals("Active", engineeringStatus, "Engineering department should be Active");
        
        expr = xpath.compile("/Organization/Departments/Department[DepartmentName='Archive']/Status");
        String archiveStatus = expr.evaluate(doc);
        assertEquals("Inactive", archiveStatus, "Archive department should be Inactive");
        
        // Test 4: Verify employee collection
        expr = xpath.compile("count(/Organization/Employees/Employee)");
        double employeeCount = (double) expr.evaluate(doc, XPathConstants.NUMBER);
        assertEquals(5.0, employeeCount, "Should have 5 employees from different sources");
        
        // Test 5: Verify locations collection with nested data
        expr = xpath.compile("count(/Organization/Locations/Office)");
        double officeCount = (double) expr.evaluate(doc, XPathConstants.NUMBER);
        assertEquals(3.0, officeCount, "Should have 3 offices");
        
        // Test 6: Verify nested address data is correctly mapped
        expr = xpath.compile("/Organization/Locations/Office[Name='Headquarters']/Address/City");
        String hqCity = expr.evaluate(doc);
        assertEquals("San Francisco", hqCity, "HQ city should be San Francisco");
        
        // Test 7: Verify data from primitive source is correctly mapped
        expr = xpath.compile("/Organization/CompanyInfo/YearFounded");
        String yearFounded = expr.evaluate(doc);
        assertEquals("1998", yearFounded, "Year founded should be mapped from primitive");
        
        // Test 8: Verify complex nested structures from multiple sources
        expr = xpath.compile("/Organization/Projects/Project[Name='Mobile App']/Manager");
        String mobileAppManager = expr.evaluate(doc);
        assertEquals("Jane Smith", mobileAppManager, "Mobile App manager should be Jane Smith");
        
        // Test 9: Verify collector with derived field
        expr = xpath.compile("/Organization/Departments/Department[DepartmentName='Engineering']/HeadCount");
        String engineeringHeadCount = expr.evaluate(doc);
        assertEquals("85", engineeringHeadCount, "Engineering HeadCount should be derived");
    }
} 
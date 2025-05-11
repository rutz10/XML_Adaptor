import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;

import org.rutz.JsonToXmlSteam;
import org.rutz.Mapping;
import org.rutz.MappingGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonToXmlStreamTest {

    @Test
    public void testJsonToXmlTransformation() throws IOException, CsvValidationException, Exception{
        // Use relative paths for resources
        Path jsonFilePath = Paths.get("src/main/resources/complex_company.json");
        Path csvFilePath = Paths.get("src/main/resources/complex_mappings.csv");

        // Read JSON data from a resource file
        String jsonData = Files.readString(jsonFilePath, StandardCharsets.UTF_8);

        // Read mappings from CSV
        List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(csvFilePath.toString());

        // Transform JSON to XML
        String outputFilePath = "TTT.xml";
        JsonToXmlSteam jsonToXmlSteam = new JsonToXmlSteam();
        jsonToXmlSteam.transformJsonToXml(jsonData, mappings, outputFilePath);

        // Read the generated XML file
        String generatedXml = Files.lines(Paths.get(outputFilePath)).collect(Collectors.joining(System.lineSeparator()));

        // Assert that the XML file is not empty
        assertNotNull(generatedXml);
        assertTrue(generatedXml.contains("<Organization>"), "XML should contain Organization element");

        // Additional assertions can be added here to verify the XML content
    }
}
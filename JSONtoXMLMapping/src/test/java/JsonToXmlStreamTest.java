import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;

import org.rutz.JsonToXmlSteam;
import org.rutz.Mapping;
import org.rutz.MappingGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class JsonToXmlStreamTest {

    @Test
    public void testJsonToXmlTransformation() throws IOException, CsvValidationException, Exception{
        // Read JSON data from a resource file
        String jsonData = new String(Files.readAllBytes(Paths.get("src/main/resources/complex_company.json")));

        String csvFile = "C:\\Users\\rushi\\Downloads\\CursorProject\\XML_Adaptor-main\\JSONtoXMLMapping\\src\\main\\resources\\complex_mappings.csv";

        // Step 3: Transform JSON to XML using JsonToXmlMapper
        List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(csvFile);

        // Transform JSON to XML
        JsonToXmlSteam.transformJsonToXml(jsonData, mappings, "TTT.xml");

        System.out.println("XML file created successfully!");

        // Assert that the XML string is not null
//        assertNotNull(xmlString);

        // You can further assert the correctness of the XML string using XML parsing libraries or comparison tools.
    }
}
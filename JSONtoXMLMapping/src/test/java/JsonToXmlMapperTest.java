//import com.opencsv.exceptions.CsvValidationException;
//import org.junit.jupiter.api.Test;
//
//import org.rutz.Mapping;
//import org.rutz.MappingGenerator;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//public class JsonToXmlMapperTest {
//
//    @Test
//    public void testJsonToXmlTransformation() throws IOException, CsvValidationException, Exception{
//        // Read JSON data from a resource file
//        String jsonData = new String(Files.readAllBytes(Paths.get("src/main/resources/data1.json")));
//
//        String csvFile = "C:\\Users\\rushi\\IdeaProjects\\JSONtoXMLMapping\\src\\main\\resources\\mappings1.csv";
//
//        // Step 3: Transform JSON to XML using JsonToXmlMapper
//        List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(csvFile);
//
//        // Transform JSON to XML
//        JsonToXmlSteam.transformJsonToXml(jsonData, mappings, "output.xml");
//
////        System.out.println(xmlString);
//
//        // Assert that the XML string is not null
////        assertNotNull(xmlString);
//
//        // You can further assert the correctness of the XML string using XML parsing libraries or comparison tools.
//    }
//}
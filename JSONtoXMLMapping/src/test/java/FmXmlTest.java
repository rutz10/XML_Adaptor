////import org.junit.jupiter.api.BeforeEach;
////import org.junit.jupiter.api.Test;
////import javax.xml.stream.XMLOutputFactory;
////import javax.xml.stream.XMLStreamWriter;
////import java.io.StringWriter;
////import java.util.List;
////
////import com.fasterxml.jackson.databind.JsonNode;
////import com.fasterxml.jackson.databind.ObjectMapper;
////import org.rutz.FmXml;
////import org.rutz.Mapping;
////
////import static org.junit.jupiter.api.Assertions.*;
////
////class FmXmlTest {
////    private FmXml converter;
////    private StringWriter stringWriter;
////    private XMLStreamWriter xmlWriter;
////    private ObjectMapper objectMapper = new ObjectMapper();
////
////    @BeforeEach
////    void setup() throws Exception {
////        converter = new FmXml();
////        stringWriter = new StringWriter();
////        xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
////    }
////
////    @Test
////    void simpleElementGeneration() throws Exception {
////        JsonNode node = objectMapper.readTree("{\"name\":\"John\"}");
////        Mapping mapping = new Mapping("$.name", "FullName", false, "String", "xs:string", "", "", "");
////
////        converter.transformJsonToXml(node, List.of(mapping), xmlWriter);
////        xmlWriter.flush();
////
////        assertEquals("<FullName>John</FullName>", stringWriter.toString());
////    }
////
////    @Test
////    void nestedElementStructure() throws Exception {
////        JsonNode node = objectMapper.readTree("{\"address\":{\"street\":\"Main St\"}}");
////        Mapping mapping = new Mapping("$.address.street", "Address/Street", false, "String", "xs:string", "", "", "");
////
////        converter.transformJsonToXml(node, List.of(mapping), xmlWriter);
////        xmlWriter.flush();
////
////        assertEquals("<Address><Street>Main St</Street></Address>", stringWriter.toString());
////    }
////
////    @Test
////    void arrayHandling() throws Exception {
////        JsonNode node = objectMapper.readTree("{\"scores\":[85,90,78]}");
////        Mapping mapping = new Mapping("$.scores", "Scores/Score", true, "Array", "xs:integer", "", "", "");
////
////        converter.transformJsonToXml(node, List.of(mapping), xmlWriter);
////        xmlWriter.flush();
////
////        assertEquals("<Scores><Score>85</Score><Score>90</Score><Score>78</Score></Scores>",
////                stringWriter.toString());
////    }
////
////    @Test
////    void jsonPathConversion() {
////        assertAll("JSONPath conversions",
////                () -> assertEquals("/a/b", FmXml.convertJsonPathToJsonPointer("$.a.b")),
////                () -> assertEquals("/array", FmXml.convertJsonPathToJsonPointer("$.array[*]")),
////                () -> assertEquals("", FmXml.convertJsonPathToJsonPointer("$"))
////        );
////    }
////
////    @Test
////    void specialCharacterEscaping() throws Exception {
////        JsonNode node = objectMapper.readTree("{\"comment\":\"<>&'\\\"\"}");
////        Mapping mapping = new Mapping("$.comment", "Text", false, "String", "xs:string", "", "", "");
////
////        converter.transformJsonToXml(node, List.of(mapping), xmlWriter);
////        xmlWriter.flush();
////
////        assertEquals("<Text>&lt;&gt;&amp;'\"</Text>", stringWriter.toString());
////    }
////}
//
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.rutz.FmXml;
//import org.rutz.Mapping;
//
//import javax.xml.stream.XMLOutputFactory;
//import javax.xml.stream.XMLStreamWriter;
//import java.io.StringWriter;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class FmXmlTest {
//
//    private FmXml fmXml;
//    private StringWriter stringWriter;
//    private XMLStreamWriter xmlWriter;
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        fmXml = new FmXml();
//        stringWriter = new StringWriter();
//        xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
//        objectMapper = new ObjectMapper();
//    }
//
//    // Helper method to create mappings with full parameters
//    private Mapping createMapping(String jPath, String xPath, boolean isList,
//                                  String jsonType, String xmlType, String exprsn,
//                                  String namespace, String parentXPath) {
//        return new Mapping(
//                jPath,
//                xPath,
//                isList,
//                jsonType,
//                xmlType,
//                exprsn,
//                namespace,
//                parentXPath
//        );
//    }
//
//    @Test
//    void testCompanyStructure() throws Exception {
//        // Arrange
//        String json = """
//        {
//            "companyName": "TestCo",
//            "companyLocation": "New York",
//            "branches": [
//                {"branchNameNA": "NA1", "branchNameEU": "EU1"},
//                {"branchNameNA": "NA2", "branchNameEU": "EU2"}
//            ]
//        }
//        """;
//        JsonNode rootNode = objectMapper.readTree(json);
//
//        // Build mappings according to the CSV structure
//        List<Mapping> mappings = new ArrayList<>();
//
//        // Root Company mapping
//        Mapping companyMapping = createMapping(
//                "$", "Company", false,
//                "String", "String", "",
//                "Default", "Root"
//        );
//
//        // Company child mappings
//        Mapping nameMapping = createMapping(
//                "$.companyName", "CompanyName", false,
//                "String", "String", "",
//                "Default", "Company"
//        );
//
//        Mapping locationMapping = createMapping(
//                "$.companyLocation", "CompanyLocation", false,
//                "String", "String", "",
//                "Default", "Company"
//        );
//
//        Mapping branchesMapping = createMapping(
//                "$.branches[*]", "Branches/Branch", true,
//                "String", "String", "",
//                "Default", "Company"
//        );
//
//        // Branch child mappings
//        Mapping branchNaMapping = createMapping(
//                "$.branchNameNA", "BranchNA", false,
//                "String", "String", "",
//                "Default", "Branches/Branch"
//        );
//
//        Mapping branchEuMapping = createMapping(
//                "$.branchNameEU", "BranchEU", false,
//                "String", "String", "",
//                "Default", "Branches/Branch"
//        );
//
//        // Build hierarchy
//        branchesMapping.addChildMapping(branchNaMapping);
//        branchesMapping.addChildMapping(branchEuMapping);
//
//        companyMapping.addChildMapping(nameMapping);
//        companyMapping.addChildMapping(locationMapping);
//        companyMapping.addChildMapping(branchesMapping);
//
//        mappings.add(companyMapping);
//
//        // Act
//        fmXml.transformJsonToXml(rootNode, mappings, xmlWriter);
//        xmlWriter.flush();
//        String xml = stringWriter.toString().replaceAll("\\s+", "");
//
//        // Assert
//        String expected = """
//        <Company>
//            <CompanyName>TestCo</CompanyName>
//            <CompanyLocation>NewYork</CompanyLocation>
//            <Branches>
//                <Branch>
//                    <BranchNA>NA1</BranchNA>
//                    <BranchEU>EU1</BranchEU>
//                </Branch>
//                <Branch>
//                    <BranchNA>NA2</BranchNA>
//                    <BranchEU>EU2</BranchEU>
//                </Branch>
//            </Branches>
//        </Company>
//        """.replaceAll("\\s+", "");
//
//        assertTrue(xml.contains(expected),
//                "Actual XML:\n" + formatXml(stringWriter.toString()));
//    }
//
//    @Test
////    void testDataTransformation() throws Exception {
////        // Arrange
////        JsonNode rootNode = objectMapper.readTree("{\"age\":\"30\"}");
////
////        Mapping ageMapping = createMapping(
////                "$.age", "Age", false,
////                "String", "Integer", "val", // Uses expression "val" with Integer conversion
////                "Default", "Root"
////        );
////
////        // Act
////        fmXml.transformJsonToXml(rootNode, List.of(ageMapping), xmlWriter);
////        xmlWriter.flush();
////        String xml = stringWriter.toString();
////
////        // Assert
////        assertTrue(xml.contains("<Age>30</Age>"),
////                "Transformed value should be integer");
////    }
//
//    private String formatXml(String xml) {
//        // Simple XML formatting for test output
//        return xml.replaceAll("><", ">\n<");
//    }
//}
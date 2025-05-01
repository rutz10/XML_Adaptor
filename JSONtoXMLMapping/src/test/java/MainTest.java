
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutz.FmXml;
import org.rutz.Mapping;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {

    private FmXml main;
    private ObjectMapper objectMapper;
    private XMLStreamWriter writer;
    private StringWriter stringWriter;

    @BeforeEach
    public void setUp() throws Exception {
        main = new FmXml();
        objectMapper = new ObjectMapper();
        stringWriter = new StringWriter();
        writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
    }

    @Test
    public void testTransformJsonToXml_simpleMapping() throws Exception {
        String json = "{\"name\":\"John\", \"age\":\"30\"}";
        JsonNode rootNode = objectMapper.readTree(json);

        List<Mapping> mappings = new ArrayList<>();
        List<String> derivedFromName = new ArrayList<>();
        derivedFromName.add("source1");
        Mapping parentMapping = new Mapping("$", "person", false, "String", "String", "", "Default","Root");
        mappings.add(parentMapping);
        Mapping nameMapping = new Mapping("$.name", "name", false, "String", "String", "", "Default","person");
        mappings.add(nameMapping);

        List<String> derivedFromAge = new ArrayList<>();
        derivedFromAge.add("source2");
        Mapping ageMapping = new Mapping("$.age", "age", false, "String", "String", "", "Default","person");
        mappings.add(ageMapping);

        main.transformJsonToXml(rootNode, mappings, writer);
        writer.flush();

        String expectedXml = "<person><name>John</name><age>30</age></person>";
        assertEquals(expectedXml, stringWriter.toString());
    }

    @Test
    public void testTransformJsonToXml_nestedMapping() throws Exception {
        String json = "{\"address\":{\"city\":\"New York\", \"zip\":\"10001\"}}";
        JsonNode rootNode = objectMapper.readTree(json);

        List<Mapping> mappings = new ArrayList<>();
        List<String> derivedFromCity = new ArrayList<>();
        derivedFromCity.add("source3");
        Mapping cityMapping = new Mapping("$.address.city", "location/city", false, "string", "string", null, null,  null);
        mappings.add(cityMapping);

        List<String> derivedFromZip = new ArrayList<>();
        derivedFromZip.add("source4");
        Mapping zipMapping = new Mapping("$.address.zip", "location/zip", false, "string", "string", null, null,  null);
        mappings.add(zipMapping);

        main.transformJsonToXml(rootNode, mappings, writer);
        writer.flush();

        String expectedXml = "<location><city>New York</city><zip>10001</zip></location>";
        assertEquals(expectedXml, stringWriter.toString());
    }

    @Test
    public void testTransformJsonToXml_arrayMapping() throws Exception {
        String json = "{\"items\":[\"apple\", \"banana\", \"cherry\"]}";
        JsonNode rootNode = objectMapper.readTree(json);

        List<Mapping> mappings = new ArrayList<>();
        List<String> derivedFromItems = new ArrayList<>();
        derivedFromItems.add("source5");
        Mapping itemsMapping = new Mapping("$.items[*]", "list/item", true, "array", "list", null, null,  null);
        mappings.add(itemsMapping);

        main.transformJsonToXml(rootNode, mappings, writer);
        writer.flush();

        String expectedXml = "<list><item>apple</item><item>banana</item><item>cherry</item></list>";
        assertEquals(expectedXml, stringWriter.toString());
    }

    @Test
    public void testTransformJsonToXml_missingNode() throws Exception {
        String json = "{\"name\":\"John\"}";
        JsonNode rootNode = objectMapper.readTree(json);

        List<Mapping> mappings = new ArrayList<>();
        List<String> derivedFromMissing = new ArrayList<>();
        derivedFromMissing.add("source6");
        Mapping missingMapping = new Mapping("$.age", "person/age", false, "number", "integer", null, null,  null);
        mappings.add(missingMapping);

        main.transformJsonToXml(rootNode, mappings, writer);
        writer.flush();

        String expectedXml = "";
        assertEquals(expectedXml, stringWriter.toString());
    }

    @Test
    public void testProcessObjectElement_withChildMappings() throws Exception {
        String json = "{\"address\":{\"city\":\"New York\", \"zip\":\"10001\"}}";
        JsonNode jsonNode = objectMapper.readTree(json).get("address");

        Mapping mapping = new Mapping(null, "address", false, null, null, null, null, null);

        List<Mapping> childMappings = new ArrayList<>();
        List<String> derivedFromCityChild = new ArrayList<>();
        derivedFromCityChild.add("source7");
        Mapping cityMapping = new Mapping("$.city", "city", false, "string", "string", null, null,  null);
        childMappings.add(cityMapping);

        List<String> derivedFromZipChild = new ArrayList<>();
        derivedFromZipChild.add("source8");
        Mapping zipMapping = new Mapping("$.zip", "zip", false, "string", "string", null, null,  null);
        childMappings.add(zipMapping);

        mapping.setChildMappings(childMappings);

        FmXml.processObjectElement(writer, jsonNode, mapping, "address");
        writer.flush();

        String expectedXml = "<address><city>New York</city><zip>10001</zip></address>";
        assertEquals(expectedXml, stringWriter.toString());
    }

    @Test
    public void testProcessValueNode() throws Exception {
        String json = "{\"value\":\"testValue\"}";
        JsonNode jsonNode = objectMapper.readTree(json).get("value");

        List<String> derivedFromValue = new ArrayList<>();
        derivedFromValue.add("source9");
        Mapping mapping = new Mapping(null, "value", false, "string", "string", null, null,  null);

        FmXml.processValueNode(writer, jsonNode, mapping, "value");
        writer.flush();

        String expectedXml = "<value>testValue</value>";
        assertEquals(expectedXml, stringWriter.toString());
    }

    @Test
    public void testTransformJsonToXml_complexNestedMapping() throws Exception {
        String json = "{\"person\":{\"name\":\"Alice\",\"details\":{\"address\":{\"city\":\"London\",\"postcode\":\"SW1A 1AA\"}}}}";
        JsonNode rootNode = objectMapper.readTree(json);

        List<Mapping> mappings = new ArrayList<>();
        List<String> derivedFromNameComplex = new ArrayList<>();
        derivedFromNameComplex.add("source10");
        Mapping personMapping = new Mapping("$.person.name", "person/name", false, "string", "string", null, null,  null);
        mappings.add(personMapping);

        List<String> derivedFromCityComplex = new ArrayList<>();
        derivedFromCityComplex.add("source11");
        Mapping addressMapping = new Mapping("$.person.details.address.city", "city", false, "string", "string", null, null,"person/details/address/");
        mappings.add(addressMapping);

        List<String> derivedFromPostcodeComplex = new ArrayList<>();
        derivedFromPostcodeComplex.add("source12");
        Mapping postcodeMapping = new Mapping("$.person.details.address.postcode", "person/details/address/postcode", false, "string", "string", null, null,"person/details/address/");
        mappings.add(postcodeMapping);

        main.transformJsonToXml(rootNode, mappings, writer);
        writer.flush();

        String expectedXml = "<person><name>Alice</name><details><address><city>London</city><postcode>SW1A 1AA</postcode></address></details></person>";
        assertEquals(expectedXml, stringWriter.toString());
    }
}
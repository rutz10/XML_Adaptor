//import org.junit.jupiter.api.Test;
//import org.rutz.JsonCopyUtil;
//import org.rutz.JsonUtils;
//import org.rutz.PreProcess;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class JsonUtilsTest {
//
//
//    @Test
//    public void testCopyObjectToNewContainer() throws IOException {
//        // Define the file path for the input JSON
//        String inputFilePath =  new String(Files.readAllBytes(Paths.get("src/main/resources/data5.json")));
//
//        String modifiedResponse = JsonCopyUtil.copyNestedArrayElements(
//                inputFilePath,               // Original JSON
//                "departments",              // Array to search
//                "departmentObject.departmentName", // Nested field path
//                "Sales",                    // Value to match
//                "Derived_departments"       // Name of new array
//        );
//
//        System.out.println(modifiedResponse);
//
//
//    }
//
//    @Test
//    public void testCopyObjectToNewContainerWithInvalidObject() throws IOException {
//        // Define the file path for the input JSON
//        String inputFilePath =  new String(Files.readAllBytes(Paths.get("src/main/resources/data5.json")));
//
//        String as = PreProcess.cleanGraphQLResponse(inputFilePath);
//        System.out.println(as);
//
//
//
//    }
//}

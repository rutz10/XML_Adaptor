
import org.junit.jupiter.api.Test;
import org.rutz.TransformerExpressionFunctions;

import static org.junit.jupiter.api.Assertions.*;

class TransformerExpressionFunctionsTest {



    // Test for convertUTCToEST method
    @Test
    void testConvertUTCToEST() {
        // Test case 1: Valid UTC date
        String utcDate = "2024-12-30T12:00:00Z";
        String expectedEstDate = "2024-12-30T07:00:00";  // EST is UTC - 5 hours
        String result = TransformerExpressionFunctions.convertUTCToEST(utcDate);
        assertEquals(expectedEstDate, result, "The converted UTC time to EST should match the expected value");

        // Test case 2: Another valid UTC date
        utcDate = "2024-12-31T00:00:00Z";
        expectedEstDate = "2024-12-30T19:00:00";  // EST is UTC - 5 hours
        result = TransformerExpressionFunctions.convertUTCToEST(utcDate);
        assertEquals(expectedEstDate, result, "The converted UTC time to EST should match the expected value");

    }

    // Test for convertUTCToESTDateOnly method
    @Test
    void testConvertUTCToESTDateOnly() {
        // Test case 1: Valid UTC date
        String utcDate = "2024-12-30T12:00:00Z";
        String expectedEstDate = "2024-12-30";  // EST date should match after conversion
        String result = TransformerExpressionFunctions.convertUTCToESTDateOnly(utcDate);
        assertEquals(expectedEstDate, result, "The converted UTC date to EST should match the expected date");

        // Test case 2: Another valid UTC date
        utcDate = "2024-12-31T00:00:00Z";
        expectedEstDate = "2024-12-30";  // EST date should match after conversion
        result = TransformerExpressionFunctions.convertUTCToESTDateOnly(utcDate);
        assertEquals(expectedEstDate, result, "The converted UTC date to EST should match the expected date");


    }
}

package org.rutz.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DivisionExample {
    
    public static String divideBy100AndFormat(String input) {
        try {
            // Convert the input string to a BigDecimal for precise arithmetic
            BigDecimal value = new BigDecimal(input);
            
            // Divide the value by 100
            BigDecimal result = value.divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);
            
            // Format the result to ensure it has at most 8 fractional digits and 12 total digits
            String formattedResult = result.toPlainString();
            
            // Ensure the total length does not exceed 12 characters (including the decimal point)
            if (formattedResult.length() > 12) {
                // Trim the result to 12 characters
                formattedResult = formattedResult.substring(0, 12);
            }
            
            return formattedResult;
        } catch (NumberFormatException e) {
            return "Invalid input"; // Return a message for invalid input
        }
    }

    public static void main(String[] args) {
        String input = "456"; // Example input string
        String result = divideBy100AndFormat(input);
        System.out.println("Formatted Result: " + result); // Output the result
    }
}

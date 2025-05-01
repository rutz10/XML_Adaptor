package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class TransformerExpressionFunctions {

    public String addFive(int val) {
        return val + 1000 + "";
    }

    // Function to convert UTC to EST and return the full datetime
    public static String convertUTCToEST(String utcDateString) {
        Instant utcInstant = Instant.parse(utcDateString);
        // Convert the Instant to EST time zone (Eastern Standard Time)
        ZonedDateTime estDateTime = utcInstant.atZone(ZoneId.of("America/New_York"));

        // Format the EST ZonedDateTime to a full date and time string
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return estDateTime.format(formatter);
    }

    // Function to convert UTC to EST and return only the date (YYYY-MM-DD)
    public static String convertUTCToESTDateOnly(String utcDateString) {
        // Parse the input UTC date string to an Instant
        Instant utcInstant = Instant.parse(utcDateString);

        // Convert the Instant to EST time zone (Eastern Standard Time)
        ZonedDateTime estDateTime = utcInstant.atZone(ZoneId.of("America/New_York"));

        // Format the EST ZonedDateTime to a date-only string (YYYY-MM-DD)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        return estDateTime.format(dateFormatter);
    }

}
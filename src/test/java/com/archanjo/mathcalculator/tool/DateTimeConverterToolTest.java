package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DateTimeConverterToolTest {

    private static final String ERROR_PREFIX = "Error:";
    private static final String UTC_ZONE = "UTC";
    private static final String ISO_FMT = "iso";
    private static final String NOON_UTC = "2026-03-03T12:00:00";
    private static final String SAO_PAULO = "America/Sao_Paulo";

    private final DateTimeConverterTool tool = new DateTimeConverterTool();

    @Nested
    @DisplayName("convertTimezone")
    class ConvertTimezone {

        @Test
        void utcToSaoPauloOffset() {
            final String result = tool.convertTimezone(
                    NOON_UTC, UTC_ZONE, SAO_PAULO);
            assertTrue(result.contains("-03:00"),
                    "Sao Paulo offset should be -03:00");
        }

        @Test
        void utcToSaoPauloTime() {
            final String result = tool.convertTimezone(
                    NOON_UTC, UTC_ZONE, SAO_PAULO);
            assertTrue(result.contains("09:00:00"),
                    "12:00 UTC = 09:00 in Sao Paulo");
        }

        @Test
        void utcToTokyoOffset() {
            final String result = tool.convertTimezone(
                    NOON_UTC, UTC_ZONE, "Asia/Tokyo");
            assertTrue(result.contains("+09:00"),
                    "Tokyo offset should be +09:00");
        }

        @Test
        void utcToTokyoTime() {
            final String result = tool.convertTimezone(
                    NOON_UTC, UTC_ZONE, "Asia/Tokyo");
            assertTrue(result.contains("21:00:00"),
                    "12:00 UTC = 21:00 in Tokyo");
        }

        @Test
        void invalidTimezoneReturnsError() {
            final String result = tool.convertTimezone(
                    NOON_UTC, UTC_ZONE, "Invalid/Zone");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid timezone should return error");
        }

        @Test
        void invalidDatetimeReturnsError() {
            final String result = tool.convertTimezone(
                    "not-a-date", UTC_ZONE, SAO_PAULO);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid datetime should return error");
        }
    }

    @Nested
    @DisplayName("formatDateTime")
    class FormatDateTime {

        @Test
        void epochToIsoContainsDate() {
            final String result = tool.formatDateTime(
                    "1709424000", "epoch", ISO_FMT, UTC_ZONE);
            assertTrue(result.contains("2024-03-03"),
                    "Epoch 1709424000 = 2024-03-03");
        }

        @Test
        void isoToCustomFormat() {
            final String result = tool.formatDateTime(
                    "2026-03-03T10:30:00", ISO_FMT,
                    "yyyy-MM-dd HH:mm:ss", UTC_ZONE);
            assertTrue(result.contains("2026-03-03 10:30:00"),
                    "Should format to custom pattern");
        }

        @Test
        void customToIsoNotError() {
            final String result = tool.formatDateTime(
                    "2026-03-03 15:30:00",
                    "yyyy-MM-dd HH:mm:ss", ISO_FMT, UTC_ZONE);
            assertEquals(false, result.startsWith(ERROR_PREFIX),
                    "Valid input should not error");
        }

        @Test
        void epochMillisContainsDate() {
            final String result = tool.formatDateTime(
                    "1709424000000", "epochMillis",
                    ISO_FMT, UTC_ZONE);
            assertTrue(result.contains("2024-03-03"),
                    "Epoch millis should resolve to date");
        }

        @Test
        void invalidEpochReturnsError() {
            final String result = tool.formatDateTime(
                    "not-a-number", "epoch", ISO_FMT, UTC_ZONE);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Non-numeric epoch should return error");
        }
    }

    @Nested
    @DisplayName("currentDateTime")
    class CurrentDateTime {

        @Test
        void returnsIsoFormat() {
            final String result = tool.currentDateTime(
                    UTC_ZONE, ISO_FMT);
            assertEquals(false, result.startsWith(ERROR_PREFIX),
                    "Should not be an error");
        }

        @Test
        void returnsEpochFormat() {
            final String result = tool.currentDateTime(
                    UTC_ZONE, "epoch");
            assertTrue(result.matches("\\d+"),
                    "Epoch should be numeric");
        }

        @Test
        void uppercaseDdFormatReturnsDayOfMonth() {
            final String result = tool.currentDateTime(
                    UTC_ZONE, "DD/MM/yyyy");
            final String dayPart = result.split("/")[0];
            final int day = Integer.parseInt(dayPart);
            assertTrue(day >= 1 && day <= 31,
                    "DD should be normalized to dd (day of month), got: "
                            + day);
        }

        @Test
        void invalidTimezoneReturnsError() {
            final String result = tool.currentDateTime(
                    "Invalid/Zone", ISO_FMT);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid timezone should return error");
        }
    }

    @Nested
    @DisplayName("listTimezones")
    class ListTimezones {

        @Test
        void americaContainsNewYork() {
            final String result = tool.listTimezones("America");
            assertTrue(result.contains("America/New_York"),
                    "Should contain New_York");
        }

        @Test
        void americaContainsSaoPaulo() {
            final String result = tool.listTimezones("America");
            assertTrue(result.contains(SAO_PAULO),
                    "Should contain Sao_Paulo");
        }

        @Test
        void europeContainsLondon() {
            final String result = tool.listTimezones("Europe");
            assertTrue(result.contains("Europe/London"),
                    "Should contain London");
        }

        @Test
        void invalidRegionReturnsError() {
            final String result = tool.listTimezones("Invalid");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid region should return error");
        }
    }

    @Nested
    @DisplayName("dateTimeDifference")
    class DateTimeDifference {

        @Test
        void twoMonthsDifference() {
            final String result = tool.dateTimeDifference(
                    "2026-01-01T00:00:00",
                    "2026-03-03T15:30:00", UTC_ZONE);
            assertTrue(result.contains("\"months\":2"),
                    "Should have 2 months difference");
        }

        @Test
        void zeroDifference() {
            final String result = tool.dateTimeDifference(
                    NOON_UTC, NOON_UTC, UTC_ZONE);
            assertTrue(result.contains("\"totalSeconds\":0"),
                    "Same datetime should have 0 total seconds");
        }

        @Test
        void reversedOrderStillWorks() {
            final String result = tool.dateTimeDifference(
                    "2026-03-03T15:30:00",
                    "2026-01-01T00:00:00", UTC_ZONE);
            assertTrue(result.contains("\"months\":2"),
                    "Reversed order should still work");
        }

        @Test
        void invalidDateReturnsError() {
            final String result = tool.dateTimeDifference(
                    "not-a-date", "2026-01-01T00:00:00",
                    UTC_ZONE);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid date should return error");
        }
    }
}

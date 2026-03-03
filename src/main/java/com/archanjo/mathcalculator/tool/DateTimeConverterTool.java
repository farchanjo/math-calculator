package com.archanjo.mathcalculator.tool;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DateTimeConverterTool {

    private static final String ERR_PREFIX = "Error: ";

    private static final List<DateTimeFormatter> PARSERS = List.of(
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"));

    private static final long SEC_PER_MIN = 60L;
    private static final long SEC_PER_HOUR = 3600L;
    private static final long SEC_PER_DAY = 86_400L;

    @Tool(description = """
            Convert datetime between timezones.""")
    public String convertTimezone(
            @ToolParam(description = """
                    ISO-8601 datetime.""") final String datetime,
            @ToolParam(description = """
                    Source IANA timezone.""") final String fromTimezone,
            @ToolParam(description = """
                    Target IANA timezone.""") final String toTimezone) {

        String result;
        try {
            final ZoneId fromZone = ZoneId.of(fromTimezone);
            final ZoneId toZone = ZoneId.of(toTimezone);
            final ZonedDateTime source =
                    parseDateTime(datetime, fromZone);
            final ZonedDateTime target =
                    source.withZoneSameInstant(toZone);
            result = target.format(
                    DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } catch (DateTimeParseException ex) {
            result = ERR_PREFIX + "Cannot parse datetime: " + datetime;
        } catch (DateTimeException ex) {
            result = ERR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Reformat a datetime string.""")
    public String formatDateTime(
            @ToolParam(description = """
                    Datetime string.""") final String datetime,
            @ToolParam(description = """
                    Input format or 'iso'.""") final String inputFormat,
            @ToolParam(description = """
                    Output format or 'iso'.""") final String outputFormat,
            @ToolParam(description = """
                    Timezone for parsing.""") final String timezone) {

        String result;
        try {
            final ZoneId zone = ZoneId.of(timezone);
            final ZonedDateTime parsed =
                    parseWithFormat(datetime, inputFormat, zone);
            result = formatOutput(parsed, outputFormat);
        } catch (DateTimeParseException ex) {
            result = ERR_PREFIX + "Cannot parse datetime: " + datetime;
        } catch (DateTimeException | NumberFormatException ex) {
            result = ERR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            Get current datetime in a timezone.""")
    public String currentDateTime(
            @ToolParam(description = """
                    IANA timezone.""") final String timezone,
            @ToolParam(description = """
                    Output format.""") final String format) {

        String result;
        try {
            final ZoneId zone = ZoneId.of(timezone);
            final ZonedDateTime now = ZonedDateTime.now(zone);
            result = formatOutput(now, format);
        } catch (DateTimeException ex) {
            result = ERR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = """
            List timezone IDs by region.""")
    public String listTimezones(
            @ToolParam(description = """
                    Region: America, Europe, Asia...""")
            final String region) {

        final String prefix = region + "/";
        final List<String> zones = ZoneId.getAvailableZoneIds()
                .stream()
                .filter(zoneId -> zoneId.startsWith(prefix))
                .sorted()
                .toList();

        final String result;
        if (zones.isEmpty()) {
            result = ERR_PREFIX
                    + "No timezones found for region: " + region;
        } else {
            result = zones.stream()
                    .map("\"%s\""::formatted)
                    .collect(Collectors.joining(",", "[", "]"));
        }
        return result;
    }

    @Tool(description = """
            Calculate time difference between two datetimes.""")
    public String dateTimeDifference(
            @ToolParam(description = """
                    First datetime.""") final String datetime1,
            @ToolParam(description = """
                    Second datetime.""") final String datetime2,
            @ToolParam(description = """
                    Timezone for parsing.""") final String timezone) {

        String result;
        try {
            final ZoneId zone = ZoneId.of(timezone);
            final ZonedDateTime first =
                    parseDateTime(datetime1, zone);
            final ZonedDateTime second =
                    parseDateTime(datetime2, zone);
            result = computeDifference(first, second);
        } catch (DateTimeParseException ex) {
            result = ERR_PREFIX + "Cannot parse datetime";
        } catch (DateTimeException ex) {
            result = ERR_PREFIX + ex.getMessage();
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Parsing helpers
    // ------------------------------------------------------------------ //

    private ZonedDateTime parseDateTime(
            final String datetime, final ZoneId zone) {

        ZonedDateTime result = tryParsers(datetime, zone);
        if (result == null) {
            result = tryLocalDate(datetime, zone);
        }
        if (result == null) {
            throw new DateTimeParseException(
                    "Cannot parse: " + datetime, datetime, 0);
        }
        return result;
    }

    private ZonedDateTime tryParsers(
            final String datetime, final ZoneId zone) {

        ZonedDateTime result = null;
        for (final DateTimeFormatter fmt : PARSERS) {
            if (result != null) {
                break;
            }
            try {
                result = ZonedDateTime.parse(
                        datetime, fmt.withZone(zone));
            } catch (DateTimeParseException ignored) {
                // try next parser
            }
        }
        return result;
    }

    private ZonedDateTime tryLocalDate(
            final String datetime, final ZoneId zone) {

        ZonedDateTime result = null;
        try {
            final LocalDate date = LocalDate.parse(datetime);
            result = date.atStartOfDay(zone);
        } catch (DateTimeParseException ignored) {
            // not a local date
        }
        return result;
    }

    private ZonedDateTime parseWithFormat(
            final String datetime,
            final String inputFormat,
            final ZoneId zone) {

        final String fmt = inputFormat.toLowerCase(Locale.ROOT);
        return switch (fmt) {
            case "iso" -> parseDateTime(datetime, zone);
            case "epoch" -> Instant.ofEpochSecond(
                    Long.parseLong(datetime)).atZone(zone);
            case "epochmillis" -> Instant.ofEpochMilli(
                    Long.parseLong(datetime)).atZone(zone);
            default -> parseCustomFormat(
                    datetime, inputFormat, zone);
        };
    }

    private ZonedDateTime parseCustomFormat(
            final String datetime,
            final String pattern,
            final ZoneId zone) {

        final DateTimeFormatter custom =
                DateTimeFormatter.ofPattern(pattern);
        ZonedDateTime result;
        try {
            result = ZonedDateTime.parse(
                    datetime, custom.withZone(zone));
        } catch (DateTimeParseException ex) {
            final LocalDateTime local =
                    LocalDateTime.parse(datetime, custom);
            result = local.atZone(zone);
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Output formatting
    // ------------------------------------------------------------------ //

    private String formatOutput(
            final ZonedDateTime dateTime, final String format) {

        final String fmt = format.toLowerCase(Locale.ROOT);
        return switch (fmt) {
            case "iso" -> dateTime.format(
                    DateTimeFormatter.ISO_ZONED_DATE_TIME);
            case "iso-offset" -> dateTime.format(
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            case "iso-local" -> dateTime.toLocalDateTime().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "epoch" -> String.valueOf(
                    dateTime.toEpochSecond());
            case "epochmillis" -> String.valueOf(
                    dateTime.toInstant().toEpochMilli());
            case "rfc1123" -> dateTime.format(
                    DateTimeFormatter.RFC_1123_DATE_TIME);
            default -> dateTime.format(
                    DateTimeFormatter.ofPattern(format));
        };
    }

    // ------------------------------------------------------------------ //
    //  Difference calculation
    // ------------------------------------------------------------------ //

    private String computeDifference(
            final ZonedDateTime first,
            final ZonedDateTime second) {

        final ZonedDateTime earlier;
        final ZonedDateTime later;
        if (first.isBefore(second)) {
            earlier = first;
            later = second;
        } else {
            earlier = second;
            later = first;
        }

        final Period period = Period.between(
                earlier.toLocalDate(), later.toLocalDate());
        final Duration duration = Duration.between(earlier, later);
        final long totalSec = duration.getSeconds();
        final long hours = totalSec % SEC_PER_DAY / SEC_PER_HOUR;
        final long minutes = totalSec % SEC_PER_HOUR / SEC_PER_MIN;
        final long seconds = totalSec % SEC_PER_MIN;

        return "{\"years\":%d,\"months\":%d,\"days\":%d,\"hours\":%d,\"minutes\":%d,\"seconds\":%d,\"totalSeconds\":%d}"
                .formatted(period.getYears(), period.getMonths(),
                        period.getDays(), hours, minutes,
                        seconds, totalSec);
    }
}

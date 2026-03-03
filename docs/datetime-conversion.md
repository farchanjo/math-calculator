# DateTime Conversion Reference

## Overview

The `DateTimeConverterTool` provides timezone conversion, format transformation, and time difference calculation using the `java.time` API with IANA timezone identifiers.

## MCP Methods

| Method | Description |
|--------|-------------|
| `convertTimezone` | Convert datetime between IANA timezones |
| `formatDateTime` | Reformat datetime strings between formats |
| `currentDateTime` | Get current datetime in a timezone |
| `listTimezones` | List timezone IDs by region |
| `dateTimeDifference` | Calculate difference between two datetimes |

## Supported Input Formats

The tool auto-detects common formats:

| Format | Example |
|--------|---------|
| ISO zoned | `2026-03-03T10:00:00+05:30[Asia/Kolkata]` |
| ISO offset | `2026-03-03T10:00:00+05:30` |
| ISO local | `2026-03-03T10:00:00` |
| Standard | `2026-03-03 10:00:00` |
| Date only | `2026-03-03` |

## Format Keywords

| Keyword | Output |
|---------|--------|
| `iso` | ISO-8601 with timezone (default) |
| `iso-offset` | ISO-8601 with offset only |
| `iso-local` | ISO-8601 without timezone |
| `epoch` | Unix epoch seconds |
| `epochMillis` | Unix epoch milliseconds |
| `rfc1123` | RFC-1123 format |
| Custom pattern | Java `DateTimeFormatter` pattern |

## IANA Timezone Regions

| Region | Example IDs |
|--------|-------------|
| America | `America/New_York`, `America/Sao_Paulo`, `America/Chicago` |
| Europe | `Europe/London`, `Europe/Paris`, `Europe/Berlin` |
| Asia | `Asia/Tokyo`, `Asia/Kolkata`, `Asia/Shanghai` |
| Pacific | `Pacific/Auckland`, `Pacific/Honolulu` |
| Australia | `Australia/Sydney`, `Australia/Melbourne` |
| Africa | `Africa/Cairo`, `Africa/Nairobi` |
| Atlantic | `Atlantic/Reykjavik` |
| Indian | `Indian/Maldives` |
| Antarctica | `Antarctica/McMurdo` |
| UTC | `UTC` |

## Examples

### Timezone Conversion

```
convertTimezone("2026-03-03T12:00:00", "UTC", "America/Sao_Paulo")
-> "2026-03-03T09:00:00-03:00[America/Sao_Paulo]"

convertTimezone("2026-03-03T12:00:00", "UTC", "Asia/Tokyo")
-> "2026-03-03T21:00:00+09:00[Asia/Tokyo]"
```

### Format Conversion

```
formatDateTime("1709424000", "epoch", "iso", "UTC")
-> "2024-03-03T00:00:00Z[UTC]"

formatDateTime("2026-03-03T10:30:00", "iso", "yyyy-MM-dd HH:mm:ss", "UTC")
-> "2026-03-03 10:30:00"
```

### Current DateTime

```
currentDateTime("America/Sao_Paulo", "iso")
-> "2026-03-03T09:30:00-03:00[America/Sao_Paulo]"

currentDateTime("UTC", "epoch")
-> "1772629800"
```

### Time Difference

```
dateTimeDifference("2026-01-01T00:00:00", "2026-03-03T15:30:00", "UTC")
-> {"years":0,"months":2,"days":2,"hours":15,"minutes":30,"seconds":0,"totalSeconds":5305800}
```

## Edge Cases

- **DST transitions**: Handled by `java.time.ZoneId` rules automatically
- **Ambiguous times**: During fall-back, the earlier offset is used (standard `java.time` behavior)
- **Leap seconds**: Java uses UTC-SLS (no leap seconds)
- **Invalid timezone**: Returns `"Error: Unknown timezone: ..."`
- **Invalid datetime**: Returns `"Error: Cannot parse datetime: ..."`

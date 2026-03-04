package com.archanjo.mathcalculator.tool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.archanjo.mathcalculator.engine.UnitRegistry;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
@Component
public class NetworkCalculatorTool {

    private static final int IPV4_BITS = 32;
    private static final int IPV6_BITS = 128;
    private static final int OCTET_COUNT = 4;
    private static final int IPV6_GROUP_COUNT = 8;
    private static final int OCTET_BITS = 8;
    private static final int IPV6_GROUP_BITS = 16;
    private static final int HEX_GROUP_LEN = 4;
    private static final int HEX_RADIX = 16;
    private static final long IPV4_MAX = 0xFFFFFFFFL;
    private static final int OCTET_MAX = 255;
    private static final int CLASS_A_MAX = 127;
    private static final int CLASS_B_MAX = 191;
    private static final int CLASS_C_MAX = 223;
    private static final int CLASS_D_MAX = 239;
    private static final int CIDR_31 = 31;
    private static final int BITS_PER_BYTE = 8;
    private static final int HOST_BITS_ONE = 1;
    private static final int MIN_COMPRESS_LEN = 2;
    private static final int IP_VERSION_6 = 6;
    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final int SCALE = 20;
    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal SECONDS_PER_HOUR = new BigDecimal("3600");
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal KILO_BITS = new BigDecimal("8192");
    private static final String ERROR_PREFIX = "Error: ";
    private static final String TRUE_STR = "true";
    private static final String FALSE_STR = "false";
    private static final String ZERO_GROUP = "0000";

    @Tool(description = "Calculate subnet details from an IP address and CIDR prefix length."
            + " Returns JSON with network, broadcast, mask, wildcard, firstHost, lastHost,"
            + " usableHosts, and ipClass. Supports both IPv4 and IPv6.")
    public String subnetCalculator(
            @ToolParam(description = "IP address (IPv4 dotted or IPv6 colon notation)")
            final String address,
            @ToolParam(description = "CIDR prefix length (0-32 for IPv4, 0-128 for IPv6)")
            final int cidr) {

        String result;
        try {
            if (isIpv6(address)) {
                result = subnetV6(address, cidr);
            } else {
                result = subnetV4(address, cidr);
            }
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Convert an IP address to binary representation."
            + " IPv4 returns dotted 8-bit groups, IPv6 returns colon-separated 16-bit groups.")
    public String ipToBinary(
            @ToolParam(description = "IP address (IPv4 or IPv6)")
            final String address) {

        String result;
        try {
            if (isIpv6(address)) {
                result = ipv6ToBinary(address);
            } else {
                result = ipv4ToBinary(address);
            }
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Convert a binary IP representation back to decimal notation."
            + " Auto-detects IPv4 (dot-separated) vs IPv6 (colon-separated).")
    public String binaryToIp(
            @ToolParam(description = "Binary IP string (e.g. '11000000.10101000.00000001.00000001'"
                    + " for IPv4 or 8 colon-separated 16-bit groups for IPv6)")
            final String binary) {

        String result;
        try {
            if (binary.contains(":")) {
                result = binaryToIpv6(binary);
            } else {
                result = binaryToIpv4(binary);
            }
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Convert an IP address to its unsigned decimal integer representation.")
    public String ipToDecimal(
            @ToolParam(description = "IP address (IPv4 or IPv6)")
            final String address) {

        String result;
        try {
            if (isIpv6(address)) {
                result = parseIpv6(address).toString();
            } else {
                result = Long.toString(parseIpv4(address));
            }
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Convert an unsigned decimal integer to an IP address string.")
    public String decimalToIp(
            @ToolParam(description = "Unsigned decimal integer string")
            final String decimal,
            @ToolParam(description = "IP version: 4 or 6")
            final int version) {

        String result;
        try {
            result = convertDecimalToIp(decimal, version);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Check whether an IP address falls within a given subnet.")
    public String ipInSubnet(
            @ToolParam(description = "IP address to check (IPv4 or IPv6)")
            final String address,
            @ToolParam(description = "Network address of the subnet")
            final String network,
            @ToolParam(description = "CIDR prefix length")
            final int cidr) {

        String result;
        try {
            if (isIpv6(address)) {
                result = checkIpv6InSubnet(address, network, cidr);
            } else {
                result = checkIpv4InSubnet(address, network, cidr);
            }
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Perform Variable Length Subnet Masking (VLSM) allocation."
            + " Divides a network into subnets based on required host counts.")
    public String vlsmSubnets(
            @ToolParam(description = "Base network in CIDR notation (e.g. '192.168.1.0/24')")
            final String networkCidr,
            @ToolParam(description = "JSON array of required host counts (e.g. '[50,25,10]')")
            final String hostCounts) {

        String result;
        try {
            result = computeVlsm(networkCidr, hostCounts);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Summarize (supernet) a list of subnets into a single CIDR block.")
    public String summarizeSubnets(
            @ToolParam(description = "JSON array of CIDR strings"
                    + " (e.g. '[\"192.168.1.0/24\",\"192.168.2.0/24\"]')")
            final String subnets) {

        String result;
        try {
            result = computeSummary(subnets);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Expand a compressed IPv6 address to its full 8-group form.")
    public String expandIpv6(
            @ToolParam(description = "IPv6 address (e.g. '::1' or '2001:db8::1')")
            final String address) {

        String result;
        try {
            result = bigIntToIpv6Full(parseIpv6(address));
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Compress an IPv6 address to its shortest canonical form using '::'.")
    public String compressIpv6(
            @ToolParam(description = "Full IPv6 address")
            final String address) {

        String result;
        try {
            final String full = bigIntToIpv6Full(parseIpv6(address));
            result = compressIpv6Groups(full);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Estimate file transfer time given file size and bandwidth."
            + " Returns JSON with seconds, minutes, and hours.")
    public String transferTime(
            @ToolParam(description = "File size value")
            final String fileSize,
            @ToolParam(description = "File size unit (e.g. 'mb', 'gb')")
            final String fileSizeUnit,
            @ToolParam(description = "Bandwidth value")
            final String bandwidth,
            @ToolParam(description = "Bandwidth unit (e.g. 'mbps', 'gbps')")
            final String bandwidthUnit) {

        String result;
        try {
            result = computeTransferTime(
                    fileSize, fileSizeUnit, bandwidth, bandwidthUnit);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Calculate data throughput given data size and time.")
    public String throughput(
            @ToolParam(description = "Data size value")
            final String dataSize,
            @ToolParam(description = "Data size unit (e.g. 'mb', 'gb')")
            final String dataSizeUnit,
            @ToolParam(description = "Time value")
            final String time,
            @ToolParam(description = "Time unit (e.g. 's', 'min')")
            final String timeUnit,
            @ToolParam(description = "Output throughput unit (e.g. 'mbps', 'gbps')")
            final String outputUnit) {

        String result;
        try {
            result = computeThroughput(
                    dataSize, dataSizeUnit, time, timeUnit, outputUnit);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    @Tool(description = "Calculate effective TCP throughput using the Bandwidth-Delay Product."
            + " Returns effective throughput in Mbps.")
    public String tcpThroughput(
            @ToolParam(description = "Link bandwidth in Mbps")
            final String bandwidthMbps,
            @ToolParam(description = "Round-trip time in milliseconds")
            final String rttMs,
            @ToolParam(description = "TCP window size in kilobytes")
            final String windowSizeKb) {

        String result;
        try {
            result = computeTcpThroughput(
                    bandwidthMbps, rttMs, windowSizeKb);
        } catch (IllegalArgumentException ex) {
            result = ERROR_PREFIX + ex.getMessage();
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Decimal-to-IP dispatch
    // ------------------------------------------------------------------ //

    private String convertDecimalToIp(final String decimal,
                                      final int version) {
        final String result;
        if (version == IP_VERSION_6) {
            result = bigIntToIpv6Full(new BigInteger(decimal));
        } else if (version == OCTET_COUNT) {
            result = longToIpv4(Long.parseLong(decimal));
        } else {
            throw new IllegalArgumentException(
                    "Version must be 4 or 6");
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  IPv4 helpers
    // ------------------------------------------------------------------ //

    private long parseIpv4(final String address) {
        final String[] parts = address.split("\\.");
        if (parts.length != OCTET_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 address: " + address);
        }
        long value = 0;
        for (final String part : parts) {
            final int octet = Integer.parseInt(part);
            if (octet < 0 || octet > OCTET_MAX) {
                throw new IllegalArgumentException(
                        "Octet out of range: " + octet);
            }
            value = (value << OCTET_BITS) | octet;
        }
        return value;
    }

    private String longToIpv4(final long value) {
        if (value < 0 || value > IPV4_MAX) {
            throw new IllegalArgumentException(
                    "Value out of IPv4 range: " + value);
        }
        return "%d.%d.%d.%d".formatted(
                (value >> 24) & OCTET_MAX,
                (value >> 16) & OCTET_MAX,
                (value >> 8) & OCTET_MAX,
                value & OCTET_MAX);
    }

    private long cidrToMaskV4(final int cidr) {
        final long mask;
        if (cidr == 0) {
            mask = 0L;
        } else {
            mask = IPV4_MAX << (IPV4_BITS - cidr) & IPV4_MAX;
        }
        return mask;
    }

    private String ipClass(final long ipValue) {
        final int firstOctet = (int) ((ipValue >> 24) & OCTET_MAX);
        final String cls;
        if (firstOctet <= CLASS_A_MAX) {
            cls = "A";
        } else if (firstOctet <= CLASS_B_MAX) {
            cls = "B";
        } else if (firstOctet <= CLASS_C_MAX) {
            cls = "C";
        } else if (firstOctet <= CLASS_D_MAX) {
            cls = "D";
        } else {
            cls = "E";
        }
        return cls;
    }

    // ------------------------------------------------------------------ //
    //  IPv6 helpers
    // ------------------------------------------------------------------ //

    private BigInteger parseIpv6(final String address) {
        final String expanded = expandIpv6Str(address);
        final String[] groups = expanded.split(":");
        if (groups.length != IPV6_GROUP_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid IPv6 address: " + address);
        }
        BigInteger value = BigInteger.ZERO;
        for (final String group : groups) {
            final int parsed = Integer.parseInt(group, HEX_RADIX);
            value = value.shiftLeft(IPV6_GROUP_BITS)
                    .or(BigInteger.valueOf(parsed));
        }
        return value;
    }

    private String expandIpv6Str(final String address) {
        final String resolved;
        if (address.contains("::")) {
            resolved = expandDoubleColon(address);
        } else {
            resolved = address;
        }
        final String[] groups = resolved.split(":");
        final StringBuilder normalized = new StringBuilder();
        for (int idx = 0; idx < groups.length; idx++) {
            if (idx > 0) {
                normalized.append(':');
            }
            normalized.append(padHex(groups[idx]));
        }
        return normalized.toString();
    }

    private String expandDoubleColon(final String address) {
        final String[] halves = address.split("::", -1);
        final String[] left = halves[0].isEmpty()
                ? new String[0] : halves[0].split(":");
        final String[] right = halves[1].isEmpty()
                ? new String[0] : halves[1].split(":");
        final int missing =
                IPV6_GROUP_COUNT - left.length - right.length;
        final StringBuilder builder = new StringBuilder();
        appendIpv6Groups(builder, left);
        for (int idx = 0; idx < missing; idx++) {
            if (!builder.isEmpty()) {
                builder.append(':');
            }
            builder.append(ZERO_GROUP);
        }
        appendIpv6Groups(builder, right);
        return builder.toString();
    }

    @SuppressWarnings("PMD.UseVarargs")
    private void appendIpv6Groups(final StringBuilder builder,
                                  final String[] groups) {
        for (final String group : groups) {
            if (!builder.isEmpty()) {
                builder.append(':');
            }
            builder.append(padHex(group));
        }
    }

    private String padHex(final String hex) {
        final String padded;
        if (hex.length() >= HEX_GROUP_LEN) {
            padded = hex;
        } else {
            padded = "0".repeat(HEX_GROUP_LEN - hex.length()) + hex;
        }
        return padded;
    }

    private String bigIntToIpv6Full(final BigInteger value) {
        final String hex = value.toString(HEX_RADIX);
        final String padded =
                "0".repeat(IPV4_BITS - hex.length()) + hex;
        final StringBuilder builder = new StringBuilder();
        for (int idx = 0; idx < IPV6_GROUP_COUNT; idx++) {
            if (idx > 0) {
                builder.append(':');
            }
            final int start = idx * HEX_GROUP_LEN;
            builder.append(
                    padded.substring(start, start + HEX_GROUP_LEN));
        }
        return builder.toString();
    }

    private BigInteger cidrToMaskV6(final int cidr) {
        final BigInteger allOnes = BigInteger.ONE
                .shiftLeft(IPV6_BITS).subtract(BigInteger.ONE);
        final BigInteger mask;
        if (cidr == 0) {
            mask = BigInteger.ZERO;
        } else {
            mask = allOnes.shiftRight(cidr).not().and(allOnes);
        }
        return mask;
    }

    private boolean isIpv6(final String address) {
        return address.contains(":");
    }

    // ------------------------------------------------------------------ //
    //  Binary helpers
    // ------------------------------------------------------------------ //

    private String toBinary8(final int octet) {
        final String bin = Integer.toBinaryString(octet);
        return "0".repeat(OCTET_BITS - bin.length()) + bin;
    }

    private String toBinary16(final int group) {
        final String bin = Integer.toBinaryString(group);
        return "0".repeat(IPV6_GROUP_BITS - bin.length()) + bin;
    }

    // ------------------------------------------------------------------ //
    //  Validation
    // ------------------------------------------------------------------ //

    private void validateCidr(final int cidr, final boolean ipv6) {
        final int max = ipv6 ? IPV6_BITS : IPV4_BITS;
        if (cidr < 0 || cidr > max) {
            throw new IllegalArgumentException(
                    "CIDR must be 0-%d, got %d".formatted(max, cidr));
        }
    }

    private String strip(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // ------------------------------------------------------------------ //
    //  IP-in-subnet checks
    // ------------------------------------------------------------------ //

    private String checkIpv6InSubnet(final String address,
                                     final String network,
                                     final int cidr) {
        validateCidr(cidr, true);
        final BigInteger ipVal = parseIpv6(address);
        final BigInteger netVal = parseIpv6(network);
        final BigInteger mask = cidrToMaskV6(cidr);
        return ipVal.and(mask).equals(netVal.and(mask))
                ? TRUE_STR : FALSE_STR;
    }

    private String checkIpv4InSubnet(final String address,
                                     final String network,
                                     final int cidr) {
        validateCidr(cidr, false);
        final long ipVal = parseIpv4(address);
        final long netVal = parseIpv4(network);
        final long mask = cidrToMaskV4(cidr);
        return (ipVal & mask) == (netVal & mask)
                ? TRUE_STR : FALSE_STR;
    }

    // ------------------------------------------------------------------ //
    //  Subnet calculation (v4)
    // ------------------------------------------------------------------ //

    private String subnetV4(final String address, final int cidr) {
        validateCidr(cidr, false);
        final long ipVal = parseIpv4(address);
        final long mask = cidrToMaskV4(cidr);
        final long network = ipVal & mask;
        final long wildcard = ~mask & IPV4_MAX;
        final long broadcast = network | wildcard;
        return buildSubnetV4Json(
                network, broadcast, mask, wildcard, cidr, ipVal);
    }

    private String buildSubnetV4Json(
            final long network, final long broadcast,
            final long mask, final long wildcard,
            final int cidr, final long ipValue) {

        final long firstHost;
        final long lastHost;
        final long usableHosts;

        if (cidr == IPV4_BITS) {
            firstHost = network;
            lastHost = network;
            usableHosts = 0;
        } else if (cidr == CIDR_31) {
            firstHost = network;
            lastHost = broadcast;
            usableHosts = 2;
        } else {
            firstHost = network + 1;
            lastHost = broadcast - 1;
            usableHosts = broadcast - network - 1;
        }

        return "{\"network\":\"%s\",\"broadcast\":\"%s\","
                .formatted(longToIpv4(network), longToIpv4(broadcast))
                + "\"mask\":\"%s\",\"wildcard\":\"%s\","
                .formatted(longToIpv4(mask), longToIpv4(wildcard))
                + "\"firstHost\":\"%s\",\"lastHost\":\"%s\","
                .formatted(longToIpv4(firstHost), longToIpv4(lastHost))
                + "\"usableHosts\":%d,\"ipClass\":\"%s\"}"
                .formatted(usableHosts, ipClass(ipValue));
    }

    // ------------------------------------------------------------------ //
    //  Subnet calculation (v6)
    // ------------------------------------------------------------------ //

    private String subnetV6(final String address, final int cidr) {
        validateCidr(cidr, true);
        final BigInteger ipVal = parseIpv6(address);
        final BigInteger mask = cidrToMaskV6(cidr);
        final BigInteger network = ipVal.and(mask);
        final int hostBits = IPV6_BITS - cidr;
        return buildSubnetV6Json(network, mask, hostBits);
    }

    private String buildSubnetV6Json(
            final BigInteger network, final BigInteger mask,
            final int hostBits) {

        final BigInteger hostRange;
        if (hostBits == 0) {
            hostRange = BigInteger.ZERO;
        } else {
            hostRange = BigInteger.ONE.shiftLeft(hostBits)
                    .subtract(BigInteger.ONE);
        }
        final BigInteger last = network.or(hostRange);
        final BigInteger firstHost;
        final BigInteger lastHost;
        final BigInteger usableHosts;

        if (hostBits == 0) {
            firstHost = network;
            lastHost = network;
            usableHosts = BigInteger.ZERO;
        } else if (hostBits == HOST_BITS_ONE) {
            firstHost = network;
            lastHost = last;
            usableHosts = BigInteger.TWO;
        } else {
            firstHost = network.add(BigInteger.ONE);
            lastHost = last.subtract(BigInteger.ONE);
            usableHosts = hostRange.subtract(BigInteger.ONE);
        }

        return ("{\"network\":\"%s\",\"mask\":\"%s\","
                + "\"firstHost\":\"%s\",\"lastHost\":\"%s\","
                + "\"usableHosts\":%s}")
                .formatted(
                        bigIntToIpv6Full(network),
                        bigIntToIpv6Full(mask),
                        bigIntToIpv6Full(firstHost),
                        bigIntToIpv6Full(lastHost),
                        usableHosts.toString());
    }

    // ------------------------------------------------------------------ //
    //  Binary conversion methods
    // ------------------------------------------------------------------ //

    private String ipv4ToBinary(final String address) {
        final long value = parseIpv4(address);
        return "%s.%s.%s.%s".formatted(
                toBinary8((int) ((value >> 24) & OCTET_MAX)),
                toBinary8((int) ((value >> 16) & OCTET_MAX)),
                toBinary8((int) ((value >> 8) & OCTET_MAX)),
                toBinary8((int) (value & OCTET_MAX)));
    }

    private String ipv6ToBinary(final String address) {
        final String full = bigIntToIpv6Full(parseIpv6(address));
        final String[] groups = full.split(":");
        final StringBuilder builder = new StringBuilder();
        for (int idx = 0; idx < groups.length; idx++) {
            if (idx > 0) {
                builder.append(':');
            }
            builder.append(toBinary16(
                    Integer.parseInt(groups[idx], HEX_RADIX)));
        }
        return builder.toString();
    }

    private String binaryToIpv4(final String binary) {
        final String[] parts = binary.split("\\.");
        if (parts.length != OCTET_COUNT) {
            throw new IllegalArgumentException(
                    "Expected 4 dot-separated 8-bit groups");
        }
        long value = 0;
        for (final String part : parts) {
            value = (value << OCTET_BITS)
                    | Integer.parseInt(part, 2);
        }
        return longToIpv4(value);
    }

    private String binaryToIpv6(final String binary) {
        final String[] parts = binary.split(":");
        if (parts.length != IPV6_GROUP_COUNT) {
            throw new IllegalArgumentException(
                    "Expected 8 colon-separated 16-bit groups");
        }
        BigInteger value = BigInteger.ZERO;
        for (final String part : parts) {
            value = value.shiftLeft(IPV6_GROUP_BITS)
                    .or(BigInteger.valueOf(
                            Integer.parseInt(part, 2)));
        }
        return bigIntToIpv6Full(value);
    }

    // ------------------------------------------------------------------ //
    //  IPv6 compress
    // ------------------------------------------------------------------ //

    private String compressIpv6Groups(final String full) {
        final String[] groups = full.split(":");
        int bestStart = -1;
        int bestLen = 0;
        int curStart = -1;
        int curLen = 0;

        for (int idx = 0; idx < groups.length; idx++) {
            if (ZERO_GROUP.equals(groups[idx])) {
                if (curStart < 0) {
                    curStart = idx;
                    curLen = 1;
                } else {
                    curLen++;
                }
            } else {
                if (curLen > bestLen) {
                    bestStart = curStart;
                    bestLen = curLen;
                }
                curStart = -1;
                curLen = 0;
            }
        }
        if (curLen > bestLen) {
            bestStart = curStart;
            bestLen = curLen;
        }
        return buildCompressed(groups, bestStart, bestLen);
    }

    private String buildCompressed(final String[] groups,
                                   final int bestStart,
                                   final int bestLen) {
        final String result;
        if (bestLen < MIN_COMPRESS_LEN) {
            result = joinTrimmed(groups, 0, groups.length);
        } else {
            final String left =
                    joinTrimmed(groups, 0, bestStart);
            final String right = joinTrimmed(
                    groups, bestStart + bestLen, groups.length);
            result = left + "::" + right;
        }
        return result;
    }

    private String joinTrimmed(final String[] groups,
                               final int from, final int end) {
        final StringBuilder builder = new StringBuilder();
        for (int idx = from; idx < end; idx++) {
            if (!builder.isEmpty()) {
                builder.append(':');
            }
            builder.append(groups[idx]
                    .replaceFirst("^0+(?!$)", ""));
        }
        return builder.toString();
    }

    // ------------------------------------------------------------------ //
    //  VLSM
    // ------------------------------------------------------------------ //

    private String computeVlsm(final String networkCidr,
                                final String hostCounts) {
        final String[] cidrParts = networkCidr.split("/");
        final long baseNetwork = parseIpv4(cidrParts[0]);
        final int baseCidr = Integer.parseInt(cidrParts[1]);
        final long baseMask = cidrToMaskV4(baseCidr);
        final long baseEnd = baseNetwork | (~baseMask & IPV4_MAX);

        final List<Integer> counts = parseIntArray(hostCounts);
        counts.sort((first, second) -> Integer.compare(second, first));

        long pointer = baseNetwork;
        final StringBuilder builder = new StringBuilder("[");

        for (int idx = 0; idx < counts.size(); idx++) {
            final int needed = counts.get(idx);
            final int hostBits = ceilLog2(needed + 2);
            final int subnetCidr = IPV4_BITS - hostBits;
            validateVlsmFit(needed, subnetCidr, baseCidr);

            final long subMask = cidrToMaskV4(subnetCidr);
            final long subBroadcast =
                    pointer | (~subMask & IPV4_MAX);
            if (subBroadcast > baseEnd) {
                throw new IllegalArgumentException(
                        "Address space exhausted");
            }

            if (idx > 0) {
                builder.append(',');
            }
            appendVlsmEntry(builder, pointer, subnetCidr,
                    subBroadcast);
            pointer = subBroadcast + 1;
        }

        builder.append(']');
        return builder.toString();
    }

    private void validateVlsmFit(final int needed,
                                  final int subnetCidr,
                                  final int baseCidr) {
        if (subnetCidr < baseCidr) {
            throw new IllegalArgumentException(
                    "Cannot fit %d hosts in /%d"
                            .formatted(needed, baseCidr));
        }
    }

    private void appendVlsmEntry(final StringBuilder builder,
                                  final long network,
                                  final int cidr,
                                  final long broadcast) {
        final long usable = broadcast - network - 1;
        builder.append("{\"network\":\"")
                .append(longToIpv4(network))
                .append("\",\"cidr\":").append(cidr)
                .append(",\"firstHost\":\"")
                .append(longToIpv4(network + 1))
                .append("\",\"lastHost\":\"")
                .append(longToIpv4(broadcast - 1))
                .append("\",\"usableHosts\":")
                .append(usable).append('}');
    }

    private int ceilLog2(final int value) {
        int bits = 0;
        int remaining = value - 1;
        while (remaining > 0) {
            remaining >>= 1;
            bits++;
        }
        return bits;
    }

    // ------------------------------------------------------------------ //
    //  Summarize subnets
    // ------------------------------------------------------------------ //

    private String computeSummary(final String subnets) {
        final List<String> cidrList = parseStringArray(subnets);
        if (cidrList.isEmpty()) {
            throw new IllegalArgumentException("Empty subnet list");
        }
        long minNetwork = IPV4_MAX;
        long maxBroadcast = 0;
        for (final String cidr : cidrList) {
            final String[] parts = cidr.split("/");
            final long network = parseIpv4(parts[0]);
            final int prefix = Integer.parseInt(parts[1]);
            final long mask = cidrToMaskV4(prefix);
            final long broadcast = network | (~mask & IPV4_MAX);
            if (network < minNetwork) {
                minNetwork = network;
            }
            if (broadcast > maxBroadcast) {
                maxBroadcast = broadcast;
            }
        }
        final long range = maxBroadcast - minNetwork + 1;
        final int superBits = ceilLog2((int) range);
        final int superCidr = IPV4_BITS - superBits;
        final long superMask = cidrToMaskV4(superCidr);
        final long superNetwork = minNetwork & superMask;
        return "%s/%d".formatted(longToIpv4(superNetwork), superCidr);
    }

    // ------------------------------------------------------------------ //
    //  Transfer time / throughput
    // ------------------------------------------------------------------ //

    private String computeTransferTime(
            final String fileSize, final String fileSizeUnit,
            final String bandwidth, final String bandwidthUnit) {

        final BigDecimal sizeBytes = UnitRegistry.convert(
                new BigDecimal(fileSize),
                fileSizeUnit.toLowerCase(Locale.ROOT), "byte");
        final BigDecimal sizeBits = sizeBytes.multiply(
                new BigDecimal(BITS_PER_BYTE), PRECISION);
        final BigDecimal bps = UnitRegistry.convert(
                new BigDecimal(bandwidth),
                bandwidthUnit.toLowerCase(Locale.ROOT), "bps");

        final BigDecimal seconds = sizeBits.divide(
                bps, SCALE, RoundingMode.HALF_UP);
        final BigDecimal minutes = seconds.divide(
                SIXTY, SCALE, RoundingMode.HALF_UP);
        final BigDecimal hours = seconds.divide(
                SECONDS_PER_HOUR, SCALE, RoundingMode.HALF_UP);

        return "{\"seconds\":\"%s\",\"minutes\":\"%s\",\"hours\":\"%s\"}"
                .formatted(strip(seconds), strip(minutes), strip(hours));
    }

    private String computeThroughput(
            final String dataSize, final String dataSizeUnit,
            final String time, final String timeUnit,
            final String outputUnit) {

        final BigDecimal sizeBytes = UnitRegistry.convert(
                new BigDecimal(dataSize),
                dataSizeUnit.toLowerCase(Locale.ROOT), "byte");
        final BigDecimal sizeBits = sizeBytes.multiply(
                new BigDecimal(BITS_PER_BYTE), PRECISION);
        final BigDecimal seconds = UnitRegistry.convert(
                new BigDecimal(time),
                timeUnit.toLowerCase(Locale.ROOT), "s");
        final BigDecimal bps = sizeBits.divide(
                seconds, SCALE, RoundingMode.HALF_UP);
        final BigDecimal result = UnitRegistry.convert(
                bps, "bps",
                outputUnit.toLowerCase(Locale.ROOT));
        return strip(result);
    }

    private String computeTcpThroughput(
            final String bandwidthMbps, final String rttMs,
            final String windowSizeKb) {

        final BigDecimal bwBps = new BigDecimal(bandwidthMbps)
                .multiply(MILLION, PRECISION);
        final BigDecimal rttSec = new BigDecimal(rttMs)
                .divide(THOUSAND, SCALE, RoundingMode.HALF_UP);
        final BigDecimal windowBits = new BigDecimal(windowSizeKb)
                .multiply(KILO_BITS, PRECISION);
        final BigDecimal maxByWindow = windowBits.divide(
                rttSec, SCALE, RoundingMode.HALF_UP);
        final BigDecimal effective = bwBps.min(maxByWindow);
        final BigDecimal effectiveMbps = effective.divide(
                MILLION, SCALE, RoundingMode.HALF_UP);
        return strip(effectiveMbps);
    }

    // ------------------------------------------------------------------ //
    //  JSON array parsing helpers
    // ------------------------------------------------------------------ //

    private List<Integer> parseIntArray(final String json) {
        final String trimmed = json.trim();
        final String inner =
                trimmed.substring(1, trimmed.length() - 1).trim();
        final List<Integer> result = new ArrayList<>();
        if (!inner.isEmpty()) {
            for (final String element : inner.split(",")) {
                result.add(Integer.parseInt(element.trim()));
            }
        }
        return result;
    }

    private List<String> parseStringArray(final String json) {
        final String trimmed = json.trim();
        final String inner =
                trimmed.substring(1, trimmed.length() - 1).trim();
        final List<String> result = new ArrayList<>();
        if (!inner.isEmpty()) {
            for (final String element : inner.split(",")) {
                result.add(element.trim()
                        .replace("\"", ""));
            }
        }
        return result;
    }
}

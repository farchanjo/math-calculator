package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class NetworkCalculatorToolTest {

    private static final String ERROR_PREFIX = "Error:";
    private static final String ADDR_NET = "192.168.1.0";
    private static final String ADDR_HOST = "192.168.1.1";
    private static final String ADDR_BCAST = "192.168.1.255";
    private static final String ADDR_OTHER = "192.168.2.0";
    private static final String ADDR_ALL_ONES = "255.255.255.255";
    private static final String ADDR_ALL_ZERO = "0.0.0.0";
    private static final String V6_LOOPBACK = "::1";
    private static final String V6_LOOP_FULL = "0000:0000:0000:0000:0000:0000:0000:0001";
    private static final String V6_2001_FULL = "2001:0db8:0000:0000:0000:0000:0000:0001";
    private static final String V6_ALL_ZERO = "0000:0000:0000:0000:0000:0000:0000:0000";
    private static final String V6_2001_SHORT = "2001:db8::1";
    private static final String V6_2001_NET = "2001:db8::";
    private static final String V6_NO_COMPRESS = "0001:0002:0003:0004:0005:0006:0007:0008";
    private static final String NET_CIDR_24 = "192.168.1.0/24";
    private static final String CIDR_24_STR = "/24";
    private static final String TRUE_STR = "true";
    private static final String FALSE_STR = "false";
    private static final String ADDR_CLASS_A = "10.0.0.1";
    private static final String HUNDRED = "100";
    private static final String UNIT_MBPS = "mbps";
    private static final String UNIT_GB = "gb";
    private static final String UNIT_MB = "mb";

    private final NetworkCalculatorTool tool = new NetworkCalculatorTool();

    // ------------------------------------------------------------------ //
    //  subnetCalculator
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("subnetCalculator")
    class SubnetCalcTests {

        @Test
        @DisplayName("IPv4 /24 returns 254 usable hosts")
        void ipv4Slash24UsableHosts() {
            final String result = tool.subnetCalculator(ADDR_NET, 24);
            assertTrue(result.contains("\"usableHosts\":254"),
                    "A /24 network should have 254 usable hosts");
        }

        @Test
        @DisplayName("IPv4 /24 returns correct network address")
        void ipv4Slash24NetworkAddr() {
            final String result = tool.subnetCalculator(ADDR_HOST, 24);
            assertTrue(result.contains("\"network\":\"192.168.1.0\""),
                    "Network address for 192.168.1.1/24 should be 192.168.1.0");
        }

        @Test
        @DisplayName("IPv4 /24 returns correct broadcast address")
        void ipv4Slash24BcastAddr() {
            final String result = tool.subnetCalculator(ADDR_HOST, 24);
            assertTrue(result.contains("\"broadcast\":\"192.168.1.255\""),
                    "Broadcast for 192.168.1.1/24 should be 192.168.1.255");
        }

        @Test
        @DisplayName("IPv4 /24 returns correct subnet mask")
        void ipv4Slash24Mask() {
            final String result = tool.subnetCalculator(ADDR_HOST, 24);
            assertTrue(result.contains("\"mask\":\"255.255.255.0\""),
                    "Mask for /24 should be 255.255.255.0");
        }

        @Test
        @DisplayName("IPv4 /32 returns 0 usable hosts")
        void ipv4Slash32ZeroHosts() {
            final String result = tool.subnetCalculator(ADDR_HOST, 32);
            assertTrue(result.contains("\"usableHosts\":0"),
                    "A /32 network should have 0 usable hosts");
        }

        @Test
        @DisplayName("IPv4 /31 returns 2 usable hosts")
        void ipv4Slash31TwoHosts() {
            final String result = tool.subnetCalculator(ADDR_NET, 31);
            assertTrue(result.contains("\"usableHosts\":2"),
                    "A /31 network should have 2 usable hosts (point-to-point link)");
        }

        @Test
        @DisplayName("IPv6 /64 returns JSON with network field")
        void ipv6Slash64HasNetwork() {
            final String result = tool.subnetCalculator(V6_LOOPBACK, 64);
            assertTrue(result.contains("\"network\":\""),
                    "IPv6 /64 result should contain a network field");
        }

        @Test
        @DisplayName("IPv6 /128 returns 0 usable hosts")
        void ipv6Slash128ZeroHosts() {
            final String result = tool.subnetCalculator(V6_LOOPBACK, 128);
            assertTrue(result.contains("\"usableHosts\":0"),
                    "IPv6 /128 should have 0 usable hosts");
        }

        @Test
        @DisplayName("IPv4 class A identification for 10.x.x.x")
        void ipv4ClassAForTenNet() {
            final String result = tool.subnetCalculator(ADDR_CLASS_A, 8);
            assertTrue(result.contains("\"ipClass\":\"A\""),
                    "10.0.0.1 should be class A");
        }

        @Test
        @DisplayName("IPv4 class C identification for 192.x.x.x")
        void ipv4ClassCFor192Net() {
            final String result = tool.subnetCalculator(ADDR_HOST, 24);
            assertTrue(result.contains("\"ipClass\":\"C\""),
                    "192.168.1.1 should be class C");
        }

        @Test
        @DisplayName("Invalid CIDR returns error")
        void invalidCidrError() {
            final String result = tool.subnetCalculator(ADDR_NET, 33);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "CIDR 33 is out of range for IPv4 and should return an error");
        }
    }

    // ------------------------------------------------------------------ //
    //  ipToBinary
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("ipToBinary")
    class IpToBinaryTests {

        @Test
        @DisplayName("192.168.1.1 converts to correct binary")
        void convertsStandardIpv4() {
            final String result = tool.ipToBinary(ADDR_HOST);
            assertEquals("11000000.10101000.00000001.00000001", result,
                    "192.168.1.1 should produce the correct binary representation");
        }

        @Test
        @DisplayName("0.0.0.0 converts to all zeros")
        void convertsAllZerosIpv4() {
            final String result = tool.ipToBinary(ADDR_ALL_ZERO);
            assertEquals("00000000.00000000.00000000.00000000", result,
                    "0.0.0.0 should produce all-zero binary");
        }

        @Test
        @DisplayName("255.255.255.255 converts to all ones")
        void convertsAllOnesIpv4() {
            final String result = tool.ipToBinary(ADDR_ALL_ONES);
            assertEquals("11111111.11111111.11111111.11111111", result,
                    "255.255.255.255 should produce all-one binary");
        }

        @Test
        @DisplayName("IPv6 ::1 produces colon-separated binary groups")
        void convertsIpv6ToBinary() {
            final String result = tool.ipToBinary(V6_LOOPBACK);
            assertTrue(result.contains(":"),
                    "IPv6 binary should use colon separators");
        }

        @Test
        @DisplayName("IPv6 ::1 ends with 0000000000000001")
        void ipv6BinaryEndsWithOne() {
            final String result = tool.ipToBinary(V6_LOOPBACK);
            assertTrue(result.endsWith("0000000000000001"),
                    "IPv6 ::1 binary should end with single 1 bit");
        }

        @Test
        @DisplayName("Invalid IP returns error")
        void invalidIpReturnsError() {
            final String result = tool.ipToBinary("999.999.999.999");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid IP should return an error message");
        }
    }

    // ------------------------------------------------------------------ //
    //  binaryToIp
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("binaryToIp")
    class BinaryToIpTests {

        @Test
        @DisplayName("Dotted binary converts back to 192.168.1.1")
        void convertsBinaryBack() {
            final String binary = "11000000.10101000.00000001.00000001";
            final String result = tool.binaryToIp(binary);
            assertEquals(ADDR_HOST, result,
                    "Binary representation should convert back to 192.168.1.1");
        }

        @Test
        @DisplayName("All-zero binary converts to 0.0.0.0")
        void convertsAllZeroBinary() {
            final String binary = "00000000.00000000.00000000.00000000";
            final String result = tool.binaryToIp(binary);
            assertEquals(ADDR_ALL_ZERO, result,
                    "All-zero binary should convert to 0.0.0.0");
        }

        @Test
        @DisplayName("All-one binary converts to 255.255.255.255")
        void convertsAllOneBinary() {
            final String binary = "11111111.11111111.11111111.11111111";
            final String result = tool.binaryToIp(binary);
            assertEquals(ADDR_ALL_ONES, result,
                    "All-one binary should convert to 255.255.255.255");
        }

        @Test
        @DisplayName("Round-trip IPv4 binary conversion preserves address")
        void roundTripBinaryConv() {
            final String binary = tool.ipToBinary(ADDR_HOST);
            final String result = tool.binaryToIp(binary);
            assertEquals(ADDR_HOST, result,
                    "Round-trip binary conversion should preserve the original IP");
        }

        @Test
        @DisplayName("Invalid binary format returns error")
        void invalidBinaryError() {
            final String result = tool.binaryToIp("11000000.10101000");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Incomplete binary IP should return error");
        }
    }

    // ------------------------------------------------------------------ //
    //  ipToDecimal
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("ipToDecimal")
    class IpToDecimalTests {

        @Test
        @DisplayName("192.168.1.1 converts to 3232235777")
        void convertsStandardIp() {
            final String result = tool.ipToDecimal(ADDR_HOST);
            assertEquals("3232235777", result,
                    "192.168.1.1 decimal representation should be 3232235777");
        }

        @Test
        @DisplayName("0.0.0.0 converts to 0")
        void convertsZeroIp() {
            final String result = tool.ipToDecimal(ADDR_ALL_ZERO);
            assertEquals("0", result,
                    "0.0.0.0 decimal representation should be 0");
        }

        @Test
        @DisplayName("255.255.255.255 converts to 4294967295")
        void convertsMaxIp() {
            final String result = tool.ipToDecimal(ADDR_ALL_ONES);
            assertEquals("4294967295", result,
                    "255.255.255.255 decimal representation should be 4294967295");
        }

        @Test
        @DisplayName("IPv6 ::1 converts to decimal 1")
        void convertsIpv6Loopback() {
            final String result = tool.ipToDecimal(V6_LOOPBACK);
            assertEquals("1", result,
                    "IPv6 ::1 decimal representation should be 1");
        }

        @Test
        @DisplayName("Invalid IP returns error")
        void invalidIpReturnsError() {
            final String result = tool.ipToDecimal("not.an.ip.address");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid IP should return an error message");
        }
    }

    // ------------------------------------------------------------------ //
    //  decimalToIp
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("decimalToIp")
    class DecimalToIpTests {

        @Test
        @DisplayName("3232235777 converts back to 192.168.1.1")
        void convertsDecimalToIpv4() {
            final String result = tool.decimalToIp("3232235777", 4);
            assertEquals(ADDR_HOST, result,
                    "Decimal 3232235777 should produce 192.168.1.1");
        }

        @Test
        @DisplayName("Round-trip IPv4 decimal conversion preserves address")
        void roundTripDecimalConv() {
            final String decimal = tool.ipToDecimal(ADDR_HOST);
            final String result = tool.decimalToIp(decimal, 4);
            assertEquals(ADDR_HOST, result,
                    "Round-trip decimal conversion should preserve the original IP");
        }

        @Test
        @DisplayName("Decimal 1 with version 6 returns IPv6 loopback full form")
        void convertsDecimalToV6() {
            final String result = tool.decimalToIp("1", 6);
            assertEquals(V6_LOOP_FULL, result,
                    "Decimal 1 as IPv6 should be the full loopback address");
        }

        @Test
        @DisplayName("Invalid version returns error")
        void invalidVersionError() {
            final String result = tool.decimalToIp("123", 5);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Version 5 is invalid and should return an error");
        }
    }

    // ------------------------------------------------------------------ //
    //  ipInSubnet
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("ipInSubnet")
    class IpInSubnetTests {

        @Test
        @DisplayName("192.168.1.1 is in 192.168.1.0/24")
        void ipInsideReturnsTrue() {
            final String result = tool.ipInSubnet(ADDR_HOST, ADDR_NET, 24);
            assertEquals(TRUE_STR, result,
                    "192.168.1.1 should be inside 192.168.1.0/24");
        }

        @Test
        @DisplayName("192.168.2.0 is NOT in 192.168.1.0/24")
        void ipOutsideReturnsFalse() {
            final String result = tool.ipInSubnet(ADDR_OTHER, ADDR_NET, 24);
            assertEquals(FALSE_STR, result,
                    "192.168.2.0 should not be inside 192.168.1.0/24");
        }

        @Test
        @DisplayName("Broadcast address is in subnet")
        void broadcastIsInSubnet() {
            final String result = tool.ipInSubnet(ADDR_BCAST, ADDR_NET, 24);
            assertEquals(TRUE_STR, result,
                    "192.168.1.255 (broadcast) should be inside 192.168.1.0/24");
        }

        @Test
        @DisplayName("Network address itself is in subnet")
        void networkAddrIsInSubnet() {
            final String result = tool.ipInSubnet(ADDR_NET, ADDR_NET, 24);
            assertEquals(TRUE_STR, result,
                    "Network address 192.168.1.0 should be inside 192.168.1.0/24");
        }

        @Test
        @DisplayName("IPv6 address in same /64 returns true")
        void ipv6InsideReturnsTrue() {
            final String result = tool.ipInSubnet("2001:db8::5", V6_2001_NET, 64);
            assertEquals(TRUE_STR, result,
                    "2001:db8::5 should be inside 2001:db8::/64");
        }

        @Test
        @DisplayName("IPv6 address outside /64 returns false")
        void ipv6OutsideReturnsFalse() {
            final String result = tool.ipInSubnet("2001:db9::1", V6_2001_NET, 64);
            assertEquals(FALSE_STR, result,
                    "2001:db9::1 should not be inside 2001:db8::/64");
        }

        @Test
        @DisplayName("Invalid CIDR returns error")
        void invalidCidrReturnsError() {
            final String result = tool.ipInSubnet(ADDR_HOST, ADDR_NET, -1);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Negative CIDR should return an error");
        }
    }

    // ------------------------------------------------------------------ //
    //  vlsmSubnets
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("vlsmSubnets")
    class VlsmSubnetsTests {

        @Test
        @DisplayName("Allocates subnets for [50,25,10] in 192.168.1.0/24")
        void allocatesThreeSubnets() {
            final String result = tool.vlsmSubnets(NET_CIDR_24, "[50,25,10]");
            assertTrue(result.startsWith("["),
                    "VLSM result should be a JSON array");
        }

        @Test
        @DisplayName("First VLSM subnet starts at 192.168.1.0")
        void firstSubnetAtBase() {
            final String result = tool.vlsmSubnets(NET_CIDR_24, "[50,25,10]");
            assertTrue(result.contains("\"network\":\"192.168.1.0\""),
                    "Largest subnet should start at the base network");
        }

        @Test
        @DisplayName("VLSM result contains three subnet entries")
        void threeSubnetEntries() {
            final String result = tool.vlsmSubnets(NET_CIDR_24, "[50,25,10]");
            final long entryCount = result.chars()
                    .filter(chr -> chr == '{').count();
            assertEquals(3L, entryCount,
                    "Three host-count requests should produce three subnet entries");
        }

        @Test
        @DisplayName("VLSM with too many hosts returns error")
        void tooManyHostsError() {
            final String result = tool.vlsmSubnets(NET_CIDR_24, "[300]");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Requesting 300 hosts in /24 should fail");
        }

        @Test
        @DisplayName("VLSM exhausting address space returns error")
        void exhaustedSpaceError() {
            final String result = tool.vlsmSubnets(NET_CIDR_24, "[100,100,100]");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Three subnets of 100 hosts should exhaust /24 space");
        }
    }

    // ------------------------------------------------------------------ //
    //  summarizeSubnets
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("summarizeSubnets")
    class SummarizeTests {

        @Test
        @DisplayName("Two adjacent /24 subnets summarize to /23")
        void twoAdjacentToSlash23() {
            final String input = "[\"192.168.0.0/24\",\"192.168.1.0/24\"]";
            final String result = tool.summarizeSubnets(input);
            assertEquals("192.168.0.0/23", result,
                    "Two adjacent /24 networks should summarize to a /23");
        }

        @Test
        @DisplayName("Single subnet summarizes to itself")
        void singleSubnetKeepsCidr() {
            final String input = "[\"192.168.1.0/24\"]";
            final String result = tool.summarizeSubnets(input);
            assertTrue(result.contains(CIDR_24_STR),
                    "A single /24 subnet should remain /24");
        }

        @Test
        @DisplayName("Empty list returns error")
        void emptyListReturnsError() {
            final String result = tool.summarizeSubnets("[]");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Empty subnet list should return an error");
        }
    }

    // ------------------------------------------------------------------ //
    //  expandIpv6
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("expandIpv6")
    class ExpandIpv6Tests {

        @Test
        @DisplayName("::1 expands to full form with trailing 0001")
        void expandsLoopback() {
            final String result = tool.expandIpv6(V6_LOOPBACK);
            assertEquals(V6_LOOP_FULL, result,
                    "::1 should expand to full 8-group form");
        }

        @Test
        @DisplayName("2001:db8::1 expands with zero-filled middle groups")
        void expandsMiddleCompressed() {
            final String result = tool.expandIpv6(V6_2001_SHORT);
            assertEquals(V6_2001_FULL, result,
                    "2001:db8::1 should expand with zero-filled groups");
        }

        @Test
        @DisplayName(":: expands to all zeros")
        void expandsAllZeros() {
            final String result = tool.expandIpv6("::");
            assertEquals(V6_ALL_ZERO, result,
                    ":: should expand to all-zero full form");
        }

        @Test
        @DisplayName("Already full form remains unchanged")
        void fullFormUnchanged() {
            final String result = tool.expandIpv6(V6_LOOP_FULL);
            assertEquals(V6_LOOP_FULL, result,
                    "Already expanded address should remain the same");
        }
    }

    // ------------------------------------------------------------------ //
    //  compressIpv6
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("compressIpv6")
    class CompressIpv6Tests {

        @Test
        @DisplayName("Full loopback compresses to ::1")
        void compressesLoopback() {
            final String result = tool.compressIpv6(V6_LOOP_FULL);
            assertEquals(V6_LOOPBACK, result,
                    "Full loopback should compress to ::1");
        }

        @Test
        @DisplayName("All-zero address compresses to ::")
        void compressesAllZeros() {
            final String result = tool.compressIpv6(V6_ALL_ZERO);
            assertEquals("::", result,
                    "All-zero address should compress to ::");
        }

        @Test
        @DisplayName("2001:0db8::1 round-trip preserves compressed form")
        void roundTripCompression() {
            final String expanded = tool.expandIpv6(V6_2001_SHORT);
            final String compressed = tool.compressIpv6(expanded);
            assertEquals(V6_2001_SHORT, compressed,
                    "Round-trip expand/compress should return canonical form");
        }

        @Test
        @DisplayName("Address with no compressible groups stays expanded")
        void noCompressibleGroups() {
            final String result = tool.compressIpv6(V6_NO_COMPRESS);
            assertEquals("1:2:3:4:5:6:7:8", result,
                    "Address with no consecutive zero groups should trim leading zeros only");
        }
    }

    // ------------------------------------------------------------------ //
    //  transferTime
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("transferTime")
    class TransferTimeTests {

        @Test
        @DisplayName("1 GB at 100 mbps returns JSON with seconds")
        void resultContainsSeconds() {
            final String result = tool.transferTime("1", UNIT_GB, HUNDRED, UNIT_MBPS);
            assertTrue(result.contains("\"seconds\":\""),
                    "Transfer time result should contain seconds field");
        }

        @Test
        @DisplayName("1 GB at 100 mbps returns JSON with minutes")
        void resultContainsMinutes() {
            final String result = tool.transferTime("1", UNIT_GB, HUNDRED, UNIT_MBPS);
            assertTrue(result.contains("\"minutes\":\""),
                    "Transfer time result should contain minutes field");
        }

        @Test
        @DisplayName("1 GB at 100 mbps returns JSON with hours")
        void resultContainsHours() {
            final String result = tool.transferTime("1", UNIT_GB, HUNDRED, UNIT_MBPS);
            assertTrue(result.contains("\"hours\":\""),
                    "Transfer time result should contain hours field");
        }

        @Test
        @DisplayName("Invalid unit returns error")
        void invalidUnitReturnsError() {
            final String result = tool.transferTime("1", "invalidunit", HUNDRED, UNIT_MBPS);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid file size unit should return an error");
        }
    }

    // ------------------------------------------------------------------ //
    //  throughput
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("throughput")
    class ThroughputTests {

        @Test
        @DisplayName("100 MB in 10 seconds returns result in mbps")
        void basicThroughputCalc() {
            final String result = tool.throughput(HUNDRED, UNIT_MB, "10", "s", UNIT_MBPS);
            assertTrue(!result.startsWith(ERROR_PREFIX),
                    "Valid throughput calculation should not return an error");
        }

        @Test
        @DisplayName("1 GB in 1 second returns numeric result")
        void oneGbInOneSecond() {
            final String result = tool.throughput("1", UNIT_GB, "1", "s", "gbps");
            assertTrue(!result.isEmpty(),
                    "Throughput result should not be empty");
        }

        @Test
        @DisplayName("Invalid time unit returns error")
        void invalidTimeUnitError() {
            final String result = tool.throughput(HUNDRED, UNIT_MB, "10", "invalidunit", UNIT_MBPS);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid time unit should return an error");
        }
    }

    // ------------------------------------------------------------------ //
    //  tcpThroughput
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("tcpThroughput")
    class TcpThroughputTests {

        @Test
        @DisplayName("BDP-limited throughput returns numeric result")
        void bdpLimitedResult() {
            final String result = tool.tcpThroughput(HUNDRED, "20", "64");
            assertTrue(!result.startsWith(ERROR_PREFIX),
                    "Valid TCP throughput should not return an error");
        }

        @Test
        @DisplayName("Large window at low RTT is bandwidth-capped")
        void largeWindowIsCapped() {
            final String result = tool.tcpThroughput("10", "1", "10000");
            assertEquals("10", result,
                    "With a huge window and 1ms RTT the effective throughput "
                            + "should be capped at the link bandwidth of 10 Mbps");
        }

        @Test
        @DisplayName("Zero RTT throws ArithmeticException")
        void zeroRttThrowsException() {
            assertThrows(ArithmeticException.class,
                    () -> tool.tcpThroughput(HUNDRED, "0", "64"),
                    "Zero RTT should cause a division-by-zero exception");
        }
    }

    // ------------------------------------------------------------------ //
    //  Error cases
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Error cases")
    class ErrorCaseTests {

        @Test
        @DisplayName("subnetCalculator with invalid IP returns error")
        void subnetInvalidIpError() {
            final String result = tool.subnetCalculator("not.valid", 24);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Non-numeric octets should produce an error");
        }

        @Test
        @DisplayName("ipToBinary with octet > 255 returns error")
        void ipToBinaryOctetOverflow() {
            final String result = tool.ipToBinary("256.1.1.1");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Octet > 255 should produce an error");
        }

        @Test
        @DisplayName("ipToDecimal with malformed address returns error")
        void ipToDecimalMalformed() {
            final String result = tool.ipToDecimal("1.2.3");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Three-octet address should produce an error");
        }

        @Test
        @DisplayName("decimalToIp with negative value for IPv4 returns error")
        void decimalToIpNegativeError() {
            final String result = tool.decimalToIp("-1", 4);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Negative decimal should produce an error for IPv4");
        }

        @Test
        @DisplayName("expandIpv6 with invalid address returns error")
        void expandIpv6InvalidError() {
            final String result = tool.expandIpv6("gggg::1");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Non-hex characters should produce an error");
        }

        @Test
        @DisplayName("ipInSubnet with CIDR 33 returns error")
        void ipInSubnetCidr33Error() {
            final String result = tool.ipInSubnet(ADDR_HOST, ADDR_NET, 33);
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "CIDR 33 is invalid for IPv4 and should return an error");
        }

        @Test
        @DisplayName("vlsmSubnets with invalid network CIDR returns error")
        void vlsmInvalidNetError() {
            final String result = tool.vlsmSubnets("invalid/24", "[10]");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid network address should produce an error");
        }

        @Test
        @DisplayName("summarizeSubnets with invalid subnet returns error")
        void summarizeInvalidError() {
            final String result = tool.summarizeSubnets("[\"invalid/24\"]");
            assertTrue(result.startsWith(ERROR_PREFIX),
                    "Invalid subnet in list should produce an error");
        }
    }
}

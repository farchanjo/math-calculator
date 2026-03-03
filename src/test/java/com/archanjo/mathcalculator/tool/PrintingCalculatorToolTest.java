package com.archanjo.mathcalculator.tool;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrintingCalculatorToolTest {

    private static final String ADD_100_50 =
            "[{\"op\":\"+\",\"value\":\"100\"},{\"op\":\"+\",\"value\":\"50\"}]";

    private static final String ADD_50_SUBTOTAL =
            "[{\"op\":\"+\",\"value\":\"100\"},{\"op\":\"+\",\"value\":\"50\"}"
                    + ",{\"op\":\"=\",\"value\":\"\"}]";

    private static final String ADD_GRAND_TOTAL =
            "[{\"op\":\"+\",\"value\":\"100\"},{\"op\":\"T\",\"value\":\"\"}]";

    private static final String CLEAR_THEN_ADD =
            "[{\"op\":\"+\",\"value\":\"100\"},{\"op\":\"C\",\"value\":\"\"}"
                    + ",{\"op\":\"+\",\"value\":\"50\"},{\"op\":\"=\",\"value\":\"\"}]";

    private static final String DIVIDE_BY_ZERO =
            "[{\"op\":\"+\",\"value\":\"100\"},{\"op\":\"/\",\"value\":\"0\"}]";

    private static final String UNKNOWN_OP =
            "[{\"op\":\"X\",\"value\":\"1\"}]";

    private final PrintingCalculatorTool tool = new PrintingCalculatorTool();

    @Nested
    @DisplayName("simple addition")
    class SimpleAddition {

        @Test
        void tapeContainsFirstValue() {
            final String tape = tool.calculateWithTape(ADD_100_50);
            assertTrue(tape.contains("100.00"),
                    "Tape should contain 100.00 but was:\n" + tape);
        }

        @Test
        void tapeContainsSecondValue() {
            final String tape = tool.calculateWithTape(ADD_100_50);
            assertTrue(tape.contains("50.00"),
                    "Tape should contain 50.00 but was:\n" + tape);
        }

        @Test
        void tapeContainsPlusOperator() {
            final String tape = tool.calculateWithTape(ADD_100_50);
            assertTrue(tape.contains("+"),
                    "Tape should contain + but was:\n" + tape);
        }
    }

    @Nested
    @DisplayName("subtotal")
    class Subtotal {

        @Test
        void showsRunningTotal() {
            final String tape = tool.calculateWithTape(ADD_50_SUBTOTAL);
            assertTrue(tape.contains("150.00"),
                    "Tape should contain subtotal 150.00 but was:\n" + tape);
        }

        @Test
        void showsEqualsSign() {
            final String tape = tool.calculateWithTape(ADD_50_SUBTOTAL);
            assertTrue(tape.contains("="),
                    "Tape should contain = but was:\n" + tape);
        }
    }

    @Nested
    @DisplayName("grand total")
    class GrandTotal {

        @Test
        void showsTotalValue() {
            final String tape = tool.calculateWithTape(ADD_GRAND_TOTAL);
            assertTrue(tape.contains("100.00"),
                    "Tape should contain 100.00 but was:\n" + tape);
        }

        @Test
        void showsTotalMarker() {
            final String tape = tool.calculateWithTape(ADD_GRAND_TOTAL);
            assertTrue(tape.contains("T"),
                    "Tape should contain T but was:\n" + tape);
        }

        @Test
        void showsSeparator() {
            final String tape = tool.calculateWithTape(ADD_GRAND_TOTAL);
            assertTrue(tape.contains("--------"),
                    "Tape should contain separator but was:\n" + tape);
        }
    }

    @Nested
    @DisplayName("clear operation")
    class ClearOperation {

        @Test
        void resetsRunningTotal() {
            final String tape = tool.calculateWithTape(CLEAR_THEN_ADD);
            assertTrue(tape.contains("50.00"),
                    "Tape should contain subtotal 50.00 after clear but was:\n"
                            + tape);
        }

        @Test
        void subtotalAfterClearIsFifty() {
            final String tape = tool.calculateWithTape(CLEAR_THEN_ADD);
            final String[] lines = tape.split("\n");
            boolean found = false;
            for (final String line : lines) {
                if (line.contains("=") && line.contains("50.00")) {
                    found = true;
                    break;
                }
            }
            assertTrue(found,
                    "Subtotal after clear should be 50.00, not 150.00. Tape:\n"
                            + tape);
        }
    }

    @Nested
    @DisplayName("error cases")
    class ErrorCases {

        @Test
        void divisionByZeroThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.calculateWithTape(DIVIDE_BY_ZERO),
                    "Division by zero should throw IllegalArgumentException");
        }

        @Test
        void nullInputThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.calculateWithTape(null),
                    "Null input should throw IllegalArgumentException");
        }

        @Test
        void emptyStringThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.calculateWithTape(""),
                    "Empty input should throw IllegalArgumentException");
        }

        @Test
        void unknownOperationThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> tool.calculateWithTape(UNKNOWN_OP),
                    "Unknown operation should throw IllegalArgumentException");
        }
    }
}

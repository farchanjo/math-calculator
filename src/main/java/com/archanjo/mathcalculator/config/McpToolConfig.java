package com.archanjo.mathcalculator.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.archanjo.mathcalculator.tool.BasicCalculatorTool;
import com.archanjo.mathcalculator.tool.CookingConverterTool;
import com.archanjo.mathcalculator.tool.DateTimeConverterTool;
import com.archanjo.mathcalculator.tool.FinancialCalculatorTool;
import com.archanjo.mathcalculator.tool.GraphingCalculatorTool;
import com.archanjo.mathcalculator.tool.MeasureReferenceTool;
import com.archanjo.mathcalculator.tool.PrintingCalculatorTool;
import com.archanjo.mathcalculator.tool.ProgrammableCalculatorTool;
import com.archanjo.mathcalculator.tool.ScientificCalculatorTool;
import com.archanjo.mathcalculator.tool.UnitConverterTool;
import com.archanjo.mathcalculator.tool.VectorCalculatorTool;

@Configuration
public class McpToolConfig {

    @Bean
    ToolCallbackProvider calculatorTools(
            final BasicCalculatorTool basic,
            final ScientificCalculatorTool scientific,
            final FinancialCalculatorTool financial,
            final GraphingCalculatorTool graphing,
            final PrintingCalculatorTool printing,
            final ProgrammableCalculatorTool programmable,
            final VectorCalculatorTool vector,
            final UnitConverterTool unitConverter,
            final CookingConverterTool cookingConverter,
            final MeasureReferenceTool measureReference,
            final DateTimeConverterTool dateTimeConverter) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(basic, scientific, financial,
                        graphing, printing, programmable, vector,
                        unitConverter, cookingConverter,
                        measureReference, dateTimeConverter)
                .build();
    }
}

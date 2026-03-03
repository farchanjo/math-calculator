package com.archanjo.mathcalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MathCalculatorApplication {
    static void main(final String... args) {
        new MathCalculatorApplication().start(args);
    }

    private void start(final String... args) {
        SpringApplication.run(MathCalculatorApplication.class, args);
    }
}

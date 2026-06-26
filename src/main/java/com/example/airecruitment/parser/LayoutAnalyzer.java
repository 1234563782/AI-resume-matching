package com.example.airecruitment.parser;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LayoutAnalyzer {
    public String rebuildReadingOrder(List<TextBlock> blocks) {
        return blocks.stream()
                .sorted(Comparator.comparingInt(TextBlock::page)
                        .thenComparingDouble(TextBlock::y)
                        .thenComparingDouble(TextBlock::x))
                .map(TextBlock::text)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }
}

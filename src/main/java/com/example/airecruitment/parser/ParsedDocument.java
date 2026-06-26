package com.example.airecruitment.parser;

import java.util.List;

public record ParsedDocument(String text, List<TextBlock> blocks) {
}

package com.civil.platform.common.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class QuestionAnswerUtils {

    private QuestionAnswerUtils() {
    }

    public static boolean isMultiChoice(String questionType) {
        return "MULTI".equalsIgnoreCase(questionType);
    }

    public static boolean isSingleChoice(String questionType) {
        return "SINGLE".equalsIgnoreCase(questionType);
    }

    public static List<String> parseAnswerValues(String answer) {
        if (!StringUtils.hasText(answer)) {
            return Collections.emptyList();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String item : answer.toUpperCase().split(",")) {
            if (StringUtils.hasText(item)) {
                values.add(item.trim());
            }
        }
        List<String> normalized = new ArrayList<>(values);
        Collections.sort(normalized);
        return normalized;
    }

    public static String normalizeAnswer(String answer) {
        return String.join(",", parseAnswerValues(answer));
    }
}

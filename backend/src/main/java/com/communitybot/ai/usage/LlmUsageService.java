package com.communitybot.ai.usage;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.ai.usage.dto.LlmUsageSummaryResponse;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmUsageService {

    private final LlmUsageEventRepository repository;
    private final UserRepository          userRepository;
    private final OpenAiProperties        openAiProperties;

    @Transactional
    public void record(UUID userId, LlmUsageCategory category, String model,
                       int inputTokens, int outputTokens) {
        if (userId == null || inputTokens < 0 || outputTokens < 0) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.trace("Skipping LLM usage record — unknown user {}", userId);
            return;
        }
        BigDecimal cost = estimateCostUsd(model, inputTokens, outputTokens);
        repository.save(LlmUsageEvent.builder()
                .user(user)
                .category(category)
                .model(truncateModel(model))
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .costUsd(cost)
                .build());
    }

    @Transactional(readOnly = true)
    public LlmUsageSummaryResponse summarizeForUserAndProject(UUID userId) {
        Object[] u = aggregateRow(repository.sumForUser(userId));
        Object[] p = aggregateRow(repository.sumProject());
        return new LlmUsageSummaryResponse(
                toLong(u[0]),
                toLong(u[1]),
                toLong(p[0]),
                toLong(p[1]),
                toMoney(u[2]).doubleValue(),
                toMoney(p[2]).doubleValue()
        );
    }

    /**
     * Native aggregate queries sometimes return a single-element array whose sole entry
     * is the real {@code [in, out, cost]} row — unwrap before reading columns.
     */
    private static Object[] aggregateRow(Optional<Object[]> raw) {
        Object[] row = raw.orElse(null);
        if (row == null) {
            return new Object[] { 0L, 0L, BigDecimal.ZERO };
        }
        if (row.length == 1 && row[0] instanceof Object[] nested) {
            row = nested;
        }
        if (row.length < 3) {
            return new Object[] { 0L, 0L, BigDecimal.ZERO };
        }
        return row;
    }

    private BigDecimal estimateCostUsd(String model, int inputTokens, int outputTokens) {
        String m = model == null ? "" : model.trim();
        Map<String, OpenAiProperties.ModelRate> rates = openAiProperties.getModelRates();
        OpenAiProperties.ModelRate rate = rates.get(m);
        if (rate == null) {
            rate = rates.getOrDefault("fallback", defaultModelRate());
        }
        BigDecimal inPart = rate.getInputPerMillion()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000L), 6, RoundingMode.HALF_UP);
        BigDecimal outPart = rate.getOutputPerMillion()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000L), 6, RoundingMode.HALF_UP);
        return inPart.add(outPart).setScale(6, RoundingMode.HALF_UP);
    }

    private static OpenAiProperties.ModelRate defaultModelRate() {
        var d = new OpenAiProperties.ModelRate();
        d.setInputPerMillion(new BigDecimal("0.15"));
        d.setOutputPerMillion(new BigDecimal("0.60"));
        return d;
    }

    private static String truncateModel(String model) {
        if (model == null) return "";
        return model.length() <= 191 ? model : model.substring(0, 188) + "…";
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private static BigDecimal toMoney(Object o) {
        if (o == null) return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        if (o instanceof BigDecimal b) return b.setScale(6, RoundingMode.HALF_UP);
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(6, RoundingMode.HALF_UP);
        }
        return new BigDecimal(o.toString()).setScale(6, RoundingMode.HALF_UP);
    }
}

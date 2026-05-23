package com.communitybot.ai.agent;

import com.communitybot.scheduling.domain.ScheduleType;
import com.communitybot.scheduling.dto.CreateScheduledPostRequest;
import com.communitybot.scheduling.service.ScheduledPostService;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProposedActionService {

    private final ProposedActionRepository proposedActionRepository;
    private final ScheduledPostService     scheduledPostService;
    private final ObjectMapper            objectMapper;

    @Transactional
    public void confirmScheduledPost(UUID workspaceId, UUID userId, UUID proposalId) {
        ProposedAction pa = proposedActionRepository
                .findByIdAndWorkspaceIdAndUserIdAndStatus(
                        proposalId, workspaceId, userId, ProposedAction.Status.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSED_ACTION_NOT_FOUND));

        if (!"SCHEDULED_POST".equals(pa.getActionType())) {
            throw new AppException(ErrorCode.PROPOSED_ACTION_NOT_FOUND);
        }

        ScheduleProposalPayload payload;
        try {
            payload = objectMapper.readValue(pa.getPayloadJson(), ScheduleProposalPayload.class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid proposal payload");
        }

        ScheduleType st;
        try {
            st = ScheduleType.valueOf(payload.scheduleType().trim().toUpperCase());
        } catch (Exception e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid scheduleType");
        }

        Instant fireAt = null;
        String cron = null;
        if (st == ScheduleType.ONE_SHOT) {
            if (payload.fireAtIso() == null || payload.fireAtIso().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "fireAt required for ONE_SHOT");
            }
            fireAt = Instant.parse(payload.fireAtIso());
        } else {
            if (payload.cronExpression() == null || payload.cronExpression().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "cronExpression required for CRON");
            }
            cron = payload.cronExpression();
        }

        var req = new CreateScheduledPostRequest(
                payload.channelId(),
                payload.body(),
                st,
                fireAt,
                cron
        );

        scheduledPostService.create(workspaceId, req, userId);
        pa.confirm();
        proposedActionRepository.save(pa);
    }

    @Transactional
    public void decline(UUID workspaceId, UUID userId, UUID proposalId) {
        ProposedAction pa = proposedActionRepository
                .findByIdAndWorkspaceIdAndUserIdAndStatus(
                        proposalId, workspaceId, userId, ProposedAction.Status.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSED_ACTION_NOT_FOUND));
        pa.decline();
        proposedActionRepository.save(pa);
    }
}

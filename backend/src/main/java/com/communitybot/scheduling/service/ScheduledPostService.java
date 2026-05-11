package com.communitybot.scheduling.service;

import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.message.service.MessageService;
import com.communitybot.scheduling.domain.ScheduledPost;
import com.communitybot.scheduling.domain.ScheduleType;
import com.communitybot.scheduling.dto.CreateScheduledPostRequest;
import com.communitybot.scheduling.dto.ScheduledPostResponse;
import com.communitybot.scheduling.repository.ScheduledPostRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledPostService {

    private final ScheduledPostRepository scheduledPostRepository;
    private final ChannelService          channelService;
    private final WorkspaceService        workspaceService;
    private final MessageService          messageService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public ScheduledPostResponse create(UUID wsId, CreateScheduledPostRequest req, UUID userId) {
        workspaceService.requireMembership(wsId, userId);
        channelService.requireChannelMember(req.channelId(), userId);

        Instant nextFireAt = resolveNextFireAt(req);

        Workspace workspace = workspaceService.getOrThrow(wsId);
        Channel   channel   = channelService.getOrThrow(req.channelId());

        ScheduledPost post = scheduledPostRepository.save(
                ScheduledPost.builder()
                        .workspace(workspace)
                        .channel(channel)
                        .createdBy(userId)
                        .body(req.body())
                        .scheduleType(req.scheduleType())
                        .cronExpression(req.cronExpression())
                        .nextFireAt(nextFireAt)
                        .build()
        );

        return ScheduledPostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public List<ScheduledPostResponse> list(UUID wsId, UUID userId) {
        workspaceService.requireMembership(wsId, userId);
        return scheduledPostRepository.findAllByWorkspaceIdOrderByNextFireAtAsc(wsId)
                .stream().map(ScheduledPostResponse::from).toList();
    }

    @Transactional
    public void cancel(UUID wsId, UUID postId, UUID userId) {
        workspaceService.requireModeratorOrAdmin(wsId, userId);
        ScheduledPost post = scheduledPostRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULED_POST_NOT_FOUND));
        post.cancel();
    }

    // ── Scheduler ─────────────────────────────────────────────────────────────

    /**
     * Polls for due posts every minute and delivers them.
     * Uses {@code SELECT … FOR UPDATE SKIP LOCKED} so concurrent application instances
     * never double-deliver the same post.
     */
    @Scheduled(fixedDelayString = "${app.scheduling.poll-interval-ms:60000}")
    @Transactional
    public void processDuePosts() {
        List<ScheduledPost> due = scheduledPostRepository.findDuePostsForUpdate(Instant.now());
        if (due.isEmpty()) return;

        log.info("Delivering {} due scheduled post(s)", due.size());
        for (ScheduledPost post : due) {
            try {
                messageService.send(
                        post.getChannel().getId(),
                        post.getCreatedBy(),
                        post.getBody(),
                        null,   // no thread root
                        null    // no attachment
                );
                if (post.getScheduleType() == ScheduleType.ONE_SHOT) {
                    post.markSent();
                } else {
                    post.rescheduleFromCron();
                }
                log.debug("Delivered scheduled post {} (type={})", post.getId(), post.getScheduleType());
            } catch (Exception e) {
                log.error("Failed to deliver scheduled post {}: {}", post.getId(), e.getMessage());
                post.markError(e.getMessage());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Instant resolveNextFireAt(CreateScheduledPostRequest req) {
        if (req.scheduleType() == ScheduleType.ONE_SHOT) {
            if (req.fireAt() == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "fireAt is required for ONE_SHOT posts");
            }
            return req.fireAt();
        }

        // CRON — validate and compute the first occurrence
        if (req.cronExpression() == null || req.cronExpression().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "cronExpression is required for CRON posts");
        }
        try {
            CronExpression cron = CronExpression.parse(req.cronExpression());
            ZonedDateTime  next = cron.next(ZonedDateTime.now(ZoneOffset.UTC));
            if (next == null) throw new AppException(ErrorCode.INVALID_CRON_EXPRESSION,
                    "Cron expression has no future occurrences");
            return next.toInstant();
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_CRON_EXPRESSION, e.getMessage());
        }
    }
}

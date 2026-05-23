package com.communitybot.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exhaustive list of application error codes.
 * Each code carries an HTTP status and a human-readable message.
 * Adding a new case here is the only change needed to introduce a new error.
 */
@Getter
public enum ErrorCode {

    // --- Auth ---
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("Invalid email or password", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("Email is already in use", HttpStatus.CONFLICT),
    INVALID_TOKEN("Invalid or expired token", HttpStatus.UNAUTHORIZED),

    // --- Organisation ---
    ORG_NOT_FOUND("Organisation not found", HttpStatus.NOT_FOUND),
    ORG_SLUG_TAKEN("Organisation slug is already taken", HttpStatus.CONFLICT),
    ORG_ACCESS_DENIED("You are not a member of this organisation", HttpStatus.FORBIDDEN),
    ORG_ALREADY_MEMBER("User is already a member of this organisation", HttpStatus.CONFLICT),

    // --- Workspace ---
    WORKSPACE_NOT_FOUND("Workspace not found", HttpStatus.NOT_FOUND),
    WORKSPACE_SLUG_TAKEN("Workspace slug is already taken within this organisation", HttpStatus.CONFLICT),
    WORKSPACE_ACCESS_DENIED("You are not a member of this workspace", HttpStatus.FORBIDDEN),
    WORKSPACE_ALREADY_MEMBER("User is already a member of this workspace", HttpStatus.CONFLICT),

    // --- Channel ---
    CHANNEL_NOT_FOUND("Channel not found", HttpStatus.NOT_FOUND),
    CHANNEL_SLUG_TAKEN("Channel slug is already taken within this workspace", HttpStatus.CONFLICT),
    CHANNEL_ACCESS_DENIED("You are not a member of this channel", HttpStatus.FORBIDDEN),

    // --- Message ---
    MESSAGE_NOT_FOUND("Message not found", HttpStatus.NOT_FOUND),
    MESSAGE_FORBIDDEN("You do not have permission to modify this message", HttpStatus.FORBIDDEN),

    // --- Attachment ---
    ATTACHMENT_NOT_FOUND("Attachment not found", HttpStatus.NOT_FOUND),
    ATTACHMENT_TOO_LARGE("File exceeds the 10 MB size limit", HttpStatus.PAYLOAD_TOO_LARGE),
    ATTACHMENT_TYPE_NOT_ALLOWED("Only PDF and DOCX files are accepted", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    ATTACHMENT_INFECTED("File rejected: antivirus scanner detected a threat", HttpStatus.UNPROCESSABLE_ENTITY),
    ATTACHMENT_SCAN_FAILED("Virus scan service is unavailable; upload rejected for safety", HttpStatus.SERVICE_UNAVAILABLE),
    ATTACHMENT_UPLOAD_FAILED("File storage failed; please try again", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Scheduling ---
    SCHEDULED_POST_NOT_FOUND("Scheduled post not found", HttpStatus.NOT_FOUND),
    INVALID_CRON_EXPRESSION("Invalid cron expression", HttpStatus.BAD_REQUEST),

    // --- Moderation ---
    USER_BANNED("You are banned from this workspace", HttpStatus.FORBIDDEN),
    MODERATION_FLAG_NOT_FOUND("Moderation flag not found", HttpStatus.NOT_FOUND),
    MODERATION_INSUFFICIENT_ROLE("You need Moderator or Admin role to perform this action", HttpStatus.FORBIDDEN),
    BAN_NOT_FOUND("Active ban not found for this user", HttpStatus.NOT_FOUND),

    // --- Document / RAG ---
    DOCUMENT_NOT_FOUND("FAQ document not found", HttpStatus.NOT_FOUND),
    DOCUMENT_INGESTION_FAILED("Document ingestion failed; check that the file contains readable text", HttpStatus.UNPROCESSABLE_ENTITY),
    RAG_RATE_LIMIT_EXCEEDED("Daily AI query limit reached for this workspace", HttpStatus.TOO_MANY_REQUESTS),

    // --- Roles ---
    ROLE_NOT_FOUND("Role not found", HttpStatus.NOT_FOUND),
    ROLE_SYSTEM_PROTECTED("System roles cannot be deleted", HttpStatus.FORBIDDEN),

    // --- Generic ---
    VALIDATION_ERROR("Request validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status  = status;
    }
}

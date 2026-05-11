package com.communitybot.attachment.domain;

public enum ScanStatus {
    PENDING,    // not yet scanned
    CLEAN,      // passed AV scan
    INFECTED,   // threat detected (record kept for audit; file deleted from storage)
    ERROR       // scan service was unreachable
}

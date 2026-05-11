package com.communitybot.organization.dto;

import com.communitybot.organization.domain.OrgRole;
import com.communitybot.organization.domain.Organization;

import java.time.Instant;
import java.util.UUID;

public record OrgResponse(
        UUID    id,
        String  name,
        String  slug,
        UUID    ownerId,
        OrgRole myRole,    // populated when queried in the context of a specific member
        Instant createdAt
) {
    /** Use when role context is not available (e.g., admin listing). */
    public static OrgResponse from(Organization org) {
        return new OrgResponse(
                org.getId(), org.getName(), org.getSlug(),
                org.getOwner().getId(), null, org.getCreatedAt()
        );
    }

    public static OrgResponse from(Organization org, OrgRole myRole) {
        return new OrgResponse(
                org.getId(), org.getName(), org.getSlug(),
                org.getOwner().getId(), myRole, org.getCreatedAt()
        );
    }
}

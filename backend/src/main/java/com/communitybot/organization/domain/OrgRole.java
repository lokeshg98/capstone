package com.communitybot.organization.domain;

public enum OrgRole {
    OWNER,      // full control, cannot be removed
    ADMIN,      // can manage members and workspaces
    MEMBER      // read-only org-level access
}

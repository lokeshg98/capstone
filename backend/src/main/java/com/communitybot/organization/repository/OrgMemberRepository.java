package com.communitybot.organization.repository;

import com.communitybot.organization.domain.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgMemberRepository extends JpaRepository<OrgMember, UUID> {

    Optional<OrgMember> findByOrganizationIdAndUserId(UUID orgId, UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID orgId, UUID userId);

    List<OrgMember> findByUserId(UUID userId);
}

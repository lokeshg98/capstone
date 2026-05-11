package com.communitybot.organization.repository;

import com.communitybot.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Returns all organisations the given user belongs to (via org_members). */
    @Query("""
            SELECT om.organization
            FROM OrgMember om
            WHERE om.user.id = :userId
            ORDER BY om.organization.name
            """)
    List<Organization> findAllByMemberUserId(UUID userId);
}

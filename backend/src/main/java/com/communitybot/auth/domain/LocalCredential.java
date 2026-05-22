package com.communitybot.auth.domain;

import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "local_credentials",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_local_credentials_email",
        columnNames = {"email"}
    )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalCredential extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}

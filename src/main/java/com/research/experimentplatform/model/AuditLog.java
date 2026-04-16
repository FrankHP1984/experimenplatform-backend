package com.research.experimentplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quién hizo el acceso (supabaseId del investigador o admin)
    @Column(nullable = false)
    private String actorSupabaseId;

    // Qué acción realizó: VIEW_PARTICIPANTS, VIEW_RESPONSES, RESOLVE_PSEUDONYM
    @Column(nullable = false)
    private String action;

    // Sobre qué tipo de recurso: EXPERIMENT, PARTICIPANT
    @Column(nullable = false)
    private String resourceType;

    // ID del recurso accedido (experimentId, pseudonymCode, etc.)
    private String resourceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime accessedAt;

    @PrePersist
    protected void onCreate() {
        accessedAt = LocalDateTime.now();
    }

    public AuditLog(String actorSupabaseId, String action, String resourceType, String resourceId) {
        this.actorSupabaseId = actorSupabaseId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}

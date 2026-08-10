package com.example.minitask_pacto_mais.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum TaskStatus {
    PLANNING,
    ASSIGNED,
    IN_PROGRESS,
    AWAITING_REVIEW,
    IN_REVIEW,
    REJECTED,
    COMPLETED,
    CANCELLED;

    public boolean requiresAssignee() {
        return this != PLANNING && this != CANCELLED;
    }

    public Set<TaskStatus> allowedTransitions() {
        return switch (this) {
            case PLANNING -> EnumSet.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, PLANNING, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(AWAITING_REVIEW, ASSIGNED, CANCELLED);
            case AWAITING_REVIEW -> EnumSet.of(IN_REVIEW, IN_PROGRESS, CANCELLED);
            case IN_REVIEW -> EnumSet.of(COMPLETED, REJECTED, CANCELLED);
            case REJECTED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(TaskStatus.class);
        };
    }

    public boolean canTransitionTo(TaskStatus next) {
        return allowedTransitions().contains(next);
    }

    public String labelPt() {
        return switch (this) {
            case PLANNING -> "Planejamento";
            case ASSIGNED -> "Atribuída";
            case IN_PROGRESS -> "Em progresso";
            case AWAITING_REVIEW -> "Aguardando review";
            case IN_REVIEW -> "Em review";
            case REJECTED -> "Rejeitada";
            case COMPLETED -> "Concluída";
            case CANCELLED -> "Cancelada";
        };
    }

    @JsonValue
    public String jsonValue() {
        return name();
    }

    @JsonCreator
    public static TaskStatus fromJson(String raw) {
        return parse(raw);
    }

    public static TaskStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Status vazio");
        }
        String trimmed = raw.trim();
        for (TaskStatus status : values()) {
            if (status.name().equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        String folded = fold(trimmed);
        for (TaskStatus status : values()) {
            if (fold(status.labelPt()).equals(folded)) {
                return status;
            }
        }
        return switch (folded) {
            case "planejamento", "planejada", "planning" -> PLANNING;
            case "atribuida", "atribuido", "assigned" -> ASSIGNED;
            case "em progresso", "em andamento", "andamento", "progresso" -> IN_PROGRESS;
            case "aguardando review", "aguardando revisao", "aguardando aprovacao" -> AWAITING_REVIEW;
            case "em review", "em revisao", "review" -> IN_REVIEW;
            case "rejeitada", "rejeitado", "rejected" -> REJECTED;
            case "concluida", "concluido", "completa", "completo", "completed" -> COMPLETED;
            case "cancelada", "cancelado", "cancelled", "canceled" -> CANCELLED;
            default -> throw new IllegalArgumentException("Status inválido: " + raw);
        };
    }

    private static String fold(String value) {
        String n = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }
}


package com.example.minitask_pacto_mais.domain;

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
        return this == AWAITING_REVIEW
                || this == IN_REVIEW
                || this == COMPLETED;
    }
    /** Movimentações permitidas entre os status. */


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
}


package com.example.minitask_pacto_mais.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStatusTest {

    @Test
    void planningSoPodeIrParaAssignedOuCancelled() {
        assertTrue(TaskStatus.PLANNING.canTransitionTo(TaskStatus.ASSIGNED));
        assertTrue(TaskStatus.PLANNING.canTransitionTo(TaskStatus.CANCELLED));
        assertFalse(TaskStatus.PLANNING.canTransitionTo(TaskStatus.COMPLETED));
        assertFalse(TaskStatus.PLANNING.canTransitionTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    void concludedECancelledNaoTemProximoStatus() {
        assertTrue(TaskStatus.COMPLETED.allowedTransitions().isEmpty());
        assertTrue(TaskStatus.CANCELLED.allowedTransitions().isEmpty());
    }

    @Test
    void completedExigeResponsavel() {
        assertTrue(TaskStatus.COMPLETED.requiresAssignee());
        assertTrue(TaskStatus.ASSIGNED.requiresAssignee());
        assertFalse(TaskStatus.PLANNING.requiresAssignee());
        assertFalse(TaskStatus.CANCELLED.requiresAssignee());
    }

    @Test
    void parseAceitaEnumEPortugues() {
        assertEquals(TaskStatus.ASSIGNED, TaskStatus.parse("ASSIGNED"));
        assertEquals(TaskStatus.ASSIGNED, TaskStatus.parse("Atribuída"));
        assertEquals(TaskStatus.COMPLETED, TaskStatus.parse("Concluída"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.parse("em andamento"));
    }

    @Test
    void parseRejeitaValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> TaskStatus.parse("xyz"));
        assertThrows(IllegalArgumentException.class, () -> TaskStatus.parse(" "));
    }
}

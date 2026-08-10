package com.example.minitask_pacto_mais.notification;

import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WhatsAppNotifier {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotifier.class);

    private final EvolutionOtpSender evolutionOtpSender;
    private final EvolutionProperties evolutionProperties;
    private final UserRepository userRepository;

    public void notifyTaskAssigned(Task task) {
        User assignee = task.getAssignee();
        if (assignee == null) {
            return;
        }
        sendSafe(
                assignee,
                "Task vinculada a você: \"" + task.getTitle() + "\" (" + task.getStatus().labelPt() + ")"
        );
    }

    public void notifyAwaitingApproval(Task task) {
        List<User> admins = userRepository.findByRoleAndPhoneVerifiedTrue(Role.ADMIN);
        String message = "Task aguardando aprovação: \"" + task.getTitle()
                + "\" [" + task.getId() + "] status=" + task.getStatus().labelPt();
        for (User admin : admins) {
            sendSafe(admin, message);
        }
    }

    public void notifyTaskEvaluated(Task task) {
        String message = "Task avaliada (" + task.getStatus().labelPt() + "): \"" + task.getTitle() + "\"";
        if (task.getRating() != null) {
            message += " nota=" + task.getRating();
        }

        Set<User> recipients = new LinkedHashSet<>();
        if (task.getAssignee() != null) {
            recipients.add(task.getAssignee());
        }
        if (task.getCreatedBy() != null) {
            recipients.add(task.getCreatedBy());
        }
        for (User user : recipients) {
            sendSafe(user, message);
        }
    }

    private void sendSafe(User user, String message) {
        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return;
        }
        if (!evolutionProperties.isServerConfigured()) {
            log.info("[WHATSAPP-DEV] to={} message={}", user.getPhone(), message);
            return;
        }
        try {
            evolutionOtpSender.sendWhatsApp(user.getPhone(), message);
        } catch (Exception ex) {
            log.warn("Falha ao notificar WhatsApp userId={} phone={}: {}",
                    user.getId(), user.getPhone(), ex.getMessage());
        }
    }
}

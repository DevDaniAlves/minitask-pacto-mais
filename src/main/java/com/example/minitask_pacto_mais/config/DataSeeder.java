package com.example.minitask_pacto_mais.config;

import com.example.minitask_pacto_mais.domain.Board;
import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import com.example.minitask_pacto_mais.domain.Team;
import com.example.minitask_pacto_mais.domain.TeamMember;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.BoardRepository;
import com.example.minitask_pacto_mais.repository.TaskRepository;
import com.example.minitask_pacto_mais.repository.TeamMemberRepository;
import com.example.minitask_pacto_mais.repository.TeamRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedData(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            BoardRepository boardRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User admin = userRepository.save(User.builder()
                    .name("Admin Demo")
                    .email("admin@demo.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build());

            User funcionario = userRepository.save(User.builder()
                    .name("Funcionário Demo")
                    .email("func@demo.com")
                    .passwordHash(passwordEncoder.encode("func123"))
                    .role(Role.FUNCIONARIO)
                    .build());

            Team alpha = teamRepository.save(Team.builder()
                    .name("Time Alpha")
                    .color("#2563EB")
                    .createdBy(admin)
                    .build());
            Team beta = teamRepository.save(Team.builder()
                    .name("Time Beta")
                    .color("#DC2626")
                    .createdBy(admin)
                    .build());

            teamMemberRepository.save(TeamMember.builder().team(alpha).user(funcionario).build());
            teamMemberRepository.save(TeamMember.builder().team(alpha).user(admin).build());
            teamMemberRepository.save(TeamMember.builder().team(beta).user(admin).build());

            Board boardAlpha = boardRepository.save(Board.builder()
                    .name("Kanban Alpha")
                    .team(alpha)
                    .createdBy(admin)
                    .build());
            Board boardBeta = boardRepository.save(Board.builder()
                    .name("Kanban Beta")
                    .team(beta)
                    .createdBy(admin)
                    .build());

            taskRepository.save(Task.builder()
                    .title("Configurar ambiente")
                    .description("Subir API e banco localmente")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(Priority.HIGH)
                    .board(boardAlpha)
                    .assignee(funcionario)
                    .createdBy(admin)
                    .dueDate(LocalDateTime.now().plusDays(3))
                    .build());

            taskRepository.save(Task.builder()
                    .title("Revisar layout do Kanban")
                    .description("Ajustar colunas e cores por time")
                    .status(TaskStatus.ASSIGNED)
                    .priority(Priority.MEDIUM)
                    .board(boardAlpha)
                    .assignee(funcionario)
                    .createdBy(admin)
                    .dueDate(LocalDateTime.now().plusDays(5))
                    .build());

            taskRepository.save(Task.builder()
                    .title("Planejar integração n8n")
                    .description("Definir webhook para Evolution API")
                    .status(TaskStatus.PLANNING)
                    .priority(Priority.LOW)
                    .board(boardBeta)
                    .createdBy(admin)
                    .dueDate(LocalDateTime.now().plusDays(10))
                    .build());

            log.info("Seed criado: admin@demo.com / admin123 | func@demo.com / func123");
        };
    }
}

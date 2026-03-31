package ru.urfu.TeamTaskManager.configuration;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.urfu.TeamTaskManager.domain.Task;
import ru.urfu.TeamTaskManager.domain.Team;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.dto.request.TeamRequest;
import ru.urfu.TeamTaskManager.dto.request.UserRequest;
import ru.urfu.TeamTaskManager.enums.TaskStatus;
import ru.urfu.TeamTaskManager.repository.*;
import ru.urfu.TeamTaskManager.service.*;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository,
                           TeamRepository teamRepository,
                           TaskRepository taskRepository,
                           UserService userService,
                           TeamService teamService) {
        return args -> {
            if (userRepository.count() > 0) {
                System.out.println("ttm srarted");
                return;
            }

            User leaderUser = userService.createUser(UserRequest.builder()
                    .username("leader")
                    .email("leader@mail.com")
                    .password("123")
                    .build());

            User memberUser = userService.createUser(UserRequest.builder()
                    .username("member")
                    .email("member@mail.com")
                    .password("123")
                    .build());

            Team team = teamService.createTeam(leaderUser.getId(), TeamRequest.builder()
                    .name("Backend Team")
                    .description("Java team")
                    .build());

            userService.assignUserToTeam(memberUser.getId(), team.getId());

            Task task = new Task();
            task.setTitle("Сделать API");
            task.setDescription("Написать контроллеры");
            task.setTaskStatus(TaskStatus.NEW);
            task.setDeadline(LocalDateTime.now().plusDays(3));
            task.setAssignedUser(memberUser);

            taskRepository.save(task);
            System.out.println("ttm srarted");
        };
    }
}

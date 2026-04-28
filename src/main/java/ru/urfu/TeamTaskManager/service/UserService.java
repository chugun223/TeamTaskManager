package ru.urfu.TeamTaskManager.service;


import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.Team;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.dto.request.UserRequest;
import ru.urfu.TeamTaskManager.enums.Role;
import ru.urfu.TeamTaskManager.exception.ConflictException;
import ru.urfu.TeamTaskManager.exception.ForbiddenException;
import ru.urfu.TeamTaskManager.exception.NotFoundException;
import ru.urfu.TeamTaskManager.exception.ValidationException;
import ru.urfu.TeamTaskManager.repository.TaskRepository;
import ru.urfu.TeamTaskManager.repository.TeamRepository;
import ru.urfu.TeamTaskManager.repository.UserRepository;

import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public User createUser(UserRequest request) {
        log.info("Attempt to create user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new ConflictException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ConflictException("Email '" + request.getEmail() + "' already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.NONE)
                .build();
        User saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getId());
        return saved;
    }

    public Page<User> getAllUsers(int page, int size) {
        log.info("Getting all users with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long userId) {
        log.info("Getting user by id: {}", userId);
        return userRepository.findById(userId).orElseThrow(() -> {
            log.error("User not found with id: {}", userId);
            return new NotFoundException("User not found with id: " + userId);
        });
    }

    public List<User> getUsersByTeam(Long teamId) {
        log.info("Getting users by team id: {}", teamId);
        if(!teamRepository.existsById(teamId)) {
            log.error("Team not found with id: {}", teamId);
            throw new NotFoundException("Team not found with id: " + teamId);
        }
        return userRepository.findByTeamId(teamId);
    }

    @Transactional
    public User assignUserToTeam(Long userId, Long teamId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User {} tries to assign user {} to team {}", username, userId, teamId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("Authenticated user not found: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        User user = getUserById(userId);
        if(leader.getTeam() == null || !leader.getTeam().getId().equals(teamId)) {
            log.warn("Leader {} попытался назначить в чужую команду {}", username, teamId);
            throw new ForbiddenException("Team leaders can only assign users to their own team");
        }
        if (user.getTeam() != null) {
            log.warn("User {} already in team", userId);
            throw new ConflictException("User is already in a team");
        }

        Team team = teamRepository.findById(teamId).orElseThrow(() -> {
            log.error("Team not found with id: {}", teamId);
            return new NotFoundException("Team not found with id: " + teamId);
        });

        user.setTeam(team);
        user.setRole(Role.MEMBER);
        log.info("User {} successfully assigned to team {}", userId, teamId);
        return user;
    }

    @Transactional
    public void removeUserFromTeam(Long userId) {
        log.info("Attempt to remove user {} from team", userId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        User user = getUserById(userId);
        if(currentUser.getTeam() == null){
            log.warn("User {} is not in any team", username);
            throw new ForbiddenException("You are not in any team");
        }
        if(user.getTeam() == null) {
            log.warn("User {} is not in any team", userId);
            throw new ConflictException("User is not in any team");
        }
        Long teamId = user.getTeam().getId();
        if(currentUser.getRole() == Role.TEAMLEADER && !currentUser.getTeam().getId().equals(teamId)) {
            log.warn("Team leader {} cannot remove user from team {}", username, teamId);
            throw new ForbiddenException("Team leaders can only remove users from their own team");
        }
        if(currentUser.getRole() == Role.MEMBER && !currentUser.getId().equals(userId)) {
            log.warn("Member {} cannot remove user {}", username, userId);
            throw new ForbiddenException("Members can only remove themselves from a team");
        }

        Role previousRole = user.getRole();
        user.setRole(Role.NONE);
        user.setTeam(null);
        userRepository.save(user);
        List<User> members = userRepository.findByTeamId(teamId);
        if (previousRole == Role.TEAMLEADER && members != null && !members.isEmpty()) {
            User newLeader = members.getFirst();
            newLeader.setRole(Role.TEAMLEADER);
            userRepository.save(newLeader);
        }
        if(members.isEmpty()) {
            Team team = teamRepository.findById(teamId).orElseThrow(() -> {
                log.error("Team not found with id: {}", teamId);
                return new NotFoundException("Team not found with id: " + teamId);
            });
            teamRepository.delete(team);
        }
        log.info("User {} removed from team", userId);
    }

    @Transactional
    public void deleteUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("Attempt to delete user: {}", username);
        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        taskRepository.findByAssignedUserId(user.getId()).forEach(task -> task.setAssignedUser(null));
        Team team = user.getTeam();
        if (team != null) {
            List<User> members = userRepository.findByTeamId(team.getId());
            members.remove(user);
            if (user.getRole() == Role.TEAMLEADER) {
                if (!members.isEmpty()) {
                    User newLeader = members.get(new Random().nextInt(members.size()));
                    newLeader.setRole(Role.TEAMLEADER);
                }
            }
            user.setTeam(null);
            user.setRole(Role.NONE);
            List<User> after = userRepository.findByTeamId(team.getId());
            if (after.isEmpty()) {
                teamRepository.delete(team);
            }
        }
        userRepository.delete(user);
        SecurityContextHolder.clearContext();
        log.info("User {} deleted", username);
    }

    @Transactional
    public User updateUser(UserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("Attempt to update user: {}", username);
        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new ConflictException("Username already exists");
        }

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already exists");
        }
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        log.info("User {} updated", username);
        return user;
    }

    @Transactional
    public User transferTeamLeaderRole(Long newLeaderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("Attempt to transfer team leader role to user {}", newLeaderId);
        User currentLeader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        User newLeader = getUserById(newLeaderId);

        if (!currentLeader.getTeam().getId().equals(newLeader.getTeam().getId())) {
            log.warn("Users {} and {} are not in the same team", username, newLeaderId);
            throw new ForbiddenException("Both users must be in the same team");
        }

        currentLeader.setRole(Role.MEMBER);
        newLeader.setRole(Role.TEAMLEADER);
        log.info("Team leader role transferred to user {}", newLeaderId);
        return newLeader;
    }
}

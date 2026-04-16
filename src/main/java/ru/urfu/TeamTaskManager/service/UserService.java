package ru.urfu.TeamTaskManager.service;


import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.Team;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.dto.request.UserRequest;
import ru.urfu.TeamTaskManager.enums.Role;
import ru.urfu.TeamTaskManager.repository.TeamRepository;
import ru.urfu.TeamTaskManager.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public User createUser(UserRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(Role.NONE)
                .build();
        return userRepository.save(user);
    }

    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public List<User> getUsersByTeam(Long teamId) {
        return userRepository.findByTeamId(teamId);
    }

    @Transactional
    public User assignUserToTeam(Long userId, Long teamId) {
        User user = getUserById(userId);
        if (user.getTeam() != null) {
            throw new RuntimeException("User is already in a team");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        user.setTeam(team);
        user.setRole(Role.MEMBER);
        return userRepository.save(user);
    }

    @Transactional
    public void removeUserFromTeam(Long teamId, Long userId) {
        User user = getUserById(userId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        if (user.getTeam() == null || !user.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("User is not in this team");
        }

        Role previousRole = user.getRole();
        user.setRole(Role.NONE);
        user.setTeam(null);
        userRepository.save(user);
        List<User> members = userRepository.findByTeamId(teamId);
        if (previousRole == Role.TEAMLEADER && members != null && !members.isEmpty()) {
            User newLeader = members.get(0);
            newLeader.setRole(Role.TEAMLEADER);
            userRepository.save(newLeader);
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    @Transactional
    public User updateUser(Long userId, UserRequest request) {
        User user = getUserById(userId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        return userRepository.save(user);
    }

    @Transactional
    public User transferTeamLeaderRole(Long currentLeaderId, Long newLeaderId) {
        User currentLeader = getUserById(currentLeaderId);
        User newLeader = getUserById(newLeaderId);

        if (currentLeader.getRole() != Role.TEAMLEADER) {
            throw new IllegalArgumentException("Only TeamLeader can transfer their role");
        }

        if (currentLeader.getTeam() == null || !currentLeader.getTeam().getId().equals(newLeader.getTeam().getId())) {
            throw new IllegalArgumentException("Both users must be in the same team");
        }

        if (newLeader.getRole() != Role.MEMBER) {
            throw new IllegalArgumentException("User must have MEMBER role to become TeamLeader");
        }

        currentLeader.setRole(Role.MEMBER);
        newLeader.setRole(Role.TEAMLEADER);

        userRepository.save(currentLeader);
        return userRepository.save(newLeader);
    }
}

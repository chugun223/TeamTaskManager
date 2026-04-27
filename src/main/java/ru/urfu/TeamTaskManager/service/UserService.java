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
import ru.urfu.TeamTaskManager.exception.ConflictException;
import ru.urfu.TeamTaskManager.exception.ForbiddenException;
import ru.urfu.TeamTaskManager.exception.NotFoundException;
import ru.urfu.TeamTaskManager.exception.ValidationException;
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
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' already exists");
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
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    public List<User> getUsersByTeam(Long teamId) {
        if(!teamRepository.existsById(teamId)) {
            throw new NotFoundException("Team not found with id: " + teamId);
        }
        return userRepository.findByTeamId(teamId);
    }

    @Transactional
    public User assignUserToTeam(Long userId, Long teamId) {
        User user = getUserById(userId);
        if (user.getTeam() != null) {
            throw new ConflictException("User is already in a team");
        }

        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found with id: " + teamId));

        user.setTeam(team);
        user.setRole(Role.MEMBER);
        return user;
    }

    @Transactional
    public void removeUserFromTeam(Long teamId, Long userId) {
        User user = getUserById(userId);
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException("Team not found with id: " + teamId);
        }

        if (user.getTeam() == null || !user.getTeam().getId().equals(teamId)) {
            throw new ForbiddenException("User is not in this team");
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
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    @Transactional
    public User updateUser(Long userId, UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' already exists");
        }
        User user = getUserById(userId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        return user;
    }

    @Transactional
    public User transferTeamLeaderRole(Long currentLeaderId, Long newLeaderId) {
        User currentLeader = getUserById(currentLeaderId);
        User newLeader = getUserById(newLeaderId);

        if (currentLeader.getRole() != Role.TEAMLEADER) {
            throw new ForbiddenException("Only TeamLeader can transfer their role");
        }

        if (currentLeader.getTeam() == null || !currentLeader.getTeam().getId().equals(newLeader.getTeam().getId())) {
            throw new ForbiddenException("Both users must be in the same team");
        }

        if (newLeader.getRole() != Role.MEMBER) {
            throw new ValidationException("User must have MEMBER role to become TeamLeader");
        }

        currentLeader.setRole(Role.MEMBER);
        newLeader.setRole(Role.TEAMLEADER);

        return newLeader;
    }
}

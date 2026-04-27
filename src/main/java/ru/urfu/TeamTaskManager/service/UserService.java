package ru.urfu.TeamTaskManager.service;


import lombok.AllArgsConstructor;
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

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;

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
                .password(passwordEncoder.encode(request.getPassword()))
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        User user = getUserById(userId);
        if(leader.getTeam() == null || !leader.getTeam().getId().equals(teamId)) {
            throw new ForbiddenException("Team leaders can only assign users to their own team");
        }
        if (user.getTeam() != null) {
            throw new ConflictException("User is already in a team");
        }

        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found with id: " + teamId));

        user.setTeam(team);
        user.setRole(Role.MEMBER);
        return user;
    }

    @Transactional
    public void removeUserFromTeam(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        User user = getUserById(userId);
        if(currentUser.getTeam() == null){
            throw new ForbiddenException("You are not in any team");
        }
        if(user.getTeam() == null) {
            throw new ConflictException("User is not in any team");
        }
        Long teamId = user.getTeam().getId();
        if(currentUser.getRole() == Role.TEAMLEADER && !currentUser.getTeam().getId().equals(teamId)) {
            throw new ForbiddenException("Team leaders can only remove users from their own team");
        }
        if(currentUser.getRole() == Role.MEMBER && !currentUser.getId().equals(userId)) {
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
            Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found with id: " + teamId));
            teamRepository.delete(team);
        }
    }

    @Transactional
    public void deleteUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
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
    }

    @Transactional
    public User updateUser(UserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        return user;
    }

    @Transactional
    public User transferTeamLeaderRole(Long newLeaderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentLeader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        User newLeader = getUserById(newLeaderId);

        if (!currentLeader.getTeam().getId().equals(newLeader.getTeam().getId())) {
            throw new ForbiddenException("Both users must be in the same team");
        }

        currentLeader.setRole(Role.MEMBER);
        newLeader.setRole(Role.TEAMLEADER);

        return newLeader;
    }
}

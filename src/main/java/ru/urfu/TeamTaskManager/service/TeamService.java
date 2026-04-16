package ru.urfu.TeamTaskManager.service;


import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.Team;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.dto.request.TeamRequest;
import ru.urfu.TeamTaskManager.enums.Role;
import ru.urfu.TeamTaskManager.repository.TeamRepository;
import ru.urfu.TeamTaskManager.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Transactional
    public Team createTeam(Long userId, TeamRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be null or empty");
        }
        User teamCreator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(teamCreator.getTeam() != null) {
            throw new IllegalArgumentException("User is already a member of another team");
        }
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Team savedTeam = teamRepository.save(team);

        teamCreator.setTeam(savedTeam);
        teamCreator.setRole(Role.TEAMLEADER);
        userRepository.save(teamCreator);

        return savedTeam;
    }

    public Page<Team> getAllTeams(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return teamRepository.findAll(pageable);
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = getTeamById(teamId);
        List<User> members = userRepository.findByTeamId(teamId);
        for (User user : members) {
            user.setTeam(null);
            user.setRole(Role.NONE);
            userRepository.save(user);
        }
        teamRepository.delete(team);
    }

    public List<User> getTeamMembers(Long teamId) {
        Team team = getTeamById(teamId);
        return team.getMembers();
    }

    @Transactional
    public Team updateTeam(Long teamId, TeamRequest request) {
        Team team = getTeamById(teamId);
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        return teamRepository.save(team);
    }
}

package ru.urfu.TeamTaskManager.service;


import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.TeamTaskManager.domain.Team;
import ru.urfu.TeamTaskManager.domain.User;
import ru.urfu.TeamTaskManager.dto.request.TeamRequest;
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
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Transactional
    public Team createTeam(TeamRequest request) {
        log.info("Attempt to create team with name: {}", request.getName());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User teamCreator = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        if(teamCreator.getTeam() != null) {
            log.warn("User {} is already a member of another team", username);
            throw new ConflictException("User is already a member of another team");
        }
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Team savedTeam = teamRepository.save(team);

        teamCreator.setTeam(savedTeam);
        teamCreator.setRole(Role.TEAMLEADER);
        log.info("Team created successfully with id: {}", savedTeam.getId());
        return savedTeam;
    }

    public Page<Team> getAllTeams(int page, int size) {
        log.info("Getting all teams with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return teamRepository.findAll(pageable);
    }

    public Team getTeamById(Long id) {
        log.info("Getting team by id: {}", id);
        return teamRepository.findById(id).orElseThrow(() -> {
            log.error("Team not found with id: {}", id);
            return new NotFoundException("Team not found with id: " + id);
        });
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        log.info("Attempt to delete team with id: {}", teamId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        Team team = getTeamById(teamId);
        if (!team.equals(leader.getTeam())) {
            log.warn("User {} cannot delete team {}", username, teamId);
            throw new ForbiddenException("Only team leaders can delete their teams");
        }
        List<User> members = userRepository.findByTeamId(teamId);
        for (User user : members) {
            user.setTeam(null);
            user.setRole(Role.NONE);
        }
        teamRepository.delete(team);
        log.info("Team {} deleted", teamId);
    }

    public List<User> getTeamMembers(Long teamId) {
        log.info("Getting team members for team id: {}", teamId);
        Team team = getTeamById(teamId);
        return team.getMembers();
    }

    @Transactional
    public Team updateTeam(Long teamId, TeamRequest request) {
        log.info("Attempt to update team with id: {}", teamId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("User not found with username: {}", username);
            return new NotFoundException("User not found with username: " + username);
        });
        Team team = getTeamById(teamId);
        if (!team.equals(leader.getTeam())) {
            log.warn("User {} cannot update team {}", username, teamId);
            throw new ForbiddenException("Only team leaders can update their teams");
        }
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        log.info("Team {} updated", teamId);
        return teamRepository.save(team);
    }
}

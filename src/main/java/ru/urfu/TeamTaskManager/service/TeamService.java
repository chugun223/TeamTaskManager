package ru.urfu.TeamTaskManager.service;


import lombok.AllArgsConstructor;
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

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Transactional
    public Team createTeam(TeamRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User teamCreator = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        if(teamCreator.getTeam() != null) {
            throw new ConflictException("User is already a member of another team");
        }
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Team savedTeam = teamRepository.save(team);

        teamCreator.setTeam(savedTeam);
        teamCreator.setRole(Role.TEAMLEADER);

        return savedTeam;
    }

    public Page<Team> getAllTeams(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return teamRepository.findAll(pageable);
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new NotFoundException("Team not found with id: " + id));
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        Team team = getTeamById(teamId);
        if (!team.equals(leader.getTeam())) {
            throw new ForbiddenException("Only team leaders can delete their teams");
        }
        List<User> members = userRepository.findByTeamId(teamId);
        for (User user : members) {
            user.setTeam(null);
            user.setRole(Role.NONE);
        }
        teamRepository.delete(team);
    }

    public List<User> getTeamMembers(Long teamId) {
        Team team = getTeamById(teamId);
        return team.getMembers();
    }

    @Transactional
    public Team updateTeam(Long teamId, TeamRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User leader = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        Team team = getTeamById(teamId);
        if (!team.equals(leader.getTeam())) {
            throw new ForbiddenException("Only team leaders can update their teams");
        }
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        return teamRepository.save(team);
    }
}

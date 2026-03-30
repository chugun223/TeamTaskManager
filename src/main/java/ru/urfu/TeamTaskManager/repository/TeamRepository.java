package ru.urfu.TeamTaskManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.urfu.TeamTaskManager.domain.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
}

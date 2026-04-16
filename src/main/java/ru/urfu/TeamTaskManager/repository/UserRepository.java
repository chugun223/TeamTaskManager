package ru.urfu.TeamTaskManager.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.urfu.TeamTaskManager.domain.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByTeamId(Long teamId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
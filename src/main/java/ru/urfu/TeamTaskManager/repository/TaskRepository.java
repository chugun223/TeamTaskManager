package ru.urfu.TeamTaskManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.urfu.TeamTaskManager.domain.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}

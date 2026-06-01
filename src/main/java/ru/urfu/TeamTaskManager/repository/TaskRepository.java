package ru.urfu.TeamTaskManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.urfu.TeamTaskManager.domain.Task;
import ru.urfu.TeamTaskManager.domain.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedUserId(Long userId);

    @Query("SELECT t FROM Task t WHERE t.deadline BETWEEN :start AND :end " + "AND t.taskStatus != 'DONE' AND t.deadlineNotified = false")
    List<Task> findByDeadlineBetweenAndTaskStatusNot(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

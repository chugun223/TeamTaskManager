package ru.urfu.TeamTaskManager.domain;

import jakarta.persistence.*;
import lombok.*;
import ru.urfu.TeamTaskManager.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(name = "tasks")
@Setter
@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private LocalDateTime deadline;

    private LocalDateTime created_at;

    private LocalDateTime updated_at;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;

    @Column(name = "deadline_notified")
    private boolean deadlineNotified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    User assignedUser;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<File> files = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        created_at = LocalDateTime.now();
        taskStatus = TaskStatus.NEW;
    }

    @PreUpdate
    private void onUpdate() {
        updated_at = LocalDateTime.now();
    }
}

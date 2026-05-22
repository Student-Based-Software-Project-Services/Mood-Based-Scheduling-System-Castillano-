package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MoodLog {

    private int id;
    private int userId;
    private String mood;
    private LocalDate loggedDate;
    private LocalDateTime createdAt;

    // For History display (not stored in DB — computed from JOIN)
    private int tasksCompleted;
    private String taskTitles;

    public MoodLog() {
    }

    public MoodLog(int userId, String mood, LocalDate loggedDate) {
        this.userId = userId;
        this.mood = mood;
        this.loggedDate = loggedDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public LocalDate getLoggedDate() {
        return loggedDate;
    }

    public void setLoggedDate(LocalDate loggedDate) {
        this.loggedDate = loggedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public String getTaskTitles() {
        return taskTitles;
    }

    public void setTaskTitles(String taskTitles) {
        this.taskTitles = taskTitles;
    }
}

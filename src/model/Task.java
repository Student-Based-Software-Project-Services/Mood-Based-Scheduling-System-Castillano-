package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Task {

    private int id;
    private int userId;
    private String title;
    private String description;
    private String moodTag;
    private String status; // "pending" or "done"
    private LocalDate scheduledDate;
    private LocalDateTime createdAt;

    public Task() {
    }

    public Task(int userId, String title, String description, String moodTag, LocalDate scheduledDate) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.moodTag = moodTag;
        this.scheduledDate = scheduledDate;
    }

    // Getters and Setters
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMoodTag() {
        return moodTag;
    }

    public void setMoodTag(String moodTag) {
        this.moodTag = moodTag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

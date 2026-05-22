package util;

import java.sql.*;

public class RepoManager {
    
    private  static RepoManager instance;
    
    private final UserRepository userRepo;
    private final TaskRepository taskRepo;
    private final MoodLogRepository moodLogRepo;

    private RepoManager() {
        Connection connection = DBConnection.getConnection();
        this.userRepo = new UserRepository(connection);
        this.taskRepo = new TaskRepository(connection);
        this.moodLogRepo = new MoodLogRepository(connection);
    }
    
    public static RepoManager getInstance() {
        if (instance == null) {
            return instance = new RepoManager();
        }
        return instance;
    }

    public UserRepository getUserRepo() {
        return userRepo;
    }
    
    public TaskRepository getTaskRepo() {
        return taskRepo;
    }
    
    public MoodLogRepository getMoodLogRepo() {
        return moodLogRepo;
    }
}

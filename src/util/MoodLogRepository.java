package util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.MoodLog;

public class MoodLogRepository {

    private final Connection connection;

    public MoodLogRepository(Connection connection) {
        this.connection = connection;
    }

    // INSERT mood log for today
    public boolean save(MoodLog log) {
        String sql = "INSERT INTO mood_log (user_id, mood, logged_date) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, log.getUserId());
            stmt.setString(2, log.getMood());
            stmt.setDate(3, Date.valueOf(log.getLoggedDate()));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // CHECK if user already logged mood today
    public boolean hasLoggedToday(int userId) {
        String sql = "SELECT id FROM mood_log WHERE user_id = ? AND logged_date = CURDATE()";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // GET all history entries with task counts (for History panel)
    public List<MoodLog> getHistory(int userId) {
        List<MoodLog> list = new ArrayList<>();
        String sql = """
            SELECT 
                ml.id, ml.mood, ml.logged_date, ml.created_at,
                COUNT(t.id) AS tasks_completed,
                GROUP_CONCAT(t.title SEPARATOR ' · ') AS task_titles
            FROM mood_log ml
            LEFT JOIN tasks t 
                ON t.user_id = ml.user_id 
                AND t.scheduled_date = ml.logged_date
                AND t.status = 'done'
            WHERE ml.user_id = ?
            GROUP BY ml.id, ml.mood, ml.logged_date, ml.created_at
            ORDER BY ml.logged_date DESC
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MoodLog log = new MoodLog();
                log.setId(rs.getInt("id"));
                log.setMood(rs.getString("mood"));
                log.setLoggedDate(rs.getDate("logged_date").toLocalDate());
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    log.setCreatedAt(createdAt.toLocalDateTime());
                }
                log.setTasksCompleted(rs.getInt("tasks_completed"));
                log.setTaskTitles(rs.getString("task_titles") != null ? rs.getString("task_titles") : "—");
                list.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // COUNT total days logged (for stat card)
    public int countDaysLogged(int userId) {
        String sql = "SELECT COUNT(*) FROM mood_log WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // GET most frequent mood (for Top Mood stat card)
    public String getTopMood(int userId) {
        String sql = """
            SELECT mood FROM mood_log 
            WHERE user_id = ? 
            GROUP BY mood 
            ORDER BY COUNT(*) DESC 
            LIMIT 1
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("mood");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "—";
    }
}

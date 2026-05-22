package frames;


import java.awt.CardLayout;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import model.MoodLog;
import model.Task;
import model.User;
import util.MoodLogRepository;
import util.RepoManager;
import util.TaskRepository;

public class Dashboard extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Dashboard.class.getName());

    // ─── FIELDS (declare at top of your Dashboard class) ─────────────────────────
    private String selectedMood = null;       // tracks which mood button is selected
    private boolean moodConfirmed = false;    // true only after confirmation dialog
    private JButton lastSelectedMoodBtn = null; // tracks highlighted button

    private final CardLayout cardLayout;
    private final User currentUser;
    private final DefaultTableModel tableModel;
    private final TaskRepository taskRepo;
    private final MoodLogRepository moodLogRepo;

    public Dashboard(User user) {

        this.currentUser = user;
        moodLogRepo = RepoManager.getInstance().getMoodLogRepo();

        // Check if user already confirmed a mood today
        if (moodLogRepo.hasLoggedToday(currentUser.getId())) {
            moodConfirmed = true;
            // Optional: show a note that mood was already logged today
        }

        initComponents();

        tiredButton.addActionListener(e -> onMoodButtonClicked("Tired", tiredButton));
        hypedButton.addActionListener(e -> onMoodButtonClicked("Hyped", hypedButton));
        happyButton.addActionListener(e -> onMoodButtonClicked("Happy", happyButton));
        stressedButton.addActionListener(e -> onMoodButtonClicked("Stressed", stressedButton));
        neutralButton.addActionListener(e -> onMoodButtonClicked("Neutral", neutralButton));
        focusedButton.addActionListener(e -> onMoodButtonClicked("Focused", focusedButton));

        this.setResizable(false);
        this.getContentPane().setBackground(new Color(60, 60, 60));

        cardLayout = (CardLayout) pnlContent.getLayout();
        tableModel = (DefaultTableModel) taskTable.getModel();
        taskRepo = RepoManager.getInstance().getTaskRepo();

        displayDateTime();
        greetFirstname.setText("Good day, " + currentUser.getFirstname() + "!");
        displayFullname.setText(currentUser.getFirstname() + " " + currentUser.getLastname());
    }

    public final void displayDateTime() {

        Timer timer = new Timer(1000, e -> {

            LocalDateTime now = LocalDateTime.now();

            DateTimeFormatter formatter
                = DateTimeFormatter.ofPattern("EEE, MMMM dd, hh:mm a");

            displayTimestamp.setText(now.format(formatter));
            displayTimestamp1.setText(now.format(formatter));
        });

        timer.start();
    }

    private void loadTasks() {
        tableModel.setRowCount(0); // clear existing rows

        List<Task> tasks = taskRepo.findAllByUser(currentUser.getId());
        for (Task task : tasks) {
            tableModel.addRow(new Object[]{
                task.getId(), // hidden col 0 — store ID
                task.getTitle(), // col 1 — Title
                task.getMoodTag(), // col 2 — Mood
                task.getStatus() // col 3 — Status
            });
        }

        int remaining = taskRepo.countRemaining(currentUser.getId());
        int complete = taskRepo.countComplete(currentUser.getId());
        summaryLabel.setText(remaining + " remaining, " + complete + " complete.");
    }

// ─── HELPER: get selected Task from table ────────────────────────────────────
    private Task getSelectedTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task first.");
            return null;
        }
        int taskId = (int) tableModel.getValueAt(selectedRow, 0);
        return taskRepo.findById(taskId);
    }

    private void loadHistory() {
        // ── Stat cards ──────────────────────────────────────
        int tasksDone = taskRepo.countComplete(currentUser.getId());
        int daysLogged = moodLogRepo.countDaysLogged(currentUser.getId());
        String topMood = moodLogRepo.getTopMood(currentUser.getId());

        tasksDoneLabel.setText(String.valueOf(tasksDone));
        daysLoggedLabel.setText(String.valueOf(daysLogged));
        topMoodLabel.setText(topMood);

        // ── Recent entries table/list ────────────────────────
        List<MoodLog> history = moodLogRepo.getHistory(currentUser.getId());
        DefaultTableModel model = (DefaultTableModel) historyTable.getModel();
        model.setRowCount(0);

        for (MoodLog log : history) {
            model.addRow(new Object[]{
                log.getMood(), // Mood badge
                log.getTasksCompleted() + " tasks completed", // Count
                log.getTaskTitles(), // Task names
                log.getLoggedDate().toString() // Date
            });
        }
    }

    // ─── MOOD BUTTON CLICKED ──────────────────────────────────────────────────────
    private void onMoodButtonClicked(String mood, JButton clickedBtn) {

        // If mood already confirmed today, don't allow re-picking
        if (moodConfirmed) {
            JOptionPane.showMessageDialog(this,
                "You have already set your mood for today.",
                "Mood Already Logged",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Highlight selected button, reset previous
        if (lastSelectedMoodBtn != null) {
            lastSelectedMoodBtn.setBackground(Color.WHITE);
            lastSelectedMoodBtn.setForeground(Color.BLACK);
        }
        clickedBtn.setBackground(new java.awt.Color(70, 130, 180));
        clickedBtn.setForeground(java.awt.Color.WHITE);
        clickedBtn.setOpaque(true);

        selectedMood = mood;
        lastSelectedMoodBtn = clickedBtn;

        // Ask user to confirm their mood choice
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "You selected: " + mood + "\nIs this how you're feeling today?",
            "Confirm Mood",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // Save mood to database
            MoodLog log = new MoodLog(currentUser.getId(), mood, java.time.LocalDate.now());
            boolean saved = moodLogRepo.save(log);

            if (saved) {
                moodConfirmed = true;
                JOptionPane.showMessageDialog(this,
                    "Mood logged! Have a great day, " + currentUser.getFirstname() + "!",
                    "Mood Confirmed",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Reset if save failed
                selectedMood = null;
                clickedBtn.setBackground(null);
                clickedBtn.setForeground(null);
                lastSelectedMoodBtn = null;
                JOptionPane.showMessageDialog(this,
                    "Failed to save mood. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }

        } else {
            // User clicked No — reset selection
            selectedMood = null;
            clickedBtn.setBackground(Color.WHITE);
            clickedBtn.setForeground(Color.BLACK);
            lastSelectedMoodBtn = null;
        }
    }

    // ─── SUGGEST TASKS BUTTON ─────────────────────────────────────────────────────
    private void onSuggestTasks() {
        if (!moodConfirmed) {
            JOptionPane.showMessageDialog(this,
                "Please pick and confirm your mood first!",
                "Pick a Mood",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // TODO: populate the [Task Suggestion] panel on the right
        loadSuggestedTasks(selectedMood);
    }

    // ─── SIDEBAR NAV GUARD ────────────────────────────────────────────────────────
    private void onNavClicked(String destination) {
        if (!moodConfirmed) {
            JOptionPane.showMessageDialog(this,
                "Please pick your mood for today first before navigating!",
                "Pick a Mood",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Navigate to the correct panel
        switch (destination) {
            case "dashboard" -> {
                // TODO: show dashboard panel
                cardLayout.show(pnlContent, "DASHBOARD");
            }
            case "schedule" -> {
                // TODO: show schedule panel
                loadScheduleTable();
                cardLayout.show(pnlContent, "SCHEDULE");
            }
            case "myTask" -> {
                // TODO: show my task panel
                loadTasks();
                cardLayout.show(pnlContent, "MY_TASK");
            }
            case "history" -> {
                // TODO: show history panel
                loadHistory();
                cardLayout.show(pnlContent, "HISTORY");
            }
        }
    }

    // ─── LOG OUT ──────────────────────────────────────────────────────────────────
    private void onLogOut() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out?",
            "Log Out",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            // TODO: new LoginFrame().setVisible(true);
            new AuthUI().setVisible(true);
        }
    }

    private void loadSuggestedTasks(String mood) {
        DefaultTableModel model = (DefaultTableModel) suggestionTable.getModel();
        model.setRowCount(0); // clear previous results

        List<Task> tasks = taskRepo.findByMoodTag(currentUser.getId(), mood);

        if (tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No tasks found for mood: " + mood + ".\n"
                + "Try adding tasks with this mood tag first.",
                "No Suggestions",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (Task task : tasks) {
            model.addRow(new Object[]{
                task.getTitle(),
                task.getMoodTag(),
                task.getStatus(),
                task.getScheduledDate() != null ? task.getScheduledDate().toString() : "—"
            });
        }
    }
    
    private void loadScheduleTable() {
        DefaultTableModel model = (DefaultTableModel) taskSchedulesTable.getModel();
        model.setRowCount(0); // clear existing rows

        List<Task> tasks = taskRepo.findAllByUser(currentUser.getId());
        for (Task task : tasks) {
            model.addRow(new Object[]{
                task.getTitle(), // col 1 — Title
                task.getScheduledDate().toString()
            });
        }

        int remaining = taskRepo.countRemaining(currentUser.getId());
        int complete = taskRepo.countComplete(currentUser.getId());
        summaryLabel.setText(remaining + " remaining, " + complete + " complete.");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnDashboard = new javax.swing.JButton();
        btnSchedule = new javax.swing.JButton();
        btnMyTask = new javax.swing.JButton();
        btnHistory = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        displayFullname = new javax.swing.JLabel();
        btnLogOut = new javax.swing.JButton();
        pnlContent = new javax.swing.JPanel();
        pnlDashboard = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        greetFirstname = new javax.swing.JLabel();
        displayTimestamp = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        tiredButton = new javax.swing.JButton();
        hypedButton = new javax.swing.JButton();
        happyButton = new javax.swing.JButton();
        stressedButton = new javax.swing.JButton();
        neutralButton = new javax.swing.JButton();
        focusedButton = new javax.swing.JButton();
        onSuggestTask = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        suggestionTable = new javax.swing.JTable();
        jLabel11 = new javax.swing.JLabel();
        pnlSchedule = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        displayTimestamp1 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        taskSchedulesTable = new javax.swing.JTable();
        pnlMyTask = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        summaryLabel = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        taskTable = new javax.swing.JTable();
        onAdd = new javax.swing.JButton();
        onRemove = new javax.swing.JButton();
        onView = new javax.swing.JButton();
        onFinish = new javax.swing.JButton();
        pnlHistory = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        displayTimestamp2 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        historyTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        tasksDoneLabel = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        daysLoggedLabel = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        topMoodLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Dashboard");
        setBackground(new java.awt.Color(60, 60, 60));
        getContentPane().setLayout(new java.awt.BorderLayout(2, 2));

        jPanel1.setBackground(new java.awt.Color(60, 60, 60));

        btnDashboard.setBackground(new java.awt.Color(60, 60, 60));
        btnDashboard.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnDashboard.setForeground(new java.awt.Color(255, 255, 255));
        btnDashboard.setText("Dashboard");
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashboardActionPerformed(evt);
            }
        });

        btnSchedule.setBackground(new java.awt.Color(60, 60, 60));
        btnSchedule.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSchedule.setForeground(new java.awt.Color(255, 255, 255));
        btnSchedule.setText("Schedule");
        btnSchedule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnScheduleActionPerformed(evt);
            }
        });

        btnMyTask.setBackground(new java.awt.Color(60, 60, 60));
        btnMyTask.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMyTask.setForeground(new java.awt.Color(255, 255, 255));
        btnMyTask.setText("My Task");
        btnMyTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMyTaskActionPerformed(evt);
            }
        });

        btnHistory.setBackground(new java.awt.Color(60, 60, 60));
        btnHistory.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnHistory.setForeground(new java.awt.Color(255, 255, 255));
        btnHistory.setText("History");
        btnHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHistoryActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("@MoodTask");

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("User");

        displayFullname.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        displayFullname.setForeground(new java.awt.Color(255, 255, 255));
        displayFullname.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        displayFullname.setText("[name]");

        btnLogOut.setBackground(new java.awt.Color(60, 60, 60));
        btnLogOut.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLogOut.setForeground(new java.awt.Color(255, 255, 255));
        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addComponent(btnSchedule, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnDashboard, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMyTask, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnHistory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(displayFullname, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(btnLogOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addGap(27, 27, 27)
                .addComponent(btnDashboard, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSchedule, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnMyTask, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnHistory, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 76, Short.MAX_VALUE)
                .addComponent(btnLogOut)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(displayFullname)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.LINE_START);

        pnlContent.setLayout(new java.awt.CardLayout());

        pnlDashboard.setBackground(new java.awt.Color(60, 60, 60));
        pnlDashboard.setLayout(new java.awt.BorderLayout(3, 3));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setForeground(new java.awt.Color(255, 255, 255));

        greetFirstname.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        greetFirstname.setForeground(new java.awt.Color(0, 0, 0));
        greetFirstname.setText("Good day, [name]");

        displayTimestamp.setForeground(new java.awt.Color(0, 0, 0));
        displayTimestamp.setText("Thu, May 7, 8:00 AM");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(greetFirstname)
                    .addComponent(displayTimestamp, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(251, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addComponent(greetFirstname)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(displayTimestamp)
                .addGap(22, 22, 22))
        );

        pnlDashboard.add(jPanel2, java.awt.BorderLayout.PAGE_START);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setForeground(new java.awt.Color(255, 255, 255));

        tiredButton.setText("Tired");

        hypedButton.setText("Hyped");

        happyButton.setText("Happy");

        stressedButton.setText("Stressed");

        neutralButton.setText("Neutral");

        focusedButton.setText("Focused");

        onSuggestTask.setText("Suggest tasks");
        onSuggestTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onSuggestTaskActionPerformed(evt);
            }
        });

        jLabel10.setText("How are you feeling?");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(141, 141, 141)
                        .addComponent(jSeparator2))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(tiredButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hypedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(happyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(stressedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(neutralButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(focusedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator3)
                    .addComponent(onSuggestTask, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tiredButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hypedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(happyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stressedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(neutralButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(focusedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(onSuggestTask, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(10, Short.MAX_VALUE))
        );

        pnlDashboard.add(jPanel3, java.awt.BorderLayout.LINE_START);

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setForeground(new java.awt.Color(255, 255, 255));

        suggestionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title", "Mood", "Status", "Schedule"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        suggestionTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane4.setViewportView(suggestionTable);
        if (suggestionTable.getColumnModel().getColumnCount() > 0) {
            suggestionTable.getColumnModel().getColumn(0).setResizable(false);
            suggestionTable.getColumnModel().getColumn(1).setResizable(false);
            suggestionTable.getColumnModel().getColumn(2).setResizable(false);
            suggestionTable.getColumnModel().getColumn(3).setResizable(false);
        }

        jLabel11.setText("Suggested Tasks");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE))
        );

        pnlDashboard.add(jPanel4, java.awt.BorderLayout.CENTER);

        pnlContent.add(pnlDashboard, "DASHBOARD");

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 0, 0));
        jLabel4.setText("Schedule");

        displayTimestamp1.setForeground(new java.awt.Color(0, 0, 0));
        displayTimestamp1.setText("Thu, May 7, 8:00 AM");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(displayTimestamp1, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(displayTimestamp1)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        taskSchedulesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title", "Schedule"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        taskSchedulesTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(taskSchedulesTable);
        if (taskSchedulesTable.getColumnModel().getColumnCount() > 0) {
            taskSchedulesTable.getColumnModel().getColumn(0).setResizable(false);
            taskSchedulesTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            taskSchedulesTable.getColumnModel().getColumn(1).setResizable(false);
        }

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout pnlScheduleLayout = new javax.swing.GroupLayout(pnlSchedule);
        pnlSchedule.setLayout(pnlScheduleLayout);
        pnlScheduleLayout.setHorizontalGroup(
            pnlScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlScheduleLayout.setVerticalGroup(
            pnlScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlScheduleLayout.createSequentialGroup()
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pnlContent.add(pnlSchedule, "SCHEDULE");

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 0, 0));
        jLabel3.setText("My Task");

        summaryLabel.setForeground(new java.awt.Color(0, 0, 0));
        summaryLabel.setText("0 remaining, 0 complete.");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(summaryLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(summaryLabel)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));

        taskTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Title", "Mood", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        taskTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(taskTable);
        if (taskTable.getColumnModel().getColumnCount() > 0) {
            taskTable.getColumnModel().getColumn(0).setResizable(false);
            taskTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            taskTable.getColumnModel().getColumn(1).setResizable(false);
            taskTable.getColumnModel().getColumn(2).setResizable(false);
            taskTable.getColumnModel().getColumn(3).setResizable(false);
        }

        onAdd.setText("Add");
        onAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAddActionPerformed(evt);
            }
        });

        onRemove.setText("Remove");
        onRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onRemoveActionPerformed(evt);
            }
        });

        onView.setText("View");
        onView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onViewActionPerformed(evt);
            }
        });

        onFinish.setText("Finish");
        onFinish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onFinishActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(onFinish)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 109, Short.MAX_VALUE)
                .addComponent(onView)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(onRemove)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(onAdd)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(onAdd)
                    .addComponent(onRemove)
                    .addComponent(onView)
                    .addComponent(onFinish))
                .addContainerGap())
        );

        javax.swing.GroupLayout pnlMyTaskLayout = new javax.swing.GroupLayout(pnlMyTask);
        pnlMyTask.setLayout(pnlMyTaskLayout);
        pnlMyTaskLayout.setHorizontalGroup(
            pnlMyTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlMyTaskLayout.setVerticalGroup(
            pnlMyTaskLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMyTaskLayout.createSequentialGroup()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pnlContent.add(pnlMyTask, "MY_TASK");

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        jLabel5.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(0, 0, 0));
        jLabel5.setText("History");

        displayTimestamp2.setForeground(new java.awt.Color(0, 0, 0));
        displayTimestamp2.setText("Your mood & task log");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(displayTimestamp2, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(220, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(displayTimestamp2, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        historyTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Mood", "Task Complete", "Tasks", "Logged Date"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        historyTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(historyTable);
        if (historyTable.getColumnModel().getColumnCount() > 0) {
            historyTable.getColumnModel().getColumn(0).setResizable(false);
            historyTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            historyTable.getColumnModel().getColumn(1).setResizable(false);
        }

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        jLabel7.setText("Task Complete");

        tasksDoneLabel.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        tasksDoneLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tasksDoneLabel.setText("0");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(tasksDoneLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tasksDoneLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        jLabel8.setText("Days Logged");

        daysLoggedLabel.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        daysLoggedLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        daysLoggedLabel.setText("0");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(daysLoggedLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(daysLoggedLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));
        jPanel7.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        jLabel9.setText("Top Mood");

        topMoodLabel.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        topMoodLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        topMoodLabel.setText("Happy");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(topMoodLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(topMoodLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnlHistoryLayout = new javax.swing.GroupLayout(pnlHistory);
        pnlHistory.setLayout(pnlHistoryLayout);
        pnlHistoryLayout.setHorizontalGroup(
            pnlHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
        );
        pnlHistoryLayout.setVerticalGroup(
            pnlHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHistoryLayout.createSequentialGroup()
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlContent.add(pnlHistory, "HISTORY");

        getContentPane().add(pnlContent, java.awt.BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        onNavClicked("dashboard");
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void btnScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnScheduleActionPerformed
        onNavClicked("schedule");
    }//GEN-LAST:event_btnScheduleActionPerformed

    private void btnMyTaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMyTaskActionPerformed
        onNavClicked("myTask");
    }//GEN-LAST:event_btnMyTaskActionPerformed

    private void btnHistoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHistoryActionPerformed
        onNavClicked("history");
    }//GEN-LAST:event_btnHistoryActionPerformed

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        onLogOut();
    }//GEN-LAST:event_btnLogOutActionPerformed

    private void onAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAddActionPerformed
        // TODO: new AddTaskFrame(currentUser).setVisible(true);
        new AddTaskDialog(this, true, currentUser).setVisible(true);
        // After the frame closes, refresh the table:
        loadTasks();
    }//GEN-LAST:event_onAddActionPerformed

    private void onRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onRemoveActionPerformed
        Task selected = getSelectedTask();
        if (selected == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to remove \"" + selected.getTitle() + "\"?",
            "Confirm Remove",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = taskRepo.delete(selected.getId());
            if (success) {
                JOptionPane.showMessageDialog(this, "Task removed.");
                loadTasks();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove task.");
            }
        }
    }//GEN-LAST:event_onRemoveActionPerformed

    private void onFinishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onFinishActionPerformed
        Task selected = getSelectedTask();
        if (selected == null) {
            return;
        }

        selected.setStatus("done");
        taskRepo.update(selected);

        // After the frame closes, refresh the table:
        loadTasks();
    }//GEN-LAST:event_onFinishActionPerformed

    private void onViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onViewActionPerformed
        Task selected = getSelectedTask();
        if (selected == null) {
            return;
        }

        // TODO: new ViewTaskFrame(selected).setVisible(true);
        new ViewTaskDialog(this, true, selected).setVisible(true);
    }//GEN-LAST:event_onViewActionPerformed

    private void onSuggestTaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onSuggestTaskActionPerformed
        onSuggestTasks();
    }//GEN-LAST:event_onSuggestTaskActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnHistory;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnMyTask;
    private javax.swing.JButton btnSchedule;
    private javax.swing.JLabel daysLoggedLabel;
    private javax.swing.JLabel displayFullname;
    private javax.swing.JLabel displayTimestamp;
    private javax.swing.JLabel displayTimestamp1;
    private javax.swing.JLabel displayTimestamp2;
    private javax.swing.JButton focusedButton;
    private javax.swing.JLabel greetFirstname;
    private javax.swing.JButton happyButton;
    private javax.swing.JTable historyTable;
    private javax.swing.JButton hypedButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JButton neutralButton;
    private javax.swing.JButton onAdd;
    private javax.swing.JButton onFinish;
    private javax.swing.JButton onRemove;
    private javax.swing.JButton onSuggestTask;
    private javax.swing.JButton onView;
    private javax.swing.JPanel pnlContent;
    private javax.swing.JPanel pnlDashboard;
    private javax.swing.JPanel pnlHistory;
    private javax.swing.JPanel pnlMyTask;
    private javax.swing.JPanel pnlSchedule;
    private javax.swing.JButton stressedButton;
    private javax.swing.JTable suggestionTable;
    private javax.swing.JLabel summaryLabel;
    private javax.swing.JTable taskSchedulesTable;
    private javax.swing.JTable taskTable;
    private javax.swing.JLabel tasksDoneLabel;
    private javax.swing.JButton tiredButton;
    private javax.swing.JLabel topMoodLabel;
    // End of variables declaration//GEN-END:variables
}

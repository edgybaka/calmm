import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.table.DefaultTableModel;

public class CalmCanvasApp {
    private static Connection connection;
    private static String currentUser;

    static JLabel ImageLogo;


    public static void main(String[] args) {
        initializeDatabase();

        SwingUtilities.invokeLater(() -> {
            createAndShowLoginPage();
        });
    }

    private static void initializeDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3307/calmCanvasDB", "root", "root123");

            Statement statement = connection.createStatement();

            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS calmCanvasDB");

            // Switch to the calmCanvasDB database
            statement.executeUpdate("USE calmCanvasDB");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS User (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(255) UNIQUE, " +
                    "password VARCHAR(255))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Entry (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "userId INT, " +
                    "entryDate DATE, " +
                    "content TEXT, " +
                    "FOREIGN KEY(userId) REFERENCES User(id))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Goal (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "userId INT, " +
                    "goalText TEXT, " +
                    "isDone BOOLEAN, " +
                    "FOREIGN KEY(userId) REFERENCES User(id))");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowLoginPage() {
        JFrame loginFrame = new JFrame("Welcome to Calm Canvas");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel loginPanel = createLoginPanel(loginFrame);
        loginFrame.getContentPane().add(loginPanel);

        loginFrame.setSize(500, 300);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    private static boolean authenticateUser(String username, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM User WHERE username=? AND password=?");
            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();

            return resultSet.next();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    private static JPanel createLoginPanel(JFrame loginFrame) {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BorderLayout());
        loginPanel.setBackground(new Color(82, 129, 206));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        ImageLogo = new JLabel(new ImageIcon("logo.png"));
        ImageLogo.setPreferredSize(new Dimension(120, 73));

        JTextField usernameInput = new JTextField(20);
        JPasswordField passwordInput = new JPasswordField(20);

        JButton loginButton = new JButton("Login");
        JButton signUpButton = new JButton("Sign Up");

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameInput.getText();
                String password = new String(passwordInput.getPassword());

                if (authenticateUser(username, password)) {
                    currentUser = username;
                    loginFrame.getContentPane().removeAll();
                    createAndShowMainPage(loginFrame);
                } else {
                    showAlert("Invalid login credentials");
                }
            }
        });

        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSignUpDialog(loginFrame);
            }
        });

        formPanel.add(ImageLogo);
        formPanel.add(new JLabel("Username:"));
        formPanel.add(usernameInput);
        formPanel.add(new JLabel("Password:"));
        formPanel.add(passwordInput);
        formPanel.add(loginButton);
        formPanel.add(signUpButton);
        formPanel.add(exitButton);

        loginPanel.add(formPanel, BorderLayout.CENTER);

        return loginPanel;
    }

    private static void showSignUpDialog(JFrame loginFrame) {
        JTextField usernameInput = new JTextField(20);
        JPasswordField passwordInput = new JPasswordField(20);

        JPanel signUpPanel = new JPanel();
        signUpPanel.setLayout(new BoxLayout(signUpPanel, BoxLayout.Y_AXIS));
        signUpPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        signUpPanel.add(new JLabel("Create an Account"));
        signUpPanel.add(new JLabel("Username:"));
        signUpPanel.add(usernameInput);
        signUpPanel.add(new JLabel("Password:"));
        signUpPanel.add(passwordInput);

        int result = JOptionPane.showConfirmDialog(loginFrame, signUpPanel, "Sign Up",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameInput.getText();
            String password = new String(passwordInput.getPassword());

            if (createUser(username, password)) {
                showAlert("Account created successfully. Please log in.");
            } else {
                showAlert("Error creating account. Please try again.");
            }
        }
    }

    private static boolean createUser(String username, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO User (username, password) VALUES (?, ?)");
            statement.setString(1, username);
            statement.setString(2, password);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void createAndShowMainPage(JFrame loginFrame) {
        JFrame mainFrame = new JFrame("Calm Canvas - Welcome, " + currentUser);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Journal Entries", createJournalPanel(mainFrame));
        tabbedPane.addTab("Goal Tracker", createGoalTrackerPanel(mainFrame));
        tabbedPane.addTab("Mood Tracker", createMoodTrackerPanel(mainFrame, currentUser));
        tabbedPane.addTab("Game", createGamePanel(mainFrame, currentUser)); // Add Game Panel

        mainFrame.getContentPane().add(tabbedPane);

        mainFrame.setSize(800, 600);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);

        // Close login frame after successful login
        loginFrame.dispose();
    }


    private static JPanel createJournalPanel(JFrame mainFrame) {
        JPanel journalPanel = new JPanel();
        journalPanel.setLayout(new BorderLayout());
        journalPanel.setBackground(new Color(186, 73, 78));
        journalPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 2, 10, 10));

        JButton viewEntriesButton = new JButton("View Entries");
        viewEntriesButton.addActionListener(e -> {
            viewEntries(mainFrame, currentUser);
        });

        JButton addEntryButton = new JButton("Add Entry");
        addEntryButton.addActionListener(e -> {
            addEntry(mainFrame, currentUser);
        });

        JButton editEntryButton = new JButton("Edit Entry");
        editEntryButton.addActionListener(e -> {
            editEntry(mainFrame, currentUser);
        });

        JButton deleteEntryButton = new JButton("Delete Entry");
        deleteEntryButton.addActionListener(e -> {
            deleteEntry(mainFrame, currentUser);
        });

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            mainFrame.dispose();
            createAndShowLoginPage();
        });

        buttonPanel.add(viewEntriesButton);
        buttonPanel.add(addEntryButton);
        buttonPanel.add(editEntryButton);
        buttonPanel.add(deleteEntryButton);
        buttonPanel.add(logoutButton);

        journalPanel.add(new JLabel("Welcome, " + currentUser + "!"), BorderLayout.NORTH);
        journalPanel.add(buttonPanel, BorderLayout.CENTER);

        return journalPanel;
    }

    private static void viewEntries(JFrame mainFrame, String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM Entry WHERE userId = (SELECT id FROM User WHERE username = ?)");
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            JTextArea entriesArea = new JTextArea();
            while (resultSet.next()) {
                entriesArea.append("Date: " + resultSet.getString("entryDate") + "\n"
                        + "Content: " + resultSet.getString("content") + "\n\n");
            }

            entriesArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(entriesArea);

            JOptionPane.showMessageDialog(mainFrame, scrollPane, "Journal Entries", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error retrieving entries.");
        }
    }

    private static void addEntry(JFrame mainFrame, String username) {
        JTextField entryDateField = new JTextField(10);
        JTextArea contentArea = new JTextArea(5, 20);

        JPanel addEntryPanel = new JPanel();
        addEntryPanel.setLayout(new BoxLayout(addEntryPanel, BoxLayout.Y_AXIS));
        addEntryPanel.add(new JLabel("Entry Date (yyyy-MM-dd):"));
        addEntryPanel.add(entryDateField);
        addEntryPanel.add(new JLabel("Journal Entry:"));
        addEntryPanel.add(new JScrollPane(contentArea));

        int result = JOptionPane.showConfirmDialog(mainFrame, addEntryPanel, "Add Entry",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date entryDate = dateFormat.parse(entryDateField.getText());

                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Entry (userId, entryDate, content) VALUES ((SELECT id FROM User WHERE username = ?), ?, ?)");
                statement.setString(1, username);
                statement.setDate(2, new java.sql.Date(entryDate.getTime()));
                statement.setString(3, contentArea.getText());

                statement.executeUpdate();
                showAlert("Entry added successfully.");

            } catch (SQLException | ParseException ex) {
                ex.printStackTrace();
                showAlert("Error adding entry.");
            }
        }
    }

    private static void editEntry(JFrame mainFrame, String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM Entry WHERE userId = (SELECT id FROM User WHERE username = ?)");
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            DefaultListModel<String> entriesListModel = new DefaultListModel<>();
            JList<String> entriesList = new JList<>(entriesListModel);

            while (resultSet.next()) {
                entriesListModel.addElement("Date: " + resultSet.getString("entryDate") + "\n"
                        + "Content: " + resultSet.getString("content") + "\n\n");
            }

            JScrollPane scrollPane = new JScrollPane(entriesList);
            JOptionPane.showMessageDialog(mainFrame, scrollPane, "Select Entry to Edit", JOptionPane.INFORMATION_MESSAGE);

            int selectedIndex = entriesList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedEntry = entriesListModel.getElementAt(selectedIndex);

                // Check if the selected entry has the expected structure
                String[] entryLines = selectedEntry.split("\n");
                if (entryLines.length >= 2) {
                    // Extract entry date from the selected entry string
                    String entryDate = entryLines[0].substring(6);

                    // Prompt the user for new content
                    JTextArea contentArea = new JTextArea(5, 20);
                    contentArea.setText(entryLines[1].substring(9));

                    JPanel editEntryPanel = new JPanel();
                    editEntryPanel.setLayout(new BoxLayout(editEntryPanel, BoxLayout.Y_AXIS));
                    editEntryPanel.add(new JLabel("Edit Entry:"));
                    editEntryPanel.add(new JLabel("Entry Date (yyyy-MM-dd): " + entryDate));
                    editEntryPanel.add(new JScrollPane(contentArea));

                    int editResult = JOptionPane.showConfirmDialog(mainFrame, editEntryPanel, "Edit Entry",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (editResult == JOptionPane.OK_OPTION) {
                        // Update the entry in the database
                        PreparedStatement updateStatement = connection.prepareStatement(
                                "UPDATE Entry SET content = ? WHERE userId = (SELECT id FROM User WHERE username = ?) " +
                                        "AND entryDate = ?");
                        updateStatement.setString(1, contentArea.getText());
                        updateStatement.setString(2, username);
                        updateStatement.setDate(3, java.sql.Date.valueOf(entryDate));

                        updateStatement.executeUpdate();
                        showAlert("Entry edited successfully.");
                    }
                } else {
                    showAlert("Selected entry does not have the expected structure.");
                }
            } else {
                showAlert("No entry selected for editing.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error retrieving entries.");
        }
    }


    private static void deleteEntry(JFrame mainFrame, String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM Entry WHERE userId = (SELECT id FROM User WHERE username = ?)");
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            DefaultListModel<String> entriesListModel = new DefaultListModel<>();
            JList<String> entriesList = new JList<>(entriesListModel);

            while (resultSet.next()) {
                entriesListModel.addElement("Date: " + resultSet.getString("entryDate") + "\n"
                        + "Content: " + resultSet.getString("content") + "\n\n");
            }

            JScrollPane scrollPane = new JScrollPane(entriesList);
            JOptionPane.showMessageDialog(mainFrame, scrollPane, "Select Entry to Delete", JOptionPane.INFORMATION_MESSAGE);

            int selectedIndex = entriesList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedEntry = entriesListModel.getElementAt(selectedIndex);

                // Extract entry date from the selected entry string
                String entryDate = selectedEntry.split("\n")[0].substring(6);

                try {
                    PreparedStatement deleteStatement = connection.prepareStatement(
                            "DELETE FROM Entry WHERE userId = (SELECT id FROM User WHERE username = ?) AND entryDate = ?");
                    deleteStatement.setString(1, username);
                    deleteStatement.setDate(2, java.sql.Date.valueOf(entryDate));

                    deleteStatement.executeUpdate();
                    showAlert("Entry deleted successfully.");

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Error deleting entry.");
                }
            } else {
                showAlert("No entry selected for deletion.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error retrieving entries.");
        }
    }

    private static JPanel createGoalTrackerPanel(JFrame mainFrame) {
        JPanel goalPanel = new JPanel();
        goalPanel.setLayout(new BoxLayout(goalPanel, BoxLayout.Y_AXIS));

        DefaultTableModel goalsTableModel = new DefaultTableModel();
        goalsTableModel.addColumn("Goal");
        goalsTableModel.addColumn("Status");

        JTable goalsTable = new JTable(goalsTableModel);

        loadGoals(currentUser, goalsTableModel);

        JButton addGoalButton = new JButton("Add Goal");
        addGoalButton.addActionListener(e -> {
            addGoal(mainFrame, currentUser, goalsTableModel);
        });

        JButton deleteGoalButton = new JButton("Delete Goal");
        deleteGoalButton.addActionListener(e -> {
            deleteGoal(mainFrame, currentUser, goalsTable.getSelectedRow(), goalsTableModel);
        });

        JButton markAsDoneButton = new JButton("Mark as Done");
        markAsDoneButton.addActionListener(e -> {
            markAsDone(currentUser, goalsTable.getSelectedRow(), true, goalsTableModel);
        });

        JButton markAsUndoneButton = new JButton("Mark as Undone");
        markAsUndoneButton.addActionListener(e -> {
            markAsDone(currentUser, goalsTable.getSelectedRow(), false, goalsTableModel);
        });

        goalPanel.add(new JLabel("Goals:"));
        goalPanel.add(new JScrollPane(goalsTable));
        goalPanel.add(addGoalButton);
        goalPanel.add(deleteGoalButton);
        goalPanel.add(markAsDoneButton);
        goalPanel.add(markAsUndoneButton);

        return goalPanel;
    }

    private static void markAsDone(String username, int selectedRowIndex, boolean isDone, DefaultTableModel goalsTableModel) {
        if (selectedRowIndex != -1) {
            try {
                String selectedGoal = (String) goalsTableModel.getValueAt(selectedRowIndex, 0);

                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE Goal SET isDone = ? WHERE userId = (SELECT id FROM User WHERE username = ?) AND goalText = ?");
                statement.setBoolean(1, isDone);
                statement.setString(2, username);
                statement.setString(3, selectedGoal);

                statement.executeUpdate();
                showAlert("Goal status updated successfully.");
                loadGoals(username, goalsTableModel);

            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Error updating goal status.");
            }
        } else {
            showAlert("No goal selected for status update.");
        }
    }

    private static void loadGoals(String username, DefaultTableModel goalsTableModel) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM Goal WHERE userId = (SELECT id FROM User WHERE username = ?)");
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            goalsTableModel.setRowCount(0); // Clear the table before loading new data

            while (resultSet.next()) {
                String goalText = resultSet.getString("goalText");
                boolean isDone = resultSet.getBoolean("isDone");

                goalsTableModel.addRow(new Object[]{goalText, isDone ? "Done" : "Not Done"});
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error retrieving goals.");
        }
    }

    private static void addGoal(JFrame mainFrame, String username, DefaultTableModel goalsTableModel) {
        JTextField goalTextField = new JTextField(20);

        JPanel addGoalPanel = new JPanel();
        addGoalPanel.setLayout(new BoxLayout(addGoalPanel, BoxLayout.Y_AXIS));
        addGoalPanel.add(new JLabel("Add Goal:"));
        addGoalPanel.add(goalTextField);

        int result = JOptionPane.showConfirmDialog(mainFrame, addGoalPanel, "Add Goal",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String goalText = goalTextField.getText();

                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Goal (userId, goalText, isDone) VALUES ((SELECT id FROM User WHERE username = ?), ?, ?)");
                statement.setString(1, username);
                statement.setString(2, goalText);
                statement.setBoolean(3, false);

                statement.executeUpdate();
                showAlert("Goal added successfully.");

                loadGoals(username, goalsTableModel); // Refresh the goals table

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error adding goal.");
            }
        }
    }

    private static void deleteGoal(JFrame mainFrame, String username, int selectedRow, DefaultTableModel goalsTableModel) {
        if (selectedRow != -1) {
            String goalText = (String) goalsTableModel.getValueAt(selectedRow, 0);

            try {
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM Goal WHERE userId = (SELECT id FROM User WHERE username = ?) AND goalText = ?");
                statement.setString(1, username);
                statement.setString(2, goalText);

                statement.executeUpdate();
                showAlert("Goal deleted successfully.");

                loadGoals(username, goalsTableModel); // Refresh the goals table

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error deleting goal.");
            }
        } else {
            showAlert("No goal selected for deletion.");
        }
    }

    private static void editGoal(JFrame mainFrame, String username, int selectedRow, DefaultTableModel goalsTableModel) {
        if (selectedRow != -1) {
            String currentGoalText = (String) goalsTableModel.getValueAt(selectedRow, 0);

            JTextField goalTextField = new JTextField(20);
            goalTextField.setText(currentGoalText);

            JPanel editGoalPanel = new JPanel();
            editGoalPanel.setLayout(new BoxLayout(editGoalPanel, BoxLayout.Y_AXIS));
            editGoalPanel.add(new JLabel("Edit Goal:"));
            editGoalPanel.add(goalTextField);

            int result = JOptionPane.showConfirmDialog(mainFrame, editGoalPanel, "Edit Goal",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    String newGoalText = goalTextField.getText();

                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE Goal SET goalText = ? WHERE userId = (SELECT id FROM User WHERE username = ?) " +
                                    "AND goalText = ?");
                    statement.setString(1, newGoalText);
                    statement.setString(2, username);
                    statement.setString(3, currentGoalText);

                    statement.executeUpdate();
                    showAlert("Goal edited successfully.");

                    loadGoals(username, goalsTableModel); // Refresh the goals table

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error editing goal.");
                }
            }
        } else {
            showAlert("No goal selected for editing.");
        }
    }

    private static void showAlert(String message) {
        JOptionPane.showMessageDialog(null, message, "Calm Canvas", JOptionPane.INFORMATION_MESSAGE);
    }
    private static JPanel createMoodTrackerPanel(JFrame mainFrame, String username) {
        JPanel moodPanel = new JPanel();
        moodPanel.setLayout(new BoxLayout(moodPanel, BoxLayout.Y_AXIS));

        JLabel welcomeLabel = new JLabel("Welcome, " + username + "!");
        JLabel moodLabel = new JLabel("How was your mood today? (1 being worst, 10 being best)");

        JSpinner moodSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));

        JButton submitMoodButton = new JButton("Submit Mood");
        submitMoodButton.addActionListener(e -> {
            submitMood(username, (int) moodSpinner.getValue());
        });

        JButton viewAvgMoodButton = new JButton("View Average Mood");
        viewAvgMoodButton.addActionListener(e -> {
            viewAverageMood(username);
        });

        moodPanel.add(welcomeLabel);
        moodPanel.add(moodLabel);
        moodPanel.add(moodSpinner);
        moodPanel.add(submitMoodButton);
        moodPanel.add(viewAvgMoodButton);

        return moodPanel;
    }
    private static void submitMood(String username, int mood) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Mood (userId, mood) VALUES ((SELECT id FROM User WHERE username = ?), ?)");
            statement.setString(1, username);
            statement.setInt(2, mood);

            statement.executeUpdate();
            showAlert("Mood submitted successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error submitting mood.");
        }
    }

    private static void viewAverageMood(String username) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT AVG(mood) AS avgMood FROM Mood WHERE userId = (SELECT id FROM User WHERE username = ?)");
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                double avgMood = resultSet.getDouble("avgMood");
                showAlert("Your average mood is: " + avgMood);
            } else {
                showAlert("No mood data available.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error retrieving average mood.");
        }
    }
    private static void playGuessTheNumber(JFrame mainFrame, String username) {
        int randomNumber = (int) (Math.random() * 100) + 1; // Generate a random number between 1 and 100
        int attempts = 0;

        while (true) {
            String userGuessStr = JOptionPane.showInputDialog(mainFrame, "Guess the number (1-100):");
            if (userGuessStr == null) {
                break; // User clicked Cancel or closed the dialog
            }

            try {
                int userGuess = Integer.parseInt(userGuessStr);
                attempts++;

                if (userGuess == randomNumber) {
                    showAlert("Congratulations! You guessed the number in " + attempts + " attempts.");
                    updateGameScore(username, "GuessTheNumber", attempts);
                    break;
                } else if (userGuess < randomNumber) {
                    showAlert("Try a higher number!");
                } else {
                    showAlert("Try a lower number!");
                }

            } catch (NumberFormatException e) {
                showAlert("Please enter a valid number.");
            }
        }
    }

    private static void updateGameScore(String username, String gameName, int score) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO GameScore (userId, gameName, score) VALUES ((SELECT id FROM User WHERE username = ?), ?, ?)");
            statement.setString(1, username);
            statement.setString(2, gameName);
            statement.setInt(3, score);

            statement.executeUpdate();
            showAlert("Highscore updated!");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error updating highscore.");
        }
    }
    private static JPanel createGamePanel(JFrame mainFrame, String username) {
        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));

        JButton playGuessTheNumberButton = new JButton("Play Guess the Number");
        playGuessTheNumberButton.addActionListener(e -> {
            playGuessTheNumber(mainFrame, username);
        });

        JButton playRockPaperScissorsButton = new JButton("Play Rock, Paper, Scissors");
        playRockPaperScissorsButton.addActionListener(e -> {
            playRockPaperScissors(mainFrame, username);
        });

        JButton playHangmanButton = new JButton("Play Hangman");
        playHangmanButton.addActionListener(e -> {
            playHangman(mainFrame, username);
        });

        JButton viewGuessTheNumberHighscoreButton = new JButton("View Guess the Number Highscore");
        viewGuessTheNumberHighscoreButton.addActionListener(e -> {
            viewHighscore(username, "GuessTheNumber");
        });

        JButton viewRockPaperScissorsHighscoreButton = new JButton("View Rock, Paper, Scissors Highscore");
        viewRockPaperScissorsHighscoreButton.addActionListener(e -> {
            viewHighscore(username, "RockPaperScissors");
        });

        JButton viewHangmanHighscoreButton = new JButton("View Hangman Highscore");
        viewHangmanHighscoreButton.addActionListener(e -> {
            viewHighscore(username, "Hangman");
        });

        gamePanel.add(playGuessTheNumberButton);
        gamePanel.add(viewGuessTheNumberHighscoreButton);
        gamePanel.add(playRockPaperScissorsButton);
        gamePanel.add(viewRockPaperScissorsHighscoreButton);
        gamePanel.add(playHangmanButton);
        gamePanel.add(viewHangmanHighscoreButton);

        return gamePanel;
    }

    private static void viewHighscore(String username, String gameName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM GameScore WHERE userId = (SELECT id FROM User WHERE username = ?) AND gameName = ? " +
                            "ORDER BY score ASC LIMIT 1");
            statement.setString(1, username);
            statement.setString(2, gameName);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int highscore = resultSet.getInt("score");
                showAlert("Your highscore for " + gameName + " is: " + highscore);
            } else {
                showAlert("No highscore available for " + gameName + ". Try playing!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error retrieving highscore.");
        }
    }
    private static void playRockPaperScissors(JFrame mainFrame, String username) {
        String[] choices = {"Rock", "Paper", "Scissors"};
        String computerChoice = choices[(int) (Math.random() * 3)];

        String userChoice = (String) JOptionPane.showInputDialog(mainFrame,
                "Choose Rock, Paper, or Scissors:", "Rock, Paper, Scissors",
                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);

        if (userChoice == null) {
            return; // User clicked Cancel or closed the dialog
        }

        showAlert("Computer chose: " + computerChoice);

        if (userChoice.equals(computerChoice)) {
            showAlert("It's a tie!");
        } else if ((userChoice.equals("Rock") && computerChoice.equals("Scissors")) ||
                (userChoice.equals("Paper") && computerChoice.equals("Rock")) ||
                (userChoice.equals("Scissors") && computerChoice.equals("Paper"))) {
            showAlert("Congratulations! You win!");
            updateGameScore(username, "RockPaperScissors", 1);
        } else {
            showAlert("Sorry, you lose. Try again!");
        }
    }
    private static void playHangman(JFrame mainFrame, String username) {
        String[] words = {"java", "programming", "hangman", "developer", "openai", "coding"};
        String selectedWord = words[(int) (Math.random() * words.length)];

        StringBuilder displayWord = new StringBuilder("_".repeat(selectedWord.length()));

        int attempts = 6; // Number of incorrect attempts allowed

        while (attempts > 0) {
            String userGuess = JOptionPane.showInputDialog(mainFrame,
                    "Current word: " + displayWord + "\nAttempts left: " + attempts +
                            "\nEnter a letter:");

            if (userGuess == null || userGuess.length() != 1 || !Character.isLetter(userGuess.charAt(0))) {
                continue; // Ignore invalid input
            }

            char guessChar = userGuess.toLowerCase().charAt(0);

            boolean correctGuess = false;
            for (int i = 0; i < selectedWord.length(); i++) {
                if (selectedWord.charAt(i) == guessChar) {
                    displayWord.setCharAt(i, guessChar);
                    correctGuess = true;
                }
            }

            if (!correctGuess) {
                attempts--;
            }

            if (displayWord.toString().equals(selectedWord)) {
                showAlert("Congratulations! You guessed the word: " + selectedWord);
                updateGameScore(username, "Hangman", 1);
                break;
            }
        }

        if (attempts == 0) {
            showAlert("Sorry, you ran out of attempts. The word was: " + selectedWord);
        }
    }

}

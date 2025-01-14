// Task Planner Application
// SE3317 - Software Design and Architecture Project

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TaskModel model = new TaskModel();
            TaskView view = new TaskView();
            TaskController controller = new TaskController(model, view);

            controller.initialize();
            view.setVisible(true);
        });
    }
}

// Model
class Task {
    private int id;
    private String name;
    private String description;
    private String category;
    private LocalDate deadline;

    // Constructor, getters, and setters
    public Task(int id, String name, String description, String category, LocalDate deadline) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    @Override
    public String toString() {
        return name + " (" + category + ") - Due: " + deadline;
    }
}

class TaskModel {
    private final String DB_URL = "jdbc:mysql://localhost:3306/taskplanner";
    private final String USER = "root";
    private final String PASSWORD = "password";

    public List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM tasks");
            while (rs.next()) {
                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getDate("deadline").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public void addTask(Task task) {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO tasks (name, description, category, deadline) VALUES (?, ?, ?, ?)")
             {
            ps.setString(1, task.getName());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getCategory());
            ps.setDate(4, Date.valueOf(task.getDeadline()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTask(int taskId) {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTask(Task task) {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE tasks SET name = ?, description = ?, category = ?, deadline = ? WHERE id = ?")) {
            ps.setString(1, task.getName());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getCategory());
            ps.setDate(4, Date.valueOf(task.getDeadline()));
            ps.setInt(5, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// View
class TaskView extends JFrame {
    private final DefaultListModel<Task> taskListModel = new DefaultListModel<>();
    private final JList<Task> taskList = new JList<>(taskListModel);
    private final JButton addButton = new JButton("Add Task");
    private final JButton deleteButton = new JButton("Delete Task");
    private final JButton editButton = new JButton("Edit Task");

    public TaskView() {
        setTitle("Task Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);

        add(new JScrollPane(taskList), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public DefaultListModel<Task> getTaskListModel() {
        return taskListModel;
    }

    public JList<Task> getTaskList() {
        return taskList;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JButton getEditButton() {
        return editButton;
    }
}

// Controller
class TaskController {
    private final TaskModel model;
    private final TaskView view;

    public TaskController(TaskModel model, TaskView view) {
        this.model = model;
        this.view = view;
    }

    public void initialize() {
        refreshTaskList();

        view.getAddButton().addActionListener(e -> addTask());
        view.getDeleteButton().addActionListener(e -> deleteTask());
        view.getEditButton().addActionListener(e -> editTask());
    }

    private void refreshTaskList() {
        view.getTaskListModel().clear();
        for (Task task : model.getTasks()) {
            view.getTaskListModel().addElement(task);
        }
    }

    private void addTask() {
        String name = JOptionPane.showInputDialog("Task Name:");
        String description = JOptionPane.showInputDialog("Task Description:");
        String category = JOptionPane.showInputDialog("Task Category:");
        String deadline = JOptionPane.showInputDialog("Task Deadline (YYYY-MM-DD):");

        if (name != null && description != null && category != null && deadline != null) {
            Task task = new Task(0, name, description, category, LocalDate.parse(deadline));
            model.addTask(task);
            refreshTaskList();
        }
    }

    private void deleteTask() {
        Task selectedTask = view.getTaskList().getSelectedValue();
        if (selectedTask != null) {
            model.deleteTask(selectedTask.getId());
            refreshTaskList();
        } else {
            JOptionPane.showMessageDialog(view, "No task selected.");
        }
    }

    private void editTask() {
        Task selectedTask = view.getTaskList().getSelectedValue();
        if (selectedTask != null) {
            String name = JOptionPane.showInputDialog("Task Name:", selectedTask.getName());
            String description = JOptionPane.showInputDialog("Task Description:", selectedTask.getDescription());
            String category = JOptionPane.showInputDialog("Task Category:", selectedTask.getCategory());
            String deadline = JOptionPane.showInputDialog("Task Deadline (YYYY-MM-DD):", selectedTask.getDeadline());

            if (name != null && description != null && category != null && deadline != null) {
                Task task = new Task(selectedTask.getId(), name, description, category, LocalDate.parse(deadline));
                model.updateTask(task);
                refreshTaskList();
            }
        } else {
            JOptionPane.showMessageDialog(view, "No task selected.");
        }
    }
}

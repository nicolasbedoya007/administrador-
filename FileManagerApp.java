package fileapp;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

public class FileApp {
    private static Connection connection;
    private static JFrame mainFrame;
    private static JPanel loginPanel;
    private static JPanel fileManagerPanel;
    private static boolean isAdmin = false;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            mainFrame = new JFrame("EADJ Administrador de Archivos");
            mainFrame.setSize(800, 600);
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setLayout(new CardLayout());

            createLoginPanel();
            createFileManagerPanel();

            mainFrame.add(loginPanel, "login");
            mainFrame.add(fileManagerPanel, "fileManager");

            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        });

        testDatabaseConnection();
        initializeUsers();
    }

    private static void createLoginPanel() {
        loginPanel = new JPanel(null);
        loginPanel.setBackground(new Color(240, 248, 255));

        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                Color color1 = new Color(41, 128, 185);
                Color color2 = new Color(109, 213, 250);
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        titlePanel.setBounds(0, 0, 800, 150);
        titlePanel.setLayout(null);

        JLabel titleLabel = new JLabel("EA");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 60));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(330, 20, 100, 70);
        titlePanel.add(titleLabel);
        
        JLabel djLabel = new JLabel("DJ");
        djLabel.setFont(new Font("Arial", Font.BOLD, 60));
        djLabel.setForeground(new Color(255, 215, 0));
        djLabel.setBounds(410, 20, 100, 70);
        titlePanel.add(djLabel);
        
        JLabel subtitleLabel = new JLabel("ADMINISTRADOR DE ARCHIVOS");
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setBounds(250, 100, 300, 30);
        titlePanel.add(subtitleLabel);

        loginPanel.add(titlePanel);

        JTextField emailField = new JTextField();
        emailField.setBounds(250, 200, 300, 50);
        styleTextField(emailField, "Email");
        
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(250, 270, 300, 50);
        stylePasswordField(passwordField, "Password");

        JButton loginButton = new JButton("INGRESAR");
        loginButton.setBounds(250, 340, 300, 50);
        styleButton(loginButton, new Color(52, 152, 219));

        JButton registerButton = new JButton("REGISTRARSE");
        registerButton.setBounds(250, 410, 300, 50);
        styleButton(registerButton, new Color(46, 204, 113));

        loginPanel.add(emailField);
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);
        loginPanel.add(registerButton);

        loginButton.addActionListener(e -> {
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticateUser(email, password)) {
                isAdmin = email.equals("camilop@correo.com");
                JOptionPane.showMessageDialog(mainFrame, "Bienvenido a EADJ" + (isAdmin ? " (Administrador)" : ""), "Éxito", JOptionPane.INFORMATION_MESSAGE);
                showFileManager();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Usuario o contraseña incorrectos", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerButton.addActionListener(e -> {
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            if (userExists(email)) {
                JOptionPane.showMessageDialog(mainFrame, "Este usuario ya está registrado", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (registerUser(email, password)) {
                JOptionPane.showMessageDialog(mainFrame, "Usuario registrado con éxito", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Error al registrar el usuario", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void createFileManagerPanel() {
        fileManagerPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Cerrar Sesión");
        logoutButton.addActionListener(e -> showLoginPanel());
        topPanel.add(logoutButton);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new File(System.getProperty("user.home")));
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree fileTree = new JTree(treeModel);
        populateFileTree(root);
        JScrollPane treeScrollPane = new JScrollPane(fileTree);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Actualizar");
        JButton addButton = new JButton("Agregar Archivo");
        JButton deleteButton = new JButton("Eliminar Archivo");
        JButton renameButton = new JButton("Renombrar Archivo");
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(renameButton);

        fileManagerPanel.add(topPanel, BorderLayout.NORTH);
        fileManagerPanel.add(treeScrollPane, BorderLayout.CENTER);
        fileManagerPanel.add(buttonPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> refreshFileTree(root, treeModel));
        addButton.addActionListener(e -> addFile(root, fileTree));
        deleteButton.addActionListener(e -> deleteFile(fileTree));
        renameButton.addActionListener(e -> renameFile(fileTree));

        if (!isAdmin) {
            deleteButton.setEnabled(false);
            renameButton.setEnabled(false);
        }

        fileTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
            if (node != null && !node.isRoot()) {
                File file = (File) node.getUserObject();
                showFileDetails(file);
            }
        });
    }

    private static void populateFileTree(DefaultMutableTreeNode node) {
        File file = (File) node.getUserObject();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                    node.add(childNode);
                    if (child.isDirectory()) {
                        populateFileTree(childNode);
                    }
                }
            }
        }
    }

    private static void refreshFileTree(DefaultMutableTreeNode root, DefaultTreeModel model) {
        root.removeAllChildren();
        populateFileTree(root);
        model.reload();
    }

    private static void addFile(DefaultMutableTreeNode root, JTree tree) {
        String fileName = JOptionPane.showInputDialog(mainFrame, "Ingrese el nombre del archivo:");
        if (fileName != null && !fileName.trim().isEmpty()) {
            File parentFile = (File) root.getUserObject();
            File newFile = new File(parentFile, fileName);
            try {
                if (newFile.createNewFile()) {
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFile);
                    root.add(newNode);
                    ((DefaultTreeModel)tree.getModel()).reload();
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "No se pudo crear el archivo", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Error al crear el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void deleteFile(JTree tree) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        if (selectedNode != null && !selectedNode.isRoot()) {
            File file = (File) selectedNode.getUserObject();
            int confirm = JOptionPane.showConfirmDialog(mainFrame, "¿Está seguro de que desea eliminar " + file.getName() + "?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (file.delete()) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.removeNodeFromParent(selectedNode);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "No se pudo eliminar el archivo", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private static void renameFile(JTree tree) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        if (selectedNode != null && !selectedNode.isRoot()) {
            File file = (File) selectedNode.getUserObject();
            String newName = JOptionPane.showInputDialog(mainFrame, "Ingrese el nuevo nombre:", file.getName());
            if (newName != null && !newName.trim().isEmpty()) {
                File newFile = new File(file.getParentFile(), newName);
                if (file.renameTo(newFile)) {
                    selectedNode.setUserObject(newFile);
                    ((DefaultTreeModel)tree.getModel()).nodeChanged(selectedNode);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "No se pudo renombrar el archivo", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private static void showFileDetails(File file) {
        JOptionPane.showMessageDialog(mainFrame,
            "Nombre: " + file.getName() + "\n" +
            "Tipo: " + (file.isDirectory() ? "Carpeta" : "Archivo") + "\n" +
            "Tamaño: " + file.length() + " bytes\n" +
            "Fecha de modificación: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(file.lastModified())),
            "Detalles del archivo",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showFileManager() {
        CardLayout cl = (CardLayout)(mainFrame.getContentPane().getLayout());
        cl.show(mainFrame.getContentPane(), "fileManager");
    }

    private static void showLoginPanel() {
        CardLayout cl = (CardLayout)(mainFrame.getContentPane().getLayout());
        cl.show(mainFrame.getContentPane(), "login");
        isAdmin = false;
    }

    private static void styleTextField(JTextField textField, String placeholder) {
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);
        textField.setFont(new Font("Arial", Font.PLAIN, 16));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(Color.GRAY);
                    textField.setText(placeholder);
                }
            }
        });
    }

    private static void stylePasswordField(JPasswordField passwordField, String placeholder) {
        passwordField.setEchoChar((char) 0);
        passwordField.setText(placeholder);
        passwordField.setForeground(Color.GRAY);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals(placeholder)) {
                    passwordField.setText("");
                    passwordField.setEchoChar('•');
                    passwordField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).isEmpty()) {
                    passwordField.setEchoChar((char) 0);
                    passwordField.setForeground(Color.GRAY);
                    passwordField.setText(placeholder);
                }
            }
        });
    }

    private static void styleButton(JButton button, Color backgroundColor) {
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(backgroundColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(backgroundColor);
            }
        });
    }

    private static void connectToDatabase() {
        try {
            // Cargar explícitamente el driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // URL de conexión con parámetros adicionales
            String url = "jdbc:mysql://127.0.0.1:3305/eadj?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            String user = "root";
            String password = "";
            
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión exitosa a la base de datos");
            
        } catch (ClassNotFoundException e) {
            System.out.println("Error: No se pudo cargar el driver JDBC");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error al conectar a la base de datos");
            e.printStackTrace();
        }
    }

    private static void testDatabaseConnection() {
        try {
            connectToDatabase();
            System.out.println("Conexión exitosa a la base de datos.");
            
            // Realizar una consulta de prueba
            String query = "SELECT COUNT(*) FROM users";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("Número de usuarios en la base de datos: " + count);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al conectar o consultar la base de datos:");
            e.printStackTrace();
        }
    }

    private static void initializeUsers() {
        String[] emails = {"camilop@correo.com", "vaneza@correo.com", "luis23@correo.com", "carlos69@correo.com", "luchodiaz7@correo.com"};
        String[] passwords = {"1234", "1235", "2546", "8460", "2024"};

        for (int i = 0; i < emails.length; i++) {
            if (!userExists(emails[i])) {
                registerUser(emails[i], passwords[i]);
            }
        }
    }

    private static boolean userExists(String email) {
        String query = "SELECT * FROM users WHERE correo = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean registerUser(String email, String password) {
        String query = "INSERT INTO users (correo, password) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean authenticateUser(String email, String password) {
        String query = "SELECT * FROM users WHERE correo = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
} 
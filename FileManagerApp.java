package filemanagerapp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class FileManagerApp {
    private static Connection connection;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("EADJ Administrador de Archivos");
            frame.setSize(500, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(null);
            frame.getContentPane().setBackground(new Color(240, 248, 255));

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
            titlePanel.setBounds(0, 0, 500, 150);
            titlePanel.setLayout(null);

            JLabel titleLabel = new JLabel("EA");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 60));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setBounds(180, 20, 100, 70);
            titlePanel.add(titleLabel);
            
            JLabel djLabel = new JLabel("DJ");
            djLabel.setFont(new Font("Arial", Font.BOLD, 60));
            djLabel.setForeground(new Color(255, 215, 0));
            djLabel.setBounds(260, 20, 100, 70);
            titlePanel.add(djLabel);
            
            JLabel subtitleLabel = new JLabel("ADMINISTRADOR DE ARCHIVOS");
            subtitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            subtitleLabel.setForeground(Color.WHITE);
            subtitleLabel.setBounds(100, 100, 300, 30);
            titlePanel.add(subtitleLabel);

            frame.add(titlePanel);

            JTextField emailField = new JTextField();
            emailField.setBounds(100, 200, 300, 50);
            styleTextField(emailField, "Email");
            
            JPasswordField passwordField = new JPasswordField();
            passwordField.setBounds(100, 270, 300, 50);
            stylePasswordField(passwordField, "Password");

            JButton loginButton = new JButton("INGRESAR");
            loginButton.setBounds(100, 340, 300, 60);
            styleButton(loginButton, new Color(52, 152, 219));

            JButton registerButton = new JButton("REGISTRARSE");
            registerButton.setBounds(100, 420, 300, 60);
            styleButton(registerButton, new Color(46, 204, 113));

            frame.add(emailField);
            frame.add(passwordField);
            frame.add(loginButton);
            frame.add(registerButton);

            loginButton.addActionListener(e -> {
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());
                if (authenticateUser(email, password)) {
                    JOptionPane.showMessageDialog(frame, "Bienvenido a EADJ", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "Usuario o contraseña incorrectos", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            registerButton.addActionListener(e -> {
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());
                if (userExists(email)) {
                    JOptionPane.showMessageDialog(frame, "Este usuario ya está registrado", "Error", JOptionPane.ERROR_MESSAGE);
                } else if (registerUser(email, password)) {
                    JOptionPane.showMessageDialog(frame, "Usuario registrado con éxito", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "Error al registrar el usuario", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        connectToDatabase();
        initializeUsers();
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
            String url = "jdbc:mysql://localhost:3305/eadj";
            String user = "tu_usuario";
            String password = "tu_contraseña";
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión exitosa a la base de datos");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error al conectar a la base de datos");
        }
    }

    private static void initializeUsers() {
        String[] emails = {"camilop@correo.com", "vaneza@correo", "luis23@correo.com", "carlos69@correo.com", "luchodiaz7@correo.com"};
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
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean authenticateUser(String email, String password) {
        String query = "SELECT * FROM users WHERE correo = ? AND contraseña = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean registerUser(String email, String password) {
        String query = "INSERT INTO users (correo, contraseña) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
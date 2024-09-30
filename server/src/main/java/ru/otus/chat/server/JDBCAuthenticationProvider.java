package ru.otus.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JDBCAuthenticationProvider implements AuthenticatedProvider {
    private static Connection connection;

    private class User {
        private String login;
        private String password;
        private String username;
        private Role role;

        public User(String login, String password, String username, Role role) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.role = role;
        }

        public String getUsername(){
            return username;
        }
        public Role getUserrole(){
            return role;
        }
    }
    private Server server;
    private List<JDBCAuthenticationProvider.User> users;

    public JDBCAuthenticationProvider(Server server) {
        this.server = server;
        this.users = new ArrayList<>();

        String dbURL = "jdbc:postgresql://192.168.1.91:5433/postgres";
        String user = "postgres";
        String password = "password";

        try {
            connection = DriverManager.getConnection(dbURL, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: JDBC режим");
    }

    private User getUsernameByLoginAndPassword(String login, String password) {
        String sql = "SELECT login, password, username, role FROM console_chat.users WHERE login = ? AND password = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        return new User(resultSet.getString("login"),
                                        resultSet.getString("password"),
                                        resultSet.getString("username"),
                                        Role.valueOf(resultSet.getString("role"))
                                       );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        return null;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        User user = getUsernameByLoginAndPassword(login, password);

        if (user == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }

        if (server.isUsernameBusy(user.getUsername())) {
            clientHandler.sendMessage("Учетная запись уже занята");
            return false;
        }

        clientHandler.setUserName(user.getUsername());
        clientHandler.setUserRole(user.getUserrole());
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + user.getUsername());
        return true;
    }

    private boolean isLoginAlreadyExist(String login) {
        String sql = "SELECT 1 FROM console_chat.users WHERE login = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, login);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        String sql = "SELECT 1 FROM console_chat.users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6
                || username.trim().length() < 2) {
            clientHandler.sendMessage("Требования логин 3+ символа, пароль 6+ символа," +
                    "имя пользователя 2+ символа не выполнены");
            return false;
        }

        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }

        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        String sql = "INSERT INTO console_chat.users(login, password, username, role)\n" +
                     "\tVALUES (?, ?, ?, ?);";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
             preparedStatement.setString(1, login);
             preparedStatement.setString(2, password);
             preparedStatement.setString(3, username);
             preparedStatement.setString(4, String.valueOf(Role.USER));

             preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        users.add(new JDBCAuthenticationProvider.User(login, password, username, Role.USER));
        clientHandler.setUserName(username);
        clientHandler.setUserRole(Role.USER);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);

        return true;
    }

    @Override
    public boolean isUserAdmin(ClientHandler clientHandler) {
        if (clientHandler.getUserRole().equals(Role.ADMIN)) {
            return true;
        }
        return false;
    }
}

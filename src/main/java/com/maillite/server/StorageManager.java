package com.maillite.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StorageManager {
    private static final String STORAGE_DIR = "storage";
    private static final String USERS_FILE = "storage/users.json";
    private static final String MAIL_DIR = "storage/mail";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Map<String, User> users = new ConcurrentHashMap<>();
    // Map<Username, List<Mail>>
    private Map<String, List<Mail>> userMails = new ConcurrentHashMap<>();

    public StorageManager() {
        init();
    }

    private void init() {
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
            Files.createDirectories(Paths.get(MAIL_DIR));
            loadUsers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                List<User> userList = gson.fromJson(reader, new TypeToken<List<User>>(){}.getType());
                if (userList != null) {
                    for (User u : userList) {
                        users.put(u.getUsername(), u);
                        loadMails(u.getUsername());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Create default users if not exists
            createUser("alice", "123");
            createUser("bob", "123");
            createUser("charlie", "123");
        }
    }

    private void loadMails(String username) {
        File userDir = new File(MAIL_DIR, username);
        if (!userDir.exists()) userDir.mkdirs();
        
        File indexFile = new File(userDir, "index.json");
        if (indexFile.exists()) {
            try (Reader reader = new FileReader(indexFile)) {
                List<Mail> mails = gson.fromJson(reader, new TypeToken<List<Mail>>(){}.getType());
                if (mails != null) {
                    userMails.put(username, new ArrayList<>(mails));
                } else {
                    userMails.put(username, new ArrayList<>());
                }
            } catch (IOException e) {
                e.printStackTrace();
                userMails.put(username, new ArrayList<>());
            }
        } else {
            userMails.put(username, new ArrayList<>());
        }
    }

    public synchronized void createUser(String username, String password) {
        if (!users.containsKey(username)) {
            User user = new User(username, password);
            users.put(username, user);
            saveUsers();
            userMails.put(username, new ArrayList<>());
            new File(MAIL_DIR, username).mkdirs();
        }
    }

    public synchronized void removeUser(String username) {
        if (users.containsKey(username)) {
            users.remove(username);
            saveUsers();
            userMails.remove(username);
        }
    }

    public synchronized void saveUsers() {
        try (Writer writer = new FileWriter(USERS_FILE)) {
            gson.toJson(users.values(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveMails(String username) {
        List<Mail> mails = userMails.get(username);
        if (mails == null) return;
        
        File userDir = new File(MAIL_DIR, username);
        if (!userDir.exists()) userDir.mkdirs();
        
        try (Writer writer = new FileWriter(new File(userDir, "index.json"))) {
            gson.toJson(mails, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public List<Mail> getMails(String username) {
        return userMails.getOrDefault(username, Collections.emptyList());
    }

    public void addMail(String username, Mail mail) {
        List<Mail> mails = userMails.computeIfAbsent(username, k -> new ArrayList<>());
        mails.add(mail);
        saveMails(username);
    }

    public String saveBody(String username, byte[] body) {
        String filename = UUID.randomUUID().toString() + ".body";
        File userDir = new File(MAIL_DIR, username);
        if (!userDir.exists()) userDir.mkdirs();
        File bodyFile = new File(userDir, filename);
        try {
            Files.write(bodyFile.toPath(), body);
            return bodyFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public byte[] getBody(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            return new byte[0];
        }
    }
}

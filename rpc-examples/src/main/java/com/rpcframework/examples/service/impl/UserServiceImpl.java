package com.rpcframework.examples.service.impl;

import com.rpcframework.examples.model.User;
import com.rpcframework.examples.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * <p>基于内存的用户服务实现，用于演示RPC框架的各种特性。
 * 在生产环境中应该连接实际的数据库。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    // 模拟数据库存储
    private final Map<Long, User> userStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    // 模拟用户凭据存储
    private final Map<String, String> credentials = new ConcurrentHashMap<>();
    
    public UserServiceImpl() {
        // 初始化一些测试数据
        initTestData();
        logger.info("UserService initialized with {} users", userStorage.size());
    }
    
    /**
     * 初始化测试数据
     */
    private void initTestData() {
        // 创建测试用户
        User admin = new User(1L, "admin", "admin@example.com", "13800138000", 30);
        admin.setAddress("北京市海淀区");
        admin.setCreateTime(LocalDateTime.now().minusDays(10));
        admin.setUpdateTime(LocalDateTime.now().minusDays(1));
        
        User alice = new User(2L, "alice", "alice@example.com", "13800138001", 25);
        alice.setAddress("上海市浦东新区");
        alice.setCreateTime(LocalDateTime.now().minusDays(5));
        alice.setUpdateTime(LocalDateTime.now().minusDays(1));
        
        User bob = new User(3L, "bob", "bob@example.com", "13800138002", 28);
        bob.setAddress("深圳市南山区");
        bob.setCreateTime(LocalDateTime.now().minusDays(3));
        bob.setUpdateTime(LocalDateTime.now());
        
        userStorage.put(1L, admin);
        userStorage.put(2L, alice);
        userStorage.put(3L, bob);
        
        // 设置ID生成器
        idGenerator.set(4);
        
        // 初始化登录凭据
        credentials.put("admin", "admin123");
        credentials.put("alice", "alice123");
        credentials.put("bob", "bob123");
    }
    
    @Override
    public User getUserById(Long userId) {
        logger.debug("Getting user by ID: {}", userId);
        
        if (userId == null) {
            return null;
        }
        
        User user = userStorage.get(userId);
        if (user != null) {
            logger.debug("Found user: {}", user.getUsername());
            return user.copy(); // 返回副本以避免外部修改
        }
        
        logger.debug("User not found for ID: {}", userId);
        return null;
    }
    
    @Override
    public User createUser(User user) {
        logger.info("Creating user: {}", user != null ? user.getUsername() : "null");
        
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (!user.isValid()) {
            throw new IllegalArgumentException("User data is invalid");
        }
        
        // 检查用户名是否已存在
        boolean usernameExists = userStorage.values().stream()
            .anyMatch(u -> u.getUsername().equals(user.getUsername()));
        
        if (usernameExists) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        
        // 生成新ID并设置时间戳
        User newUser = user.copy();
        newUser.setId(idGenerator.getAndIncrement());
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        newUser.setActive(true);
        
        userStorage.put(newUser.getId(), newUser);
        
        logger.info("User created successfully: {} (ID: {})", newUser.getUsername(), newUser.getId());
        return newUser.copy();
    }
    
    @Override
    public User updateUser(User user) {
        logger.info("Updating user: {}", user != null ? user.getId() : "null");
        
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID cannot be null");
        }
        
        if (!user.isValid()) {
            throw new IllegalArgumentException("User data is invalid");
        }
        
        User existingUser = userStorage.get(user.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("User not found: " + user.getId());
        }
        
        // 检查用户名是否被其他用户使用
        boolean usernameConflict = userStorage.values().stream()
            .anyMatch(u -> !u.getId().equals(user.getId()) && 
                     u.getUsername().equals(user.getUsername()));
        
        if (usernameConflict) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        
        // 更新用户信息
        User updatedUser = user.copy();
        updatedUser.setCreateTime(existingUser.getCreateTime()); // 保持创建时间不变
        updatedUser.setUpdateTime(LocalDateTime.now());
        
        userStorage.put(updatedUser.getId(), updatedUser);
        
        logger.info("User updated successfully: {} (ID: {})", updatedUser.getUsername(), updatedUser.getId());
        return updatedUser.copy();
    }
    
    @Override
    public boolean deleteUser(Long userId) {
        logger.info("Deleting user: {}", userId);
        
        if (userId == null) {
            return false;
        }
        
        User removedUser = userStorage.remove(userId);
        boolean deleted = removedUser != null;
        
        if (deleted) {
            // 同时删除登录凭据
            credentials.remove(removedUser.getUsername());
            logger.info("User deleted successfully: {} (ID: {})", removedUser.getUsername(), userId);
        } else {
            logger.debug("User not found for deletion: {}", userId);
        }
        
        return deleted;
    }
    
    @Override
    public List<User> getAllUsers() {
        logger.debug("Getting all users, count: {}", userStorage.size());
        
        return userStorage.values().stream()
            .map(User::copy)
            .sorted(Comparator.comparing(User::getId))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<User> findUsersByUsername(String username) {
        logger.debug("Finding users by username: {}", username);
        
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String searchTerm = username.toLowerCase().trim();
        
        List<User> matchingUsers = userStorage.values().stream()
            .filter(user -> user.getUsername().toLowerCase().contains(searchTerm))
            .map(User::copy)
            .sorted(Comparator.comparing(User::getUsername))
            .collect(Collectors.toList());
        
        logger.debug("Found {} users matching username: {}", matchingUsers.size(), username);
        return matchingUsers;
    }
    
    @Override
    public int batchCreateUsers(List<User> users) {
        logger.info("Batch creating {} users", users != null ? users.size() : 0);
        
        if (users == null || users.isEmpty()) {
            return 0;
        }
        
        int createdCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (User user : users) {
            try {
                createUser(user);
                createdCount++;
            } catch (Exception e) {
                errors.add("Failed to create user " + 
                    (user != null ? user.getUsername() : "null") + ": " + e.getMessage());
            }
        }
        
        if (!errors.isEmpty()) {
            logger.warn("Batch create completed with {} errors: {}", errors.size(), errors);
        }
        
        logger.info("Batch create completed: {}/{} users created successfully", 
            createdCount, users.size());
        
        return createdCount;
    }
    
    @Override
    public boolean login(String username, String password) {
        logger.debug("User login attempt: {}", username);
        
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        String storedPassword = credentials.get(username.trim());
        boolean loginSuccess = password.equals(storedPassword);
        
        if (loginSuccess) {
            logger.info("User login successful: {}", username);
        } else {
            logger.warn("User login failed: {}", username);
        }
        
        return loginSuccess;
    }
    
    @Override
    public long getUserCount() {
        long count = userStorage.size();
        logger.debug("Current user count: {}", count);
        return count;
    }
    
    @Override
    public String slowOperation(int delayMs) {
        logger.debug("Executing slow operation with delay: {}ms", delayMs);
        
        if (delayMs < 0) {
            delayMs = 0;
        }
        
        if (delayMs > 10000) { // 最大延迟10秒，防止测试时间过长
            delayMs = 10000;
            logger.warn("Delay capped at 10000ms for safety");
        }
        
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Slow operation interrupted", e);
            return "Operation interrupted";
        }
        
        String result = "Slow operation completed after " + delayMs + "ms";
        logger.debug(result);
        return result;
    }
    
    @Override
    public String testException(boolean shouldThrow) {
        logger.debug("Testing exception handling, shouldThrow: {}", shouldThrow);
        
        if (shouldThrow) {
            logger.warn("Throwing test exception as requested");
            throw new RuntimeException("This is a test exception for demonstration purposes");
        }
        
        return "No exception thrown - test passed";
    }
    
    /**
     * 获取服务统计信息（用于监控）
     */
    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userStorage.size());
        stats.put("activeUsers", userStorage.values().stream()
            .mapToLong(user -> user.isActive() ? 1 : 0).sum());
        stats.put("lastUserId", idGenerator.get() - 1);
        stats.put("serviceStartTime", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * 重置服务状态（仅用于测试）
     */
    public void reset() {
        logger.warn("Resetting UserService - all data will be lost");
        userStorage.clear();
        credentials.clear();
        idGenerator.set(1);
        initTestData();
        logger.info("UserService reset completed");
    }
}
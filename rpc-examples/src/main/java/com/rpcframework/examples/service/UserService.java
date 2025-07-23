package com.rpcframework.examples.service;

import com.rpcframework.examples.model.User;

import java.util.List;

/**
 * 用户服务接口
 * 
 * <p>演示RPC框架的服务定义和远程调用能力。
 * 包含基础的CRUD操作、复杂对象传递和异常处理示例。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public interface UserService {
    
    /**
     * 根据用户ID获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息，如果不存在返回null
     */
    User getUserById(Long userId);
    
    /**
     * 创建新用户
     * 
     * @param user 用户信息
     * @return 创建成功后的用户信息（包含生成的ID）
     */
    User createUser(User user);
    
    /**
     * 更新用户信息
     * 
     * @param user 要更新的用户信息
     * @return 更新后的用户信息
     * @throws IllegalArgumentException 如果用户不存在
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     * 
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long userId);
    
    /**
     * 获取所有用户列表
     * 
     * @return 用户列表
     */
    List<User> getAllUsers();
    
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户列表
     */
    List<User> findUsersByUsername(String username);
    
    /**
     * 批量创建用户
     * 
     * @param users 用户列表
     * @return 创建成功的用户数量
     */
    int batchCreateUsers(List<User> users);
    
    /**
     * 用户登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录是否成功
     * @throws IllegalArgumentException 如果参数为空
     */
    boolean login(String username, String password);
    
    /**
     * 获取用户总数
     * 
     * @return 用户总数
     */
    long getUserCount();
    
    /**
     * 模拟耗时操作（用于性能测试）
     * 
     * @param delayMs 延迟毫秒数
     * @return 操作结果
     */
    String slowOperation(int delayMs);
    
    /**
     * 测试异常处理
     * 
     * @param shouldThrow 是否抛出异常
     * @return 测试结果
     * @throws RuntimeException 当shouldThrow为true时抛出
     */
    String testException(boolean shouldThrow);
}
package com.rpcframework.examples.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户模型类
 * 
 * <p>用于演示RPC框架的复杂对象序列化和传输能力。
 * 实现了Serializable接口，支持JSON序列化。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class User implements Serializable {
    
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer age;
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean active;
    
    public User() {
    }
    
    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.active = true;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    public User(Long id, String username, String email, String phone, Integer age) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.age = age;
        this.active = true;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return active == user.active &&
                Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(email, user.email) &&
                Objects.equals(phone, user.phone) &&
                Objects.equals(age, user.age) &&
                Objects.equals(address, user.address);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, phone, age, address, active);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", age=" + age +
                ", address='" + address + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", active=" + active +
                '}';
    }
    
    /**
     * 创建用户的副本
     */
    public User copy() {
        User copy = new User();
        copy.id = this.id;
        copy.username = this.username;
        copy.email = this.email;
        copy.phone = this.phone;
        copy.age = this.age;
        copy.address = this.address;
        copy.createTime = this.createTime;
        copy.updateTime = LocalDateTime.now();
        copy.active = this.active;
        return copy;
    }
    
    /**
     * 验证用户数据的有效性
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               email.contains("@");
    }
    
    /**
     * 获取用户显示名称
     */
    public String getDisplayName() {
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }
        return "User#" + (id != null ? id : "unknown");
    }
}
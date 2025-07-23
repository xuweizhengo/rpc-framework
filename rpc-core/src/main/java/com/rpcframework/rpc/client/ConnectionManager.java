package com.rpcframework.rpc.client;

import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.exception.NetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 连接管理器
 * 
 * <p>管理RPC客户端到多个服务端的连接池，支持连接复用、健康检查和故障恢复。
 * 提供高可用的连接管理机制，确保服务调用的稳定性和性能。
 * 
 * <p>主要功能：
 * <ul>
 * <li>连接池管理：维护到不同服务端的连接池</li>
 * <li>健康检查：定期检查连接状态和服务端可用性</li>
 * <li>故障恢复：自动重连和故障转移</li>
 * <li>负载均衡：支持多种负载均衡策略</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class ConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    private final NetworkConfig config;
    
    /**
     * 连接池：服务端地址 -> 连接池
     */
    private final ConcurrentMap<InetSocketAddress, ConnectionPool> connectionPools = new ConcurrentHashMap<>();
    
    /**
     * 健康检查调度器
     */
    private final ScheduledExecutorService healthCheckScheduler;
    
    /**
     * 连接管理器状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * 读写锁，保护连接池的读写操作
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public ConnectionManager() {
        this(NetworkConfig.clientConfig());
    }
    
    public ConnectionManager(NetworkConfig config) {
        this.config = config;
        this.healthCheckScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "connection-health-check");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * 启动连接管理器
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            logger.warn("ConnectionManager is already started");
            return;
        }
        
        logger.info("Starting ConnectionManager...");
        
        // 启动健康检查任务
        startHealthCheck();
        
        logger.info("ConnectionManager started successfully");
    }
    
    /**
     * 启动健康检查
     */
    private void startHealthCheck() {
        // 连接健康检查
        healthCheckScheduler.scheduleWithFixedDelay(
            this::performHealthCheck,
            config.getHeartbeatInterval() / 1000,
            config.getHeartbeatInterval() / 1000,
            TimeUnit.SECONDS
        );
        
        // 超时请求清理
        healthCheckScheduler.scheduleWithFixedDelay(
            this::cleanupTimeoutRequests,
            30, 30, TimeUnit.SECONDS
        );
    }
    
    /**
     * 获取到指定服务端的连接
     * 
     * @param serverAddress 服务端地址
     * @return RPC客户端连接
     * @throws NetworkException 网络异常
     */
    public RpcClient getConnection(InetSocketAddress serverAddress) throws NetworkException {
        if (!started.get()) {
            throw new NetworkException("ConnectionManager not started");
        }
        
        ConnectionPool pool = getOrCreateConnectionPool(serverAddress);
        return pool.getConnection();
    }
    
    /**
     * 获取或创建连接池
     */
    private ConnectionPool getOrCreateConnectionPool(InetSocketAddress serverAddress) {
        lock.readLock().lock();
        try {
            ConnectionPool pool = connectionPools.get(serverAddress);
            if (pool != null) {
                return pool;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // 需要创建新的连接池
        lock.writeLock().lock();
        try {
            // 双重检查
            ConnectionPool pool = connectionPools.get(serverAddress);
            if (pool != null) {
                return pool;
            }
            
            // 创建新连接池
            pool = new ConnectionPool(serverAddress, config);
            connectionPools.put(serverAddress, pool);
            
            logger.info("Created connection pool for server: {}", serverAddress);
            
            return pool;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 释放连接
     * 
     * @param serverAddress 服务端地址
     * @param client RPC客户端
     */
    public void releaseConnection(InetSocketAddress serverAddress, RpcClient client) {
        if (client == null) {
            return;
        }
        
        lock.readLock().lock();
        try {
            ConnectionPool pool = connectionPools.get(serverAddress);
            if (pool != null) {
                pool.releaseConnection(client);
            } else {
                // 连接池不存在，直接关闭连接
                logger.warn("Connection pool not found for server: {}, closing connection", serverAddress);
                client.shutdown();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 移除连接池
     * 
     * @param serverAddress 服务端地址
     */
    public void removeConnectionPool(InetSocketAddress serverAddress) {
        lock.writeLock().lock();
        try {
            ConnectionPool pool = connectionPools.remove(serverAddress);
            if (pool != null) {
                pool.shutdown();
                logger.info("Removed connection pool for server: {}", serverAddress);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        if (!started.get()) {
            return;
        }
        
        try {
            lock.readLock().lock();
            try {
                for (ConnectionPool pool : connectionPools.values()) {
                    pool.performHealthCheck();
                }
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            logger.error("Error during health check", e);
        }
    }
    
    /**
     * 清理超时请求
     */
    private void cleanupTimeoutRequests() {
        if (!started.get()) {
            return;
        }
        
        try {
            lock.readLock().lock();
            try {
                for (ConnectionPool pool : connectionPools.values()) {
                    pool.cleanupTimeoutRequests();
                }
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            logger.error("Error during timeout cleanup", e);
        }
    }
    
    /**
     * 获取连接池统计信息
     */
    public ConnectionPoolStats getStats(InetSocketAddress serverAddress) {
        lock.readLock().lock();
        try {
            ConnectionPool pool = connectionPools.get(serverAddress);
            return pool != null ? pool.getStats() : null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有连接池的统计信息
     */
    public ConnectionPoolStats getTotalStats() {
        ConnectionPoolStats totalStats = new ConnectionPoolStats();
        
        lock.readLock().lock();
        try {
            for (ConnectionPool pool : connectionPools.values()) {
                ConnectionPoolStats poolStats = pool.getStats();
                totalStats.merge(poolStats);
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return totalStats;
    }
    
    /**
     * 获取连接池数量
     */
    public int getConnectionPoolCount() {
        lock.readLock().lock();
        try {
            return connectionPools.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有服务端地址
     */
    public InetSocketAddress[] getServerAddresses() {
        lock.readLock().lock();
        try {
            return connectionPools.keySet().toArray(new InetSocketAddress[0]);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started.get();
    }
    
    /**
     * 关闭连接管理器
     */
    public void shutdown() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        
        logger.info("Shutting down ConnectionManager...");
        
        // 关闭健康检查调度器
        healthCheckScheduler.shutdown();
        try {
            if (!healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            healthCheckScheduler.shutdownNow();
        }
        
        // 关闭所有连接池
        lock.writeLock().lock();
        try {
            for (ConnectionPool pool : connectionPools.values()) {
                pool.shutdown();
            }
            connectionPools.clear();
        } finally {
            lock.writeLock().unlock();
        }
        
        logger.info("ConnectionManager shutdown completed");
    }
    
    /**
     * 连接池统计信息
     */
    public static class ConnectionPoolStats {
        private int totalConnections;
        private int activeConnections;
        private int idleConnections;
        private int failedConnections;
        private long totalRequests;
        private long failedRequests;
        private double averageResponseTime;
        
        public void merge(ConnectionPoolStats other) {
            if (other == null) return;
            
            this.totalConnections += other.totalConnections;
            this.activeConnections += other.activeConnections;
            this.idleConnections += other.idleConnections;
            this.failedConnections += other.failedConnections;
            this.totalRequests += other.totalRequests;
            this.failedRequests += other.failedRequests;
            
            // 简单平均，实际应该按权重计算
            this.averageResponseTime = (this.averageResponseTime + other.averageResponseTime) / 2;
        }
        
        // Getters and setters
        public int getTotalConnections() { return totalConnections; }
        public void setTotalConnections(int totalConnections) { this.totalConnections = totalConnections; }
        
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        
        public int getIdleConnections() { return idleConnections; }
        public void setIdleConnections(int idleConnections) { this.idleConnections = idleConnections; }
        
        public int getFailedConnections() { return failedConnections; }
        public void setFailedConnections(int failedConnections) { this.failedConnections = failedConnections; }
        
        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
        
        public long getFailedRequests() { return failedRequests; }
        public void setFailedRequests(long failedRequests) { this.failedRequests = failedRequests; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        @Override
        public String toString() {
            return "ConnectionPoolStats{" +
                    "totalConnections=" + totalConnections +
                    ", activeConnections=" + activeConnections +
                    ", idleConnections=" + idleConnections +
                    ", failedConnections=" + failedConnections +
                    ", totalRequests=" + totalRequests +
                    ", failedRequests=" + failedRequests +
                    ", averageResponseTime=" + averageResponseTime +
                    '}';
        }
    }
}
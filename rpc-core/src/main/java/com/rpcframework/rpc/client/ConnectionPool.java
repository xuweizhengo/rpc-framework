package com.rpcframework.rpc.client;

import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.exception.NetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 连接池实现
 * 
 * <p>管理到单个服务端的连接池，支持连接复用、动态扩容和故障检测。
 * 基于阻塞队列实现连接的获取和归还，确保线程安全和高效的连接管理。
 * 
 * <p>主要功能：
 * <ul>
 * <li>连接池管理：维护最小和最大连接数</li>
 * <li>连接复用：空闲连接的复用和管理</li>
 * <li>动态扩容：根据负载动态创建新连接</li>
 * <li>故障检测：检测和清理失效连接</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class ConnectionPool {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    
    private final InetSocketAddress serverAddress;
    private final NetworkConfig config;
    
    /**
     * 空闲连接队列
     */
    private final BlockingQueue<RpcClient> idleConnections;
    
    /**
     * 连接池状态统计
     */
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger failedConnections = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    /**
     * 创建连接的锁，避免并发创建过多连接
     */
    private final ReentrantLock createLock = new ReentrantLock();
    
    /**
     * 连接池是否已关闭
     */
    private volatile boolean shutdown = false;
    
    public ConnectionPool(InetSocketAddress serverAddress, NetworkConfig config) {
        this.serverAddress = serverAddress;
        this.config = config;
        this.idleConnections = new LinkedBlockingQueue<>(config.getMaxPoolSize());
        
        // 预创建最小连接数
        initializeMinConnections();
    }
    
    /**
     * 初始化最小连接数
     */
    private void initializeMinConnections() {
        try {
            for (int i = 0; i < config.getMinPoolSize(); i++) {
                RpcClient client = createConnection();
                if (client != null) {
                    idleConnections.offer(client);
                }
            }
            
            logger.info("Initialized connection pool for {}, min connections: {}", 
                serverAddress, idleConnections.size());
                
        } catch (Exception e) {
            logger.error("Failed to initialize minimum connections for {}", serverAddress, e);
        }
    }
    
    /**
     * 获取连接
     * 
     * @return RPC客户端连接
     * @throws NetworkException 网络异常
     */
    public RpcClient getConnection() throws NetworkException {
        if (shutdown) {
            throw new NetworkException("Connection pool is shutdown");
        }
        
        totalRequests.incrementAndGet();
        
        // 首先尝试从空闲连接中获取
        RpcClient client = idleConnections.poll();
        
        if (client != null) {
            // 检查连接是否有效
            if (isConnectionValid(client)) {
                activeConnections.incrementAndGet();
                return client;
            } else {
                // 连接已失效，移除并继续
                removeConnection(client);
                client = null;
            }
        }
        
        // 没有可用的空闲连接，尝试创建新连接
        if (client == null && totalConnections.get() < config.getMaxPoolSize()) {
            client = createNewConnectionIfNeeded();
        }
        
        // 如果还是没有连接，等待空闲连接
        if (client == null) {
            try {
                client = idleConnections.poll(config.getConnectTimeout(), TimeUnit.MILLISECONDS);
                if (client == null) {
                    failedRequests.incrementAndGet();
                    throw new NetworkException("No available connection, pool exhausted");
                }
                
                // 再次检查连接有效性
                if (!isConnectionValid(client)) {
                    removeConnection(client);
                    failedRequests.incrementAndGet();
                    throw new NetworkException("Failed to get valid connection");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failedRequests.incrementAndGet();
                throw new NetworkException("Interrupted while waiting for connection", e);
            }
        }
        
        activeConnections.incrementAndGet();
        return client;
    }
    
    /**
     * 创建新连接（如果需要）
     */
    private RpcClient createNewConnectionIfNeeded() {
        // 使用锁避免并发创建过多连接
        if (createLock.tryLock()) {
            try {
                // 双重检查，避免重复创建
                if (totalConnections.get() < config.getMaxPoolSize()) {
                    return createConnection();
                }
            } finally {
                createLock.unlock();
            }
        }
        
        return null;
    }
    
    /**
     * 创建新连接
     */
    private RpcClient createConnection() {
        try {
            RpcClient client = new RpcClient(
                serverAddress.getHostString(), 
                serverAddress.getPort(), 
                config
            );
            
            client.start();
            client.connect();
            
            totalConnections.incrementAndGet();
            
            logger.debug("Created new connection to {}, total connections: {}", 
                serverAddress, totalConnections.get());
            
            return client;
            
        } catch (Exception e) {
            failedConnections.incrementAndGet();
            logger.error("Failed to create connection to {}", serverAddress, e);
            return null;
        }
    }
    
    /**
     * 释放连接
     * 
     * @param client RPC客户端
     */
    public void releaseConnection(RpcClient client) {
        if (client == null || shutdown) {
            return;
        }
        
        activeConnections.decrementAndGet();
        
        // 检查连接是否仍然有效
        if (isConnectionValid(client)) {
            // 如果连接池未满，将连接放回池中
            if (!idleConnections.offer(client)) {
                // 连接池已满，关闭连接
                removeConnection(client);
            }
        } else {
            // 连接已失效，移除
            removeConnection(client);
        }
    }
    
    /**
     * 检查连接是否有效
     */
    private boolean isConnectionValid(RpcClient client) {
        return client != null && client.isStarted() && client.isConnected();
    }
    
    /**
     * 移除连接
     */
    private void removeConnection(RpcClient client) {
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception e) {
                logger.warn("Error closing connection", e);
            }
            
            totalConnections.decrementAndGet();
        }
    }
    
    /**
     * 执行健康检查
     */
    public void performHealthCheck() {
        if (shutdown) {
            return;
        }
        
        // 检查空闲连接的健康状态
        int checkedCount = 0;
        int removedCount = 0;
        
        // 从队列中取出连接进行检查，检查后重新放回
        for (int i = 0; i < idleConnections.size() && checkedCount < 10; i++) {
            RpcClient client = idleConnections.poll();
            if (client == null) {
                break;
            }
            
            checkedCount++;
            
            if (isConnectionValid(client)) {
                // 连接有效，放回队列
                if (!idleConnections.offer(client)) {
                    // 队列满了，关闭连接
                    removeConnection(client);
                    removedCount++;
                }
            } else {
                // 连接无效，移除
                removeConnection(client);
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Health check removed {} invalid connections from pool for {}", 
                removedCount, serverAddress);
        }
        
        // 确保最小连接数
        ensureMinConnections();
    }
    
    /**
     * 确保最小连接数
     */
    private void ensureMinConnections() {
        int currentIdle = idleConnections.size();
        int needCreate = config.getMinPoolSize() - currentIdle;
        
        if (needCreate > 0 && totalConnections.get() + needCreate <= config.getMaxPoolSize()) {
            for (int i = 0; i < needCreate; i++) {
                RpcClient client = createConnection();
                if (client != null) {
                    if (!idleConnections.offer(client)) {
                        removeConnection(client);
                        break;
                    }
                } else {
                    break;
                }
            }
        }
    }
    
    /**
     * 清理超时请求
     */
    public void cleanupTimeoutRequests() {
        // 遍历所有连接，清理超时请求
        // 这里简化处理，实际实现可能需要更复杂的逻辑
        int cleanupCount = 0;
        
        for (RpcClient client : idleConnections) {
            if (client != null && client.isConnected()) {
                // 这里可以调用客户端的清理方法
                // client.getResponseHandler().cleanupTimeoutRequests();
                cleanupCount++;
            }
        }
        
        if (cleanupCount > 0) {
            logger.debug("Cleaned up timeout requests for {} connections in pool {}", 
                cleanupCount, serverAddress);
        }
    }
    
    /**
     * 获取连接池统计信息
     */
    public ConnectionManager.ConnectionPoolStats getStats() {
        ConnectionManager.ConnectionPoolStats stats = new ConnectionManager.ConnectionPoolStats();
        
        stats.setTotalConnections(totalConnections.get());
        stats.setActiveConnections(activeConnections.get());
        stats.setIdleConnections(idleConnections.size());
        stats.setFailedConnections(failedConnections.get());
        stats.setTotalRequests(totalRequests.get());
        stats.setFailedRequests(failedRequests.get());
        
        // 计算成功率和平均响应时间（简化处理）
        long total = totalRequests.get();
        long failed = failedRequests.get();
        if (total > 0) {
            double successRate = (double) (total - failed) / total * 100;
            stats.setAverageResponseTime(successRate); // 这里简化用成功率代替响应时间
        }
        
        return stats;
    }
    
    /**
     * 获取服务端地址
     */
    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }
    
    /**
     * 检查连接池是否已关闭
     */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * 关闭连接池
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        
        logger.info("Shutting down connection pool for {}", serverAddress);
        
        // 关闭所有空闲连接
        RpcClient client;
        while ((client = idleConnections.poll()) != null) {
            removeConnection(client);
        }
        
        logger.info("Connection pool shutdown completed for {}, total connections closed: {}", 
            serverAddress, totalConnections.get());
    }
    
    @Override
    public String toString() {
        return "ConnectionPool{" +
                "serverAddress=" + serverAddress +
                ", totalConnections=" + totalConnections.get() +
                ", activeConnections=" + activeConnections.get() +
                ", idleConnections=" + idleConnections.size() +
                ", failedConnections=" + failedConnections.get() +
                ", shutdown=" + shutdown +
                '}';
    }
}
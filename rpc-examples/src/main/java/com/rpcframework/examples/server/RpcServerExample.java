package com.rpcframework.examples.server;

import com.rpcframework.examples.service.UserService;
import com.rpcframework.examples.service.impl.UserServiceImpl;
import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.server.RpcRequestHandler;
import com.rpcframework.rpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC服务端示例
 * 
 * <p>演示如何启动RPC服务端并注册服务。
 * 展示了服务注册、配置管理和优雅关闭等核心功能。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcServerExample {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcServerExample.class);
    
    public static void main(String[] args) {
        RpcServerExample example = new RpcServerExample();
        example.startServer();
    }
    
    /**
     * 启动RPC服务端
     */
    public void startServer() {
        logger.info("Starting RPC Server Example...");
        
        try {
            // 1. 创建网络配置
            NetworkConfig serverConfig = createServerConfig();
            logger.info("Server configuration: {}", serverConfig);
            
            // 2. 创建请求处理器
            RpcRequestHandler requestHandler = new RpcRequestHandler();
            
            // 3. 注册服务
            registerServices(requestHandler);
            
            // 4. 创建并启动服务端
            RpcServer server = new RpcServer(serverConfig, requestHandler);
            
            // 5. 添加优雅关闭钩子
            addShutdownHook(server);
            
            // 6. 启动服务端
            server.start();
            
            logger.info("RPC Server started successfully!");
            logger.info("Server is listening on port: {}", serverConfig.getServerPort());
            logger.info("Press Ctrl+C to stop the server");
            
            // 7. 保持服务端运行
            keepServerRunning();
            
        } catch (Exception e) {
            logger.error("Failed to start RPC server", e);
            System.exit(1);
        }
    }
    
    /**
     * 创建服务端配置
     */
    private NetworkConfig createServerConfig() {
        return NetworkConfig.serverConfig()
            .setServerPort(8080)                    // 服务端端口
            .setIoThreads(4)                        // IO线程数
            .setWorkerThreads(8)                    // 工作线程数
            .setMaxConnections(1000)                // 最大连接数
            .setUseEpoll(true)                      // 启用Epoll优化
            .setTcpNodelay(true)                    // 禁用Nagle算法
            .setKeepAlive(true)                     // 启用TCP Keep-Alive
            .setBacklog(128)                        // 连接队列大小
            .setRecvBufferSize(64 * 1024)           // 接收缓冲区大小
            .setSendBufferSize(64 * 1024)           // 发送缓冲区大小
            .setUsePooledAllocator(true);           // 使用池化内存分配器
    }
    
    /**
     * 注册服务
     */
    private void registerServices(RpcRequestHandler requestHandler) {
        logger.info("Registering services...");
        
        // 注册用户服务
        UserService userService = new UserServiceImpl();
        requestHandler.registerService(UserService.class, userService);
        
        // 可以注册更多服务
        // OrderService orderService = new OrderServiceImpl();
        // requestHandler.registerService(OrderService.class, orderService);
        
        logger.info("Service registration completed");
        logger.info("Registered services: {}", requestHandler.getServiceCount());
        
        // 打印已注册的服务列表
        String[] serviceNames = requestHandler.getRegisteredServices();
        for (String serviceName : serviceNames) {
            logger.info("  - {}", serviceName);
        }
    }
    
    /**
     * 添加优雅关闭钩子
     */
    private void addShutdownHook(RpcServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Received shutdown signal, stopping server...");
            
            try {
                server.shutdown();
                logger.info("Server stopped gracefully");
            } catch (Exception e) {
                logger.error("Error during server shutdown", e);
            }
        }, "rpc-server-shutdown"));
    }
    
    /**
     * 保持服务端运行
     */
    private void keepServerRunning() {
        try {
            // 主线程等待，直到被中断
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Server main thread interrupted");
        }
    }
    
    /**
     * 创建高性能配置的服务端（用于性能测试）
     */
    public static NetworkConfig createHighPerformanceConfig() {
        return NetworkConfig.serverConfig()
            .setServerPort(8080)
            .setIoThreads(Runtime.getRuntime().availableProcessors())  // CPU核心数
            .setWorkerThreads(Runtime.getRuntime().availableProcessors() * 2)  // CPU核心数 * 2
            .setMaxConnections(5000)                // 提高最大连接数
            .setUseEpoll(true)                      // 启用Epoll
            .setTcpNodelay(true)                    // 禁用Nagle算法提高实时性
            .setKeepAlive(true)                     // 启用Keep-Alive
            .setBacklog(512)                        // 增大连接队列
            .setRecvBufferSize(128 * 1024)          // 增大接收缓冲区
            .setSendBufferSize(128 * 1024)          // 增大发送缓冲区
            .setUsePooledAllocator(true)            // 使用池化内存分配器
            .setRequestTimeout(30000)               // 30秒超时
            .setHeartbeatInterval(30000);           // 30秒心跳间隔
    }
    
    /**
     * 创建开发环境配置的服务端
     */
    public static NetworkConfig createDevelopmentConfig() {
        return NetworkConfig.serverConfig()
            .setServerPort(8080)
            .setIoThreads(2)                        // 较少的IO线程
            .setWorkerThreads(4)                    // 较少的工作线程
            .setMaxConnections(100)                 // 较少的最大连接数
            .setUseEpoll(false)                     // 不启用Epoll（兼容性更好）
            .setTcpNodelay(true)
            .setKeepAlive(true)
            .setBacklog(50)
            .setRecvBufferSize(32 * 1024)           // 较小的缓冲区
            .setSendBufferSize(32 * 1024)
            .setUsePooledAllocator(false)           // 不使用池化分配器（便于调试）
            .setRequestTimeout(10000)               // 10秒超时
            .setHeartbeatInterval(15000);           // 15秒心跳间隔
    }
}
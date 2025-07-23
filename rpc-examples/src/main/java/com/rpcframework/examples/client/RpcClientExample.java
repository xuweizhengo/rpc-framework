package com.rpcframework.examples.client;

import com.rpcframework.examples.model.User;
import com.rpcframework.examples.service.UserService;
import com.rpcframework.rpc.client.RpcClient;
import com.rpcframework.rpc.client.RpcFuture;
import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RPC客户端示例
 * 
 * <p>演示如何使用RPC客户端进行远程服务调用。
 * 包含同步调用、异步调用、批量调用和异常处理等示例。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcClientExample {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientExample.class);
    
    private RpcClient client;
    
    public static void main(String[] args) {
        RpcClientExample example = new RpcClientExample();
        example.runExample();
    }
    
    /**
     * 运行客户端示例
     */
    public void runExample() {
        logger.info("Starting RPC Client Example...");
        
        try {
            // 1. 初始化客户端
            initializeClient();
            
            // 2. 演示同步调用
            demonstrateSyncCalls();
            
            // 3. 演示异步调用
            demonstrateAsyncCalls();
            
            // 4. 演示批量操作
            demonstrateBatchOperations();
            
            // 5. 演示异常处理
            demonstrateExceptionHandling();
            
            // 6. 演示性能测试
            demonstratePerformanceTest();
            
        } catch (Exception e) {
            logger.error("Error running client example", e);
        } finally {
            // 7. 清理资源
            cleanup();
        }
    }
    
    /**
     * 初始化RPC客户端
     */
    private void initializeClient() throws Exception {
        logger.info("Initializing RPC client...");
        
        // 创建客户端配置
        NetworkConfig clientConfig = NetworkConfig.clientConfig()
            .setConnectTimeout(5000)                // 连接超时5秒
            .setRequestTimeout(10000)               // 请求超时10秒
            .setIoThreads(2)                        // IO线程数
            .setUseEpoll(true)                      // 启用Epoll优化
            .setTcpNodelay(true)                    // 禁用Nagle算法
            .setKeepAlive(true)                     // 启用Keep-Alive
            .setRecvBufferSize(64 * 1024)           // 接收缓冲区大小
            .setSendBufferSize(64 * 1024)           // 发送缓冲区大小
            .setUsePooledAllocator(true)            // 使用池化内存分配器
            .setHeartbeatInterval(30000);           // 心跳间隔
        
        // 创建客户端实例
        client = new RpcClient("localhost", 8080, clientConfig);
        
        // 启动客户端并建立连接
        client.start();
        client.connect();
        
        logger.info("RPC client initialized and connected successfully");
    }
    
    /**
     * 演示同步调用
     */
    private void demonstrateSyncCalls() {
        logger.info("\\n=== Demonstrating Synchronous Calls ===\");\n        \n        try {\n            // 1. 获取用户总数\n            logger.info(\"1. Getting user count...\");\n            RpcResponse countResponse = callUserService(\"getUserCount\", new Object[]{}, new Class[]{});\n            if (countResponse.isSuccess()) {\n                logger.info(\"User count: {}\", countResponse.getResult());\n            }\n            \n            // 2. 根据ID获取用户\n            logger.info(\"\\n2. Getting user by ID...\");\n            RpcResponse userResponse = callUserService(\"getUserById\", \n                new Object[]{1L}, new Class[]{Long.class});\n            if (userResponse.isSuccess()) {\n                User user = (User) userResponse.getResult();\n                logger.info(\"Found user: {}\", user != null ? user.getUsername() : \"null\");\n            }\n            \n            // 3. 获取所有用户\n            logger.info(\"\\n3. Getting all users...\");\n            RpcResponse allUsersResponse = callUserService(\"getAllUsers\", new Object[]{}, new Class[]{});\n            if (allUsersResponse.isSuccess()) {\n                @SuppressWarnings(\"unchecked\")\n                List<User> users = (List<User>) allUsersResponse.getResult();\n                logger.info(\"Total users: {}\", users != null ? users.size() : 0);\n                if (users != null) {\n                    users.forEach(user -> logger.info(\"  - {}: {}\", user.getId(), user.getUsername()));\n                }\n            }\n            \n            // 4. 创建新用户\n            logger.info(\"\\n4. Creating new user...\");\n            User newUser = new User(\"john_doe\", \"john@example.com\");\n            newUser.setAge(32);\n            newUser.setPhone(\"13800138999\");\n            newUser.setAddress(\"广州市天河区\");\n            \n            RpcResponse createResponse = callUserService(\"createUser\", \n                new Object[]{newUser}, new Class[]{User.class});\n            if (createResponse.isSuccess()) {\n                User createdUser = (User) createResponse.getResult();\n                logger.info(\"User created: {} (ID: {})\", \n                    createdUser.getUsername(), createdUser.getId());\n            }\n            \n            // 5. 用户登录验证\n            logger.info(\"\\n5. Testing user login...\");\n            RpcResponse loginResponse = callUserService(\"login\", \n                new Object[]{\"admin\", \"admin123\"}, new Class[]{String.class, String.class});\n            if (loginResponse.isSuccess()) {\n                Boolean loginResult = (Boolean) loginResponse.getResult();\n                logger.info(\"Login result: {}\", loginResult);\n            }\n            \n        } catch (Exception e) {\n            logger.error(\"Error in synchronous calls demonstration\", e);\n        }\n    }\n    \n    /**\n     * 演示异步调用\n     */\n    private void demonstrateAsyncCalls() {\n        logger.info(\"\\n=== Demonstrating Asynchronous Calls ===\");\n        \n        try {\n            CountDownLatch latch = new CountDownLatch(3);\n            \n            // 1. 异步获取用户总数\n            logger.info(\"1. Async getting user count...\");\n            RpcFuture countFuture = callUserServiceAsync(\"getUserCount\", new Object[]{}, new Class[]{});\n            countFuture.onSuccess(() -> {\n                try {\n                    logger.info(\"Async user count: {}\", countFuture.getResponse().getResult());\n                } catch (Exception e) {\n                    logger.error(\"Error getting async result\", e);\n                } finally {\n                    latch.countDown();\n                }\n            });\n            \n            // 2. 异步慢操作（模拟耗时调用）\n            logger.info(\"\\n2. Async slow operation...\");\n            RpcFuture slowFuture = callUserServiceAsync(\"slowOperation\", \n                new Object[]{1000}, new Class[]{int.class});\n            slowFuture.onComplete(() -> {\n                try {\n                    if (slowFuture.isSuccess()) {\n                        logger.info(\"Slow operation result: {}\", slowFuture.getResponse().getResult());\n                    } else {\n                        logger.error(\"Slow operation failed: {}\", slowFuture.getException().getMessage());\n                    }\n                } catch (Exception e) {\n                    logger.error(\"Error processing slow operation result\", e);\n                } finally {\n                    latch.countDown();\n                }\n            });\n            \n            // 3. 异步查找用户\n            logger.info(\"\\n3. Async finding users...\");\n            RpcFuture findFuture = callUserServiceAsync(\"findUsersByUsername\", \n                new Object[]{\"a\"}, new Class[]{String.class});\n            findFuture.onSuccess(() -> {\n                try {\n                    @SuppressWarnings(\"unchecked\")\n                    List<User> users = (List<User>) findFuture.getResponse().getResult();\n                    logger.info(\"Found {} users with 'a' in username\", users != null ? users.size() : 0);\n                } catch (Exception e) {\n                    logger.error(\"Error processing find result\", e);\n                } finally {\n                    latch.countDown();\n                }\n            });\n            \n            // 等待所有异步调用完成\n            boolean completed = latch.await(15, TimeUnit.SECONDS);\n            if (completed) {\n                logger.info(\"All async calls completed successfully\");\n            } else {\n                logger.warn(\"Some async calls timed out\");\n            }\n            \n        } catch (Exception e) {\n            logger.error(\"Error in asynchronous calls demonstration\", e);\n        }\n    }\n    \n    /**\n     * 演示批量操作\n     */\n    private void demonstrateBatchOperations() {\n        logger.info(\"\\n=== Demonstrating Batch Operations ===\");\n        \n        try {\n            // 准备批量用户数据\n            List<User> batchUsers = Arrays.asList(\n                new User(\"user1\", \"user1@example.com\"),\n                new User(\"user2\", \"user2@example.com\"),\n                new User(\"user3\", \"user3@example.com\")\n            );\n            \n            logger.info(\"Creating {} users in batch...\", batchUsers.size());\n            \n            RpcResponse batchResponse = callUserService(\"batchCreateUsers\", \n                new Object[]{batchUsers}, new Class[]{List.class});\n                \n            if (batchResponse.isSuccess()) {\n                Integer createdCount = (Integer) batchResponse.getResult();\n                logger.info(\"Batch operation completed: {} users created\", createdCount);\n            } else {\n                logger.error(\"Batch operation failed: {}\", batchResponse.getErrorMessage());\n            }\n            \n        } catch (Exception e) {\n            logger.error(\"Error in batch operations demonstration\", e);\n        }\n    }\n    \n    /**\n     * 演示异常处理\n     */\n    private void demonstrateExceptionHandling() {\n        logger.info(\"\\n=== Demonstrating Exception Handling ===\");\n        \n        try {\n            // 1. 测试服务端异常\n            logger.info(\"1. Testing server-side exception...\");\n            RpcResponse exceptionResponse = callUserService(\"testException\", \n                new Object[]{true}, new Class[]{boolean.class});\n            if (!exceptionResponse.isSuccess()) {\n                logger.info(\"Expected exception caught: {}\", exceptionResponse.getErrorMessage());\n            }\n            \n            // 2. 测试不存在的方法\n            logger.info(\"\\n2. Testing non-existent method...\");\n            RpcResponse nonExistentResponse = callUserService(\"nonExistentMethod\", \n                new Object[]{}, new Class[]{});\n            if (!nonExistentResponse.isSuccess()) {\n                logger.info(\"Method not found error: {}\", nonExistentResponse.getErrorMessage());\n            }\n            \n            // 3. 测试参数验证异常\n            logger.info(\"\\n3. Testing parameter validation...\");\n            RpcResponse validationResponse = callUserService(\"login\", \n                new Object[]{null, \"password\"}, new Class[]{String.class, String.class});\n            if (!validationResponse.isSuccess()) {\n                logger.info(\"Validation error: {}\", validationResponse.getErrorMessage());\n            }\n            \n        } catch (Exception e) {\n            logger.error(\"Error in exception handling demonstration\", e);\n        }\n    }\n    \n    /**\n     * 演示性能测试\n     */\n    private void demonstratePerformanceTest() {\n        logger.info(\"\\n=== Demonstrating Performance Test ===\");\n        \n        int requestCount = 100;\n        logger.info(\"Running performance test with {} requests...\", requestCount);\n        \n        long startTime = System.currentTimeMillis();\n        int successCount = 0;\n        int errorCount = 0;\n        \n        for (int i = 0; i < requestCount; i++) {\n            try {\n                RpcResponse response = callUserService(\"getUserCount\", new Object[]{}, new Class[]{});\n                if (response.isSuccess()) {\n                    successCount++;\n                } else {\n                    errorCount++;\n                }\n            } catch (Exception e) {\n                errorCount++;\n                if (i % 20 == 0) {\n                    logger.debug(\"Request {} failed: {}\", i, e.getMessage());\n                }\n            }\n        }\n        \n        long endTime = System.currentTimeMillis();\n        long duration = endTime - startTime;\n        double qps = (double) requestCount / duration * 1000;\n        \n        logger.info(\"Performance test completed:\");\n        logger.info(\"  Total requests: {}\", requestCount);\n        logger.info(\"  Successful: {}\", successCount);\n        logger.info(\"  Failed: {}\", errorCount);\n        logger.info(\"  Duration: {}ms\", duration);\n        logger.info(\"  QPS: {:.2f}\", qps);\n        logger.info(\"  Average latency: {:.2f}ms\", (double) duration / requestCount);\n    }\n    \n    /**\n     * 调用用户服务（同步）\n     */\n    private RpcResponse callUserService(String methodName, Object[] parameters, Class<?>[] parameterTypes) \n            throws Exception {\n        RpcRequest request = new RpcRequest();\n        request.setInterfaceName(UserService.class.getName());\n        request.setMethodName(methodName);\n        request.setParameters(parameters);\n        request.setParameterTypes(parameterTypes);\n        \n        return client.sendRequest(request);\n    }\n    \n    /**\n     * 调用用户服务（异步）\n     */\n    private RpcFuture callUserServiceAsync(String methodName, Object[] parameters, Class<?>[] parameterTypes) \n            throws Exception {\n        RpcRequest request = new RpcRequest();\n        request.setInterfaceName(UserService.class.getName());\n        request.setMethodName(methodName);\n        request.setParameters(parameters);\n        request.setParameterTypes(parameterTypes);\n        \n        return client.sendRequestAsync(request);\n    }\n    \n    /**\n     * 清理资源\n     */\n    private void cleanup() {\n        logger.info(\"\\nCleaning up resources...\");\n        \n        if (client != null && client.isStarted()) {\n            try {\n                client.shutdown();\n                logger.info(\"RPC client shutdown completed\");\n            } catch (Exception e) {\n                logger.error(\"Error during client shutdown\", e);\n            }\n        }\n    }\n    \n    /**\n     * 创建高性能客户端配置\n     */\n    public static NetworkConfig createHighPerformanceClientConfig() {\n        return NetworkConfig.clientConfig()\n            .setConnectTimeout(3000)                // 连接超时\n            .setRequestTimeout(5000)                // 请求超时\n            .setIoThreads(Runtime.getRuntime().availableProcessors())  // IO线程数等于CPU核心数\n            .setUseEpoll(true)                      // 启用Epoll\n            .setTcpNodelay(true)                    // 禁用Nagle算法\n            .setKeepAlive(true)                     // 启用Keep-Alive\n            .setRecvBufferSize(128 * 1024)          // 增大接收缓冲区\n            .setSendBufferSize(128 * 1024)          // 增大发送缓冲区\n            .setUsePooledAllocator(true)            // 使用池化内存分配器\n            .setHeartbeatInterval(30000);           // 心跳间隔\n    }\n}
# RPC Framework API 使用指南

## 概述

本文档提供 RPC Framework 的完整 API 使用指南和最佳实践。RPC Framework 是基于 Java + Netty 构建的企业级远程过程调用框架，支持高性能、高并发的分布式服务通信。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rpcframework</groupId>
    <artifactId>rpc-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义服务接口

```java
public interface UserService {
    User getUserById(Long userId);
    User createUser(User user);
    List<User> getAllUsers();
}
```

### 3. 实现服务

```java
@Component
public class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long userId) {
        // 业务逻辑实现
        return userRepository.findById(userId);
    }
    
    @Override 
    public User createUser(User user) {
        // 业务逻辑实现
        return userRepository.save(user);
    }
    
    @Override
    public List<User> getAllUsers() {
        // 业务逻辑实现
        return userRepository.findAll();
    }
}
```

### 4. 启动服务端

```java
public class ServerApplication {
    public static void main(String[] args) throws Exception {
        // 创建网络配置
        NetworkConfig serverConfig = NetworkConfig.serverConfig()
            .setServerPort(8080)
            .setWorkerThreads(8)
            .setMaxConnections(1000);
        
        // 创建请求处理器
        RpcRequestHandler requestHandler = new RpcRequestHandler();
        
        // 注册服务
        UserService userService = new UserServiceImpl();
        requestHandler.registerService(UserService.class, userService);
        
        // 启动服务端
        RpcServer server = new RpcServer(serverConfig, requestHandler);
        server.start();
        
        System.out.println("RPC Server started on port 8080");
        
        // 保持运行
        Thread.currentThread().join();
    }
}
```

### 5. 客户端调用

```java
public class ClientApplication {
    public static void main(String[] args) throws Exception {
        // 创建客户端配置
        NetworkConfig clientConfig = NetworkConfig.clientConfig()
            .setConnectTimeout(3000)
            .setRequestTimeout(10000);
        
        // 创建客户端
        RpcClient client = new RpcClient("localhost", 8080, clientConfig);
        client.start();
        client.connect();
        
        // 同步调用
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(UserService.class.getName());
        request.setMethodName("getUserById");
        request.setParameters(new Object[]{1L});
        request.setParameterTypes(new Class[]{Long.class});
        
        RpcResponse response = client.sendRequest(request);
        if (response.isSuccess()) {
            User user = (User) response.getResult();
            System.out.println("获取用户: " + user);
        }
        
        // 异步调用
        RpcFuture future = client.sendRequestAsync(request);
        future.onSuccess(() -> {
            try {
                User user = (User) future.getResponse().getResult();
                System.out.println("异步获取用户: " + user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // 清理资源
        client.shutdown();
    }
}
```

## 核心 API 详解

### NetworkConfig 网络配置

NetworkConfig 提供了灵活的网络参数配置：

```java
// 服务端配置
NetworkConfig serverConfig = NetworkConfig.serverConfig()
    .setServerPort(8080)                    // 服务端口
    .setBossThreads(1)                      // Boss线程数
    .setWorkerThreads(8)                    // Worker线程数  
    .setMaxConnections(1000)                // 最大连接数
    .setBacklog(128)                        // 连接队列大小
    .setUseEpoll(true)                      // 启用Epoll (Linux)
    .setTcpNodelay(true)                    // 禁用Nagle算法
    .setKeepAlive(true)                     // 启用Keep-Alive
    .setRecvBufferSize(64 * 1024)           // 接收缓冲区大小
    .setSendBufferSize(64 * 1024)           // 发送缓冲区大小
    .setUsePooledAllocator(true);           // 使用池化内存分配器

// 客户端配置
NetworkConfig clientConfig = NetworkConfig.clientConfig()
    .setConnectTimeout(3000)                // 连接超时时间
    .setRequestTimeout(10000)               // 请求超时时间
    .setIoThreads(4)                        // IO线程数
    .setMaxPoolSize(20)                     // 连接池最大大小
    .setMinPoolSize(2)                      // 连接池最小大小
    .setMaxRetries(3)                       // 最大重试次数
    .setRetryInterval(1000)                 // 重试间隔
    .setHeartbeatInterval(30000);           // 心跳间隔
```

### RpcServer 服务端

RpcServer 负责处理客户端请求：

```java
// 创建服务端
RpcServer server = new RpcServer(networkConfig, requestHandler);

// 启动服务端
server.start();

// 检查状态
boolean isStarted = server.isStarted();

// 停止服务端
server.shutdown();
```

### RpcRequestHandler 请求处理器

请求处理器负责服务注册和方法调用：

```java
RpcRequestHandler handler = new RpcRequestHandler();

// 注册服务
handler.registerService(UserService.class, userServiceImpl);
handler.registerService("com.example.OrderService", orderServiceImpl);

// 获取注册信息
int serviceCount = handler.getServiceCount();
String[] serviceNames = handler.getRegisteredServices();
boolean isRegistered = handler.isServiceRegistered("com.example.UserService");

// 注销服务
handler.unregisterService(UserService.class);
handler.clearServices();
```

### RpcClient 客户端

RpcClient 提供同步和异步调用能力：

```java
// 创建客户端
RpcClient client = new RpcClient("localhost", 8080, config);

// 启动和连接
client.start();
client.connect();

// 同步调用
RpcResponse response = client.sendRequest(request);
RpcResponse response = client.sendRequest(request, 5000); // 指定超时

// 异步调用
RpcFuture future = client.sendRequestAsync(request);
RpcFuture future = client.sendRequestAsync(request, 8000); // 指定超时

// 状态检查
boolean isStarted = client.isStarted();
boolean isConnected = client.isConnected();
InetSocketAddress serverAddress = client.getServerAddress();

// 断开和关闭
client.disconnect();
client.shutdown();
```

### RpcFuture 异步Future

RpcFuture 支持异步编程模式：

```java
RpcFuture future = client.sendRequestAsync(request);

// 设置回调
future.onSuccess(() -> {
    System.out.println("请求成功: " + future.getResponse().getResult());
});

future.onFailure(() -> {
    System.out.println("请求失败: " + future.getException().getMessage());
});

future.onComplete(() -> {
    System.out.println("请求完成，无论成功还是失败");
});

// 阻塞等待结果
RpcResponse response = future.get();
RpcResponse response = future.get(10, TimeUnit.SECONDS);

// 状态检查
boolean isDone = future.isDone();
boolean isCancelled = future.isCancelled();
boolean isSuccess = future.isSuccess();
boolean isFailure = future.isFailure();

// 获取信息
String requestId = future.getRequestId();
long elapsedTime = future.getElapsedTime();

// 取消请求
future.cancel(true);
```

### RpcRequest/RpcResponse 请求响应模型

```java
// 构建请求
RpcRequest request = new RpcRequest();
request.setRequestId("req-001");
request.setInterfaceName("com.example.UserService");
request.setMethodName("getUserById");
request.setParameters(new Object[]{1L});
request.setParameterTypes(new Class[]{Long.class});

// 处理响应
RpcResponse response = client.sendRequest(request);
if (response.isSuccess()) {
    Object result = response.getResult();
    long processingTime = response.getProcessingTime();
} else {
    int errorCode = response.getErrorCode();
    String errorMessage = response.getErrorMessage();
    Throwable exception = response.getException();
}
```

## 高级特性

### 1. 连接池管理

```java
// 客户端自动管理连接池
NetworkConfig config = NetworkConfig.clientConfig()
    .setMaxPoolSize(50)     // 最大连接数
    .setMinPoolSize(5);     // 最小连接数

RpcClient client = new RpcClient("localhost", 8080, config);
```

### 2. 超时和重试

```java
// 全局超时配置
NetworkConfig config = NetworkConfig.clientConfig()
    .setConnectTimeout(3000)        // 连接超时
    .setRequestTimeout(10000)       // 请求超时
    .setMaxRetries(3)               // 最大重试次数
    .setRetryInterval(1000);        // 重试间隔

// 单次请求超时
RpcResponse response = client.sendRequest(request, 5000);
RpcFuture future = client.sendRequestAsync(request, 8000);
```

### 3. 错误处理

```java
try {
    RpcResponse response = client.sendRequest(request);
    if (!response.isSuccess()) {
        // 处理业务错误
        System.err.println("业务错误: " + response.getErrorMessage());
    }
} catch (NetworkException e) {
    // 处理网络错误
    System.err.println("网络错误: " + e.getMessage());
} catch (TimeoutException e) {
    // 处理超时错误
    System.err.println("超时错误: " + e.getMessage());
}
```

### 4. 异步编程模式

```java
// 链式调用
client.sendRequestAsync(getUserRequest)
    .onSuccess(() -> {
        System.out.println("用户获取成功");
        // 继续下一个异步调用
        return client.sendRequestAsync(getOrdersRequest);
    })
    .onFailure(() -> {
        System.out.println("用户获取失败");
    });

// 并发调用
List<RpcFuture> futures = new ArrayList<>();
for (Long userId : userIds) {
    RpcRequest request = createGetUserRequest(userId);
    RpcFuture future = client.sendRequestAsync(request);
    futures.add(future);
}

// 等待所有请求完成
for (RpcFuture future : futures) {
    try {
        RpcResponse response = future.get(10, TimeUnit.SECONDS);
        // 处理结果
    } catch (Exception e) {
        // 处理异常
    }
}
```

## 性能调优指南

### 1. 服务端调优

```java
// 高性能服务端配置
NetworkConfig serverConfig = NetworkConfig.serverConfig()
    .setServerPort(8080)
    .setBossThreads(1)                              // Boss线程通常1个足够
    .setWorkerThreads(Runtime.getRuntime().availableProcessors() * 2)  // Worker线程数
    .setMaxConnections(5000)                        // 增大最大连接数
    .setBacklog(1024)                               // 增大连接队列
    .setUseEpoll(true)                              // Linux环境启用Epoll
    .setTcpNodelay(true)                            // 禁用Nagle算法降低延迟
    .setKeepAlive(true)                             // 启用Keep-Alive
    .setRecvBufferSize(128 * 1024)                  // 增大接收缓冲区
    .setSendBufferSize(128 * 1024)                  // 增大发送缓冲区
    .setUsePooledAllocator(true)                    // 启用内存池
    .setUseDirectMemory(true);                      // 启用直接内存
```

### 2. 客户端调优

```java
// 高性能客户端配置
NetworkConfig clientConfig = NetworkConfig.clientConfig()
    .setConnectTimeout(3000)
    .setRequestTimeout(10000)
    .setIoThreads(Runtime.getRuntime().availableProcessors())
    .setMaxPoolSize(50)                             // 增大连接池
    .setMinPoolSize(5)
    .setUseEpoll(true)                              // Linux环境启用Epoll
    .setTcpNodelay(true)                            // 禁用Nagle算法
    .setKeepAlive(true)                             // 启用Keep-Alive
    .setRecvBufferSize(128 * 1024)                  // 增大缓冲区
    .setSendBufferSize(128 * 1024)
    .setUsePooledAllocator(true)                    // 启用内存池
    .setUseDirectMemory(true);                      // 启用直接内存
```

### 3. JVM调优

```bash
# 启动参数
java -server \
  -Xmx4g -Xms4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=20 \
  -XX:+UseCompressedOops \
  -XX:+DisableExplicitGC \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8 \
  -jar your-application.jar
```

### 4. 系统调优 (Linux)

```bash
# 增加文件描述符限制
ulimit -n 65536

# 网络参数调优
echo 'net.ipv4.tcp_fin_timeout = 30' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_keepalive_time = 120' >> /etc/sysctl.conf
echo 'net.core.somaxconn = 65535' >> /etc/sysctl.conf
sysctl -p
```

## 最佳实践

### 1. 服务设计原则

- **接口幂等性**: 确保重复调用不会产生副作用
- **超时设置**: 为每个方法设置合理的超时时间
- **异常处理**: 定义清晰的异常层次结构
- **参数验证**: 在服务端进行严格的参数校验

### 2. 客户端使用建议

- **连接复用**: 使用连接池，避免频繁创建连接
- **异步优先**: 优先使用异步调用提高吞吐量
- **超时控制**: 设置合理的连接和请求超时时间
- **错误重试**: 实现指数退避的重试机制

### 3. 监控和调试

```java
// 启用详细日志
Logger logger = LoggerFactory.getLogger(RpcClient.class);
logger.info("发送请求: {}", request.getRequestId());

// 性能监控
long startTime = System.currentTimeMillis();
RpcResponse response = client.sendRequest(request);
long duration = System.currentTimeMillis() - startTime;
logger.info("请求耗时: {}ms", duration);

// 异常监控
try {
    RpcResponse response = client.sendRequest(request);
} catch (Exception e) {
    logger.error("请求失败", e);
    // 上报监控系统
    monitoringSystem.reportError(e);
}
```

### 4. 生产环境部署

- **资源配置**: 根据业务量合理配置线程池和连接池大小
- **监控告警**: 监控QPS、延迟、错误率等关键指标
- **日志管理**: 配置结构化日志和日志轮转
- **健康检查**: 实现服务健康检查接口

## 示例代码

完整的示例代码请参考项目中的 `rpc-examples` 模块：

- **服务端示例**: `RpcServerExample.java`
- **客户端示例**: `RpcClientExample.java`
- **异步调用示例**: `AsyncCallExample.java`
- **性能测试示例**: `RpcBenchmarkTest.java`

## 故障排查

### 常见问题

1. **连接超时**
   - 检查网络连通性
   - 增加连接超时时间
   - 检查服务端是否启动

2. **请求超时**
   - 增加请求超时时间
   - 检查服务端处理性能
   - 查看网络延迟

3. **序列化错误**
   - 确保参数类型正确
   - 检查对象是否可序列化
   - 验证JSON格式

4. **内存泄漏**
   - 检查连接是否正确关闭
   - 监控JVM内存使用
   - 使用内存分析工具

### 调试技巧

```java
// 启用调试日志
<logger name="com.rpcframework" level="DEBUG" />

// 打印网络包内容
NetworkConfig config = NetworkConfig.clientConfig()
    .setDebugMode(true);     // 启用调试模式

// 监控连接状态
RpcClient client = new RpcClient("localhost", 8080);
System.out.println("是否已连接: " + client.isConnected());
System.out.println("服务端地址: " + client.getServerAddress());
```

## 版本兼容性

- **Java版本**: 支持 Java 8 及以上版本
- **Netty版本**: 基于 Netty 4.1.x
- **Jackson版本**: 使用 Jackson 2.14.x 进行JSON序列化

## 总结

RPC Framework 提供了简单易用的API和强大的性能，支持企业级应用的分布式服务通信需求。通过合理的配置和调优，可以实现3000+ QPS的高性能调用，满足大部分业务场景的要求。

如有问题或建议，请参考项目文档或提交Issue。
# RPC框架阶段1：实现指导手册

## 核心模块实现指南

基于[RPC框架阶段1设计蓝图](../protocols/rpc-stage1-design-blueprint.md)，本文档提供具体的实现指导和代码结构建议。

## 项目结构创建

### Maven模块结构
```
rpc-framework/
├── pom.xml                           # 父工程POM
├── rpc-core/                         # 核心模块
│   ├── pom.xml
│   └── src/main/java/com/yourname/rpc/
│       ├── protocol/                 # 协议层
│       │   ├── RpcProtocol.java     # 协议常量
│       │   ├── RpcRequest.java      # 请求模型
│       │   ├── RpcResponse.java     # 响应模型
│       │   ├── RpcEncoder.java      # 编码器
│       │   └── RpcDecoder.java      # 解码器
│       ├── proxy/                    # 代理层
│       │   ├── RpcProxyFactory.java # 代理工厂
│       │   └── RpcInvocationHandler.java # 调用处理器
│       ├── transport/                # 传输层
│       │   ├── client/
│       │   │   ├── NettyRpcClient.java
│       │   │   ├── RpcClientHandler.java
│       │   │   ├── ResponseFuture.java
│       │   │   └── ResponseFutureManager.java
│       │   └── server/
│       │       ├── NettyRpcServer.java
│       │       ├── RpcServerHandler.java
│       │       └── ServiceInvoker.java
│       ├── serialize/                # 序列化层
│       │   ├── Serializer.java      # 序列化接口
│       │   ├── JsonSerializer.java  # JSON实现
│       │   └── SerializerManager.java
│       ├── common/                   # 公共组件
│       │   ├── exception/           # 异常类
│       │   └── utils/               # 工具类
│       └── RpcClient.java            # 客户端门面
│       └── RpcServer.java            # 服务端门面
├── rpc-example/                      # 示例模块
│   ├── example-api/                 # 接口定义
│   ├── example-provider/            # 服务提供者
│   └── example-consumer/            # 服务消费者
└── README.md
```

### POM依赖配置

**父工程pom.xml：**
```xml
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<project xmlns=\"http://maven.apache.org/POM/4.0.0\"
         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd\">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.yourname</groupId>
    <artifactId>rpc-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <netty.version>4.1.86.Final</netty.version>
        <jackson.version>2.14.2</jackson.version>
        <slf4j.version>1.7.36</slf4j.version>
        <junit.version>4.13.2</junit.version>
    </properties>
    
    <modules>
        <module>rpc-core</module>
        <module>rpc-example</module>
    </modules>
    
    <dependencyManagement>
        <dependencies>
            <!-- Netty -->
            <dependency>
                <groupId>io.netty</groupId>
                <artifactId>netty-all</artifactId>
                <version>${netty.version}</version>
            </dependency>
            
            <!-- Jackson -->
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-databind</artifactId>
                <version>${jackson.version}</version>
            </dependency>
            
            <!-- Logging -->
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>${slf4j.version}</version>
            </dependency>
            
            <!-- Test -->
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**rpc-core模块pom.xml：**
```xml
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<project xmlns=\"http://maven.apache.org/POM/4.0.0\"
         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd\">
    <parent>
        <artifactId>rpc-framework</artifactId>
        <groupId>com.yourname</groupId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    
    <artifactId>rpc-core</artifactId>
    
    <dependencies>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

## 核心接口定义

### 1. 协议层接口

**RpcProtocol.java - 协议常量定义：**
```java
package com.yourname.rpc.protocol;

/**
 * RPC协议常量定义
 */
public class RpcProtocol {
    
    /** 协议魔数 */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    
    /** 协议版本 */
    public static final byte VERSION = 1;
    
    /** 协议头长度 */
    public static final int HEADER_LENGTH = 12;
    
    /** 最大帧长度 */
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024; // 16MB
    
    /** 序列化类型 */
    public static final class CodecType {
        public static final byte JSON = 0;
        public static final byte PROTOBUF = 1;
        public static final byte HESSIAN = 2;
    }
    
    /** 消息类型 */
    public static final class MessageType {
        public static final byte REQUEST = 0;
        public static final byte RESPONSE = 1;
    }
    
    /** 响应状态码 */
    public static final class Status {
        public static final int SUCCESS = 200;
        public static final int BAD_REQUEST = 400;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
    }
}
```

**RpcRequest.java - 请求模型：**
```java
package com.yourname.rpc.protocol;

import java.io.Serializable;
import java.util.Arrays;

/**
 * RPC请求对象
 */
public class RpcRequest implements Serializable {
    
    /** 请求唯一标识 */
    private String requestId;
    
    /** 接口类名 */
    private String className;
    
    /** 方法名 */
    private String methodName;
    
    /** 参数类型数组 */
    private Class<?>[] parameterTypes;
    
    /** 参数值数组 */
    private Object[] parameters;
    
    /** 版本号 */
    private String version;
    
    /** 创建时间戳 */
    private long createTime;
    
    public RpcRequest() {
        this.createTime = System.currentTimeMillis();
    }
    
    // 构造函数
    public RpcRequest(String requestId, String className, String methodName, 
                     Class<?>[] parameterTypes, Object[] parameters) {
        this();
        this.requestId = requestId;
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }
    
    // Getter和Setter方法
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public Class<?>[] getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(Class<?>[] parameterTypes) { this.parameterTypes = parameterTypes; }
    
    public Object[] getParameters() { return parameters; }
    public void setParameters(Object[] parameters) { this.parameters = parameters; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    
    @Override
    public String toString() {
        return \"RpcRequest{\" +
                \"requestId='\" + requestId + '\\'' +
                \", className='\" + className + '\\'' +
                \", methodName='\" + methodName + '\\'' +
                \", parameterTypes=\" + Arrays.toString(parameterTypes) +
                \", parameters=\" + Arrays.toString(parameters) +
                \", version='\" + version + '\\'' +
                '}';
    }
}
```

**RpcResponse.java - 响应模型：**
```java
package com.yourname.rpc.protocol;

import java.io.Serializable;

/**
 * RPC响应对象
 */
public class RpcResponse implements Serializable {
    
    /** 请求唯一标识 */
    private String requestId;
    
    /** 响应状态码 */
    private int status;
    
    /** 响应结果 */
    private Object result;
    
    /** 异常信息 */
    private String exception;
    
    /** 响应时间戳 */
    private long timestamp;
    
    public RpcResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // 构造函数
    public RpcResponse(String requestId) {
        this();
        this.requestId = requestId;
    }
    
    // 静态工厂方法
    public static RpcResponse success(String requestId, Object result) {
        RpcResponse response = new RpcResponse(requestId);
        response.setStatus(RpcProtocol.Status.SUCCESS);
        response.setResult(result);
        return response;
    }
    
    public static RpcResponse failure(String requestId, String exception) {
        RpcResponse response = new RpcResponse(requestId);
        response.setStatus(RpcProtocol.Status.INTERNAL_ERROR);
        response.setException(exception);
        return response;
    }
    
    // Getter和Setter方法
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    
    public String getException() { return exception; }
    public void setException(String exception) { this.exception = exception; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    // 工具方法
    public boolean isSuccess() {
        return status == RpcProtocol.Status.SUCCESS;
    }
    
    @Override
    public String toString() {
        return \"RpcResponse{\" +
                \"requestId='\" + requestId + '\\'' +
                \", status=\" + status +
                \", result=\" + result +
                \", exception='\" + exception + '\\'' +
                \", timestamp=\" + timestamp +
                '}';
    }
}
```

### 2. 序列化接口

**Serializer.java - 序列化抽象接口：**
```java
package com.yourname.rpc.serialize;

import com.yourname.rpc.common.exception.SerializationException;

/**
 * 序列化器接口
 */
public interface Serializer {
    
    /**
     * 序列化对象为字节数组
     * 
     * @param obj 待序列化对象
     * @return 字节数组
     * @throws SerializationException 序列化异常
     */
    <T> byte[] serialize(T obj) throws SerializationException;
    
    /**
     * 反序列化字节数组为对象
     * 
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @return 反序列化对象
     * @throws SerializationException 反序列化异常
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException;
    
    /**
     * 获取序列化器类型
     * 
     * @return 类型标识
     */
    byte getCodecType();
    
    /**
     * 获取序列化器名称
     * 
     * @return 名称
     */
    String getName();
}
```

### 3. 传输层接口

**RpcClient.java - 客户端门面接口：**
```java
package com.yourname.rpc;

/**
 * RPC客户端门面接口
 */
public interface RpcClient {
    
    /**
     * 创建服务代理对象
     * 
     * @param serviceInterface 服务接口
     * @param serverHost 服务端主机
     * @param serverPort 服务端端口
     * @return 代理对象
     */
    <T> T createProxy(Class<T> serviceInterface, String serverHost, int serverPort);
    
    /**
     * 设置超时时间
     * 
     * @param timeoutMs 超时时间(毫秒)
     */
    void setTimeout(int timeoutMs);
    
    /**
     * 关闭客户端
     */
    void close();
}
```

**RpcServer.java - 服务端门面接口：**
```java
package com.yourname.rpc;

/**
 * RPC服务端门面接口
 */
public interface RpcServer {
    
    /**
     * 注册服务
     * 
     * @param serviceName 服务名称
     * @param serviceImpl 服务实现
     */
    void registerService(String serviceName, Object serviceImpl);
    
    /**
     * 启动服务端
     * 
     * @param port 监听端口
     */
    void start(int port);
    
    /**
     * 停止服务端
     */
    void stop();
    
    /**
     * 检查服务端是否运行中
     * 
     * @return true表示运行中
     */
    boolean isRunning();
}
```

## 异常体系设计

### 基础异常类

**RpcException.java - RPC基础异常：**
```java
package com.yourname.rpc.common.exception;

/**
 * RPC框架基础异常
 */
public class RpcException extends RuntimeException {
    
    private int errorCode;
    
    public RpcException(String message) {
        super(message);
    }
    
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RpcException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RpcException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
```

**具体异常类：**
```java
// 序列化异常
public class SerializationException extends RpcException {
    public SerializationException(String message) { super(message); }
    public SerializationException(String message, Throwable cause) { super(message, cause); }
}

// 网络异常
public class NetworkException extends RpcException {
    public NetworkException(String message) { super(message); }
    public NetworkException(String message, Throwable cause) { super(message, cause); }
}

// 超时异常
public class TimeoutException extends RpcException {
    public TimeoutException(String message) { super(message); }
}

// 服务未找到异常
public class ServiceNotFoundException extends RpcException {
    public ServiceNotFoundException(String message) { super(message); }
}
```

## 工具类设计

### UUID生成器

**UUIDUtil.java：**
```java
package com.yourname.rpc.common.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器工具类
 */
public class UUIDUtil {
    
    private static final AtomicLong COUNTER = new AtomicLong(0);
    private static final String MACHINE_ID = getMachineId();
    
    /**
     * 生成UUID字符串
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace(\"-\", \"\");
    }
    
    /**
     * 生成有序ID（更高性能）
     */
    public static String generateId() {
        long timestamp = System.currentTimeMillis();
        long counter = COUNTER.incrementAndGet();
        return MACHINE_ID + timestamp + String.format(\"%06d\", counter % 1000000);
    }
    
    private static String getMachineId() {
        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            return String.valueOf(hostname.hashCode() & 0xFFFF);
        } catch (Exception e) {
            return \"0000\";
        }
    }
}
```

### 线程池工具

**ThreadPoolUtil.java：**
```java
package com.yourname.rpc.common.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类
 */
public class ThreadPoolUtil {
    
    /**
     * 创建可监控的线程池
     */
    public static ThreadPoolExecutor createThreadPool(String threadNamePrefix, 
                                                     int corePoolSize, 
                                                     int maximumPoolSize, 
                                                     int queueCapacity) {
        return new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new NamedThreadFactory(threadNamePrefix),
            new ThreadPoolExecutor.CallerRunsPolicy() // 调用者运行策略
        );
    }
    
    /**
     * 命名线程工厂
     */
    public static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix + \"-thread-\";
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
    
    /**
     * 优雅关闭线程池
     */
    public static void shutdown(ExecutorService executor) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println(\"Thread pool did not terminate\");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

## 开发顺序建议

### 第一阶段：基础设施（1-2天）

**Day 1: 项目结构与基础类**
1. 创建Maven多模块项目结构
2. 配置POM依赖
3. 实现协议常量类（RpcProtocol）
4. 实现请求响应模型（RpcRequest/RpcResponse）
5. 定义异常体系
6. 实现工具类（UUIDUtil, ThreadPoolUtil）

**Day 2: 序列化模块**
1. 定义Serializer接口
2. 实现JsonSerializer
3. 实现SerializerManager
4. 编写序列化测试用例

### 第二阶段：网络通信（3-4天）

**Day 3: 协议编解码**
1. 实现RpcEncoder
2. 实现RpcDecoder
3. 编写编解码测试用例

**Day 4: 客户端实现**
1. 实现ResponseFuture
2. 实现ResponseFutureManager
3. 实现NettyRpcClient
4. 实现RpcClientHandler

**Day 5: 服务端实现**
1. 实现ServiceInvoker
2. 实现NettyRpcServer
3. 实现RpcServerHandler

### 第三阶段：代理与集成（2-3天）

**Day 6: 动态代理**
1. 实现RpcInvocationHandler
2. 实现RpcProxyFactory
3. 实现客户端门面（RpcClientImpl）
4. 实现服务端门面（RpcServerImpl）

**Day 7: 端到端测试**
1. 创建示例服务接口
2. 实现示例服务
3. 编写集成测试
4. 性能测试和调优

### 验证检查清单

**功能验证：**
- [ ] 协议编解码正确性
- [ ] JSON序列化反序列化
- [ ] 动态代理生成和调用
- [ ] 客户端服务端通信
- [ ] 异常处理和传播
- [ ] 超时控制机制

**性能验证：**
- [ ] 单机QPS达到目标
- [ ] 平均延迟符合预期
- [ ] 内存使用合理
- [ ] 无明显内存泄漏

**代码质量：**
- [ ] 单元测试覆盖率>70%
- [ ] 代码注释完整
- [ ] 异常处理完善
- [ ] 资源释放正确

## 常见问题和解决方案

### 1. Netty资源泄漏
**问题**：ByteBuf未正确释放导致堆外内存泄漏  
**解决**：
- 在Handler中使用try-finally确保释放
- 启用Netty内存泄漏检测
- 定期监控DirectByteBuffer使用量

### 2. 序列化异常
**问题**：复杂对象序列化失败  
**解决**：
- 确保对象实现Serializable接口
- 处理循环引用问题
- 为Jackson配置合适的特性

### 3. 连接超时
**问题**：客户端连接服务端超时  
**解决**：
- 设置合适的连接超时时间
- 检查网络连通性
- 确认服务端端口监听正常

### 4. 方法调用失败
**问题**：反射调用方法失败  
**解决**：
- 确认方法签名匹配
- 检查参数类型转换
- 处理方法访问权限问题

通过这个详细的实现指南，你可以按照既定的步骤和结构，高效地完成RPC框架阶段1的开发工作。
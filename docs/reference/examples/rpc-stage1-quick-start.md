# RPC框架阶段1：快速上手指南

## 快速开始步骤

基于[RPC框架阶段1实现指导手册](../api/rpc-stage1-implementation-guide.md)和[设计蓝图](../protocols/rpc-stage1-design-blueprint.md)，本文档提供快速上手的具体步骤。

## 环境准备

### 开发环境要求
- **JDK**: 1.8+
- **Maven**: 3.6+
- **IDE**: IntelliJ IDEA / Eclipse
- **Git**: 版本控制

### 依赖版本
- **Netty**: 4.1.86.Final
- **Jackson**: 2.14.2
- **SLF4J**: 1.7.36
- **JUnit**: 4.13.2

## Day 1: 项目初始化

### 1. 创建项目骨架

```bash
# 创建项目目录
mkdir rpc-framework && cd rpc-framework

# 初始化Git仓库
git init

# 创建Maven项目结构
mkdir -p rpc-core/src/{main,test}/java/com/yourname/rpc
mkdir -p rpc-example/{example-api,example-provider,example-consumer}/src/{main,test}/java
```

### 2. 配置POM文件

**父工程pom.xml**：参考[实现指导手册](../api/rpc-stage1-implementation-guide.md#maven模块结构)

### 3. 创建核心常量类

**RpcProtocol.java**：
```java
package com.yourname.rpc.protocol;

public class RpcProtocol {
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    public static final byte VERSION = 1;
    public static final int HEADER_LENGTH = 12;
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024;
    
    public static final class CodecType {
        public static final byte JSON = 0;
        public static final byte PROTOBUF = 1;
        public static final byte HESSIAN = 2;
    }
    
    public static final class MessageType {
        public static final byte REQUEST = 0;
        public static final byte RESPONSE = 1;
    }
    
    public static final class Status {
        public static final int SUCCESS = 200;
        public static final int BAD_REQUEST = 400;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
    }
}
```

### 4. 实现基础模型

创建RpcRequest和RpcResponse类，参考[实现指导手册](../api/rpc-stage1-implementation-guide.md#1-协议层接口)。

### 5. 验证环境

```java
// 简单测试类
public class ProtocolTest {
    @Test
    public void testConstants() {
        assertEquals(0xCAFEBABE, RpcProtocol.MAGIC_NUMBER);
        assertEquals(1, RpcProtocol.VERSION);
        assertEquals(12, RpcProtocol.HEADER_LENGTH);
    }
}
```

## Day 2: 序列化模块

### 1. 定义序列化接口

```java
package com.yourname.rpc.serialize;

public interface Serializer {
    <T> byte[] serialize(T obj) throws SerializationException;
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException;
    byte getCodecType();
    String getName();
}
```

### 2. 实现JSON序列化器

```java
package com.yourname.rpc.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class JsonSerializer implements Serializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new SerializationException(\"JSON serialization failed\", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException {
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (Exception e) {
            throw new SerializationException(\"JSON deserialization failed\", e);
        }
    }
    
    @Override
    public byte getCodecType() {
        return RpcProtocol.CodecType.JSON;
    }
    
    @Override
    public String getName() {
        return \"json\";
    }
}
```

### 3. 序列化管理器

```java
public class SerializerManager {
    private static final Map<Byte, Serializer> SERIALIZERS = new ConcurrentHashMap<>();
    private static volatile Serializer defaultSerializer;
    
    static {
        JsonSerializer jsonSerializer = new JsonSerializer();
        registerSerializer(jsonSerializer);
        defaultSerializer = jsonSerializer;
    }
    
    public static void registerSerializer(Serializer serializer) {
        SERIALIZERS.put(serializer.getCodecType(), serializer);
    }
    
    public static Serializer getSerializer(byte codecType) {
        return SERIALIZERS.get(codecType);
    }
    
    public static Serializer getDefaultSerializer() {
        return defaultSerializer;
    }
}
```

### 4. 测试序列化功能

```java
@Test
public void testJsonSerialization() {
    JsonSerializer serializer = new JsonSerializer();
    
    // 测试简单对象
    RpcRequest request = new RpcRequest();
    request.setRequestId(\"test-123\");
    request.setClassName(\"com.test.UserService\");
    request.setMethodName(\"getUser\");
    
    byte[] bytes = serializer.serialize(request);
    RpcRequest deserialized = serializer.deserialize(bytes, RpcRequest.class);
    
    assertEquals(request.getRequestId(), deserialized.getRequestId());
    assertEquals(request.getClassName(), deserialized.getClassName());
}
```

## Day 3: 协议编解码器

### 1. 实现编码器

```java
package com.yourname.rpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder<Object> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // 1. 确定序列化类型
        byte codecType = RpcProtocol.CodecType.JSON;
        Serializer serializer = SerializerManager.getSerializer(codecType);
        
        // 2. 序列化消息体
        byte[] bodyBytes = serializer.serialize(msg);
        
        // 3. 写入协议头
        out.writeInt(RpcProtocol.MAGIC_NUMBER);           // 魔数
        out.writeByte(RpcProtocol.VERSION);               // 版本
        out.writeByte(codecType);                         // 序列化类型
        out.writeByte(getMessageType(msg));               // 消息类型
        out.writeInt(bodyBytes.length);                   // 消息长度
        out.writeByte(0);                                 // 预留字段
        
        // 4. 写入消息体
        out.writeBytes(bodyBytes);
    }
    
    private byte getMessageType(Object msg) {
        return msg instanceof RpcRequest ? 
            RpcProtocol.MessageType.REQUEST : RpcProtocol.MessageType.RESPONSE;
    }
}
```

### 2. 实现解码器

```java
package com.yourname.rpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 1. 检查协议头长度
        if (in.readableBytes() < RpcProtocol.HEADER_LENGTH) {
            return;
        }
        
        // 2. 标记读位置
        in.markReaderIndex();
        
        // 3. 读取并验证魔数
        int magicNumber = in.readInt();
        if (magicNumber != RpcProtocol.MAGIC_NUMBER) {
            in.resetReaderIndex();
            throw new RpcException(\"Invalid magic number: \" + Integer.toHexString(magicNumber));
        }
        
        // 4. 读取协议头其他字段
        byte version = in.readByte();
        byte codecType = in.readByte();
        byte messageType = in.readByte();
        int bodyLength = in.readInt();
        byte reserved = in.readByte();
        
        // 5. 验证消息长度
        if (bodyLength > RpcProtocol.MAX_FRAME_LENGTH) {
            throw new RpcException(\"Frame length too large: \" + bodyLength);
        }
        
        // 6. 检查消息体长度
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }
        
        // 7. 读取消息体
        byte[] bodyBytes = new byte[bodyLength];
        in.readBytes(bodyBytes);
        
        // 8. 反序列化消息
        Serializer serializer = SerializerManager.getSerializer(codecType);
        Object message = deserializeMessage(serializer, messageType, bodyBytes);
        
        out.add(message);
    }
    
    private Object deserializeMessage(Serializer serializer, byte messageType, byte[] bodyBytes) 
            throws SerializationException {
        if (messageType == RpcProtocol.MessageType.REQUEST) {
            return serializer.deserialize(bodyBytes, RpcRequest.class);
        } else {
            return serializer.deserialize(bodyBytes, RpcResponse.class);
        }
    }
}
```

### 3. 编解码测试

```java
@Test
public void testCodec() {
    EmbeddedChannel channel = new EmbeddedChannel(new RpcEncoder(), new RpcDecoder());
    
    // 创建测试请求
    RpcRequest request = new RpcRequest();
    request.setRequestId(\"test-123\");
    request.setClassName(\"com.test.UserService\");
    request.setMethodName(\"getUser\");
    request.setParameterTypes(new Class[]{Long.class});
    request.setParameters(new Object[]{123L});
    
    // 写入请求
    channel.writeOutbound(request);
    
    // 读取编码后的数据
    ByteBuf encoded = channel.readOutbound();
    assertNotNull(encoded);
    
    // 写入解码器
    channel.writeInbound(encoded);
    
    // 读取解码后的对象
    RpcRequest decoded = channel.readInbound();
    assertNotNull(decoded);
    assertEquals(request.getRequestId(), decoded.getRequestId());
    assertEquals(request.getClassName(), decoded.getClassName());
}
```

## Day 4: 同步等待机制

### 1. 实现ResponseFuture

```java
package com.yourname.rpc.transport.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResponseFuture {
    private final String requestId;
    private final long createTime;
    private final int timeoutMs;
    private final CountDownLatch latch = new CountDownLatch(1);
    
    private volatile RpcResponse response;
    private volatile boolean done = false;
    
    public ResponseFuture(String requestId, int timeoutMs) {
        this.requestId = requestId;
        this.timeoutMs = timeoutMs;
        this.createTime = System.currentTimeMillis();
    }
    
    public RpcResponse get() throws InterruptedException, TimeoutException {
        return get(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    public RpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        boolean success = latch.await(timeout, unit);
        if (!success) {
            throw new TimeoutException(\"Request timeout after \" + timeout + \" \" + unit);
        }
        return response;
    }
    
    public void setResponse(RpcResponse response) {
        this.response = response;
        this.done = true;
        latch.countDown();
    }
    
    // getter方法...
    public String getRequestId() { return requestId; }
    public long getCreateTime() { return createTime; }
    public int getTimeoutMs() { return timeoutMs; }
    public boolean isDone() { return done; }
}
```

### 2. Future管理器

```java
package com.yourname.rpc.transport.client;

import java.util.concurrent.*;

public class ResponseFutureManager {
    private static final Map<String, ResponseFuture> FUTURE_MAP = new ConcurrentHashMap<>();
    
    // 定时清理器
    private static final ScheduledExecutorService SCHEDULER = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, \"ResponseFuture-Cleaner\");
            t.setDaemon(true);
            return t;
        });
    
    static {
        // 每5秒清理一次超时的Future
        SCHEDULER.scheduleAtFixedRate(ResponseFutureManager::cleanTimeoutFutures, 
                                     0, 5, TimeUnit.SECONDS);
    }
    
    public static void addFuture(ResponseFuture future) {
        FUTURE_MAP.put(future.getRequestId(), future);
    }
    
    public static ResponseFuture getFuture(String requestId) {
        return FUTURE_MAP.get(requestId);
    }
    
    public static ResponseFuture removeFuture(String requestId) {
        return FUTURE_MAP.remove(requestId);
    }
    
    private static void cleanTimeoutFutures() {
        long currentTime = System.currentTimeMillis();
        FUTURE_MAP.entrySet().removeIf(entry -> {
            ResponseFuture future = entry.getValue();
            return currentTime - future.getCreateTime() > future.getTimeoutMs();
        });
    }
    
    public static int getFutureCount() {
        return FUTURE_MAP.size();
    }
}
```

### 3. 测试同步等待

```java
@Test
public void testResponseFuture() throws Exception {
    String requestId = UUIDUtil.generateId();
    ResponseFuture future = new ResponseFuture(requestId, 5000);
    
    // 异步设置结果
    new Thread(() -> {
        try {
            Thread.sleep(100);
            RpcResponse response = RpcResponse.success(requestId, \"test result\");
            future.setResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }).start();
    
    // 同步等待结果
    RpcResponse response = future.get();
    assertNotNull(response);
    assertEquals(requestId, response.getRequestId());
    assertEquals(\"test result\", response.getResult());
}
```

## Day 5: 网络通信层

### 1. 客户端Handler

```java
package com.yourname.rpc.transport.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        ResponseFuture future = ResponseFutureManager.removeFuture(requestId);
        
        if (future != null) {
            future.setResponse(response);
        } else {
            logger.warn(\"Received response for unknown request: {}\", requestId);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(\"Client handler exception\", cause);
        ctx.close();
    }
}
```

### 2. 简化的客户端实现

```java
package com.yourname.rpc.transport.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyRpcClient {
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    
    public void init() {
        eventLoopGroup = new NioEventLoopGroup(1);
        bootstrap = new Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(\"encoder\", new RpcEncoder());
                    pipeline.addLast(\"decoder\", new RpcDecoder());
                    pipeline.addLast(\"handler\", new RpcClientHandler());
                }
            });
    }
    
    public RpcResponse sendRequest(RpcRequest request, String host, int port, int timeoutMs) 
            throws Exception {
        // 创建Future
        ResponseFuture future = new ResponseFuture(request.getRequestId(), timeoutMs);
        ResponseFutureManager.addFuture(future);
        
        try {
            // 连接服务端
            ChannelFuture connectFuture = bootstrap.connect(host, port);
            connectFuture.sync();
            Channel channel = connectFuture.channel();
            
            // 发送请求
            channel.writeAndFlush(request);
            
            // 等待响应
            return future.get();
        } finally {
            ResponseFutureManager.removeFuture(request.getRequestId());
        }
    }
    
    public void close() {
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
```

### 3. 测试网络通信

```java
@Test
public void testNetworkCommunication() throws Exception {
    // 创建客户端
    NettyRpcClient client = new NettyRpcClient();
    client.init();
    
    // 创建请求
    RpcRequest request = new RpcRequest();
    request.setRequestId(UUIDUtil.generateId());
    request.setClassName(\"com.test.UserService\");
    request.setMethodName(\"ping\");
    
    try {
        // 发送请求（需要先启动服务端）
        RpcResponse response = client.sendRequest(request, \"localhost\", 8080, 5000);
        assertNotNull(response);
        assertEquals(request.getRequestId(), response.getRequestId());
    } finally {
        client.close();
    }
}
```

## Day 6-7: 完整示例

### 1. 定义服务接口

```java
// example-api模块
public interface UserService {
    User getUser(Long id);
    String ping();
}

public class User {
    private Long id;
    private String name;
    private String email;
    
    // 构造函数、getter、setter...
}
```

### 2. 服务端实现

```java
// example-provider模块
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName(\"User-\" + id);
        user.setEmail(\"user\" + id + \"@example.com\");
        return user;
    }
    
    @Override
    public String ping() {
        return \"pong\";
    }
}

// 服务端启动类
public class ProviderApplication {
    public static void main(String[] args) throws Exception {
        RpcServer server = new RpcServerImpl();
        
        // 注册服务
        server.registerService(UserService.class.getName(), new UserServiceImpl());
        
        // 启动服务
        server.start(8080);
        
        System.out.println(\"RPC Server started on port 8080\");
        
        // 等待关闭
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread.currentThread().join();
    }
}
```

### 3. 客户端调用

```java
// example-consumer模块
public class ConsumerApplication {
    public static void main(String[] args) {
        RpcClient client = new RpcClientImpl();
        
        // 创建代理对象
        UserService userService = client.createProxy(UserService.class, \"localhost\", 8080);
        
        try {
            // 调用远程方法
            String result = userService.ping();
            System.out.println(\"Ping result: \" + result);
            
            User user = userService.getUser(123L);
            System.out.println(\"User: \" + user);
            
        } finally {
            client.close();
        }
    }
}
```

## 阶段验证

### 功能验证清单

- [ ] **协议编解码**：编码后解码能正确还原对象
- [ ] **序列化功能**：复杂对象序列化反序列化正确
- [ ] **网络通信**：客户端能连接服务端并发送请求
- [ ] **同步调用**：客户端能同步等待服务端响应
- [ ] **异常处理**：网络异常、超时异常能正确处理
- [ ] **资源释放**：连接、线程等资源能正确释放

### 性能验证

```java
@Test
public void performanceTest() throws Exception {
    RpcClient client = new RpcClientImpl();
    UserService userService = client.createProxy(UserService.class, \"localhost\", 8080);
    
    int requestCount = 1000;
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < requestCount; i++) {
        userService.ping();
    }
    
    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    long avgTime = totalTime / requestCount;
    
    System.out.println(\"Total requests: \" + requestCount);
    System.out.println(\"Total time: \" + totalTime + \"ms\");
    System.out.println(\"Average time: \" + avgTime + \"ms\");
    
    assertTrue(\"Average response time should be less than 10ms\", avgTime < 10);
    
    client.close();
}
```

### 内存检查

```bash
# 启动时添加内存监控参数
-XX:+PrintGC -XX:+PrintGCDetails 
-Dio.netty.leakDetection.level=paranoid
-Dio.netty.leakDetectionTargetRecords=true

# 压测后检查内存使用
jmap -histo <pid> | head -20
```

## 常见问题解决

### 1. 连接被拒绝
```
java.net.ConnectException: Connection refused
```
**解决方案**：
- 确认服务端已启动
- 检查端口是否正确
- 确认防火墙设置

### 2. 序列化失败
```
SerializationException: JSON serialization failed
```
**解决方案**：
- 确保对象有无参构造函数
- 检查对象是否有循环引用
- 添加Jackson注解配置

### 3. 请求超时
```
TimeoutException: Request timeout after 5000 ms
```
**解决方案**：
- 增加超时时间设置
- 检查网络延迟
- 优化服务端处理逻辑

通过这个快速上手指南，你可以在7天内完成RPC框架阶段1的基础实现，并具备端到端的通信能力。
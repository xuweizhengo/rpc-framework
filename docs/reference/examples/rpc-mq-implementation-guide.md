# RPC+MQ 分阶段实现指南

## 阶段 0：设计与技术选型 (1-2周)

### RPC 技术选型

**协议设计：**
- 魔数+版本+序列化类型+消息长度+请求ID+状态+数据体
- 参考Dubbo/gRPC设计

**通信层：**
- **Netty (必选)**
- 处理TCP粘包拆包(LengthFieldBasedFrameDecoder)

**序列化支持：**
- **JSON (默认)**
- **Hessian**
- **Protobuf (重点)**
- 设计可扩展接口

**注册中心：**
- 集成 **Nacos (推荐)** 或 Zookeeper
- 抽象`RegistryService`接口

**代理机制：**
- JDK动态代理或CGLib

**负载均衡：**
- Random, RoundRobin
- 一致性Hash (可选)

**容错机制：**
- 超时(必做)
- 重试(次数+间隔)
- 基础熔断器(状态机，错误率阈值)

### MQ 技术选型

**消息模型：**
- Topic (发布订阅) + Queue (点对点)
- 一个Topic可对应多个Queue

**存储设计：**
- **MVP：纯内存** (ConcurrentHashMap + Queue) -> 高性能但易失
- **增强：内存+文件混合**
  - 内存加速读写，文件保证持久化
  - 使用`MappedByteBuffer`或`FileChannel`
  - 设计存储格式(长度+消息属性+消息体)

**ACK机制：**
- 消费者手动ACK
- Broker维护消费进度(offset)

**网络通信：**
- 基于**Netty**
- 设计生产/消费协议

**高可用(可选)：**
- 主从复制(简单异步复制)

**控制台(可选)：**
- Web界面查看Topic/Queue状态、消息堆积等

## 阶段 1：核心功能实现 (RPC & MQ 基础版 - 2-3个月)

### RPC 基础版实现

1. **定义公共接口(API)**
2. **实现RpcClient：**
   - 封装动态代理
   - 服务发现
   - 负载均衡
   - 网络请求发送(Netty)
3. **实现RpcServer：**
   - 启动Netty Server
   - 注册服务实例
   - 接收请求并反射调用本地服务实现
4. **实现序列化模块(JSON, Protobuf)**
5. **集成注册中心(Nacos)：**
   - 实现服务注册与发现
6. **实现基础负载均衡(Random)**
7. **实现超时控制：**
   - 客户端Future + 超时Timer

### MQ 基础版实现

1. **定义Producer/Consumer接口**
2. **实现Broker核心：**
   - 启动Netty Server处理客户端连接
   - 消息接收：解析生产者请求，写入存储(内存Queue)
   - 消息投递：消费者拉取消息，维护offset
   - 实现ACK：消费者处理完消息后发送ACK，Broker更新offset并清理消息
3. **实现内存存储：**
   ```java
   ConcurrentHashMap<String/*Topic*/, 
       ConcurrentHashMap<Integer/*QueueId*/, Queue<Message>>>
   ```
4. **设计简单的消息协议：**
   - 生产消息
   - 拉取消息
   - ACK消息

## 阶段 2：功能增强与优化 (1-2个月)

### RPC 功能增强

**容错增强：**
- 实现重试机制(可配置次数、间隔、避让策略)
- 实现基础熔断器(滑动窗口计数错误率)

**负载均衡增强：**
- 实现RoundRobin、Weighted
- 考虑一致性Hash

**序列化增强：**
- 优化Protobuf序列化性能
- 考虑Kryo(注意兼容性)

**连接管理：**
- 实现连接池，复用Netty Channel

**异步调用：**
- 支持CompletableFuture

**SPI扩展：**
- 使用Java SPI或自定义SPI机制
- 让序列化、注册中心、负载均衡等可插拔

### MQ 功能增强

**存储增强：**
- 实现**文件存储** (最大难点和亮点！)
- 设计存储结构(CommitLog顺序写 + Index索引)
- 实现内存与文件的协同(PageCache)

**性能优化：**
- 批量消息发送/拉取
- 零拷贝优化(文件存储时)

**消息过滤：**
- 支持Tag过滤

**管理控制台：**
- 简易Web界面(Spring Boot + Vue/React)
- 展示Topic/Queue/消费组/堆积情况

**高可用(可选)：**
- 实现主从异步复制

## 阶段 3：稳定性、工程化与深度 (1个月+)

### 通用改进

**全面测试：**
- 单元测试(JUnit)
- 集成测试(Testcontainers)
- 压力测试(JMeter/Gatling)
- 记录性能报告

**监控告警(基础)：**
- 暴露Metrics(JMX/Prometheus格式)
- 集成Grafana看板(监控调用量、耗时、错误率、MQ堆积量等)

**日志完善：**
- 关键路径日志，方便问题排查

**文档完善：**
- 详细设计文档
- 部署文档
- 使用手册
- API文档
- 性能报告

**打包与部署：**
- Maven/Gradle打包
- Docker镜像制作

### 深入方向(选做加分项)

**RPC深入功能：**
- 泛化调用
- 灰度发布
- 自适应负载均衡

**MQ深入功能：**
- 事务消息(简化版)
- 延迟消息(基于定时轮询)
- 顺序消息(难点！)

## 总结行动建议

1. **立即动手设计协议和核心接口**
2. **搭建基础骨架**：Maven多模块项目结构
3. **实现最简核心链路**：先跑通基本流程
4. **尽早集成注册中心(Nacos)**
5. **边开发边记录**：技术决策、设计思路、踩坑经验
6. **严格遵循阶段划分**：先完成基础版(MVP)，再迭代增强
7. **测试驱动(TDD)**：核心逻辑务必写单元测试
package com.rpcframework.rpc.server;

import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * RPC请求处理器
 * 
 * <p>处理客户端发送的RPC请求，支持服务注册、方法调用和异常处理。
 * 基于反射机制实现动态方法调用，支持异步处理提升并发性能。
 * 
 * <p>主要功能：
 * <ul>
 * <li>服务注册：支持接口和实现类的注册管理</li>
 * <li>方法调用：基于反射的动态方法调用</li>
 * <li>异常处理：完整的异常捕获和响应机制</li>
 * <li>异步处理：支持异步执行提升性能</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcRequest> {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
    
    /**
     * 服务注册表：接口名 -> 服务实例
     */
    private final ConcurrentMap<String, Object> serviceRegistry = new ConcurrentHashMap<>();
    
    /**
     * 方法缓存：接口名#方法名 -> Method对象
     */
    private final ConcurrentMap<String, Method> methodCache = new ConcurrentHashMap<>();
    
    /**
     * 业务线程池，用于异步处理请求
     */
    private final Executor businessExecutor;
    
    public RpcRequestHandler() {
        this.businessExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "rpc-business-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    public RpcRequestHandler(Executor businessExecutor) {
        this.businessExecutor = businessExecutor;
    }
    
    /**
     * 注册服务
     * 
     * @param interfaceClass 服务接口类
     * @param serviceImpl 服务实现对象
     * @param <T> 服务类型
     */
    public <T> void registerService(Class<T> interfaceClass, T serviceImpl) {
        if (interfaceClass == null || serviceImpl == null) {
            throw new IllegalArgumentException("Interface class and service implementation cannot be null");
        }
        
        String serviceName = interfaceClass.getName();
        serviceRegistry.put(serviceName, serviceImpl);
        
        // 预热方法缓存
        cacheServiceMethods(interfaceClass, serviceName);
        
        logger.info("Registered service: {} -> {}", serviceName, serviceImpl.getClass().getName());
    }
    
    /**
     * 注册服务（基于服务名）
     * 
     * @param serviceName 服务名称
     * @param serviceImpl 服务实现对象
     */
    public void registerService(String serviceName, Object serviceImpl) {
        if (serviceName == null || serviceImpl == null) {
            throw new IllegalArgumentException("Service name and implementation cannot be null");
        }
        
        serviceRegistry.put(serviceName, serviceImpl);
        
        // 预热方法缓存
        cacheServiceMethods(serviceImpl.getClass(), serviceName);
        
        logger.info("Registered service: {} -> {}", serviceName, serviceImpl.getClass().getName());
    }
    
    /**
     * 预热服务方法缓存
     */
    private void cacheServiceMethods(Class<?> serviceClass, String serviceName) {
        Method[] methods = serviceClass.getMethods();
        for (Method method : methods) {
            // 跳过Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            
            String methodKey = buildMethodKey(serviceName, method.getName(), method.getParameterTypes());
            methodCache.put(methodKey, method);
        }
        
        logger.debug("Cached {} methods for service: {}", methods.length, serviceName);
    }
    
    /**
     * 构建方法缓存键
     */
    private String buildMethodKey(String serviceName, String methodName, Class<?>[] paramTypes) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(serviceName).append("#").append(methodName);
        
        if (paramTypes != null && paramTypes.length > 0) {
            keyBuilder.append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    keyBuilder.append(",");
                }
                keyBuilder.append(paramTypes[i].getName());
            }
            keyBuilder.append(")");
        }
        
        return keyBuilder.toString();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received RPC request: {}", request.getRequestId());
        }
        
        // 异步处理请求
        businessExecutor.execute(() -> {
            RpcResponse response = processRequest(request);
            
            // 发送响应
            ctx.writeAndFlush(response).addListener(future -> {
                if (future.isSuccess()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Response sent successfully for request: {}", request.getRequestId());
                    }
                } else {
                    logger.error("Failed to send response for request: {}", 
                        request.getRequestId(), future.cause());
                }
            });
        });
    }
    
    /**
     * 处理RPC请求
     */
    private RpcResponse processRequest(RpcRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数验证
            if (request.getInterfaceName() == null || request.getMethodName() == null) {
                return RpcResponse.failure(request.getRequestId(), 400, 
                    "Invalid request: interface name and method name are required");
            }
            
            // 查找服务实例
            Object serviceImpl = serviceRegistry.get(request.getInterfaceName());
            if (serviceImpl == null) {
                return RpcResponse.failure(request.getRequestId(), 404, 
                    "Service not found: " + request.getInterfaceName());
            }
            
            // 查找方法
            Method method = findMethod(request);
            if (method == null) {
                return RpcResponse.failure(request.getRequestId(), 404, 
                    "Method not found: " + request.getMethodName());
            }
            
            // 转换参数类型
            Object[] convertedParams = convertParameters(request.getParameters(), method.getParameterTypes());
            
            // 调用方法
            Object result = method.invoke(serviceImpl, convertedParams);
            
            // 构建成功响应
            RpcResponse response = RpcResponse.success(request.getRequestId(), result);
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Request processed successfully: {}, processing time: {}ms", 
                    request.getRequestId(), response.getProcessingTime());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing request: {}", request.getRequestId(), e);
            
            // 构建异常响应
            RpcResponse response = RpcResponse.exception(request.getRequestId(), e);
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            
            return response;
        }
    }
    
    /**
     * 查找方法
     */
    private Method findMethod(RpcRequest request) {
        // 首先尝试从缓存中获取
        String methodKey = buildMethodKey(request.getInterfaceName(), 
            request.getMethodName(), request.getParameterTypes());
        
        Method method = methodCache.get(methodKey);
        if (method != null) {
            return method;
        }
        
        // 缓存未命中，通过反射查找
        Object serviceImpl = serviceRegistry.get(request.getInterfaceName());
        if (serviceImpl == null) {
            return null;
        }
        
        try {
            Class<?> serviceClass = serviceImpl.getClass();
            Class<?>[] paramTypes = request.getParameterTypes();
            
            if (paramTypes == null) {
                paramTypes = new Class<?>[0];
            }
            
            method = serviceClass.getMethod(request.getMethodName(), paramTypes);
            
            // 缓存方法以便后续使用
            methodCache.put(methodKey, method);
            
            return method;
            
        } catch (NoSuchMethodException e) {
            logger.warn("Method not found: {}.{}()", 
                request.getInterfaceName(), request.getMethodName());
            return null;
        }
    }
    
    /**
     * 移除服务
     * 
     * @param interfaceClass 服务接口类
     */
    public void unregisterService(Class<?> interfaceClass) {
        if (interfaceClass != null) {
            unregisterService(interfaceClass.getName());
        }
    }
    
    /**
     * 移除服务
     * 
     * @param serviceName 服务名称
     */
    public void unregisterService(String serviceName) {
        Object removed = serviceRegistry.remove(serviceName);
        if (removed != null) {
            // 清除相关方法缓存
            methodCache.entrySet().removeIf(entry -> entry.getKey().startsWith(serviceName + "#"));
            logger.info("Unregistered service: {}", serviceName);
        }
    }
    
    /**
     * 获取已注册的服务数量
     */
    public int getServiceCount() {
        return serviceRegistry.size();
    }
    
    /**
     * 检查服务是否已注册
     */
    public boolean isServiceRegistered(String serviceName) {
        return serviceRegistry.containsKey(serviceName);
    }
    
    /**
     * 获取所有已注册的服务名称
     */
    public String[] getRegisteredServices() {
        return serviceRegistry.keySet().toArray(new String[0]);
    }
    
    /**
     * 清空所有注册的服务
     */
    public void clearServices() {
        serviceRegistry.clear();
        methodCache.clear();
        logger.info("All services cleared");
    }
    
    /**
     * 转换参数类型
     */
    private Object[] convertParameters(Object[] params, Class<?>[] targetTypes) {
        if (params == null || targetTypes == null) {
            return params;
        }
        
        if (params.length != targetTypes.length) {
            return params;
        }
        
        Object[] converted = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            converted[i] = convertParameter(params[i], targetTypes[i]);
        }
        return converted;
    }
    
    /**
     * 转换单个参数类型
     */
    private Object convertParameter(Object param, Class<?> targetType) {
        if (param == null || targetType == null) {
            return param;
        }
        
        // 如果类型已经匹配，直接返回
        if (targetType.isAssignableFrom(param.getClass())) {
            return param;
        }
        
        // 处理基本类型的包装类
        if (targetType == Long.class && param instanceof Number) {
            return ((Number) param).longValue();
        }
        if (targetType == Integer.class && param instanceof Number) {
            return ((Number) param).intValue();
        }
        if (targetType == Double.class && param instanceof Number) {
            return ((Number) param).doubleValue();
        }
        if (targetType == Float.class && param instanceof Number) {
            return ((Number) param).floatValue(); 
        }
        if (targetType == Boolean.class && param instanceof Boolean) {
            return param;
        }
        if (targetType == String.class && param instanceof String) {
            return param;
        }
        
        // 对于复杂类型，暂时直接返回原参数
        // 在生产环境中，这里可以使用更复杂的类型转换逻辑
        return param;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in RpcRequestHandler", cause);
        ctx.close();
    }
}
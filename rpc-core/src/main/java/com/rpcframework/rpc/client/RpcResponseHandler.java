package com.rpcframework.rpc.client;

import com.rpcframework.rpc.exception.NetworkException;
import com.rpcframework.rpc.model.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RPC响应处理器
 * 
 * <p>处理服务端返回的RPC响应，管理待处理请求的Future对象。
 * 支持异步响应处理和超时管理，确保客户端请求的可靠性。
 * 
 * <p>主要功能：
 * <ul>
 * <li>响应路由：根据请求ID将响应路由到对应的Future</li>
 * <li>超时管理：处理超时请求的清理和异常通知</li>
 * <li>异常处理：处理网络异常和协议异常</li>
 * <li>资源清理：连接断开时清理待处理请求</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResponse> {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcResponseHandler.class);
    
    /**
     * 待处理请求映射：请求ID -> RpcFuture
     */
    private final ConcurrentMap<String, RpcFuture> pendingRequests = new ConcurrentHashMap<>();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Received response for request: {}", requestId);
        }
        
        if (requestId == null) {
            logger.warn("Received response with null request ID");
            return;
        }
        
        // 查找对应的Future
        RpcFuture future = pendingRequests.remove(requestId);
        if (future == null) {
            logger.warn("No pending request found for ID: {}", requestId);
            return;
        }
        
        // 完成Future
        try {
            future.complete(response);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Response processed successfully for request: {}", requestId);
            }
        } catch (Exception e) {
            logger.error("Error completing future for request: {}", requestId, e);
            future.completeExceptionally(e);
        }
    }
    
    /**
     * 添加待处理请求
     * 
     * @param requestId 请求ID
     * @param future RPC Future
     */
    public void addPendingRequest(String requestId, RpcFuture future) {
        if (requestId == null || future == null) {
            throw new IllegalArgumentException("Request ID and future cannot be null");
        }
        
        pendingRequests.put(requestId, future);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Added pending request: {}, total pending: {}", 
                requestId, pendingRequests.size());
        }
    }
    
    /**
     * 移除待处理请求
     * 
     * @param requestId 请求ID
     * @return 被移除的Future，如果不存在返回null
     */
    public RpcFuture removePendingRequest(String requestId) {
        if (requestId == null) {
            return null;
        }
        
        RpcFuture future = pendingRequests.remove(requestId);
        
        if (future != null && logger.isDebugEnabled()) {
            logger.debug("Removed pending request: {}, remaining: {}", 
                requestId, pendingRequests.size());
        }
        
        return future;
    }
    
    /**
     * 获取待处理请求数量
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }
    
    /**
     * 检查是否有待处理的请求
     */
    public boolean hasPendingRequest(String requestId) {
        return pendingRequests.containsKey(requestId);
    }
    
    /**
     * 连接断开时清理所有待处理请求
     */
    public void cleanupOnDisconnect() {
        if (pendingRequests.isEmpty()) {
            return;
        }
        
        logger.warn("Connection lost, cleaning up {} pending requests", pendingRequests.size());
        
        NetworkException exception = new NetworkException("Connection lost");
        
        // 完成所有待处理请求并设置异常
        pendingRequests.values().forEach(future -> {
            try {
                future.completeExceptionally(exception);
            } catch (Exception e) {
                logger.error("Error completing future with exception", e);
            }
        });
        
        // 清空映射
        pendingRequests.clear();
        
        logger.info("All pending requests cleaned up");
    }
    
    /**
     * 清理超时请求
     * 
     * <p>遍历所有待处理请求，清理已超时的请求。
     * 通常由定时任务调用。
     */
    public void cleanupTimeoutRequests() {
        if (pendingRequests.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        int timeoutCount = 0;
        
        // 收集超时的请求ID
        pendingRequests.entrySet().removeIf(entry -> {
            RpcFuture future = entry.getValue();
            if (future.isTimeout(currentTime)) {
                try {
                    future.completeExceptionally(
                        new com.rpcframework.rpc.exception.TimeoutException(
                            "Request timeout: " + entry.getKey()));
                } catch (Exception e) {
                    logger.error("Error completing timeout future", e);
                }
                return true;
            }
            return false;
        });
        
        if (timeoutCount > 0) {
            logger.warn("Cleaned up {} timeout requests", timeoutCount);
        }
    }
    
    /**
     * 强制清理所有待处理请求
     */
    public void forceCleanup() {
        if (pendingRequests.isEmpty()) {
            return;
        }
        
        logger.warn("Force cleaning up {} pending requests", pendingRequests.size());
        
        NetworkException exception = new NetworkException("Client shutdown");
        
        pendingRequests.values().forEach(future -> {
            try {
                future.completeExceptionally(exception);
            } catch (Exception e) {
                logger.error("Error completing future during force cleanup", e);
            }
        });
        
        pendingRequests.clear();
        
        logger.info("Force cleanup completed");
    }
    
    /**
     * 获取所有待处理请求的ID
     */
    public String[] getPendingRequestIds() {
        return pendingRequests.keySet().toArray(new String[0]);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in RpcResponseHandler", cause);
        
        // 清理所有待处理请求
        cleanupOnDisconnect();
        
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("Channel inactive, cleaning up pending requests");
        cleanupOnDisconnect();
        super.channelInactive(ctx);
    }
}
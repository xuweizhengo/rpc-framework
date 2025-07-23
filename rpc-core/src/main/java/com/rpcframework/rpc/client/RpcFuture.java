package com.rpcframework.rpc.client;

import com.rpcframework.rpc.exception.TimeoutException;
import com.rpcframework.rpc.model.RpcResponse;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC调用Future
 * 
 * <p>异步RPC调用的Future实现，支持阻塞等待、超时控制和回调通知。
 * 基于CompletableFuture的语义，提供完整的异步编程支持。
 * 
 * <p>主要功能：
 * <ul>
 * <li>异步等待：支持get()方法阻塞等待结果</li>
 * <li>超时控制：支持带超时时间的等待</li>
 * <li>回调支持：支持成功和失败回调</li>
 * <li>状态管理：提供完整的Future状态管理</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcFuture implements Future<RpcResponse> {
    
    private final String requestId;
    private final long createTime;
    private final int timeoutMs;
    
    private volatile RpcResponse response;
    private volatile Throwable exception;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    
    // 回调函数
    private volatile Runnable successCallback;
    private volatile Runnable failureCallback;
    
    public RpcFuture(String requestId, int timeoutMs) {
        this.requestId = requestId;
        this.timeoutMs = timeoutMs;
        this.createTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (completed.get()) {
            return false;
        }
        
        if (cancelled.compareAndSet(false, true)) {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
            
            // 执行失败回调
            executeFailureCallback();
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    @Override
    public boolean isDone() {
        return completed.get() || cancelled.get();
    }
    
    @Override
    public RpcResponse get() throws InterruptedException, ExecutionException {
        try {
            return get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ExecutionException("Request timeout", e);
        }
    }
    
    @Override
    public RpcResponse get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        
        if (isDone()) {
            return getResult();
        }
        
        long timeoutNanos = unit.toNanos(timeout);
        
        lock.lock();
        try {
            while (!isDone()) {
                if (timeoutNanos <= 0) {
                    throw new java.util.concurrent.TimeoutException("Request timeout: " + timeout + " " + unit);
                }
                try {
                    timeoutNanos = condition.awaitNanos(timeoutNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("Interrupted while waiting for response");
                }
            }
            
            return getResult();
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 获取结果，处理异常情况
     */
    private RpcResponse getResult() throws ExecutionException {
        if (cancelled.get()) {
            throw new CancellationException("Request was cancelled");
        }
        
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        
        return response;
    }
    
    /**
     * 完成Future并设置结果
     * 
     * @param response RPC响应
     */
    public void complete(RpcResponse response) {
        if (completed.compareAndSet(false, true)) {
            this.response = response;
            
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
            
            // 执行成功回调
            executeSuccessCallback();
        }
    }
    
    /**
     * 完成Future并设置异常
     * 
     * @param exception 异常
     */
    public void completeExceptionally(Throwable exception) {
        if (completed.compareAndSet(false, true)) {
            this.exception = exception;
            
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
            
            // 执行失败回调
            executeFailureCallback();
        }
    }
    
    /**
     * 检查是否超时
     * 
     * @param currentTime 当前时间
     * @return true-已超时，false-未超时
     */
    public boolean isTimeout(long currentTime) {
        return !isDone() && (currentTime - createTime) > timeoutMs;
    }
    
    /**
     * 设置成功回调
     * 
     * @param callback 回调函数
     * @return this
     */
    public RpcFuture onSuccess(Runnable callback) {
        this.successCallback = callback;
        
        // 如果已经完成且成功，立即执行回调
        if (completed.get() && exception == null && !cancelled.get()) {
            executeSuccessCallback();
        }
        
        return this;
    }
    
    /**
     * 设置失败回调
     * 
     * @param callback 回调函数
     * @return this
     */
    public RpcFuture onFailure(Runnable callback) {
        this.failureCallback = callback;
        
        // 如果已经完成且失败，立即执行回调
        if (completed.get() && (exception != null || cancelled.get())) {
            executeFailureCallback();
        }
        
        return this;
    }
    
    /**
     * 设置完成回调（无论成功还是失败）
     * 
     * @param callback 回调函数
     * @return this
     */
    public RpcFuture onComplete(Runnable callback) {
        if (isDone()) {
            safeExecuteCallback(callback);
        } else {
            // 保存原有回调
            Runnable originalSuccess = this.successCallback;
            Runnable originalFailure = this.failureCallback;
            
            // 包装新回调
            Runnable wrappedSuccess = () -> {
                if (originalSuccess != null) {
                    safeExecuteCallback(originalSuccess);
                }
                safeExecuteCallback(callback);
            };
            
            Runnable wrappedFailure = () -> {
                if (originalFailure != null) {
                    safeExecuteCallback(originalFailure);
                }
                safeExecuteCallback(callback);
            };
            
            this.successCallback = wrappedSuccess;
            this.failureCallback = wrappedFailure;
        }
        
        return this;
    }
    
    /**
     * 执行成功回调
     */
    private void executeSuccessCallback() {
        if (successCallback != null) {
            safeExecuteCallback(successCallback);
        }
    }
    
    /**
     * 执行失败回调
     */
    private void executeFailureCallback() {
        if (failureCallback != null) {
            safeExecuteCallback(failureCallback);
        }
    }
    
    /**
     * 安全执行回调函数
     */
    private void safeExecuteCallback(Runnable callback) {
        try {
            callback.run();
        } catch (Exception e) {
            // 回调异常不应该影响主流程，只记录日志
            System.err.println("Error executing callback: " + e.getMessage());
        }
    }
    
    /**
     * 获取请求ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * 获取创建时间
     */
    public long getCreateTime() {
        return createTime;
    }
    
    /**
     * 获取超时时间
     */
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    /**
     * 获取响应（非阻塞）
     */
    public RpcResponse getResponse() {
        return response;
    }
    
    /**
     * 获取异常（非阻塞）
     */
    public Throwable getException() {
        return exception;
    }
    
    /**
     * 检查是否成功完成
     */
    public boolean isSuccess() {
        return completed.get() && exception == null && !cancelled.get();
    }
    
    /**
     * 检查是否异常完成
     */
    public boolean isFailure() {
        return completed.get() && exception != null;
    }
    
    /**
     * 获取已等待时间
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - createTime;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RpcFuture{");
        sb.append("requestId='").append(requestId).append('\'');
        sb.append(", timeoutMs=").append(timeoutMs);
        sb.append(", elapsedTime=").append(getElapsedTime());
        sb.append(", completed=").append(completed.get());
        sb.append(", cancelled=").append(cancelled.get());
        
        if (completed.get()) {
            if (exception != null) {
                sb.append(", exception=").append(exception.getClass().getSimpleName());
            } else if (response != null) {
                sb.append(", success=true");
            }
        }
        
        sb.append('}');
        return sb.toString();
    }
}
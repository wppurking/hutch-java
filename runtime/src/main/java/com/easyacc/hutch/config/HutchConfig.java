package com.easyacc.hutch.config;

import com.easyacc.hutch.core.ErrorHandler;
import com.easyacc.hutch.error_handlers.MaxRetry;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.context.SmallRyeManagedExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.enterprise.inject.spi.CDI;
import lombok.ToString;

/** 提供配置 Hutch 的配置. 需要是 StaticInitSafe 状态, 能够在 static 的时候就开始处理 */
@StaticInitSafe
@ConfigRoot(name = "hutch", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ToString
public class HutchConfig {
  private static ExecutorService sharedExecutor;

  private static List<ErrorHandler> errorHandlers;

  /** Hutch 的 prefix 前缀名称 */
  @ConfigItem(defaultValue = "hutch")
  public String name;

  /** rabbitmq 的 virtualHost */
  @ConfigItem(defaultValue = "/")
  public String virtualHost;

  /** 用户名 */
  @ConfigItem(defaultValue = "guest")
  public String username;

  /** 密码 */
  @ConfigItem(defaultValue = "guest")
  public String password;

  /** 连接服务器 */
  @ConfigItem(defaultValue = "127.0.0.1")
  public String hostname;

  /** 是否使用 rabbitmq 的 quorum queue */
  @ConfigItem(defaultValue = "false")
  public boolean quorum;

  /** 连接端口 */
  @ConfigItem(defaultValue = "5672")
  public int port;

  /** 用于 schedule 的 Redis URL */
  @ConfigItem(defaultValue = "redis://localhost:6379")
  public String redisUrl;

  /** 从 IOC 中获取默认的那个 Executors */
  public static ExecutorService getSharedExecutor() {
    try {
      return CDI.current().select(SmallRyeManagedExecutor.class).get();
    } catch (NoClassDefFoundError e) {
      if (sharedExecutor == null) {
        sharedExecutor = Executors.newCachedThreadPool();
      }
      return sharedExecutor;
    }
  }

  /**
   * 获取默认的 ErrorHandlers, 如果没有初始化, 默认添加 MaxRetry. 如果需要额外自定义 ErrorHandler, 那么
   *
   * <ul>
   *   <li>实现 ErrorHandler 接口
   *   <li>在 Quarkus 应用初始化的时候, 通过 HutchConfig.getErrorHandlers 获取实例, 将对应的 ErrorHandler 实例添加到有序队列中
   * </ul>
   */
  public static List<ErrorHandler> getErrorHandlers() {
    if (errorHandlers == null) {
      errorHandlers = new ArrayList<>();
      errorHandlers.add(new MaxRetry());
    }
    return errorHandlers;
  }

  /** 获取 RabbitMQ 的 uri, 暂时不支持 tls */
  public String getUri() {
    var vh = virtualHost;
    if (virtualHost.equals("/")) {
      vh = "";
    }
    return String.format("amqp://%s:*@%s:%d/%s", username, hostname, port, vh);
  }
}

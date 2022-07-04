package com.easyacc.hutch.scheduler;

import com.easyacc.hutch.Hutch;
import com.easyacc.hutch.core.HutchConsumer;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/** 提供基于 redis 主动式 hutch scheduler job */
@Slf4j
public class HyenaJob implements Job {
  /** 需要执行的 redis keys */
  private List<String> redisKeys = new ArrayList<>();
  /** 缓存最后更新时间 */
  private Instant updatedAt = Instant.now().minusSeconds(11);

  @Override
  public void execute(JobExecutionContext context) {
    // 检查 Hutch 是否启动
    if (Hutch.current() == null) {
      throw new IllegalStateException("Hutch 还未启动!");
    }

    // 找出 HutchConsumer 实例
    var clazz = context.getJobDetail().getJobDataMap().get("consumerClass");
    var consumer = HutchConsumer.get((Class<? extends HutchConsumer>) clazz);
    if (consumer == null) {
      throw new IllegalStateException("未找到 HutchConsumer!" + clazz);
    }

    // 检查 threshold 参数
    var threshold = consumer.threshold();
    if (threshold == null) {
      throw new IllegalStateException("未找到 threshold 参数!" + clazz);
    }

    // 1. 获取 redis 实例
    var redis = Hutch.current().getRedisConnection().sync();
    // 2. 尝试刷新一次 redis keys
    this.reloadRedisKeys(consumer.queue(), redis);
    // 3. 从 redis 中获取 task
    for (var key : this.redisKeys) {
      var tasks = redis.zrange(key, 0, threshold.rate() - 1);
      if (tasks.isEmpty()) {
        log.debug("从 redis 中未找到任务数据! key: {}", key);
        continue;
      }

      // 通过 Hutch 来 publish 出去
      threshold.publish(tasks);
      // 从 redis 队列中移除 tasks
      redis.zrem(key, tasks.toArray(String[]::new));
    }
  }

  /** 刷新一次 redis keys */
  private void reloadRedisKeys(String prefix, RedisCommands<String, String> redis) {
    var intervals = Duration.between(this.updatedAt, Instant.now()).toSeconds();
    if (intervals < 10 * 60) {
      log.debug("Reload skipped. The interval must > 10m, right now is: {}s", intervals);
    }

    this.redisKeys = redis.keys(String.format("%s*", prefix));
    this.updatedAt = Instant.now();
  }
}

package com.easyacc.hutch.util;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/** Created by IntelliJ IDEA. User: kenyon Date: 2022/7/3 Time: 2:28 下午 */
@Slf4j
public class SchedulerUtils {
  public static void clear(Scheduler scheduler) {
    if (scheduler == null) return;

    try {
      scheduler.clear();
    } catch (SchedulerException e) {
      log.error("Scheduler 清理失败!", e);
    }
  }
}

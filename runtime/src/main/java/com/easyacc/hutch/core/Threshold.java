package com.easyacc.hutch.core;

import com.easyacc.hutch.Hutch;
import java.util.List;
import lombok.SneakyThrows;

/** 自定义的主动限流参数 */
public interface Threshold {
  /** 将 redis 中的 msg 通过 jackson 读取成为 clazz 的实例 */
  @SneakyThrows
  default <T> T toType(Class<T> clazz, String msg) {
    return Hutch.om().readerFor(clazz).readValue(msg);
  }

  /** 每次执行加载出来的数量 */
  default int rate() {
    return 1;
  }

  /** 每次执行的间隔时间(单位: s) */
  default int interval() {
    return 1;
  }

  /** 通过 msg 来计算 redis 队列的 suffix */
  default String key(String msg) {
    return null;
  }

  /** 将 redis 中的消息 publish 出去 */
  void publish(List<String> msgs);
}

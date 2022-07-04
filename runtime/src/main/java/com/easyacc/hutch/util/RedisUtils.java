package com.easyacc.hutch.util;

import io.lettuce.core.api.StatefulRedisConnection;

/** Created by IntelliJ IDEA. User: kenyon Date: 2022/7/3 Time: 2:31 下午 */
public class RedisUtils {
  public static void close(StatefulRedisConnection connection) {
    if (connection != null) {
      connection.close();
    }
  }
}

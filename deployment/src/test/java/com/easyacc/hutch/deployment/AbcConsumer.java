package com.easyacc.hutch.deployment;

import com.easyacc.hutch.Hutch;
import com.easyacc.hutch.core.HutchConsumer;
import com.easyacc.hutch.core.Message;
import com.easyacc.hutch.core.Threshold;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Created by IntelliJ IDEA. User: wyatt Date: 2022/3/15 Time: 16:46 */
public class AbcConsumer implements HutchConsumer {
  public static AtomicInteger Timers = new AtomicInteger(0);

  @Override
  public Threshold threshold() {
    return new Threshold() {
      @Override
      public int rate() {
        return 1;
      }

      @Override
      public int interval() {
        return 1;
      }

      @Override
      public String key(String msg) {
        return msg;
      }

      @Override
      public void publish(List<String> msgs) {
        for (var msg : msgs) {
          Hutch.publish(AbcConsumer.class, msg);
        }
      }
    };
  }

  @Override
  public void onMessage(Message message) {
    Timers.incrementAndGet();
    System.out.println("AbcConsumer received message: " + message.getBodyContentAsString());
  }

  public static void clas() {
    var c = MethodHandles.lookup().lookupClass();
    System.out.println(c.getSimpleName());
  }
}

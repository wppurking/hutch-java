package com.easyacc.hutch.deployment;

import static com.google.common.truth.Truth.assertThat;

import com.easyacc.hutch.Hutch;
import com.easyacc.hutch.config.HutchConfig;
import com.easyacc.hutch.core.HutchConsumer;
import com.easyacc.hutch.error_handlers.NoDelayMaxRetry;
import io.quarkus.test.QuarkusUnitTest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HutchTest {
  @RegisterExtension
  static final QuarkusUnitTest app =
      new QuarkusUnitTest()
          .overrideConfigKey("quarkus.application.name", "hutch-app")
          .overrideConfigKey("quarkus.hutch.name", "lake_web")
          .overrideConfigKey("quarkus.hutch.virtual-host", "test")
          .overrideConfigKey("quarkus.hutch.redis-url", "redis://localhost:6379")
          // .overrideConfigKey("quarkus.log.level", "debug")
          .withApplicationRoot(jar -> jar.addClass(AbcConsumer.class).addClass(BbcConsumer.class));

  @Inject HutchConfig config;
  @Inject AbcConsumer abcConsumer;

  @Test
  void testHutchConsumerAllInCDI() {
    var hcs = Hutch.consumers();
    assertThat(hcs).hasSize(2);
    hcs.forEach(hc -> assertThat(hc.queueArguments()).isEmpty());
  }

  @Test
  void testLoadBeanFromCDI() {
    var beans = CDI.current().getBeanManager().getBeans(HutchConsumer.class);
    for (var bean : beans) {
      var h = (HutchConsumer) CDI.current().select(bean.getBeanClass()).get();
      assertThat(h.prefetch()).isEqualTo(2);
      assertThat(h.queue()).startsWith("lake_web_");
      System.out.println(h.queue());
    }
  }

  @Test
  void hutchInIOC() {
    // 测试提供一个 Hutch 在 IOC 里面
    var h = CDI.current().select(Hutch.class).get();
    assertThat(h).isNotNull();
    assertThat(h.isStarted()).isFalse();
  }

  @Test
  void testHutchConfig() throws InterruptedException, IOException {
    var cfg = CDI.current().select(HutchConfig.class).get();
    assertThat(cfg).isNotNull();

    assertThat(cfg.name).isEqualTo("lake_web");
    assertThat(Hutch.name()).isEqualTo("lake_web");
    var h = new Hutch(cfg).start();
    assertThat(h.isStarted()).isTrue();
    h.stop();
    assertThat(h.isStarted()).isFalse();
  }

  @Test
  void testEnqueue() throws IOException, InterruptedException {
    //    var h = CDI.current().select(Hutch.class).get();
    var h = new Hutch(config);
    // 需要确保 queue 都存在, 需要调用 start 进行 declare
    h.start();
    assertThat(h.isStarted()).isTrue();

    assertThat(h).isEqualTo(Hutch.current());

    var q = h.getCh().queueDeclarePassive(abcConsumer.queue());

    abcConsumer.enqueue("abc");
    Hutch.publish(AbcConsumer.class, "ccc");

    Thread.sleep(100);
    q = h.getCh().queueDeclarePassive(abcConsumer.queue());
    // 消息被消费了
    assertThat(q.getMessageCount()).isEqualTo(0);
    assertThat(AbcConsumer.Timers.get()).isEqualTo(2);
    h.stop();
    assertThat(h.isStarted()).isFalse();
  }

  @Test
  void testAdditionalBean() {
    var s = CDI.current().select(Hutch.class).get();
    assertThat(s).isNotNull();
    assertThat(s.isStarted()).isFalse();
    assertThat(s.getConfig()).isEqualTo(config);
  }

  @Test
  void testMaxRetry() throws InterruptedException {
    HutchConfig.getErrorHandlers().clear();
    HutchConfig.getErrorHandlers().add(new NoDelayMaxRetry());
    var c = Hutch.current();
    var h = CDI.current().select(Hutch.class).get();
    assertThat(h.isStarted()).isFalse();
    h.start();
    assertThat(h).isEqualTo(Hutch.current());
    Hutch.publish(BbcConsumer.class, "bbc");
    TimeUnit.SECONDS.sleep(6);
    assertThat(BbcConsumer.Timers.get()).isEqualTo(2);
    h.stop();
  }

  @Test
  void testPublishJsonDelayRetry() throws InterruptedException {
    HutchConfig.getErrorHandlers().clear();
    HutchConfig.getErrorHandlers().add(new NoDelayMaxRetry());
    var h = CDI.current().select(Hutch.class).get();
    h.start();
    Hutch.publishJsonWithDelay(1000, AbcConsumer.class, "ccc");
    var a = AbcConsumer.Timers.get();
    // 等待在 5s 以内
    TimeUnit.SECONDS.sleep(2);
    assertThat(AbcConsumer.Timers.get()).isEqualTo(a);
    TimeUnit.SECONDS.sleep(6);
    assertThat(AbcConsumer.Timers.get()).isEqualTo(a + 1);
    h.stop();
  }

  @Test
  void testPublishWithSchedule() throws InterruptedException {
    HutchConfig.getErrorHandlers().clear();
    HutchConfig.getErrorHandlers().add(new NoDelayMaxRetry());

    var h = CDI.current().select(Hutch.class).get();
    h.start();
    Hutch.publishWithSchedule(AbcConsumer.class, "ccc");

    var a = AbcConsumer.Timers.get();
    assertThat(AbcConsumer.Timers.get()).isEqualTo(a);

    TimeUnit.SECONDS.sleep(2);
    assertThat(AbcConsumer.Timers.get()).isEqualTo(a + 1);
    h.stop();
  }
}

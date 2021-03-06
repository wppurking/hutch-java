package com.easyacc.hutch.config;

import com.easyacc.hutch.Hutch;
import io.quarkus.runtime.annotations.Recorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** 用于记录 HutchConfig 在 Deployemtn 阶段传递给 HutchProcessor */
@Recorder
@Slf4j
public class HutchRecorder {
  @Getter HutchConfig config;

  /** 交给 quarkus 对 Recorder 通过构造函数注入依赖 */
  public HutchRecorder(HutchConfig cfg) {
    this.config = cfg;
  }

  public void initHutchName() {
    Hutch.APP_NAME = this.config.name;
  }
}

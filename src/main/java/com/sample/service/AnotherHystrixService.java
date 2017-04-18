package com.sample.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.sample.conf.HttpWebClient;

@Component
public class AnotherHystrixService {

  private static final Logger logger = LoggerFactory.getLogger(AnotherHystrixService.class);

  @HystrixCommand(groupKey = HttpWebClient.GROUP, commandKey = HttpWebClient.COMMAND_GET,
      threadPoolKey = HttpWebClient.THREAD_POOL_KEY)
  public void annotatedHystrix(String payload) {
    logger.info("ON_ANNOTATED_HYSTRIX " + payload);
  }

}

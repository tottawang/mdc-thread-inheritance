package com.sample.service;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.sample.conf.Application;
import com.sample.conf.HttpWebClient;

import rx.Observable;
import rx.Observer;

@Component
public class HystrixService {

  private static final Logger logger = LoggerFactory.getLogger(HystrixService.class);

  private Setter cachedSetter;

  @Value("${HYSTRIX_TEST_URL:url_not_accessiable}")
  private String HYSTRIX_TEST_URL;

  @Autowired
  @Qualifier(Application.DEFAULT_REST_TEMPLATE)
  protected RestTemplate restTemplate;

  @Value("${HYSTERIX_TIMEOUT : 10000}")
  private Integer timeout;

  @PostConstruct
  private void init() {
    cachedSetter = com.netflix.hystrix.HystrixCommand.Setter
        .withGroupKey(HystrixCommandGroupKey.Factory.asKey(HttpWebClient.GROUP))
        .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(HttpWebClient.THREAD_POOL_KEY))
        .andCommandKey(HystrixCommandKey.Factory.asKey(HttpWebClient.COMMAND_NON_BLOCKING_GET))
        .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
            .withExecutionTimeoutEnabled(true).withExecutionTimeoutInMilliseconds(timeout));

    HystrixPlugins.getInstance().registerConcurrencyStrategy(new HystrixConcurrencyStrategy() {
      @Override
      public <T> Callable<T> wrapCallable(final Callable<T> callable) {
        return new HystrixContextCallable<T>(callable);
      }
    });
  }

  public void wrapPublish(Integer index) {

    Observable<String> pm = new PublishCommand(index.toString()).observe();
    logger.info("ON_CREATED");

    // non-blocking
    pm.subscribe(new Observer<String>() {

      @Override
      public void onCompleted() {
        logger.info("ON_COMPLETED");
      }

      @Override
      public void onError(Throwable e) {
        e.printStackTrace();
      }

      @Override
      public void onNext(String v) {
        logger.info(String.format("ON_NEXT with payload %s", v));
        System.out.println("onNEXT " + Thread.currentThread().getName() + " payload: " + v);
      }

    });
  }

  class PublishCommand extends com.netflix.hystrix.HystrixCommand<String> {

    private final String payload;

    public PublishCommand(String payload) {
      super(cachedSetter);
      this.payload = payload;
    }

    @Override
    protected String run() {
      System.out.println(Thread.currentThread().getName() + ": " + " non blocking started");
      logger.info(String.format("ON_PUBLISH_START with payload %s", payload));
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
      HttpEntity<String> entity = new HttpEntity<String>(headers);
      try {
        URI endpointUrl = new URI(HYSTRIX_TEST_URL);
        ResponseEntity<String> responseEntity =
            restTemplate.exchange(endpointUrl, HttpMethod.GET, entity, String.class);
        logger.info(String.format("ON_PUBLISH_SUCCEED with payload %s", payload));
        return responseEntity.getBody() + " - " + payload;
      } catch (ResourceAccessException | HttpServerErrorException ex) {
        // Server side exception is a proper case for circuit breaker, hystrix will translate
        // ServiceUnavailableException to HystrixRuntimeException which indicates a system failure.
        throw new ServiceUnavailableException("Service is not available", ex);
      } catch (Throwable ex) {
        // any non sever side exception should be ignored by Hystirx so that no
        // HystrixRuntimeException can be thrown
        throw new IgnoreRuntimeException(
            "client exception, can be ingored for hystrix circuit breaker: " + ex.getMessage(), ex);
      }
    }
  }
}

package com.sample.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sample.service.HystrixService;

@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/api")
public class RestResource {

  private static final Logger logger = LoggerFactory.getLogger(RestResource.class);
  private static final int POOL_SIZE = 5;
  private static final ExecutorService workers = Executors.newFixedThreadPool(POOL_SIZE);
  private static int index = 0;

  @Autowired
  private HystrixService service;

  @GET
  @Path("hystrix-non-blocking")
  public String getUserProjectsNonBlocking() {
    long start = System.currentTimeMillis();
    service.wrapPublish(Integer.valueOf(-1));
    long end = System.currentTimeMillis();
    System.out.println("Time taken to get results " + (end - start) + " milliseconds");
    return "";
  }

  @GET
  @Path("hystrix-non-blocking-currency")
  public String getUserProjectsCurrency() throws InterruptedException, ExecutionException {

    MDC.put("message.group", "msg_group_" + ++index);
    Map<String, String> parentContext = MDC.getCopyOfContextMap();
    logger.info("ON_START rest call");

    String result = "";
    long start = System.currentTimeMillis();

    // call the service.getContent 30 times in parallel
    Collection<Callable<String>> tasks = new ArrayList<Callable<String>>();
    for (int i = 0; i < POOL_SIZE; i++) {
      final Integer index = Integer.valueOf(i);
      tasks.add(new Callable<String>() {
        public String call() throws Exception {
          MDC.setContextMap(parentContext);
          MDC.put("message.id", "msg_id_" + index);
          service.wrapPublish(index);
          return "";
        }
      });
    }

    List<Future<String>> results = workers.invokeAll(tasks, 500, TimeUnit.SECONDS);
    for (Future<String> f : results) {
      result += f.get();
    }

    long end = System.currentTimeMillis();
    System.out.println("Time taken to get results " + (end - start) + " milliseconds");
    return result;
  }

}

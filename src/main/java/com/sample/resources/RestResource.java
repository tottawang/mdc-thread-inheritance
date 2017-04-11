package com.sample.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sample.service.HystrixService;

@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/api")
public class RestResource {

  private static final Logger logger = LoggerFactory.getLogger(RestResource.class);
  private static int index = 0;

  private static int POOL_SIZE = 5;
  private static final ExecutorService workers = Executors.newFixedThreadPool(POOL_SIZE);

  @Autowired
  private HystrixService service;

  @GET
  @Path("hystrix-non-blocking")
  public String getUserProjectsNonBlocking() {
    long start = System.currentTimeMillis();
    service.wrapPublish();
    long end = System.currentTimeMillis();
    System.out.println("Time taken to get results " + (end - start) + " milliseconds");
    return "";
  }

  @GET
  @Path("hystrix-non-blocking-currency")
  public String getUserProjectsCurrency() throws InterruptedException, ExecutionException {

    MDC.put("message.id", "my_test_msg_id_" + ++index);
    logger.info("ON_START rest call");

    String result = "";
    long start = System.currentTimeMillis();

    // call the service.getContent 30 times in parallel
    Collection<Callable<String>> tasks = new ArrayList<Callable<String>>();
    for (int i = 0; i < POOL_SIZE; i++) {
      tasks.add(new Callable<String>() {
        public String call() throws Exception {
          service.wrapPublish();
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

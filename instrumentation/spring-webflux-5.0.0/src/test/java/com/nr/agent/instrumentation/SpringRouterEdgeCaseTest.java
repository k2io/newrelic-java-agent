/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework" })
public class SpringRouterEdgeCaseTest {

    private static WebClient webClient;

    @BeforeClass
    public static void setup() throws Exception {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");

        int port = SocketUtils.findAvailableTcpPort();

        HttpServer httpServer = HttpServer.create("0.0.0.0", port);

        final HttpHandler httpHandler = SpringTestHandler.httpHandlerNested();
        httpServer.newHandler(new ReactorHttpHandlerAdapter(new HttpHandler() {
            @Override
            @Trace(dispatcher = true)
            public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
                return httpHandler.handle(request, response);
            }
        })).block();

        final String host = "localhost";
        webClient = WebClient.builder().baseUrl(String.format("http://%s:%d", host, port))
                .clientConnector(new ReactorClientHttpConnector()).build();
    }

    @Test
    public void personPath() {
        webClient.get().uri("/person/").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().toString(),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/person (GET)"));
    }

}

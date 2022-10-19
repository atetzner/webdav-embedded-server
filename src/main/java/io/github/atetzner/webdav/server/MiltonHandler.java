/*
 * Copyright (C) 2016 the original author or authors.
 * See the NOTICE.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.atetzner.webdav.server;

import io.milton.http.HttpManager;
import io.milton.servlet.MiltonServlet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A jetty handler to serve all request using a {@link HttpManager milton HttpManager}.
 */
public class MiltonHandler extends AbstractHandler {
    private final HttpManager httpManager;

    public MiltonHandler(HttpManager httpManager) {
        this.httpManager = httpManager;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        io.milton.http.Request miltonRequest = new io.milton.servlet.ServletRequest(request, null);
        io.milton.servlet.ServletResponse miltonResponse = new io.milton.servlet.ServletResponse(response);

        try {
            MiltonServlet.setThreadlocals(request, response);
            httpManager.process(miltonRequest, miltonResponse);
        } finally {
            MiltonServlet.clearThreadlocals();
            response.getOutputStream().flush();
            response.flushBuffer();
        }
    }
}

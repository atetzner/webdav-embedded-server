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

package de.bitinsomnia.webdav.server;


import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.*;

/**
 * An easy to use and directly startable WebDAV server.
 */
public class MiltonWebDAVFileServer {

    private final File rootFolder;
    private Server jettyServer = null;
    private ServerConnector connector = null;
    private int port = 8081;
    private Map<String, String> userCredentials = new HashMap<>();

    /**
     * @param rootFolder The folder that will be served by the created WebDAV server
     */
    public MiltonWebDAVFileServer(File rootFolder) {
        notNull(rootFolder, "'rootFolder' may not be null");
        if (!rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Given 'rootFolder' is not a directory");
        }

        this.rootFolder = rootFolder;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * A map with all authenticated users. If the map contains at least one user at the {@link #start() startup} of the
     * server, authentication is enabled, otherwise disabled.
     *
     * @return a map to put the user credentials in
     */
    public Map<String, String> getUserCredentials() {
        return userCredentials;
    }

    /**
     * Creates and starts the server with the current state (credentials, port). After startup, the method will return
     * and not block.
     *
     * @throws IllegalStateException if the server has already been started
     * @throws Exception             if creation of the server fails
     */
    public void start() throws Exception {
        if (jettyServer != null) {
            throw new IllegalStateException("Server already started");
        }

        jettyServer = new Server(0);

        connector = new ServerConnector(jettyServer); // NOSONAR
        connector.setPort(getPort());
        jettyServer.setConnectors(new Connector[]{connector});

        HttpManagerBuilder builder = new HttpManagerBuilder();
        builder.setResourceFactory(new MiltonWebDAVResourceFactory(this.rootFolder, userCredentials));
        builder.setEnableBasicAuth(userCredentials != null && !userCredentials.isEmpty());
        HttpManager mgr = builder.buildHttpManager();

        jettyServer.setHandler(new MiltonHandler(mgr));

        jettyServer.start();

        while (!jettyServer.isStarted()) {
            Thread.sleep(50);
        }
    }

    /**
     * A call to this method will not return, until another thread {@link #stop() stops} the server. If the server has
     * not yet {@link #start() started}, this method will throw an {@link IllegalStateException}.
     *
     * @throws InterruptedException if joining to the server fails
     */
    public void join() throws InterruptedException {
        assertServerRunning();
        jettyServer.join();
    }

    /**
     * Stops the server. If the server has not yet {@link #start() started}, this method will throw an {@link
     * IllegalStateException}.
     *
     * @throws Exception if stopping the server or releasing the server's port fails
     */
    public void stop() throws Exception {
        assertServerRunning();
        jettyServer.stop();
        jettyServer.join();
        connector.close();

        jettyServer = null;
        connector = null;
    }

    /**
     * @return {@code false} if the server is definitely not started, {@code true} if the server is starting or already
     * started
     */
    public boolean isStarted() {
        return jettyServer != null;
    }

    private void assertServerRunning() {
        if (jettyServer == null || jettyServer.isStopped()) {
            throw new IllegalStateException("Server not yet started");
        }
    }


}

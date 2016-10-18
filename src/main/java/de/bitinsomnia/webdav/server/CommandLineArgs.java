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

import com.beust.jcommander.Parameter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class to hold the values of the parsed command line arguments.
 *
 * @see MiltonStandaloneServer
 */
public class CommandLineArgs {

    @Parameter(names = {"-p", "--port"}, description = "Port for the server")
    private Integer port = Integer.valueOf(8080);

    @Parameter(names = {"-c", "--credentials"}, description = "Optional credentials to authenticate at the server. Can be given multiple times. If none are given, authentication is disabled. Use the form USER:PASSWORD")
    private List<String> userCredentials = new LinkedList<>();

    @Parameter(description = "FOLDER_TO_SERVE", required = true)
    private List<String> rootFolder = new LinkedList<>();

    @Parameter(names = {"-h", "--help"}, description = "Show help and exit", help = true)
    private boolean help = false;

    /**
     * Server port. Command line arg: {@code -p --port}
     *
     * @return the port on which to start the server on.
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * The raw user credentials als list of {@code USERNAME:PASSWORD}. Commandline arg: {@code -c --credentials}
     *
     * @return raw user credentials
     */
    public List<String> getUserCredentials() {
        return userCredentials;
    }

    /**
     * The parsed user credentials as map of {@code USERNAME -> PASSWORD}. Commandline arg: {@code -c --credentials}
     *
     * @return parsed user credentials
     */
    public Map<String, String> getParsedUserCredentials() {
        Map<String, String> result = new HashMap<>();

        for (String cred : userCredentials) {
            String[] splitCred = cred.split(":");
            if (splitCred.length != 2) {
                continue;
            }

            result.put(splitCred[0], splitCred[1]);
        }

        return result;
    }

    public void setUserCredentials(List<String> userCredentials) {
        this.userCredentials = userCredentials;
    }

    /**
     * The root folder that the server will serve.
     *
     * @return server's root folder
     */
    public List<String> getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(List<String> rootFolder) {
        this.rootFolder = rootFolder;
    }

    /**
     * If the help should be printed. Commandline arg: {@code -h --help}
     *
     * @return If the help should be printed.
     */
    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }
}

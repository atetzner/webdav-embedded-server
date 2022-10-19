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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.io.File;

/**
 * Main class to start a {@link MiltonWebDAVFileServer} from the command line; for command line options of the server
 * see {@link CommandLineArgs}.
 */
public class MiltonStandaloneServer {

    private MiltonStandaloneServer() {
    }

    public static void main(String[] args) throws Exception {
        CommandLineArgs cmdLineArgs = new CommandLineArgs();
        JCommander jc = new JCommander();
        jc.setProgramName(MiltonStandaloneServer.class.getSimpleName());

        jc.addObject(cmdLineArgs);
        try {
            jc.parse(args);
        } catch (ParameterException e) { //NOSONAR
            stderr(e.getLocalizedMessage());
            cmdLineArgs.setHelp(true);
        }

        if (!cmdLineArgs.isHelp() && cmdLineArgs.getRootFolder().size() != 1) {
            stderr("Give exactly one folder to serve");
            cmdLineArgs.setHelp(true);
        }

        if (cmdLineArgs.isHelp()) {
            jc.usage();
            System.exit(1);
        }

        File rootFolder = new File(cmdLineArgs.getRootFolder().get(0));
        MiltonWebDAVFileServer server = new MiltonWebDAVFileServer(rootFolder);
        server.setPort(cmdLineArgs.getPort());
        server.getUserCredentials().putAll(cmdLineArgs.getParsedUserCredentials());
        server.start();
        server.join();
    }

    private static void stderr(String msg) {
        System.err.println(msg); //NOSONAR
    }

}

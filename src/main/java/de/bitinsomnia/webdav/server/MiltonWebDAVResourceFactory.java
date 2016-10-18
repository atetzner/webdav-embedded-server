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

import io.milton.cache.CacheManager;
import io.milton.cache.LocalCacheManager;
import io.milton.http.LockManager;
import io.milton.http.ResourceFactory;
import io.milton.http.SecurityManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.SimpleLockManager;
import io.milton.http.fs.SimpleSecurityManager;
import io.milton.resource.Resource;

import java.io.File;
import java.util.Map;

import static org.apache.commons.lang3.Validate.*;

/**
 * A resource factory for the {@link MiltonHandler}. Besindes creating {@link MiltonFileResource}s and {@link
 * MiltonFolderResource}s, this class also holds a {@link SecurityManager} for authentication and an {@link
 * LockManager}.
 */
public class MiltonWebDAVResourceFactory implements ResourceFactory {
    private final File rootFolder;
    private final SecurityManager securityManager;
    private final LockManager lockManager;

    /**
     * @param rootFolder  The root folder that will be served by this server instance
     * @param credentials The usernames (key) and their respective passwords (value) of the users, that are allowed to
     *                    authenticate at the server. If {@code null} or an {@link Map#isEmpty() empty map} is given,
     *                    authentication is disabled.
     */
    public MiltonWebDAVResourceFactory(File rootFolder, Map<String, String> credentials) {
        notNull(rootFolder, "'rootFolder' may not be null");

        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Root folder does not exist or is not a folder");
        }

        this.rootFolder = rootFolder;

        CacheManager cacheManager = new LocalCacheManager();
        this.lockManager = new SimpleLockManager(cacheManager);

        if (credentials != null && !credentials.isEmpty()) {
            securityManager = new SimpleSecurityManager("", credentials);
        } else {
            securityManager = null;
        }
    }

    @Override
    public Resource getResource(String host, String path) throws NotAuthorizedException, BadRequestException {
        File fileToServe = new File(rootFolder, path);
        if (!fileToServe.exists()) {
            return null;
        } else if (fileToServe.isDirectory()) {
            return new MiltonFolderResource(fileToServe, this);
        } else {
            return new MiltonFileResource(fileToServe, this);
        }
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }
}

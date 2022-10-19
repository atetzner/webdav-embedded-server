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

import io.milton.common.ContentTypeUtils;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.LockInfo;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.FileResource;
import io.milton.resource.LockableResource;
import io.milton.resource.ReplaceableResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.mina.core.RuntimeIoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Map;

/**
 * A {@link FileResource milton FileResource} to serve a single file.
 */
public class MiltonFileResource implements FileResource, ReplaceableResource, LockableResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiltonFileResource.class);
    private final File file;
    private final MiltonWebDAVResourceFactory resourceFactory;

    public MiltonFileResource(File file, MiltonWebDAVResourceFactory resourceFactory) {
        this.file = file;
        this.resourceFactory = resourceFactory;
    }

    @Override
    public String getUniqueId() {
        return file.getAbsolutePath();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        LOGGER.debug("Authenticating user {} for resource {}", user, this.file);

        if (resourceFactory.getSecurityManager() != null) {
            return resourceFactory.getSecurityManager().authenticate(user, password);
        }
        return user;
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth != null) {
            LOGGER.debug("Authorizing user {} for resource {}", auth.getUser(), this.file);
        }

        return resourceFactory.getSecurityManager() == null || resourceFactory.getSecurityManager()
                .authorise(request, method, auth, this);
    }

    @Override
    public String getRealm() {
        return file.getAbsolutePath();
    }

    @Override
    public Date getModifiedDate() {
        return new Date(file.lastModified());
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        // no redirect
        return null;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException,
            ConflictException {
        LOGGER.debug("Copying {} to {}/{}", this.file, toCollection.getName(), name);

        File copyFilePath = new File(resourceFactory.getRootFolder(), toCollection.getName());
        File copyFile = new File(copyFilePath, name);

        try {
            FileUtils.copyFile(this.file, copyFile);
        } catch (IOException e) {
            LOGGER.error("Error copying file {} to {}/{}", this.file, toCollection, name, e);
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        LOGGER.debug("Deleting {}", this.file);
        if (!this.file.delete()) {
            LOGGER.error("Could not delete file {}", this.file);
            throw new RuntimeIoException("Could no delete file " + file);
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws
            IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        LOGGER.debug("Sending contents for {}", this.file);
        if (!this.file.isDirectory()) {
            FileUtils.copyFile(this.file, out);
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        String mime = ContentTypeUtils.findContentTypes(this.file);
        String contentType = ContentTypeUtils.findAcceptableContentType(mime, accepts);

        LOGGER.debug("Resolved content-type {} for {}", contentType, this.file);

        return contentType;

    }

    @Override
    public Long getContentLength() {
        return this.file.length();
    }

    @Override
    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException,
            PreConditionFailedException, LockedException {
        LOGGER.debug("Locking {}", this.file);
        return resourceFactory.getLockManager().lock(timeout, lockInfo, this);
    }

    @Override
    public LockResult refreshLock(String token, LockTimeout timeout) throws NotAuthorizedException, PreConditionFailedException {
        LOGGER.debug("Refreshing lock for {}", this.file);
        return resourceFactory.getLockManager().refresh(token, timeout, this);
    }

    @Override
    public void unlock(String tokenId) throws NotAuthorizedException, PreConditionFailedException {
        LOGGER.debug("Unlocking {}", this.file);
        resourceFactory.getLockManager().unlock(tokenId, this);
    }

    @Override
    public LockToken getCurrentLock() {
        return resourceFactory.getLockManager().getCurrentToken(this);
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException,
            BadRequestException {
        LOGGER.debug("Moving {} to {}/{}", this.file, rDest.getName(), name);

        File copyFilePath = new File(resourceFactory.getRootFolder(), rDest.getName());
        File copyFile = new File(copyFilePath, name);

        try {
            FileUtils.moveFile(this.file, copyFile);
        } catch (IOException e) {
            LOGGER.error("Error moving file {} to {}/{}", this.file, rDest, name, e);
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public Date getCreateDate() {
        Path filePath = Paths.get(this.file.toURI());
        try {
            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            long creationTimeMillis = attr.creationTime().toMillis();
            return new Date(creationTimeMillis);
        } catch (IOException e) {
            LOGGER.error("Error getting creation time for file {}", this.file, e);
            throw new RuntimeIoException(e);
        }

    }

    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException,
            NotAuthorizedException {
        LOGGER.debug("Replacing content of {}", this.file);

        OutputStream out = null;
        try {
            out = FileUtils.openOutputStream(this.file);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeIoException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing output stream to {}", this.file, e);
                }
            }
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException, ConflictException {
        return null;
    }
}

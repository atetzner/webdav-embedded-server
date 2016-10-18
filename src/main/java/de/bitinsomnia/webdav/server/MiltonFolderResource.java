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

import io.milton.common.ContentTypeUtils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.FolderResource;
import io.milton.resource.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.mina.core.RuntimeIoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A {@link FolderResource milton FolderResource} to serve the contents of a single folder.
 */
public class MiltonFolderResource implements FolderResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiltonFolderResource.class);

    private final File file;
    private final MiltonWebDAVResourceFactory resourceFactory;

    public MiltonFolderResource(File file, MiltonWebDAVResourceFactory resourceFactory) {
        this.file = file;
        this.resourceFactory = resourceFactory;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        LOGGER.debug("Getting child {} in {}", childName, this.file);

        File child = new File(file, childName);
        if (!child.exists()) {
            return null;
        } else if (child.isDirectory()) {
            return new MiltonFolderResource(child, resourceFactory);
        } else {
            return new MiltonFileResource(child, resourceFactory);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        LOGGER.debug("Getting children in {}", this.file);

        File[] folderContents = this.file.listFiles();
        List<Resource> result = new ArrayList<>(folderContents.length);

        for (File f : folderContents) {
            if (f.isDirectory()) {
                result.add(new MiltonFolderResource(f, resourceFactory));
            } else {
                result.add(new MiltonFileResource(f, resourceFactory));
            }
        }

        return result;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException,
            ConflictException {
        LOGGER.debug("Copying folder {} to {}/{}", this.file, toCollection.getName(), name);

        File destinationRootFolder = new File(resourceFactory.getRootFolder(), toCollection.getName());
        File destinationFolder = new File(destinationRootFolder, name);

        try {
            FileUtils.copyDirectory(this.file, destinationFolder);
        } catch (IOException e) {
            LOGGER.error("Error copying folder {}", this.file, e);
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        LOGGER.debug("Deleting {}", this.file);

        try {
            FileUtils.deleteDirectory(this.file);
        } catch (IOException e) {
            LOGGER.error("Error deleting folder {}", this.file, e);
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws
            IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        LOGGER.debug("Sending content for folder {} and contenttype {}", this.file, contentType);
        String relativePath = getRootRelativePath();

        PrintWriter w = new PrintWriter(out);
        w.println("<html><head><title>Folder listing for " + relativePath + "</title></head>");
        w.println("<body>");
        w.println("<h1>Folder listing for " + relativePath + "</h1>");
        w.println("<ul>");
        for (File f : this.file.listFiles()) {
            String childRelativePath = getRootRelativePath(f);
            w.println("<li><a href=\"" + childRelativePath + "\">" + f.getName() + "</a></li>");
        }
        w.println("</ul></body></html>");
        w.flush();
        w.close();
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
        return null;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException,
            BadRequestException {
        File subfolder = new File(this.file, newName);
        if (subfolder.mkdir()) {
            LOGGER.debug("Created folder {}", subfolder);
            return new MiltonFolderResource(subfolder, resourceFactory);
        } else {
            LOGGER.warn("Could not create subfolder {}", subfolder);
            return null;
        }
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException,
            BadRequestException {
        LOGGER.debug("Moving {} to {}/{}", this.file, rDest.getName(), name);

        File newRootDir = new File(resourceFactory.getRootFolder(), rDest.getName());
        File newDir = new File(newRootDir, name);

        try {
            FileUtils.moveDirectory(this.file, newDir);
        } catch (IOException e) {
            LOGGER.error("Error moving {} to {}", this.file, newDir);
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws
            IOException, ConflictException, NotAuthorizedException, BadRequestException {
        File newFile = new File(this.file, newName);
        OutputStream out = null;
        try {
            out = FileUtils.openOutputStream(newFile);
            IOUtils.copy(inputStream, out);

            return new MiltonFileResource(newFile, resourceFactory);
        } catch (Exception e) {
            LOGGER.error("Error creating file {}", newFile, e);
            throw new RuntimeIoException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing new file output stream for file {}", newFile, e);
                }
            }
        }
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

    private String getRootRelativePath() {
        return getRootRelativePath(this.file);
    }

    private String getRootRelativePath(File file) {
        URI toInspectURI = file.toURI();
        URI relativePath = resourceFactory.getRootFolder().toURI().relativize(toInspectURI);
        return "/" + relativePath.getPath();
    }
}

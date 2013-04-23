/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.spring.vfs;

import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * VFS based Resource.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class VFSResource extends AbstractResource {

    private Object file;

    public VFSResource(Object file) {
        if (file == null) {
            throw new IllegalArgumentException("Null file");
        }
        this.file = file;
    }

    public VFSResource(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("Null url");
        }
        try {
            if (VFSUtil.VFS_METHOD_GET_ROOT_URL != null && VFSUtil.VFS_METHOD_GET_ROOT_URL.getName().contains("getChild")){
                this.file = getJarChild(url);
            } else {
                this.file = VFSUtil.invokeVfsMethod(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, url);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot retrieve file from URL: ", e);
        }
    }

    public boolean exists() {
        try {
            return (Boolean) VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_EXISTS, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isOpen() {
        return false;
    }

    public boolean isReadable() {
        try {
            return ((Long) VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_GET_SIZE, file)) > 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long lastModified() {
        try {
            return ((Long) VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED, file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public URL getURL() throws IOException {
        return VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_TO_URL, file);
    }

    public URI getURI() throws IOException {
        try {
            return VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VIRTUAL_FILE_METHOD_TO_URI, file, URISyntaxException.class);
        } catch (URISyntaxException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    public File getFile() throws IOException {
        return VFSUtil.getPhysicalFile(file);
    }

    @SuppressWarnings("deprecation")
    public Resource createRelative(String relativePath) throws IOException {
        //VirtualFile.findChild will not scan the parent or current directory
        if (relativePath.startsWith(".") || relativePath.indexOf("/") == -1) {
            return new VFSResource(getChild(new URL(getURL(), relativePath)));
        } else {
            return new VFSResource(VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_GET_CHILD, file, relativePath));
        }
    }

    public String getFilename() {
        try {
            return VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_GET_NAME, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDescription() {
        return file.toString();
    }

    public InputStream getInputStream() throws IOException {
        return VFSUtil.invokeVfsMethod(VFSUtil.VIRTUAL_FILE_METHOD_GET_INPUT_STREAM, file);
    }

    public String toString() {
        return getDescription();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof VFSResource) {
            return file.equals(((VFSResource) other).file);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    private static Map<URL, VirtualFile> urlToVirtualFile = new HashMap<URL, VirtualFile>();

    static Object getChild(URL url) throws IOException {
        if(urlToVirtualFile.containsKey(url)){
            VirtualFile archive = urlToVirtualFile.get(url);
            return archive;
        }
        try {
            URI uri = new java.net.URI(url.toString());
            if (uri.getPath() == null) {
                String path = uri.toString();
                if (path.startsWith("jar:file:")) {
                    path = url.getPath();
                    path = path.substring(path.indexOf("file:/"));
                    path = path.substring(0,path.toLowerCase().indexOf(".jar")+4);

                    if (path.startsWith("file://")){ //UNC Path
                        path = "C:/" + path.substring(path.indexOf("file:/")+7);
                        path = "/" + new java.net.URI(path).getPath();
                    }
                    VirtualFile archive = VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, URISyntaxException.class, new URL(path));
                    TempFileProvider provider = TempFileProvider.create("tmp", Executors.newScheduledThreadPool(2));
                    Closeable handle = VFS.mountZipExpanded(new FileInputStream(archive.getPhysicalFile()), archive.getName(), archive, provider);
                    urlToVirtualFile.put(url, archive);
                    return archive;
                }
                URL newUrl = new URL(path);
                return VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, URISyntaxException.class, newUrl);
            }
            return VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, URISyntaxException.class, url);
        } catch (URISyntaxException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }

    }
    private Object getJarChild(URL url) throws IOException {
        try {
            URI uri = new java.net.URI(url.toString());
            if (uri.getPath() == null) {
                String path = uri.toString();
                if (path.startsWith("jar:file:")) {
                    path = path.substring(path.indexOf("file:/"));
                    path = path.substring(0,path.toLowerCase().indexOf(".jar")+4);

                    if (path.startsWith("file://")){ //UNC Path
                        path = "C:/" + path.substring(path.indexOf("file:/")+7);
                        path = "/" + new java.net.URI(path).getPath();
                    }
                    VirtualFile archive = VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, URISyntaxException.class, new URL(path));
                    if (archive.getChildrenRecursively().size() == 0){
                        TempFileProvider provider = TempFileProvider.create("tmp", Executors.newScheduledThreadPool(2));
                        Closeable handle = VFS.mountZipExpanded(new FileInputStream(archive.getPhysicalFile()), archive.getName() ,archive, provider);
                    }
                    String fullPath = url.getPath();
                    fullPath = fullPath.replaceFirst("file:", "");
                    fullPath = fullPath.replaceFirst("!", "");
                    List<VirtualFile> children = archive.getChildrenRecursively();
                    for (VirtualFile file: children){
                        if (file.getPathName().equals(fullPath)){
                            return file;
                        }
                    }
                    return archive;
                }
                URL newUrl = new URL(path);
                return VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, URISyntaxException.class, newUrl);
            }
            return VFSUtil.invokeMethodWithExpectedExceptionType(VFSUtil.VFS_METHOD_GET_ROOT_URL, null, URISyntaxException.class, url);
        } catch (URISyntaxException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }

    }
}

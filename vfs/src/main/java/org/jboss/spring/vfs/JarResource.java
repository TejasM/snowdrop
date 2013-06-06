package org.jboss.spring.vfs;

import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * User: tmehta
 * Date: 05/06/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JarResource extends AbstractResource {

    private JarFile jarFile;

    private JarEntry jarEntry = null;

    public JarResource(JarFile jarFile, JarEntry jarEntry) {
        this.jarEntry = jarEntry;
        this.jarFile = jarFile;
    }

    public JarResource(URL url) {
        String urlPath = url.getFile();
        if (urlPath.startsWith("file:")) {
            urlPath = urlPath.substring(5);
        }
        int p = urlPath.indexOf('!');
        String innerPath = "";
        if (p > 0) {
            innerPath = urlPath.substring(p + 2);
            urlPath = urlPath.substring(0, p);
        }
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(urlPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot retrieve file from URL: ", e);
        }
        this.jarFile = jarFile;
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            if (jarEntry.getName().equals(innerPath)) {
                this.jarEntry = jarEntry;
                return;
            }
        }
    }

    @Override
    public String getDescription() {
        return jarEntry.toString();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return jarFile.getInputStream(jarEntry);
    }

    public String getFilename() {
        int splitPoint = jarEntry.getName().lastIndexOf("/");
        return jarEntry.getName().substring(splitPoint+1);
    }

    public String toString() {
        return getDescription();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JarResource) {
            return jarEntry.equals(((JarResource) other).jarEntry) && jarFile.equals(((JarResource) other).jarFile);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return jarEntry.hashCode();
    }
    public boolean exists() {
        return jarEntry != null;
    }

    public boolean isOpen() {
        return false;
    }

    public boolean isReadable() {
        return jarEntry.getSize() > 0;
    }

    public long lastModified() {
        return jarEntry.getTime();
    }

    public URL getURL() throws IOException {
        return new URL("jar:file:" + jarFile.getName() + "!/" + jarEntry.getName());
    }

    public URI getURI() throws IOException {
        try {
            return new URI(getURL().getPath());
        } catch (URISyntaxException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }
}

package com.sakyvo.nestedpackfix.forge1710.pack;

import com.sakyvo.nestedpackfix.archive.ArchiveKind;
import com.sakyvo.nestedpackfix.archive.ArchiveLayout;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.ResourcePackFileNotFoundException;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

public final class CompatibleFileResourcePack extends FileResourcePack {
    private static final String PACK_METADATA = "pack.mcmeta";
    private static final String ASSETS = "assets/";

    private final File file;
    private ArchiveLayout layout;
    private java.util.zip.ZipFile compatibleZipFile;
    private org.apache.commons.compress.archivers.zip.ZipFile commonsZipFile;
    private boolean resolvingMetadata;

    CompatibleFileResourcePack(File file, ArchiveLayout layout) {
        super(file);
        this.file = file;
        this.layout = layout;
    }

    @Override
    public IMetadataSection getPackMetadata(
        IMetadataSerializer serializer,
        String sectionName
    ) throws IOException {
        this.resolvingMetadata = true;
        try {
            return super.getPackMetadata(serializer, sectionName);
        } catch (IOException failure) {
            this.handleMetadataFailure(failure);
            throw failure;
        } catch (RuntimeException failure) {
            this.handleMetadataFailure(failure);
            throw failure;
        } finally {
            this.resolvingMetadata = false;
        }
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        if (this.layout != null) {
            return this.openCompatible(name);
        }

        try {
            return super.getInputStreamByName(name);
        } catch (IOException failure) {
            return this.tryMetadataFallback(name, failure);
        } catch (IllegalArgumentException failure) {
            return this.tryMetadataFallback(name, asIOException(failure));
        }
    }

    @Override
    public boolean hasResourceName(String name) {
        if (this.layout == null) {
            try {
                return super.hasResourceName(name);
            } catch (IllegalArgumentException failure) {
                return false;
            }
        }

        try {
            return this.findCompatibleEntry(this.mapName(name)) != null;
        } catch (IOException | IllegalArgumentException failure) {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getResourceDomains() {
        if (this.layout == null) {
            try {
                return super.getResourceDomains();
            } catch (IllegalArgumentException failure) {
                return Collections.emptySet();
            }
        }

        try {
            return this.readCompatibleDomains();
        } catch (IOException | IllegalArgumentException failure) {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            super.close();
        } catch (IOException closeFailure) {
            failure = closeFailure;
        }

        if (this.compatibleZipFile != null) {
            try {
                this.compatibleZipFile.close();
            } catch (IOException closeFailure) {
                if (failure == null) {
                    failure = closeFailure;
                }
            } finally {
                this.compatibleZipFile = null;
            }
        }

        if (this.commonsZipFile != null) {
            try {
                this.commonsZipFile.close();
            } catch (IOException closeFailure) {
                if (failure == null) {
                    failure = closeFailure;
                }
            } finally {
                this.commonsZipFile = null;
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    public File getFile() {
        return this.file;
    }

    public boolean isNested() {
        return this.layout != null && this.layout.getKind() == ArchiveKind.PREFIX;
    }

    private InputStream tryMetadataFallback(String name, IOException rootFailure)
        throws IOException {
        if (!this.resolvingMetadata || !PACK_METADATA.equals(name)) {
            throw rootFailure;
        }

        this.layout = ArchiveRegistry.inspect(this.file);
        if (this.layout.getKind() == ArchiveKind.ILLEGAL) {
            throw rootFailure;
        }
        return this.openCompatible(name);
    }

    private InputStream openCompatible(String name) throws IOException {
        if (this.layout.getKind() == ArchiveKind.ILLEGAL) {
            throw new ResourcePackFileNotFoundException(this.file, name);
        }

        String mappedName = this.mapName(name);
        if (!this.layout.isCommonsZipRequired()) {
            ZipEntry entry = this.getCompatibleZipFile().getEntry(mappedName);
            if (entry == null) {
                throw new ResourcePackFileNotFoundException(this.file, mappedName);
            }
            return this.getCompatibleZipFile().getInputStream(entry);
        }

        ZipArchiveEntry entry = this.getCommonsZipFile().getEntry(mappedName);
        if (entry == null) {
            throw new ResourcePackFileNotFoundException(this.file, mappedName);
        }
        return this.getCommonsZipFile().getInputStream(entry);
    }

    private Object findCompatibleEntry(String name) throws IOException {
        return this.layout.isCommonsZipRequired()
            ? this.getCommonsZipFile().getEntry(name)
            : this.getCompatibleZipFile().getEntry(name);
    }

    private Set<String> readCompatibleDomains() throws IOException {
        Set<String> domains = new HashSet<String>();
        String assetsPrefix = this.mapName(ASSETS);
        if (this.layout.isCommonsZipRequired()) {
            Enumeration<ZipArchiveEntry> entries = this.getCommonsZipFile().getEntries();
            while (entries.hasMoreElements()) {
                this.addDomain(domains, assetsPrefix, entries.nextElement().getName());
            }
        } else {
            Enumeration<? extends ZipEntry> entries = this.getCompatibleZipFile().entries();
            while (entries.hasMoreElements()) {
                this.addDomain(domains, assetsPrefix, entries.nextElement().getName());
            }
        }
        return domains;
    }

    private void addDomain(Set<String> domains, String assetsPrefix, String entryName) {
        if (!entryName.startsWith(assetsPrefix)) {
            return;
        }

        int domainStart = assetsPrefix.length();
        int domainEnd = entryName.indexOf('/', domainStart);
        if (domainEnd <= domainStart) {
            return;
        }

        String domain = entryName.substring(domainStart, domainEnd);
        if (domain.equals(domain.toLowerCase(Locale.ROOT))) {
            domains.add(domain);
        } else {
            this.logNameNotLowercase(domain);
        }
    }

    private String mapName(String name) {
        return this.layout.getKind() == ArchiveKind.PREFIX
            ? this.layout.getPrefix() + name
            : name;
    }

    private java.util.zip.ZipFile getCompatibleZipFile() throws IOException {
        if (this.compatibleZipFile == null) {
            this.compatibleZipFile = new java.util.zip.ZipFile(this.file);
        }
        return this.compatibleZipFile;
    }

    private org.apache.commons.compress.archivers.zip.ZipFile getCommonsZipFile()
        throws IOException {
        if (this.commonsZipFile == null) {
            this.commonsZipFile =
                new org.apache.commons.compress.archivers.zip.ZipFile(this.file, "UTF-8", false);
        }
        return this.commonsZipFile;
    }

    private void recordMetadataFailure() {
        try {
            this.layout = ArchiveRegistry.markIllegal(this.file);
        } catch (IOException ignored) {
            this.layout = null;
        }
    }

    private void handleMetadataFailure(Throwable failure) {
        this.recordMetadataFailure();
        try {
            this.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static IOException asIOException(IllegalArgumentException failure) {
        IOException wrapped = new IOException("Could not decode ZIP entry names");
        wrapped.initCause(failure);
        return wrapped;
    }
}

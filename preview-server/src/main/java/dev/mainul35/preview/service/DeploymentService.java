package dev.mainul35.preview.service;

import dev.mainul35.preview.dto.DeploymentStatus;
import dev.mainul35.siteruntime.config.SiteRuntimeProperties;
import dev.mainul35.siteruntime.service.PageDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages snapshot deployment lifecycle.
 * Accepts zip uploads, extracts to the data directory,
 * detects plugin JAR changes, and clears page cache.
 */
@Service
@Slf4j
public class DeploymentService {

    private final SiteRuntimeProperties siteRuntimeProperties;
    private final PageDataService pageDataService;

    @Value("${preview.data-dir:#{systemProperties['user.home'] + '/.vsd/preview-data'}}")
    private String dataDir;

    private boolean deployed = false;
    private String deployedAt;
    private Long siteId;
    private boolean restartRequired = false;
    private int pageCount = 0;

    public DeploymentService(SiteRuntimeProperties siteRuntimeProperties,
                             PageDataService pageDataService) {
        this.siteRuntimeProperties = siteRuntimeProperties;
        this.pageDataService = pageDataService;
    }

    /**
     * Deploy a snapshot zip to the data directory.
     */
    public synchronized DeploymentStatus deploy(MultipartFile snapshotZip, Long deploySiteId) throws IOException {
        Path dataDirPath = Path.of(dataDir);
        Files.createDirectories(dataDirPath);

        // Compute checksums of existing lib/ JARs before deployment
        Map<String, String> oldJarChecksums = computeJarChecksums(dataDirPath.resolve("lib"));

        // Clear existing data (pages, static) but preserve lib/ for checksum comparison
        clearDirectory(dataDirPath.resolve("pages"));
        clearDirectory(dataDirPath.resolve("static"));

        // Extract zip contents
        extractZip(snapshotZip.getInputStream(), dataDirPath);

        // Check if plugin JARs changed
        Map<String, String> newJarChecksums = computeJarChecksums(dataDirPath.resolve("lib"));
        restartRequired = !oldJarChecksums.equals(newJarChecksums);
        if (restartRequired) {
            log.info("Plugin JARs changed — server restart required for backend changes to take effect");
        }

        // Count deployed pages
        Path pagesDir = dataDirPath.resolve("pages");
        pageCount = 0;
        if (Files.isDirectory(pagesDir)) {
            try (var stream = Files.list(pagesDir)) {
                pageCount = (int) stream
                        .filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().startsWith("_"))
                        .count();
            }
        }

        // Update site-runtime data-dir property so pages/assets are served from here
        siteRuntimeProperties.setDataDir(dataDir);

        // Clear page cache so new pages are served immediately
        pageDataService.clearCache();

        // Update state
        deployed = true;
        deployedAt = Instant.now().toString();
        siteId = deploySiteId;

        log.info("Snapshot deployed: {} pages, siteId={}, restartRequired={}", pageCount, siteId, restartRequired);
        return getStatus();
    }

    /**
     * Reload pages from the current data directory (clear cache).
     */
    public synchronized DeploymentStatus reload() {
        pageDataService.clearCache();
        log.info("Page cache cleared — pages will be reloaded from disk");
        return getStatus();
    }

    /**
     * Get current deployment status.
     */
    public synchronized DeploymentStatus getStatus() {
        return new DeploymentStatus(deployed, deployedAt, siteId, restartRequired, pageCount);
    }

    private void extractZip(InputStream zipStream, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName()).normalize();

                // Security: prevent zip slip
                if (!targetPath.startsWith(targetDir)) {
                    throw new IOException("Zip entry outside target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private Map<String, String> computeJarChecksums(Path libDir) {
        Map<String, String> checksums = new TreeMap<>();
        if (!Files.isDirectory(libDir)) return checksums;

        try (var stream = Files.list(libDir)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                    .forEach(jar -> {
                        try {
                            byte[] bytes = Files.readAllBytes(jar);
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            byte[] digest = md.digest(bytes);
                            checksums.put(jar.getFileName().toString(), HexFormat.of().formatHex(digest));
                        } catch (IOException | NoSuchAlgorithmException e) {
                            log.warn("Failed to compute checksum for {}", jar, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to list lib directory: {}", libDir, e);
        }
        return checksums;
    }

    private void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (!d.equals(dir)) {
                    Files.delete(d);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}

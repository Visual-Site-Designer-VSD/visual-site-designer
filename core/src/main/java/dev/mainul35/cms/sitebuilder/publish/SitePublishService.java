package dev.mainul35.cms.sitebuilder.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.plugin.entity.Plugin;
import dev.mainul35.cms.plugin.repository.PluginRepository;
import dev.mainul35.cms.sitebuilder.entity.Page;
import dev.mainul35.cms.sitebuilder.entity.Site;
import dev.mainul35.cms.sitebuilder.repository.PageRepository;
import dev.mainul35.cms.sitebuilder.repository.SiteRepository;
import dev.mainul35.cms.sitebuilder.runtimelink.RuntimeLinkService;
import dev.mainul35.cms.sitebuilder.service.PageVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Publishes a site as an immutable snapshot for the live runtime.
 *
 * <p>Each page contributes its active saved version (or its latest if none is active). The version of every
 * plugin the pages use is read from the installed, active plugins and pinned in the snapshot.
 * Snapshots land in {@code <vsd.publish.dir>/<site-slug>/<version>/}; the file {@code latest} next to them
 * names the newest version, and the runtime's {@code snapshot-dir} points at the version directory.
 */
@Service
public class SitePublishService {

    private static final Logger log = LoggerFactory.getLogger(SitePublishService.class);
    private static final DateTimeFormatter VERSION = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final PageVersionService pageVersionService;
    private final PluginRepository pluginRepository;
    private final SnapshotBuilder builder;
    private final Path publishRoot;
    private final RuntimeLinkService runtimeLink;

    public SitePublishService(SiteRepository siteRepository,
                              PageRepository pageRepository,
                              PageVersionService pageVersionService,
                              PluginRepository pluginRepository,
                              ObjectMapper mapper,
                              RuntimeLinkService runtimeLink,
                              @Value("${vsd.publish.dir:published-sites}") String publishDir) {
        this.runtimeLink = runtimeLink;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.pageVersionService = pageVersionService;
        this.pluginRepository = pluginRepository;
        this.builder = new SnapshotBuilder(mapper);
        this.publishRoot = Path.of(publishDir).toAbsolutePath().normalize();
    }

    /**
     * @throws PublishException if the site cannot be published; nothing is written in that case
     */
    @Transactional(readOnly = true)
    public SnapshotBuilder.Result publishSnapshot(Long siteId) {
        return snapshotInto(siteId, publishRoot, runtimeLink.declaredNames().orElse(null));
    }

    /**
     * Builds a throw-away snapshot of the site's saved pages under {@code previewRoot}, for the live preview.
     * Same rules as publishing, except that shared state is not checked against the declarations.
     */
    @Transactional(readOnly = true)
    public SnapshotBuilder.Result previewSnapshot(Long siteId, Path previewRoot) {
        return snapshotInto(siteId, previewRoot.toAbsolutePath().normalize(), null);
    }

    private SnapshotBuilder.Result snapshotInto(Long siteId, Path root, java.util.Set<String> declaredShared) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new PublishException("Site " + siteId + " does not exist."));

        List<SnapshotBuilder.PageInput> inputs = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Page page : pageRepository.findBySiteIdOrderByDisplayOrderAsc(siteId)) {
            Optional<String> definition = pageVersionService.getPageDefinition(page.getId());
            if (definition.isEmpty()) {
                skipped.add(page.getPageName());
                continue;
            }
            inputs.add(new SnapshotBuilder.PageInput(
                    page.getPageSlug(), page.getPageName(), page.getTitle(), page.getRoutePath(),
                    page.getPageType() == Page.PageType.HOMEPAGE, definition.get()));
        }
        if (!skipped.isEmpty()) {
            log.warn("Publishing site {} without page(s) that have no saved content: {}", siteId, skipped);
        }

        Map<String, String> installed = new TreeMap<>();
        for (Plugin p : pluginRepository.findAllActivated()) {
            installed.put(p.getPluginId(), p.getVersion());
        }

        String slug = safeDirName(site.getSiteSlug());
        Path siteRoot = root.resolve(slug);
        try {
            Files.createDirectories(siteRoot);
        } catch (IOException e) {
            throw new PublishException("Could not create " + siteRoot + ": " + e.getMessage(), e);
        }
        String version = LocalDateTime.now().format(VERSION);
        SnapshotBuilder.Result result = builder.build(
                new SnapshotBuilder.SiteInfo(site.getSiteName(), site.getSiteName()),
                inputs, installed, siteRoot, version, declaredShared);
        log.info("Built snapshot {} of site {} ({} page(s), plugins {})",
                siteId, version, result.pages(), result.plugins());
        return result;
    }

    private static String safeDirName(String slug) {
        String s = slug == null ? "" : slug.toLowerCase().replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "site" : s;
    }
}

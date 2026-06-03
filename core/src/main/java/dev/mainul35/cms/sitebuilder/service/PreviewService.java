package dev.mainul35.cms.sitebuilder.service;

import dev.mainul35.cms.sitebuilder.dto.PreviewStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Deploys site snapshots to an externally-running preview server.
 * The preview server lifecycle (start/stop) is managed by the IntelliJ plugin,
 * not by this CMS service.
 */
@Service
@Slf4j
public class PreviewService {

    private final SiteSnapshotService siteSnapshotService;
    private final RestClient restClient;

    @Value("${app.preview.server-url:http://localhost:3001}")
    private String previewServerUrl;

    public PreviewService(SiteSnapshotService siteSnapshotService) {
        this.siteSnapshotService = siteSnapshotService;
        this.restClient = RestClient.create();
    }

    /**
     * Deploy a site snapshot to the running preview server.
     */
    public PreviewStatus deployToPreview(Long siteId) {
        try {
            log.info("Creating snapshot for site {} and deploying to {}", siteId, previewServerUrl);

            // Create snapshot zip
            byte[] snapshotZip = siteSnapshotService.createSnapshotZip(siteId);
            log.info("Snapshot zip created: {} bytes", snapshotZip.length);

            // Upload to preview server
            ByteArrayResource snapshotResource = new ByteArrayResource(snapshotZip) {
                @Override
                public String getFilename() {
                    return "snapshot.zip";
                }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("snapshot", snapshotResource);
            body.add("siteId", siteId.toString());

            Map<String, Object> response = restClient.post()
                    .uri(previewServerUrl + "/api/deploy")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            boolean restartRequired = Boolean.TRUE.equals(response.get("restartRequired"));
            int pageCount = response.get("pageCount") != null ? ((Number) response.get("pageCount")).intValue() : 0;

            log.info("Snapshot deployed successfully: {} pages, restartRequired={}", pageCount, restartRequired);
            return PreviewStatus.success(previewServerUrl, siteId, restartRequired, pageCount);

        } catch (Exception e) {
            log.error("Failed to deploy to preview server at {}", previewServerUrl, e);
            return PreviewStatus.error("Preview server not reachable at " + previewServerUrl +
                    ". Start it from the IntelliJ plugin first.");
        }
    }

    /**
     * Get the current deployment status from the preview server.
     */
    public PreviewStatus getPreviewStatus() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(previewServerUrl + "/api/deploy/status")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            boolean deployed = Boolean.TRUE.equals(response.get("deployed"));
            if (!deployed) {
                return PreviewStatus.notDeployed();
            }

            Long siteId = response.get("siteId") != null ? ((Number) response.get("siteId")).longValue() : null;
            boolean restartRequired = Boolean.TRUE.equals(response.get("restartRequired"));
            int pageCount = response.get("pageCount") != null ? ((Number) response.get("pageCount")).intValue() : 0;

            return PreviewStatus.success(previewServerUrl, siteId, restartRequired, pageCount);

        } catch (Exception e) {
            log.debug("Preview server not reachable at {}: {}", previewServerUrl, e.getMessage());
            return PreviewStatus.error("Preview server not reachable at " + previewServerUrl);
        }
    }

    /**
     * Redeploy the snapshot (same as deploy).
     */
    public PreviewStatus refreshPreview(Long siteId) {
        return deployToPreview(siteId);
    }
}

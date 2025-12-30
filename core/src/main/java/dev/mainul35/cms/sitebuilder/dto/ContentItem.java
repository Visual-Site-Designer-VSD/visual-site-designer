package dev.mainul35.cms.sitebuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO representing a content item in the repository.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentItem {

    private String id;
    private String name;
    private String originalName;
    private String type; // image, video, pdf, other
    private String mimeType;
    private long size;
    private String url;
    private String thumbnailUrl;
    private String folder;
    private Instant uploadedAt;
    private int width;  // For images
    private int height; // For images
}

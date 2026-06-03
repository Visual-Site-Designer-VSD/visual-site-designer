package dev.mainul35.cms.sitebuilder.dto;

public record PreviewStatus(
        boolean deployed,
        String url,
        Long siteId,
        boolean restartRequired,
        int pageCount,
        String error
) {
    public static PreviewStatus success(String url, Long siteId, boolean restartRequired, int pageCount) {
        return new PreviewStatus(true, url, siteId, restartRequired, pageCount, null);
    }

    public static PreviewStatus notDeployed() {
        return new PreviewStatus(false, null, null, false, 0, null);
    }

    public static PreviewStatus error(String error) {
        return new PreviewStatus(false, null, null, false, 0, error);
    }
}

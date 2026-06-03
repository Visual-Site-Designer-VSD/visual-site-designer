package dev.mainul35.preview.dto;

public record DeploymentStatus(
        boolean deployed,
        String deployedAt,
        Long siteId,
        boolean restartRequired,
        int pageCount
) {
    public static DeploymentStatus notDeployed() {
        return new DeploymentStatus(false, null, null, false, 0);
    }
}

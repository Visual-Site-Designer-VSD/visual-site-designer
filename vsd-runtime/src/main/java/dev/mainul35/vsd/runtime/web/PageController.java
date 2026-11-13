package dev.mainul35.vsd.runtime.web;

import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.SessionService;
import dev.mainul35.vsd.runtime.snapshot.SiteSnapshot;
import dev.mainul35.vsd.runtime.snapshot.SnapshotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/** Serves the published site's pages. More specific routes (/_vsd/**, /assets/**) win over this catch-all. */
@RestController
public class PageController {

    private final SnapshotService snapshots;
    private final SessionService sessions;
    private final PageRenderer renderer;

    public PageController(SnapshotService snapshots, SessionService sessions, PageRenderer renderer) {
        this.snapshots = snapshots;
        this.sessions = sessions;
        this.renderer = renderer;
    }

    @GetMapping("/**")
    public ResponseEntity<String> page(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        Optional<SiteSnapshot.PageEntry> page = snapshots.byPath(path);
        if (page.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(PageRenderer.notFound());
        }
        Session session = sessions.getOrCreate(request, response);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore()) // the shell embeds this visitor's session state
                .body(renderer.render(page.get(), session));
    }
}

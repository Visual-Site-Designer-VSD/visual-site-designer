package dev.mainul35.siteruntime.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA fallback controller.
 * Forwards all non-API, non-static routes to index.html so that
 * React Router can handle client-side routing.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/{path:^(?!api|static|css|js|images|favicon\\.ico|plugins).*$}",
            "/{path:^(?!api|static|css|js|images|favicon\\.ico|plugins).*$}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}

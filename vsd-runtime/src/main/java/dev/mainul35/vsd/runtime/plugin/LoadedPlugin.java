package dev.mainul35.vsd.runtime.plugin;

import dev.mainul35.cms.sdk.runtime.RuntimeEventHandler;

import java.util.List;
import java.util.function.Supplier;

/**
 * A plugin the runtime has loaded. Bundle suppliers return null when the file is absent.
 * Development plugins read their bundle from disk on each call so rebuilds show up.
 */
public record LoadedPlugin(
        String id,
        String version,
        Supplier<byte[]> bundleJs,
        Supplier<byte[]> bundleCss,
        List<RuntimeEventHandler> handlers,
        String source) {
}

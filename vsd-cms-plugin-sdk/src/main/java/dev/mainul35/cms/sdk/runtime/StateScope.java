package dev.mainul35.cms.sdk.runtime;

/**
 * Scope of a piece of runtime state. Every scope lives inside one visitor session.
 *
 * <ul>
 *   <li>{@link #SESSION} - shared by everything the visitor sees; owner is a free-form namespace.</li>
 *   <li>{@link #PAGE} - owner is the page name.</li>
 *   <li>{@link #PLUGIN} - owner is the plugin id; the natural home for plugin-private state.</li>
 *   <li>{@link #COMPONENT} - owner is the component instance id.</li>
 * </ul>
 *
 * Owners starting with {@value StateKey#RESERVED_PREFIX} are reserved for the runtime
 * itself (for example {@code $auth}) and can only be written by server-side code.
 */
public enum StateScope {
    SESSION, PAGE, PLUGIN, COMPONENT
}

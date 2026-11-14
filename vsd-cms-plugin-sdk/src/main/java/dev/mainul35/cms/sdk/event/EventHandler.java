package dev.mainul35.cms.sdk.event;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as an event handler.
 *
 * The framework will automatically discover and register methods annotated with @EventHandler.
 * These handlers can be invoked from the frontend via the event system.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @PluginService
 * public class MyButtonService {
 *
 *     @EventHandler(
 *         componentId = "MyButton",
 *         eventType = "onClick",
 *         description = "Handles button click and logs to database"
 *     )
 *     public EventResult handleButtonClick(EventContext context) {
 *         String buttonText = context.getProp("text", String.class);
 *         // Do something...
 *         return EventResult.success()
 *             .withData("message", "Button clicked: " + buttonText)
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * <h2>Handler Method Signature:</h2>
 * <ul>
 *   <li>Must accept a single {@link EventContext} parameter</li>
 *   <li>Must return {@link EventResult}</li>
 *   <li>Can be sync or async (return CompletableFuture&lt;EventResult&gt;)</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventHandler {

    /**
     * The component ID this handler is for (e.g., "Button", "Form", "CustomWidget")
     * Use "*" to handle events for all components
     */
    String componentId();

    /**
     * The event type to handle (e.g., "onClick", "onSubmit", "onDataLoad")
     * Use "*" to handle all event types for the component
     */
    String eventType();

    /**
     * Human-readable description of what this handler does
     */
    String description() default "";

    /**
     * Priority of this handler (higher = runs first)
     * Default is 0. Use negative for low priority, positive for high priority.
     */
    int priority() default 0;

    /**
     * Whether this handler should run asynchronously
     * Async handlers don't block the response to the frontend
     */
    boolean async() default false;

    /**
     * Whether to continue to the next handler if this one succeeds
     * Set to false to stop the handler chain after this handler
     */
    boolean continueOnSuccess() default true;

    /**
     * Whether to continue to the next handler if this one fails
     * Set to true to allow other handlers to run even if this one fails
     */
    boolean continueOnError() default false;
}

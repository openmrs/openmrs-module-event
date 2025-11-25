package org.openmrs.event;

import java.util.Optional;

/**
 * This class holds an instance of a {@link EventClassScanner} meant to be shared across a single thread. The instance
 * is instantiated when constructing this class and is removed when calling the {@link #close()} method, so that it
 * can be used in the try-with-resources pattern.
 * <br/><br/>
 * The intended use is: <pre>{@code
 *  try (EventClassScannerThreadHolder holder = new EventClassScannerThreadHolder()) {
 *      // do something that needs an EventClassScanner like subscribing or unsubscribing
 *  }
 *  }</pre>
 */
public class EventClassScannerThreadHolder implements AutoCloseable {

    private static final ThreadLocal<EventClassScanner> eventClassScannerHolder = new ThreadLocal<>();

    public static Optional<EventClassScanner> getCurrentEventClassScanner() {
        return Optional.ofNullable(eventClassScannerHolder.get());
    }

    public EventClassScannerThreadHolder() {
        eventClassScannerHolder.set(new EventClassScanner());
    }

    @Override
    public void close() {
        eventClassScannerHolder.remove();
    }
}

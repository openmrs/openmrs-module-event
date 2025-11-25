package org.openmrs.event;

import java.util.Optional;

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

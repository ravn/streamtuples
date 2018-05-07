package dk.kb.stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class WasClosedTest {

    @Test
    void close() throws Exception {
        WasClosed wc = new WasClosed();
        assertFalse(wc.isClosed());
        wc.close();
        assertTrue(wc.isClosed());
    }

    @Test
    void wasAutoClosed() throws Exception {
        final WasClosed outsideWasClosed = new WasClosed();
        try (WasClosed wc = outsideWasClosed) {
            assertFalse(wc.isClosed());
        }
        assertTrue(outsideWasClosed.isClosed());
    }
}

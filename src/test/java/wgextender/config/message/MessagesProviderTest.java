package wgextender.config.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static wgextender.config.message.MessagesProvider.Serializer.*;

public class MessagesProviderTest {
    @Test
    public void serializersTest() {
        assertEquals(
                LEGACY_SECTION.decoder().deserialize("§aTest"),
                LEGACY.decoder().deserialize("&aTest")
        );
        assertEquals(
                LEGACY_AMPERSAND.decoder().deserialize("&aTest"),
                LEGACY.decoder().deserialize("§aTest")
        );
    }
}

package org.prebid.cache.helpers;

import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.model.PayloadTransfer;

import java.util.UUID;

@Slf4j
public class RandomUUID {
    public static String extractUUID(final PayloadTransfer payload) {
        return (payload.getKey() != null)
                ? payload.getKey() : String.valueOf(UUID.randomUUID());
    }

    public static boolean isExternalUUID(final PayloadTransfer payload) {
        return payload.getKey() != null;
    }

    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.length() == 0) {
            log.error("UUID cannot be NULL or zero length !!");
            return false;
        }

        // check for alphanumeric, hyphen, and underscore
        boolean isValid = uuid.matches("^[a-zA-Z0-9_-]*$");
        if (!isValid)
            log.debug("Invalid UUID: {}", uuid);

        return isValid;
    }

    private RandomUUID() {
    }
}

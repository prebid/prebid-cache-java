package org.prebid.cache.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class Json {
    private final Gson gson = new Gson();

    public String toJson(Object object) {
        log.debug("{}", gson.toJson(object));
        return gson.toJson(object);
    }

    public <T> T createPayloadFromJson(final String json, Class<T> type) {
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("Failed to decode JSON payload: '{}'", json);
            throw e;
        }
    }
}

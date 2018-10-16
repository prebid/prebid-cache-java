package org.prebid.cache.helpers;

import com.google.gson.Gson;
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
        return gson.fromJson(json, type);
    }
}

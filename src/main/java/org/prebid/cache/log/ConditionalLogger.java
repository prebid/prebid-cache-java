package org.prebid.cache.log;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

@AllArgsConstructor
public class ConditionalLogger {

    private final Logger logger;

    public void info(String message, double samplingRate) {
        if (samplingRate >= 1.0d || ThreadLocalRandom.current().nextDouble() < samplingRate) {
            logger.info(message);
        }
    }

    public void debug(String message, double samplingRate) {
        if (samplingRate >= 1.0d || ThreadLocalRandom.current().nextDouble() < samplingRate) {
            logger.debug(message);
        }
    }

    public void error(String message, double samplingRate) {
        if (samplingRate >= 1.0d || ThreadLocalRandom.current().nextDouble() < samplingRate) {
            logger.error(message);
        }
    }

    public void warn(String message, double samplingRate) {
        if (samplingRate >= 1.0d || ThreadLocalRandom.current().nextDouble() < samplingRate) {
            logger.warn(message);
        }
    }

}

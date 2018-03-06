package org.prebid.cache.helpers;

import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.function.Supplier;

@Component
public class CurrentDateProvider implements Supplier<Date> {

    @Override
    public Date get() {
        return Calendar.getInstance().getTime();
    }
}

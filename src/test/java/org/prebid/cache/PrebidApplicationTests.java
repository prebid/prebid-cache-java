package org.prebid.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.repository.aerospike.AerospikePropertyConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {PBCacheApplication.class, AerospikePropertyConfiguration.class})
class PrebidApplicationTests {

    @Test
    void contextLoads() {
    }

}

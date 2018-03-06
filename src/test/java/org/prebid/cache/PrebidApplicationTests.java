package org.prebid.cache;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PBCacheApplication.class)
public class PrebidApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void contextTest(){
		PBCacheApplication.main(new String[] {"--management.server.port: "});
	}

}

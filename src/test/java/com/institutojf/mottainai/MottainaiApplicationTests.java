package com.institutojf.mottainai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "security.staff.data.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class MottainaiApplicationTests {

	@Test
	void contextLoads() {
	}

}

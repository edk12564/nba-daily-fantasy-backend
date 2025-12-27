package com.picknroll.demo;

import com.picknroll.demo.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseConnectionIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void dataSourceShouldConnectAndExecuteSimpleQuery() throws Exception {
        assertNotNull(dataSource, "DataSource should be auto-configured");
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            assertTrue(conn.isValid(2), "Connection should be valid");
            try (ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }
}

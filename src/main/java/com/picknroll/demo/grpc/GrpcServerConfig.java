package com.picknroll.demo.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for gRPC server settings.
 * This ensures the gRPC server runs alongside the REST API without conflicts.
 */
@Configuration
public class GrpcServerConfig {

    @Bean
    @ConfigurationProperties(prefix = "grpc.server")
    public GrpcServerProperties grpcServerProperties() {
        return new GrpcServerProperties();
    }

    public static class GrpcServerProperties {
        private int port = 9090;
        private String address = "0.0.0.0";

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
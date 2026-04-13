package com.hrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.config.DatabaseConfig;
import com.hrm.model.*;
import com.hrm.repository.RuleRepository;
import com.hrm.repository.ViolationRepository;
import com.hrm.rules.ZenRuleEngine;
import com.hrm.util.DataFetcher;

import io.vertx.core.Vertx;
import com.hrm.api.MainVerticle;

import java.io.InputStream;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("HRM Tool - Eclipse Vert.x Server");
        System.out.println("========================================");

        DatabaseConfig.initializeDatabase();
        seedRulesFromExternalConfig();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), res -> {
            if (res.succeeded()) {
                System.out.println("MainVerticle deployed properly. Server is online.");
            } else {
                System.err.println("Verticle deployment failed: " + res.cause());
            }
        });
    }

    private static void seedRulesFromExternalConfig() {
        RuleRepository ruleRepo = new RuleRepository();
        if (ruleRepo.isRuleTableEmpty()) {
            System.out.println("Seeding rules from master configuration...");
            try (InputStream is = Main.class.getClassLoader().getResourceAsStream("rules/master-rules.json")) {
                if (is != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rulesArray = mapper.readTree(is);
                    if (rulesArray.isArray()) {
                        for (JsonNode r : rulesArray) {
                            ruleRepo.saveRule(r.get("id").asInt(), r.get("name").asText(), r.get("content").toString(), r.get("category").asText());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Seeding failed: " + e.getMessage());
            }
        }
    }
}

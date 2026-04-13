package com.hrm.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.model.DataBundle;
import com.hrm.model.Employee;
import com.hrm.model.Rule;
import com.hrm.model.Violation;
import com.hrm.repository.RuleRepository;
import com.hrm.repository.ViolationRepository;
import com.hrm.rules.ZenRuleEngine;
import com.hrm.util.DataFetcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RuleRepository ruleRepo = new RuleRepository();
    private final ViolationRepository reportRepo = new ViolationRepository();

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        // Allow CORS for Dashboard (Standard Vite ports & Loopback)
        router.route().handler(CorsHandler.create()
            .addOrigin("http://localhost:5173")
            .addOrigin("http://localhost:5174")
            .addOrigin("http://127.0.0.1:5173")
            .addOrigin("http://127.0.0.1:5174")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Authorization"));

        router.route().handler(BodyHandler.create());

        router.get("/api/rules").handler(this::getRules);
        router.put("/api/rules").handler(this::updateRule);
        router.post("/api/audit/run").handler(this::runAuditBatch);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                    System.out.println("HTTP API Server started on port 8080");
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }

    private void getRules(RoutingContext ctx) {
        vertx.executeBlocking(promise -> {
            try {
                List<Rule> rules = ruleRepo.getAllActiveRules();
                // We re-assemble them into the JSON format expected by the frontend UI
                java.util.List<Map<String, Object>> mappedRules = new java.util.ArrayList<>();
                for (Rule rule : rules) {
                    try {
                        mappedRules.add(Map.of(
                            "id", rule.getId(),
                            "name", rule.getName(),
                            "category", rule.getAuditCategory(),
                            "content", mapper.readTree(rule.getContent())
                        ));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                promise.complete(mapper.writeValueAsString(mappedRules));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, res -> {
            if (res.succeeded()) {
                ctx.response().putHeader("content-type", "application/json").end((String) res.result());
            } else {
                ctx.response().setStatusCode(500).end("Internal Server Error: " + res.cause().getMessage());
            }
        });
    }

    private void updateRule(RoutingContext ctx) {
        vertx.executeBlocking(promise -> {
            try {
                String reqBody = ctx.body().asString();
                if (reqBody != null && !reqBody.isEmpty()) {
                    JsonNode rootArray = mapper.readTree(reqBody);
                    if (rootArray.isArray() && rootArray.size() > 0) {
                        JsonNode root = rootArray.get(0);
                        int id = root.get("id").asInt();
                        String name = root.get("name").asText();
                        String category = root.get("category").asText();
                        String content = root.get("content").toString();
                        ruleRepo.saveRule(id, name, content, category);
                        promise.complete();
                    } else {
                        promise.fail("Invalid Payload Structure");
                    }
                } else {
                    promise.fail("Empty Body");
                }
            } catch (Exception e) {
                promise.fail(e);
            }
        }, res -> {
            if (res.succeeded()) {
                ctx.response().setStatusCode(200).end("{\"status\":\"success\"}");
            } else {
                ctx.response().setStatusCode(400).end("{\"error\":\"" + res.cause().getMessage() + "\"}");
            }
        });
    }

    private void runAuditBatch(RoutingContext ctx) {
        // Return 202 instantly, indicating process started
        ctx.response().setStatusCode(202).putHeader("content-type", "application/json").end("{\"status\":\"started\"}");

        // Execute batch heavy load on worker thread
        vertx.executeBlocking(promise -> {
            try {
                System.out.println("Triggered Async Audit Batch...");
                reportRepo.clearOldReports();
                
                DataFetcher fetcher = new DataFetcher();
                List<Employee> employees = fetcher.fetchEmployees();
                JsonNode systemData = fetcher.fetchSystemData();
                DataBundle bundle = new DataBundle(employees, systemData);
                
                ZenRuleEngine engine = new ZenRuleEngine();
                List<Violation> violations = engine.executeAudit(bundle);
                
                System.out.println("Found " + violations.size() + " total violations.");
                persistBifurcatedAudit(violations, reportRepo);
                promise.complete();
            } catch (Exception e) {
                System.err.println("Async Batch Failed: " + e.getMessage());
                promise.fail(e);
            }
        }, res -> {
            if (res.succeeded()) {
                System.out.println("Async Audit Batch Completed.");
            }
        });
    }

    private void persistBifurcatedAudit(List<Violation> violations, ViolationRepository repo) {
        violations.stream()
            .filter(v -> v.entityType().equals("EMPLOYEE"))
            .collect(Collectors.groupingBy(Violation::entityId))
            .forEach((empId, list) -> {
                repo.saveEmployeeAudit(empId, list.stream().map(Violation::ruleId).distinct().toList());
            });

        violations.stream()
            .filter(v -> v.entityType().equals("SYSTEM"))
            .collect(Collectors.groupingBy(Violation::entityId))
            .forEach((configId, list) -> {
                repo.saveSystemAudit(configId, list.stream().map(Violation::ruleId).distinct().toList());
            });
    }
}

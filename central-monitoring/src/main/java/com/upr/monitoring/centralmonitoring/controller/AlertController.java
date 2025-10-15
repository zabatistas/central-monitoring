package com.upr.monitoring.centralmonitoring.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.upr.monitoring.centralmonitoring.model.AlertRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Operations for managing alert rules and notifications")
public class AlertController {

    private static final String RULES_FOLDER = "./resources/";

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Operation(
        summary = "Create new alert rule",
        description = "Creates a new alert rule file for the specified application. " +
                     "The rule will be saved as a YAML file and Thanos will be reloaded to apply the new rules."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert rule successfully created",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid alert request data",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Failed to create alert rule file or reload Thanos",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @PostMapping("/create")
    public String createAlertRule(
            @Parameter(description = "Alert rule configuration including application ID and rules", required = true)
            @RequestBody AlertRequest request) throws IOException {
        // Build rule structure
        Map<String, Object> ruleFile = new HashMap<>();
        Map<String, Object> group = new HashMap<>();
        group.put("name", request.getApplicationId());

        // Convert alerts to rule list
        group.put("rules", request.getRules());
        ruleFile.put("groups", List.of(group));

        // Ensure the rules folder exists
        File folder = new File(RULES_FOLDER);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                throw new IOException("Failed to create rules folder: " + RULES_FOLDER);
            }
        }

        // File path for this application
        File outputFile = new File(folder, request.getApplicationId() + ".yaml");

        // If the file does not exist, create it
        if (!outputFile.exists()) {
            boolean created = outputFile.createNewFile();
            if (!created) {
                throw new IOException("Failed to create rule file: " + outputFile.getAbsolutePath());
            }
        }

        // Write YAML content to the file
        yamlMapper.writeValue(outputFile, ruleFile);

        

        // Optionally trigger Thanos reload (if reachable from the app)
        new RestTemplate().postForObject("http://82.223.13.241:10911/-/reload", null, String.class);

        return "Rule file created: " + outputFile.getAbsolutePath();
    }



    @Operation(
        summary = "Modify existing alert rule",
        description = "Appends new rules to an existing alert rule file for the specified application. " +
                     "If the application group doesn't exist, it will be created."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert rules successfully appended",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "404", description = "Alert rule file not found for the application",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid alert request data",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Failed to modify alert rule file",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @PatchMapping("/modify")
    public String modifyAlertRule(
            @Parameter(description = "Alert rule configuration with additional rules to append", required = true)
            @RequestBody AlertRequest request) throws IOException {
        File outputFile = new File(RULES_FOLDER, request.getApplicationId() + ".yaml");
        if (!outputFile.exists()) {
            throw new IOException("Rule file does not exist for applicationId: " + request.getApplicationId());
        }

        // Read the existing file
        Map<?, ?> existingYaml = yamlMapper.readValue(outputFile, Map.class);
        List<Map<String, Object>> groups = (List<Map<String, Object>>) existingYaml.get("groups");

        // Find the group with matching applicationId
        Map<String, Object> targetGroup = null;
        for (Map<String, Object> g : groups) {
            if (request.getApplicationId().equals(g.get("name"))) {
                targetGroup = g;
                break;
            }
        }

        if (targetGroup == null) {
            // No group exists for this applicationId, create one
            targetGroup = new HashMap<>();
            targetGroup.put("name", request.getApplicationId());
            targetGroup.put("rules", new ArrayList<>());
            groups.add(targetGroup);
        }

        // Append new rules
        List<Map<String, Object>> existingRules = (List<Map<String, Object>>) targetGroup.get("rules");
        existingRules.addAll(request.getRules());

        // Save back to file
        yamlMapper.writeValue(outputFile, existingYaml);

        return "Rules appended to file: " + outputFile.getAbsolutePath();
    }



}
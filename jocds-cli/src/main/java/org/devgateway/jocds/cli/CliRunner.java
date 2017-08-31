/*
 * Copyright (c) 2017. Development Gateway and contributors. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package org.devgateway.jocds.cli;

import com.github.fge.jackson.JsonLoader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import org.devgateway.jocds.OcdsValidatorNodeRequest;
import org.devgateway.jocds.OcdsValidatorRequest;
import org.devgateway.jocds.OcdsValidatorService;
import org.devgateway.jocds.OcdsValidatorUrlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Validator;

@Component
public class CliRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CliRunner.class);
    @Autowired
    private OcdsValidatorService ocdsValidatorService;
    @Autowired
    private Validator validator;
    private Properties properties = new Properties();

    private void printUsage() throws IOException {
        System.out.println(StreamUtils.copyToString(getClass().getResourceAsStream("/usage.txt"),
                Charset.defaultCharset()));
    }

    private OcdsValidatorRequest createCommonValidatorRequest() {
        OcdsValidatorRequest request = new OcdsValidatorRequest();
        if (properties.containsKey(CliConstants.PARAM_OPERATION)) {
            request.setOperation(properties.getProperty(CliConstants.PARAM_OPERATION));
        }

        if (properties.containsKey(CliConstants.PARAM_SCHEMA_TYPE)) {
            request.setSchemaType(properties.getProperty(CliConstants.PARAM_SCHEMA_TYPE));
        }

        if (properties.containsKey(CliConstants.PARAM_VERSION)) {
            request.setVersion(properties.getProperty(CliConstants.PARAM_VERSION));
        }

        return request;
    }

    private OcdsValidatorNodeRequest createFileValidationRequest() throws IOException {
        OcdsValidatorNodeRequest nodeRequest = new OcdsValidatorNodeRequest(createCommonValidatorRequest(),
                JsonLoader.fromFile(new File(properties.getProperty(CliConstants.PARAM_FILE))));
        return nodeRequest;
    }

    private OcdsValidatorUrlRequest createUrlValidationRequest() throws IOException {
        OcdsValidatorUrlRequest urlRequest = new OcdsValidatorUrlRequest(createCommonValidatorRequest(),
                properties.getProperty(CliConstants.PARAM_URL));
        return urlRequest;
    }

    private boolean validateObject(Object object, String name) {
        DirectFieldBindingResult result = new DirectFieldBindingResult(object, name);
        validator.validate(object, result);
        if (result.hasErrors()) {
            System.out.println(result.toString());
            return false;
        }
        return true;
    }

    @Override
    public void run(String... strings) throws Exception {
        if (strings.length == 0) {
            printUsage();
            return;
        }

        for (String option : strings) {
            String[] keyValue = option.split("=");
            if (keyValue.length != 2) {
                printUsage();
                return;
            }
            properties.setProperty(keyValue[0], keyValue[1]);
        }

        System.out.println("jocds invoked with parameters: " + properties.toString());
        System.out.println();

        if (properties.containsKey(CliConstants.PARAM_FILE)) {
            OcdsValidatorNodeRequest fileValidationRequest = createFileValidationRequest();
            if (!validateObject(fileValidationRequest, "fileValidationRequest")) {
                printUsage();
                return;
            }
            System.out.println(ocdsValidatorService.processingReportToJsonNode(
                    ocdsValidatorService.validate(fileValidationRequest), fileValidationRequest));
        } else {
            if (properties.containsKey(CliConstants.PARAM_URL)) {
                OcdsValidatorUrlRequest urlValidationRequest = createUrlValidationRequest();
                if (!validateObject(urlValidationRequest, "urlValidationRequest")) {
                    printUsage();
                    return;
                }
                System.out.println(ocdsValidatorService.processingReportToJsonNode(
                        ocdsValidatorService.validate(urlValidationRequest), urlValidationRequest));
            } else {
                printUsage();
            }
        }
    }
}
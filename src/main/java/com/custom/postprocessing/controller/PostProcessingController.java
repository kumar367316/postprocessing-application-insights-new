package com.custom.postprocessing.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.custom.postprocessing.service.PostProcessingService;
/**
 * @author kumar.charanswain
 *
 */

@RestController
public class PostProcessingController {

    @Autowired
    private PostProcessingService postProcessingService;

    @GetMapping(path = "/postprocessors")
    public String manualPostProcessBatch() {
        return postProcessingService.smartComPostProcessing();
    }
    
    @GetMapping(path = "/manualarchive")
    public String manualManualArchive() {
        return postProcessingService.archivePostProcessing();
    }
}
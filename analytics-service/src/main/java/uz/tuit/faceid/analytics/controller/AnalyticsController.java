package uz.tuit.faceid.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tuit.faceid.analytics.dto.response.BucketResponse;
import uz.tuit.faceid.analytics.dto.response.ReportResponse;
import uz.tuit.faceid.analytics.dto.response.SummaryResponse;
import uz.tuit.faceid.analytics.dto.response.TimeBucketResponse;
import uz.tuit.faceid.analytics.service.AnalyticsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Reporting over recognition events")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report produced"),
        @ApiResponse(responseCode = "503", description = "recognition-service is not answering")
})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Headline figures",
            description = "Counts per decision, spoof rate, mean similarity")
    public SummaryResponse summary(@Parameter(description = "Event window")
                                   @RequestParam(required = false) Integer window) {
        return analyticsService.summarise(window);
    }

    @GetMapping("/expressions")
    @Operation(summary = "Expression distribution", description = "Most frequent first")
    public List<BucketResponse> expressions(@RequestParam(required = false) Integer window) {
        return analyticsService.expressionDistribution(window);
    }

    @GetMapping("/cameras")
    @Operation(summary = "Distribution across cameras")
    public List<BucketResponse> cameras(@RequestParam(required = false) Integer window) {
        return analyticsService.cameraDistribution(window);
    }

    @GetMapping("/timeline")
    @Operation(summary = "Timeline", description = "Grouped by minute, oldest first")
    public List<TimeBucketResponse> timeline(@RequestParam(required = false) Integer window) {
        return analyticsService.timeline(window);
    }

    @GetMapping("/report")
    @Operation(summary = "Full report",
            description = "Every figure in one response, from a single upstream call")
    public ReportResponse report(@RequestParam(required = false) Integer window) {
        return analyticsService.fullReport(window);
    }
}

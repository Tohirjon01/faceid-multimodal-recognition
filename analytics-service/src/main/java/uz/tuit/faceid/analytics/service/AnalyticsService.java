package uz.tuit.faceid.analytics.service;

import uz.tuit.faceid.analytics.dto.response.BucketResponse;
import uz.tuit.faceid.analytics.dto.response.ReportResponse;
import uz.tuit.faceid.analytics.dto.response.SummaryResponse;
import uz.tuit.faceid.analytics.dto.response.TimeBucketResponse;

import java.util.List;

/** Aggregations over recognition events. */
public interface AnalyticsService {

    SummaryResponse summarise(Integer window);

    List<BucketResponse> expressionDistribution(Integer window);

    List<BucketResponse> cameraDistribution(Integer window);

    List<TimeBucketResponse> timeline(Integer window);

    /** Everything in one response — convenient for export, and one upstream call. */
    ReportResponse fullReport(Integer window);
}

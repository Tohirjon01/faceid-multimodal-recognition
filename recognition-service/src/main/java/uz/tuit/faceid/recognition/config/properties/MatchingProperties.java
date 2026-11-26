package uz.tuit.faceid.recognition.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Matching policy parameters.
 * <p>
 * {@code similarityThreshold} is the cosine similarity cut-off. For ArcFace
 * embeddings, genuine pairs typically fall in 0.6–0.8 and impostor pairs in
 * 0.0–0.2, which is why 0.65 is the default. In production this threshold is
 * tuned from the EER or a TAR@FAR operating point.
 * <p>
 * Values are checked through {@code @Validated}, so a bad configuration fails
 * at startup rather than on the first request.
 */
@Validated
@ConfigurationProperties(prefix = "faceid.matching")
public class MatchingProperties {

    /** Embedding dimension. The ArcFace head emits a 512-d vector. */
    @Min(value = 1, message = "must be positive")
    private int embeddingDimension = 512;

    /** Cosine similarity threshold: below this the result is UNKNOWN. */
    @DecimalMin(value = "-1.0") @DecimalMax(value = "1.0")
    private double similarityThreshold = 0.65;

    /** Liveness threshold: below this the result is SPOOF. */
    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private double livenessThreshold = 0.5;

    /** How many candidates to return in the response. */
    @Min(value = 1, message = "must be at least 1")
    private int topK = 5;

    /** Identifier of the active model version. */
    @Min(value = 1, message = "must be positive")
    private int activeModelId = 1;

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public double getLivenessThreshold() {
        return livenessThreshold;
    }

    public void setLivenessThreshold(double livenessThreshold) {
        this.livenessThreshold = livenessThreshold;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getActiveModelId() {
        return activeModelId;
    }

    public void setActiveModelId(int activeModelId) {
        this.activeModelId = activeModelId;
    }
}

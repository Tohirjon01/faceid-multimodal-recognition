package uz.tuit.faceid.recognition.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VectorUtils — vector operations")
class VectorUtilsTest {

    private static final int DIMENSION = 512;

    @Test
    @DisplayName("Cosine of a unit vector with itself is 1")
    void cosineWithItselfIsOne() {
        float[] vector = VectorUtils.randomUnit(DIMENSION, new Random(1));

        assertEquals(1.0, VectorUtils.cosine(vector, vector), 1e-6);
    }

    @Test
    @DisplayName("Two independent 512-d vectors are nearly orthogonal")
    void independentVectorsAreNearlyOrthogonal() {
        Random random = new Random(7);
        float[] first = VectorUtils.randomUnit(DIMENSION, random);
        float[] second = VectorUtils.randomUnit(DIMENSION, random);

        // The expected spread is about 1/sqrt(512) ≈ 0.044, so a bound of 0.2
        // is generous and keeps the test from being flaky.
        double cosine = VectorUtils.cosine(first, second);
        assertTrue(Math.abs(cosine) < 0.2, "impostor similarity is too high: " + cosine);
    }

    @Test
    @DisplayName("genuine-noise=0.039 yields a similarity of about 0.75, as ArcFace does")
    void perturbProducesRealisticGenuineSimilarity() {
        Random random = new Random(42);
        float[] base = VectorUtils.randomUnit(DIMENSION, random);
        float[] probe = VectorUtils.perturb(base, 0.039, random);

        // Theoretical value: 1 / sqrt(1 + dim * sigma^2) = 1 / sqrt(1.7788) = 0.7498
        double cosine = VectorUtils.cosine(base, probe);
        assertTrue(cosine > 0.70 && cosine < 0.80,
                "similarity outside the expected range: " + cosine);
    }

    @Test
    @DisplayName("perturb returns a normalised vector")
    void perturbReturnsUnitVector() {
        Random random = new Random(3);
        float[] probe = VectorUtils.perturb(VectorUtils.randomUnit(DIMENSION, random), 0.05, random);

        double norm = 0.0;
        for (float value : probe) {
            norm += (double) value * value;
        }
        assertEquals(1.0, Math.sqrt(norm), 1e-5);
    }

    @Test
    @DisplayName("Mismatched lengths yield 0")
    void cosineOfMismatchedLengthsIsZero() {
        assertEquals(0.0, VectorUtils.cosine(new float[]{1f, 0f}, new float[]{1f, 0f, 0f}));
        assertEquals(0.0, VectorUtils.cosine(null, new float[]{1f}));
    }

    @Test
    @DisplayName("Zero vector yields 0 — no division by zero")
    void cosineOfZeroVectorIsZero() {
        assertEquals(0.0, VectorUtils.cosine(new float[]{0f, 0f}, new float[]{1f, 0f}));
    }

    @Test
    @DisplayName("pgvector literal uses a dot separator inside square brackets")
    void vectorLiteralFormat() {
        assertEquals("[0.5,-0.25]", VectorUtils.toVectorLiteral(new float[]{0.5f, -0.25f}));
    }

    @Test
    @DisplayName("text[] literal is quoted")
    void textArrayLiteralFormat() {
        assertEquals("{\"rgb\",\"depth\"}", VectorUtils.toTextArrayLiteral(List.of("rgb", "depth")));
        assertEquals("{}", VectorUtils.toTextArrayLiteral(List.of()));
        assertEquals("{}", VectorUtils.toTextArrayLiteral(null));
    }

    @Test
    @DisplayName("real[] literal")
    void realArrayLiteralFormat() {
        assertEquals("{0.1,0.9}", VectorUtils.toRealArrayLiteral(new float[]{0.1f, 0.9f}));
    }

    @Test
    @DisplayName("Literal round-trip preserves the values")
    void vectorLiteralRoundTrip() {
        float[] original = VectorUtils.randomUnit(16, new Random(11));

        float[] parsed = VectorUtils.parseVectorLiteral(VectorUtils.toVectorLiteral(original));

        assertEquals(original.length, parsed.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], parsed[i], 1e-6f);
        }
    }

    @Test
    @DisplayName("Empty literal parses to an empty array")
    void emptyLiteralParsesToEmptyArray() {
        assertEquals(0, VectorUtils.parseVectorLiteral("[]").length);
    }
}

package uz.tuit.faceid.recognition.util;

import java.util.List;
import java.util.Random;

/**
 * Vector helpers.
 * <p>
 * The PostgreSQL literal formats are built by hand on purpose: pgvector is not
 * a standard JDBC type, so the value is passed as text and cast inside the SQL
 * with {@code CAST(? AS vector)}. {@link Float#toString} always emits a dot as
 * the decimal separator, so the output does not depend on the default locale.
 */
public final class VectorUtils {

    private VectorUtils() {
        throw new AssertionError("Utility class — not instantiable");
    }

    /** Cosine similarity. Returns 0 for zero-length vectors. */
    public static double cosine(float[] first, float[] second) {
        if (first == null || second == null || first.length != second.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normFirst = 0.0;
        double normSecond = 0.0;
        for (int i = 0; i < first.length; i++) {
            dot += (double) first[i] * second[i];
            normFirst += (double) first[i] * first[i];
            normSecond += (double) second[i] * second[i];
        }
        if (normFirst == 0.0 || normSecond == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normFirst) * Math.sqrt(normSecond));
    }

    /** Random unit vector: each component drawn from N(0,1), then normalised. */
    public static float[] randomUnit(int dimension, Random random) {
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) random.nextGaussian();
        }
        normalizeInPlace(vector);
        return vector;
    }

    /**
     * Adds Gaussian noise and returns a freshly normalised vector.
     * <p>
     * For a unit vector the resulting cosine similarity is approximately
     * {@code 1 / sqrt(1 + dim * noiseStd^2)}. At 512 dimensions,
     * {@code noiseStd = 0.039} gives a similarity of about 0.75, which is the
     * typical value for a genuine ArcFace pair.
     */
    public static float[] perturb(float[] source, double noiseStd, Random random) {
        float[] result = new float[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = (float) (source[i] + random.nextGaussian() * noiseStd);
        }
        normalizeInPlace(result);
        return result;
    }

    public static void normalizeInPlace(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += (double) value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }

    /** pgvector literal: {@code [0.1,-0.2,0.3]} */
    public static String toVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder(vector.length * 12 + 2);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(vector[i]));
        }
        return builder.append(']').toString();
    }

    /** PostgreSQL {@code real[]} literal: {@code {0.1,0.2}} */
    public static String toRealArrayLiteral(float[] vector) {
        if (vector == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(vector.length * 12 + 2);
        builder.append('{');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(vector[i]));
        }
        return builder.append('}').toString();
    }

    /** PostgreSQL {@code text[]} literal: {@code {"rgb","depth"}} */
    public static String toTextArrayLiteral(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(values.get(i).replace("\"", "\\\"")).append('"');
        }
        return builder.append('}').toString();
    }

    /** Parses {@code [0.1,-0.2]} back into a float array. */
    public static float[] parseVectorLiteral(String literal) {
        String body = literal.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.isBlank()) {
            return new float[0];
        }
        String[] parts = body.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}

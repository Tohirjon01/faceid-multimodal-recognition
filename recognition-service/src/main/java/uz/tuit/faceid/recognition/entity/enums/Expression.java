package uz.tuit.faceid.recognition.entity.enums;

/**
 * Facial expression classes.
 * <p>
 * Order matters — it must match the output indices of the expression head
 * exactly (see {@code models.py: ExpressionHead.LABELS} on the Python side).
 */
public enum Expression {

    NEUTRAL("neutral"),
    HAPPY("happy"),
    SAD("sad"),
    ANGRY("angry"),
    FEAR("fear"),
    SURPRISE("surprise"),
    DISGUST("disgust");

    public static final int COUNT = values().length;

    private final String label;

    Expression(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Returns the class with the highest probability. */
    public static Expression argmax(float[] probabilities) {
        if (probabilities == null || probabilities.length == 0) {
            return null;
        }
        int best = 0;
        for (int i = 1; i < probabilities.length && i < COUNT; i++) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return values()[best];
    }

    /** Null-safe label accessor. */
    public static String labelOf(Expression expression) {
        return expression == null ? null : expression.getLabel();
    }
}

package org.rutz;

import org.apache.commons.jexl3.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates JEXL expressions within a given context.
 * This class provides methods to evaluate expressions and build JEXL contexts.
 */
public class ExpressionEvaluator {
    // Singleton instance of JexlEngine to reuse across evaluations
    private static JexlEngine JEXL_ENGINE;

    static {
        // Initialize the JexlEngine with optional custom functions
        Map<String, Object> functions = new HashMap<>();
        functions.put("fmfcn", new TransformerExpressionFunctions());

        JEXL_ENGINE = new JexlBuilder()
                .cache(512)                    // Caching expressions for performance
                .strict(true)                  // Enable strict mode to throw exceptions for undefined variables/functions
                .silent(false)                 // Disable silent mode to allow exceptions to propagate
                .namespaces(functions)         // Register custom functions namespaces
                .create();
    }

    // Private constructor to prevent instantiation
    private ExpressionEvaluator() {}

    /**
     * Evaluates a JEXL expression within the provided context.
     *
     * @param expression The JEXL expression to evaluate.
     * @param context    The JEXL context containing variables and functions.
     * @return The result of the expression evaluation.
     * @throws Exception If the expression is invalid or evaluation fails.
     */
    public static Object evaluate(String expression, JexlContext context) throws Exception {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be null or empty.");
        }

        // Create a JexlExpression object from the expression string
        JexlExpression jexlExpression = JEXL_ENGINE.createExpression(expression);

        // Evaluate the expression within the provided context
        return jexlExpression.evaluate(context);
    }

    public static <T> T attrEval(String expression, JexlContext context, Class<T> returnType) {
        JexlExpression e  = JEXL_ENGINE.createExpression(expression);
        T result = returnType.cast(e.evaluate(context));
        return result;
    }

    /**
     * Builds a JexlContext from a map of variables.
     *
     * @param variables A map containing variable names and their corresponding values.
     * @return A populated JexlContext.
     */
    public static JexlContext buildJexlContext(Map<String, Object> variables) {
        if (variables == null) {
            throw new IllegalArgumentException("Variables map cannot be null.");
        }

        JexlContext context = new MapContext();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            context.set(entry.getKey(), entry.getValue());
        }
        return context;
    }
}
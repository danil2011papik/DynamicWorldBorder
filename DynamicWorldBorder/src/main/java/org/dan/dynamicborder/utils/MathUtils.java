package org.dan.dynamicborder.utils;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MathUtils {

    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z_]+)\\}");

    static {
        // Настраиваем ScriptEngine для безопасности
        if (engine != null) {
            engine.put("Math", Math.class);
        }
    }

    /**
     * Вычисление математического выражения
     */
    public static double evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Заменяем математические функции
            String expr = expression
                    .replace("{sqrt}", "Math.sqrt")
                    .replace("{pow}", "Math.pow")
                    .replace("{log}", "Math.log")
                    .replace("{log10}", "Math.log10")
                    .replace("{sin}", "Math.sin")
                    .replace("{cos}", "Math.cos")
                    .replace("{tan}", "Math.tan")
                    .replace("{asin}", "Math.asin")
                    .replace("{acos}", "Math.acos")
                    .replace("{atan}", "Math.atan")
                    .replace("{abs}", "Math.abs")
                    .replace("{round}", "Math.round")
                    .replace("{floor}", "Math.floor")
                    .replace("{ceil}", "Math.ceil")
                    .replace("{random}", "Math.random")
                    .replace("{E}", String.valueOf(Math.E))
                    .replace("{PI}", String.valueOf(Math.PI))
                    .replace("^", "**") // Заменяем ^ на ** для JavaScript
                    .replace(",", "."); // Заменяем запятые на точки

            // Удаляем оставшиеся фигурные скобки
            expr = expr.replace("{", "").replace("}", "");

            // Проверяем на наличие опасных конструкций
            if (containsDangerousCode(expr)) {
                throw new SecurityException("Выражение содержит опасный код: " + expr);
            }

            Object result = engine.eval(expr);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            } else if (result instanceof Boolean) {
                return ((Boolean) result) ? 1.0 : 0.0;
            }
            return 0.0;

        } catch (ScriptException e) {
            // Если JavaScript недоступен, используем простой парсер
            return evaluateSimpleExpression(expression);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Проверка на опасный код
     */
    private static boolean containsDangerousCode(String expression) {
        String[] dangerousPatterns = {
                "System\\.", "Runtime\\.", "Process\\.", "exec\\s*\\(", "eval\\s*\\(",
                "getClass\\(\\)", "forName\\s*\\(", "invoke\\s*\\(", "new\\s+",
                "import\\s+", "package\\s+", "void\\s+", "class\\s+", "function\\s+"
        };

        for (String pattern : dangerousPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(expression).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Простой парсер математических выражений (резервный вариант)
     */
    private static double evaluateSimpleExpression(String expression) {
        try {
            // Удаляем пробелы
            String expr = expression.replaceAll("\\s+", "");

            // Обрабатываем скобки
            while (expr.contains("(") && expr.contains(")")) {
                int open = expr.lastIndexOf("(");
                int close = expr.indexOf(")", open);

                if (close == -1) break;

                String subExpr = expr.substring(open + 1, close);
                double subResult = evaluateSimpleExpression(subExpr);
                expr = expr.substring(0, open) + subResult + expr.substring(close + 1);
            }

            // Обрабатываем операции в порядке приоритета
            expr = processOperations(expr, new String[]{"^"});          // Степень
            expr = processOperations(expr, new String[]{"*", "/"});    // Умножение и деление
            expr = processOperations(expr, new String[]{"+", "-"});    // Сложение и вычитание

            return Double.parseDouble(expr);

        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String processOperations(String expression, String[] operators) {
        String expr = expression;

        for (String op : operators) {
            // Экранируем операторы для регулярных выражений
            String escapedOp;
            switch (op) {
                case "^":
                    escapedOp = "\\^"; // ^ - спецсимвол
                    break;
                case "*":
                    escapedOp = "\\*"; // * - спецсимвол
                    break;
                case "/":
                    escapedOp = "/";   // / - не спецсимвол
                    break;
                case "+":
                    escapedOp = "\\+"; // + - спецсимвол
                    break;
                case "-":
                    escapedOp = "-";   // - иногда спецсимвол, но в данном контексте нормально
                    break;
                default:
                    escapedOp = Pattern.quote(op); // для других операторов
            }

            Pattern pattern = Pattern.compile("(-?\\d+\\.?\\d*)(" + escapedOp + ")(-?\\d+\\.?\\d*)");
            Matcher matcher = pattern.matcher(expr);

            while (matcher.find()) {

                double left = Double.parseDouble(matcher.group(1));
                double right = Double.parseDouble(matcher.group(3));
                double result = 0.0;

                switch (op) {
                    case "^":
                        result = Math.pow(left, right);
                        break;
                    case "*":
                        result = left * right;
                        break;
                    case "/":
                        if (right == 0) result = 0;
                        else result = left / right;
                        break;
                    case "+":
                        result = left + right;
                        break;
                    case "-":
                        result = left - right;
                        break;
                }

                expr = expr.substring(0, matcher.start()) + result + expr.substring(matcher.end());
                matcher = pattern.matcher(expr);
            }
        }

        return expr;
    }

    /**
     * Замена переменных в выражении
     */
    public static String replaceVariables(String expression, java.util.Map<String, Object> variables) {
        if (expression == null || variables == null) {
            return expression;
        }

        String result = expression;
        Matcher matcher = VARIABLE_PATTERN.matcher(result);

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);

            if (value != null) {
                result = result.replace("{" + varName + "}", value.toString());
            }
        }

        return result;
    }

    /**
     * Ограничение значения в диапазоне
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Линейная интерполяция
     */
    public static double lerp(double a, double b, double t) {
        t = clamp(t, 0.0, 1.0);
        return a + (b - a) * t;
    }

    /**
     * Экспоненциальная интерполяция
     */
    public static double expLerp(double a, double b, double t, double exponent) {
        t = clamp(t, 0.0, 1.0);
        return a + (b - a) * Math.pow(t, exponent);
    }

    /**
     * Форматирование числа с указанием десятичных знаков
     */
    public static String formatDouble(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.0";
        }

        if (value == (int) value) {
            return String.format("%d", (int) value);
        }

        return String.format("%." + decimals + "f", value).replace(",", ".");
    }

    /**
     * Форматирование числа (автоматически определяет десятичные знаки)
     */
    public static String formatDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.0";
        }

        // Для целых чисел
        if (value == (int) value) {
            return String.format("%d", (int) value);
        }

        // Для очень маленьких чисел
        if (Math.abs(value) < 0.0001) {
            return String.format("%.6f", value).replace(",", ".");
        }

        // Для обычных чисел
        double absValue = Math.abs(value);
        if (absValue < 10) {
            return String.format("%.2f", value).replace(",", ".");
        } else if (absValue < 100) {
            return String.format("%.1f", value).replace(",", ".");
        } else {
            return String.format("%.0f", value).replace(",", ".");
        }
    }

    /**
     * Форматирование времени
     */
    public static String formatTime(double seconds) {
        if (seconds < 0) return "0с";

        long secs = (long) seconds;
        long minutes = secs / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dд %dч", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%dч %dм", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dм %dс", minutes, secs % 60);
        } else {
            return String.format("%dс", secs);
        }
    }

    /**
     * Форматирование времени (краткая версия)
     */
    public static String formatTimeShort(double seconds) {
        if (seconds < 60) {
            return String.format("%.0fс", seconds);
        } else if (seconds < 3600) {
            return String.format("%.1fм", seconds / 60);
        } else if (seconds < 86400) {
            return String.format("%.1fч", seconds / 3600);
        } else {
            return String.format("%.1fд", seconds / 86400);
        }
    }

    /**
     * Расчет процента
     */
    public static double calculatePercentage(double value, double total) {
        if (total == 0) return 0.0;
        return (value / total) * 100.0;
    }

    /**
     * Форматирование процента
     */
    public static String formatPercentage(double percentage) {
        return formatDouble(percentage, 1) + "%";
    }

    /**
     * Проверка, находится ли значение в диапазоне
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Нормализация значения в диапазоне 0-1
     */
    public static double normalize(double value, double min, double max) {
        if (max <= min) return 0.0;
        return clamp((value - min) / (max - min), 0.0, 1.0);
    }

    /**
     * Преобразование значения из нормализованного диапазона
     */
    public static double denormalize(double normalized, double min, double max) {
        normalized = clamp(normalized, 0.0, 1.0);
        return min + (max - min) * normalized;
    }

    /**
     * Округление до определенного шага
     */
    public static double roundToStep(double value, double step) {
        if (step <= 0) return value;
        return Math.round(value / step) * step;
    }

    /**
     * Проверка, является ли строка числом
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            Double.parseDouble(str.replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Безопасное преобразование строки в число
     */
    public static double parseDoubleSafe(String str, double defaultValue) {
        if (str == null || str.isEmpty()) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(str.replace(",", "."));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Безопасное преобразование строки в целое число
     */
    public static int parseIntSafe(String str, int defaultValue) {
        if (str == null || str.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            // Пробуем преобразовать double в int
            try {
                return (int) Double.parseDouble(str.replace(",", "."));
            } catch (NumberFormatException e2) {
                return defaultValue;
            }
        }
    }

    /**
     * Вычисление расстояния между двумя точками в 2D
     */
    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Вычисление расстояния между двумя точками в 3D
     */
    public static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Вычисление множителя на основе уровня
     */
    public static double calculateMultiplier(int level, double base, double step, String formula) {
        if (formula == null || formula.isEmpty()) {
            // Линейная формула по умолчанию
            return base + (step * level);
        }

        try {
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("level", level);
            vars.put("base", base);
            vars.put("step", step);

            String expr = replaceVariables(formula, vars);
            return evaluateExpression(expr);

        } catch (Exception e) {
            return base + (step * level);
        }
    }

    /**
     * Создание прогрессивной последовательности
     */
    public static java.util.List<Double> createProgressiveSequence(int count, double start, double multiplier) {
        java.util.List<Double> sequence = new java.util.ArrayList<>();
        double current = start;

        for (int i = 0; i < count; i++) {
            sequence.add(current);
            current *= multiplier;
        }

        return sequence;
    }

    /**
     * Вычисление суммы прогрессивной последовательности
     */
    public static double sumProgressiveSequence(int count, double start, double multiplier) {
        if (multiplier == 1.0) {
            return start * count;
        }

        return start * (1 - Math.pow(multiplier, count)) / (1 - multiplier);
    }
}
package org.dan.dynamicborder.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dan.dynamicborder.DynamicBorderPlugin;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>(.*?)</rainbow>");
    private static final Pattern PROGRESS_BAR_PATTERN = Pattern.compile("\\{progress:([0-9.]+):([0-9.]+):([0-9]+):([#&A-Fa-f0-9]{1,7}):([#&A-Fa-f0-9]{1,7})\\}");

    // Градиентные цвета для радуги
    private static final String[] RAINBOW_COLORS = {
            "&#FF0000", "&#FF7F00", "&#FFFF00", "&#00FF00",
            "&#0000FF", "&#4B0082", "&#9400D3"
    };

    /**
     * Форматирование сообщения с цветами
     */
    public static String format(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String formatted = message;

        // Обработка градиентов
        formatted = processGradients(formatted);

        // Обработка радуги
        formatted = processRainbow(formatted);

        // Обработка HEX цветов
        formatted = translateHexColors(formatted);

        // Обработка стандартных цветов
        formatted = ChatColor.translateAlternateColorCodes('&', formatted);

        // Обработка прогресс-баров
        formatted = processProgressBars(formatted);

        return formatted;
    }

    /**
     * Отправка форматированного сообщения игроку
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        sender.sendMessage(format(message));
    }

    /**
     * Отправка форматированного сообщения с префиксом
     */
    public static void sendMessage(CommandSender sender, String prefix, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        String fullMessage = prefix + message;
        sender.sendMessage(format(fullMessage));
    }

    /**
     * Отправка списка сообщений
     */
    public static void sendMessages(CommandSender sender, List<String> messages) {
        if (sender == null || messages == null || messages.isEmpty()) {
            return;
        }

        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    /**
     * Отправка сообщения с заменой переменных
     */
    public static void sendMessage(CommandSender sender, String message, Map<String, String> replacements) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        String formatted = replaceVariables(message, replacements);
        sendMessage(sender, formatted);
    }

    /**
     * Замена переменных в сообщении
     */
    public static String replaceVariables(String message, Map<String, String> replacements) {
        if (message == null || message.isEmpty() || replacements == null || replacements.isEmpty()) {
            return message;
        }

        String result = message;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String key = "%" + entry.getKey() + "%";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(key, value);
        }

        return result;
    }

    /**
     * Создание прогресс-бара
     */
    public static String createProgressBar(double current, double max, int length, String filledColor, String emptyColor) {
        if (max <= 0) max = 1;
        if (length <= 0) length = 10;

        double percentage = MathUtils.clamp(current / max, 0.0, 1.0);
        int filledLength = (int) Math.round(percentage * length);
        int emptyLength = length - filledLength;

        String filled = format(filledColor + "█").repeat(Math.max(0, filledLength));
        String empty = format(emptyColor + "█").repeat(Math.max(0, emptyLength));

        return filled + empty + " " + MathUtils.formatPercentage(percentage * 100);
    }

    /**
     * Создание таблицы
     */
    public static List<String> createTable(List<String> headers, List<List<String>> rows) {
        List<String> table = new ArrayList<>();

        if (headers == null || headers.isEmpty() || rows == null || rows.isEmpty()) {
            return table;
        }

        // Вычисляем ширину колонок
        int[] columnWidths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            columnWidths[i] = stripColor(headers.get(i)).length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < Math.min(row.size(), columnWidths.length); i++) {
                String cell = row.get(i);
                if (cell != null) {
                    columnWidths[i] = Math.max(columnWidths[i], stripColor(cell).length());
                }
            }
        }

        // Добавляем отступы
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] += 2; // По одному пробелу с каждой стороны
        }

        // Создаем разделительную линию
        StringBuilder separator = new StringBuilder("§7+");
        for (int width : columnWidths) {
            separator.append("-".repeat(width)).append("+");
        }

        // Создаем заголовок
        StringBuilder headerLine = new StringBuilder("§7|");
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            int width = columnWidths[i];
            int padding = width - stripColor(header).length();
            int leftPadding = padding / 2;
            int rightPadding = padding - leftPadding;

            headerLine.append(" ".repeat(leftPadding))
                    .append("§e").append(header)
                    .append(" ".repeat(rightPadding))
                    .append("§7|");
        }

        // Добавляем заголовок в таблицу
        table.add(separator.toString());
        table.add(headerLine.toString());
        table.add(separator.toString());

        // Добавляем строки
        for (List<String> row : rows) {
            StringBuilder rowLine = new StringBuilder("§7|");
            for (int i = 0; i < headers.size(); i++) {
                String cell = i < row.size() ? row.get(i) : "";
                if (cell == null) cell = "";

                int width = columnWidths[i];
                int padding = width - stripColor(cell).length();
                int leftPadding = padding / 2;
                int rightPadding = padding - leftPadding;

                rowLine.append(" ".repeat(leftPadding))
                        .append("§f").append(cell)
                        .append(" ".repeat(rightPadding))
                        .append("§7|");
            }
            table.add(rowLine.toString());
        }

        table.add(separator.toString());
        return table;
    }

    /**
     * Создание нумерованного списка
     */
    public static List<String> createNumberedList(List<String> items, String numberColor, String itemColor) {
        List<String> list = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            return list;
        }

        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if (item != null) {
                list.add(format(numberColor + (i + 1) + ". " + itemColor + item));
            }
        }

        return list;
    }

    /**
     * Создание информационного блока
     */
    public static List<String> createInfoBlock(String title, List<String> content) {
        List<String> block = new ArrayList<>();

        block.add("§6══════════════════════════════════════");
        block.add("§e" + title);
        block.add("§6══════════════════════════════════════");

        if (content != null) {
            block.addAll(content);
            block.add("§6══════════════════════════════════════");
        }

        return block;
    }

    /**
     * Создание сообщения об ошибке
     */
    public static String createErrorMessage(String message) {
        return format("&c✗ &7" + message);
    }

    /**
     * Создание сообщения об успехе
     */
    public static String createSuccessMessage(String message) {
        return format("&a✓ &7" + message);
    }

    /**
     * Создание сообщения с предупреждением
     */
    public static String createWarningMessage(String message) {
        return format("&e⚠ &7" + message);
    }

    /**
     * Создание сообщения с информацией
     */
    public static String createInfoMessage(String message) {
        return format("&bℹ &7" + message);
    }

    /**
     * Перевод HEX цветов в коды Minecraft
     */
    private static String translateHexColors(String message) {
        if (message == null || !message.contains("&#")) {
            return message;
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, parseHexColor(hex));
        }

        return matcher.appendTail(buffer).toString();
    }

    /**
     * Парсинг HEX цвета в код Minecraft
     */
    private static String parseHexColor(String hex) {
        // Для версий Minecraft 1.16+ с поддержкой HEX цветов
        try {
            // Используем рефлексию для совместимости
            java.lang.reflect.Method ofMethod = ChatColor.class.getMethod("of", String.class);
            String cleanHex = hex.startsWith("#") ? hex : "#" + hex;
            ChatColor color = (ChatColor) ofMethod.invoke(null, cleanHex);
            return color.toString();
        } catch (Exception e) {
            // Для старых версий или если что-то пошло не так
            return ChatColor.WHITE.toString();
        }
    }

    /**
     * Обработка градиентов
     */
    private static String processGradients(String message) {
        if (message == null || !message.contains("<gradient:")) {
            return message;
        }

        // Паттерн для поиска <gradient:#HEX1:#HEX2>текст</gradient>
        java.util.regex.Pattern GRADIENT_PATTERN = java.util.regex.Pattern.compile(
                "<gradient:#([0-9A-Fa-f]{6}):#([0-9A-Fa-f]{6})>(.*?)</gradient>"
        );

        java.util.regex.Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startHex = "#" + matcher.group(1);
            String endHex = "#" + matcher.group(2);
            String text = matcher.group(3);

            String gradientText = createGradient(text, startHex, endHex);
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(gradientText));
        }

        return matcher.appendTail(buffer).toString();
    }

    /**
     * Создание градиентного текста
     */
    private static String createGradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            java.awt.Color startColor = java.awt.Color.decode(startHex);
            java.awt.Color endColor = java.awt.Color.decode(endHex);

            StringBuilder gradient = new StringBuilder();
            int length = text.length();

            for (int i = 0; i < length; i++) {
                float ratio = (float) i / (float) Math.max(1, length - 1);
                int red = (int) (startColor.getRed() + ratio * (endColor.getRed() - startColor.getRed()));
                int green = (int) (startColor.getGreen() + ratio * (endColor.getGreen() - startColor.getGreen()));
                int blue = (int) (startColor.getBlue() + ratio * (endColor.getBlue() - startColor.getBlue()));

                String hex = String.format("#%02X%02X%02X", red, green, blue);
                // Используем наш исправленный метод parseHexColor
                gradient.append(parseHexColor(hex)).append(text.charAt(i));
            }

            return gradient.toString();

        } catch (Exception e) {
            return text;
        }
    }

    /**
     * Обработка радужного текста
     */
    private static String processRainbow(String message) {
        if (message == null || !message.contains("<rainbow>")) {
            return message;
        }

        Matcher matcher = RAINBOW_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String text = matcher.group(1);
            String rainbowText = createRainbow(text);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(rainbowText));
        }

        return matcher.appendTail(buffer).toString();
    }

    /**
     * Создание радужного текста
     */
    private static String createRainbow(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder rainbow = new StringBuilder();
        int length = text.length();
        int colorCount = RAINBOW_COLORS.length;

        for (int i = 0; i < length; i++) {
            int colorIndex = i % colorCount;
            rainbow.append(RAINBOW_COLORS[colorIndex]).append(text.charAt(i));
        }

        return rainbow.toString();
    }

    /**
     * Обработка прогресс-баров
     */
    private static String processProgressBars(String message) {
        if (message == null || !message.contains("{progress:")) {
            return message;
        }

        Matcher matcher = PROGRESS_BAR_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            double current = MathUtils.parseDoubleSafe(matcher.group(1), 0);
            double max = MathUtils.parseDoubleSafe(matcher.group(2), 1);
            int length = MathUtils.parseIntSafe(matcher.group(3), 10);
            String filledColor = matcher.group(4);
            String emptyColor = matcher.group(5);

            String progressBar = createProgressBar(current, max, length, filledColor, emptyColor);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(progressBar));
        }

        return matcher.appendTail(buffer).toString();
    }

    /**
     * Удаление цветовых кодов из строки
     */
    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Удаляем стандартные коды цвета
        String stripped = ChatColor.stripColor(text);

        // Удаляем HEX коды
        stripped = stripped.replaceAll("&#[A-Fa-f0-9]{6}", "");

        // Удаляем теги градиентов и радуги
        stripped = stripped.replaceAll("<gradient:[^>]+>", "")
                .replaceAll("</gradient>", "")
                .replaceAll("<rainbow>", "")
                .replaceAll("</rainbow>", "");

        return stripped;
    }

    /**
     * Получение длины строки без цветовых кодов
     */
    public static int getVisibleLength(String text) {
        return stripColor(text).length();
    }

    /**
     * Центрирование текста
     */
    public static String centerText(String text, int width) {
        if (text == null || width <= 0) {
            return text;
        }

        String stripped = stripColor(text);
        int textLength = stripped.length();

        if (textLength >= width) {
            return text;
        }

        int padding = (width - textLength) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    /**
     * Создание заголовка
     */
    public static String createTitle(String title, char lineChar, String color) {
        int length = 40; // Стандартная длина
        String line = String.valueOf(lineChar).repeat(length);
        String centered = centerText(" " + title + " ", length);

        return format(color + line + "\n" + centered + "\n" + color + line);
    }

    /**
     * Форматирование валюты
     */
    public static String formatCurrency(double amount) {
        if (amount == (int) amount) {
            return String.format("%,d", (int) amount);
        } else {
            return String.format("%,.2f", amount);
        }
    }

    /**
     * Создание сообщения о кошельке
     */
    public static String createWalletMessage(double balance, String currencyName) {
        return format("&7Ваш баланс: &e" + formatCurrency(balance) + " " + currencyName);
    }

    /**
     * Логирование сообщения
     */
    public static void log(DynamicBorderPlugin plugin, String message) {
        if (plugin != null && message != null) {
            plugin.getLogger().info(format("&7[&bDWB&7] &f" + message));
        }
    }

    /**
     * Логирование ошибки
     */
    public static void logError(DynamicBorderPlugin plugin, String message) {
        if (plugin != null && message != null) {
            plugin.getLogger().severe(format("&7[&bDWB&7] &c" + message));
        }
    }

    /**
     * Логирование предупреждения
     */
    public static void logWarning(DynamicBorderPlugin plugin, String message) {
        if (plugin != null && message != null) {
            plugin.getLogger().warning(format("&7[&bDWB&7] &e" + message));
        }
    }

    /**
     * Отправка широковещательного сообщения
     */
    public static void broadcast(DynamicBorderPlugin plugin, String message) {
        if (plugin != null && message != null) {
            String formatted = format(message);
            plugin.getServer().broadcastMessage(formatted);
            log(plugin, "Broadcast: " + stripColor(formatted));
        }
    }

    /**
     * Отправка широковещательного сообщения с разрешением
     */
    public static void broadcast(DynamicBorderPlugin plugin, String message, String permission) {
        if (plugin != null && message != null) {
            String formatted = format(message);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission(permission)) {
                    player.sendMessage(formatted);
                }
            }

            log(plugin, "Broadcast (permission: " + permission + "): " + stripColor(formatted));
        }
    }
}
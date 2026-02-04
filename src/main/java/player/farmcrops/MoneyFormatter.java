package player.farmcrops;

/**
 * Utility class for formatting money values
 * Converts large numbers to readable format (e.g. 6000.00 -> 6.0k)
 */
public class MoneyFormatter {
    
    /**
     * Format money with K/M/B suffixes
     * Examples:
     * - 500.00 -> $500
     * - 1234.56 -> $1.2k
     * - 45678.90 -> $45.7k
     * - 1234567.00 -> $1.2M
     * - 1234567890.00 -> $1.2B
     */
    public static String format(double amount) {
        if (amount < 1000) {
            // Less than 1k: show as-is (no decimals if whole number)
            if (amount == (long) amount) {
                return "$" + (long) amount;
            } else {
                return "$" + String.format("%.2f", amount);
            }
        } else if (amount < 1000000) {
            // 1k - 999k: show as X.Xk
            return "$" + String.format("%.1f", amount / 1000) + "k";
        } else if (amount < 1000000000) {
            // 1M - 999M: show as X.XM
            return "$" + String.format("%.1f", amount / 1000000) + "M";
        } else {
            // 1B+: show as X.XB
            return "$" + String.format("%.1f", amount / 1000000000) + "B";
        }
    }
    
    /**
     * Format money for compact display (shorter)
     * Examples:
     * - 500.00 -> $500
     * - 1234.56 -> $1.2k
     * - 1234567.00 -> $1.2M
     */
    public static String formatCompact(double amount) {
        return format(amount).replace(".0k", "k")
                             .replace(".0M", "M")
                             .replace(".0B", "B");
    }
    
    /**
     * Format money for detailed display (with cents)
     * Examples:
     * - 500.00 -> $500.00
     * - 1234.56 -> $1,234.56
     * - 1234567.89 -> $1,234,567.89
     */
    public static String formatDetailed(double amount) {
        return "$" + String.format("%,.2f", amount);
    }
}

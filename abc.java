java
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class abc {

    private static class Entry {
        final BigInteger x;
        final int base;
        final String rawValue;
        final BigInteger y;
        Entry(BigInteger x, int base, String rawValue, BigInteger y) {
            this.x = x;
            this.base = base;
            this.rawValue = rawValue;
            this.y = y;
        }
    }

    private record Quadratic(BigInteger a, BigInteger b, BigInteger c) {
        BigInteger eval(BigInteger x) {
            // a*x^2 + b*x + c
            return a.multiply(x).multiply(x).add(b.multiply(x)).add(c);
        }
        @Override public String toString() {
            return "f(x)=" + a + "x^2+" + b + "x+" + c;
        }
    }

    public static void main(String[] args) {
        try {
            String json = Files.readString(Path.of("src", "read.JSON"));

            int n = extractInKeys(json, "n");
            int k = extractInKeys(json, "k");

            Map<BigInteger, Entry> entries = parseEntries(json);

            System.out.println("n=" + n + " k=" + k);
            entries.values().forEach(e ->
                    System.out.printf("x=%s base=%d raw=%s decodedY=%s%n",
                            e.x, e.base, e.rawValue, e.y));

            if (args.length > 0) {
                Quadratic quad = parseQuadratic(args[0]);
                System.out.println("Parsed " + quad);
                // Evaluate quadratic at each x
                for (Entry e : entries.values()) {
                    BigInteger fx = quad.eval(e.x);
                    System.out.printf("f(%s)=%s (original y=%s)%n", e.x, fx, e.y);
                }
                // Optional second arg: a single X to evaluate
                if (args.length > 1) {
                    BigInteger targetX = new BigInteger(args[1]);
                    System.out.println("f(" + targetX + ")=" + quad.eval(targetX));
                }
            } else {
                System.out.println("Supply argument like: f(x)=3x^2-4x+7 [optionalX]");
            }

        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    private static int extractInKeys(String json, String field) {
        Pattern keysPattern = Pattern.compile("\"keys\"\\s*:\\s*\\{([^}]*)}");
        Matcher km = keysPattern.matcher(json);
        if (!km.find()) throw new IllegalStateException("Missing keys object");
        String keysBlock = km.group(1);
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(keysBlock);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new IllegalStateException("Field " + field + " not found");
    }

    private static Map<BigInteger, Entry> parseEntries(String json) {
        Pattern entryPattern = Pattern.compile(
                "\"(\\d+)\"\\s*:\\s*\\{\\s*\"base\"\\s*:\\s*\"(\\d{1,2})\"\\s*,\\s*\"value\"\\s*:\\s*\"([0-9A-Za-z]+)\"\\s*}\\s*,?");
        Matcher em = entryPattern.matcher(json);
        Map<BigInteger, Entry> map = new LinkedHashMap<>();
        while (em.find()) {
            String key = em.group(1);
            if ("keys".equals(key)) continue;
            BigInteger x = new BigInteger(key);
            int base = Integer.parseInt(em.group(2));
            if (base < 2 || base > 36) throw new IllegalArgumentException("Bad base " + base);
            String val = em.group(3);
            BigInteger y = new BigInteger(val, base);
            map.put(x, new Entry(x, base, val, y));
        }
        return map;
    }

    private static Quadratic parseQuadratic(String s) {
        // Accept forms like: f(x)=3x^2-4x+7, optional spaces, coefficients may be +/- integers.
        String norm = s.replaceAll("\\s+", "");
        Pattern p = Pattern.compile("^f\\(x\\)=([+-]?\\d+)x\\^2([+-]\\d+)x([+-]\\d+)$");
        Matcher m = p.matcher(norm);
        if (!m.matches())
            throw new IllegalArgumentException("Bad quadratic format: " + s);
        BigInteger a = new BigInteger(m.group(1));
        BigInteger b = new BigInteger(m.group(2).replace("+", ""));
        BigInteger c = new BigInteger(m.group(3).replace("+", ""));
        return new Quadratic(a, b, c);
    }
}
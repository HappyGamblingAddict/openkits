package eu.fakemoon.altarkits;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MCLicense {

    /** Base URL of your MCLicense server (no trailing slash). */
    private static final String BASE_URL = "http://localhost:8091";

    /** This deployment's Ed25519 public key (base64, X.509/SPKI). */
    private static final String PUBLIC_KEY_B64 = "MCowBQYDK2VwAyEAzUXtrXfjDOwGBigN+Bdwe52xGyULKt1ZpesEpWdC/EE=";

    /** Reject signed responses older/newer than this many milliseconds. */
    private static final long MAX_CLOCK_SKEW_MS = 5 * 60 * 1000;

    private static final Logger FALLBACK_LOG = Logger.getLogger("MCLicense");

    private MCLicense() {}

    /** Detailed validation outcome. */
    public static final class Result {
        public final boolean valid;
        public final String code;
        public final String message;
        /** True when the server could not be reached or answered garbage —
         *  lets you decide whether to fail open or closed on outages. */
        public final boolean networkError;

        Result(boolean valid, String code, String message, boolean networkError) {
            this.valid = valid;
            this.code = code;
            this.message = message;
            this.networkError = networkError;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Bukkit-friendly entry point (uses reflection: no Bukkit imports,   */
    /*  so this file compiles anywhere).                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Reads the license key from {@code <dataFolder>/license.key} (creating
     * the file with instructions if missing), validates it, and logs the
     * outcome to the plugin's logger. Returns {@code true} when licensed.
     */
    public static boolean init(Object plugin, String pluginId) {
        Logger log = FALLBACK_LOG;
        try {
            log = (Logger) plugin.getClass().getMethod("getLogger").invoke(plugin);
            File dataFolder = (File) plugin.getClass().getMethod("getDataFolder").invoke(plugin);
            return init(dataFolder.toPath(), pluginId, log, detectPort(plugin));
        } catch (ReflectiveOperationException e) {
            log.severe("[MCLicense] init(plugin, id) expects a Bukkit JavaPlugin; use init(Path, id, Logger) instead.");
            return false;
        }
    }

    /** Framework-agnostic variant. */
    public static boolean init(Path dataFolder, String pluginId, Logger log, int port) {
        String key = readOrCreateKeyFile(dataFolder, log);
        if (key == null) return false;

        log.info("[MCLicense] Validating license " + mask(key) + "…");
        Result r = check(pluginId, key, port);
        if (r.valid) {
            log.info("[MCLicense] ✔ License valid — signature verified (Ed25519).");
        } else if (r.networkError) {
            log.severe("[MCLicense] ✘ Could not reach the license server: " + r.message);
        } else {
            log.severe("[MCLicense] ✘ License rejected (" + r.code + "): " + r.message);
        }
        return r.valid;
    }

    /* ------------------------------------------------------------------ */
    /*  Core validation                                                    */
    /* ------------------------------------------------------------------ */

    /** Simple boolean validation (network errors count as invalid). */
    public static boolean validate(String pluginId, String licenseKey) {
        return check(pluginId, licenseKey).valid;
    }

    public static Result check(String pluginId, String licenseKey) {
        return check(pluginId, licenseKey, 0);
    }

    public static Result check(String pluginId, String licenseKey, int port) {
        String nonce = randomNonce();
        String url = BASE_URL + "/api/v1/validate/" + pluginId + "/" + licenseKey
                + "?nonce=" + nonce + "&port=" + port;
        String body;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "MCLicense-Java/1.0")
                    .GET().build();
            body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return new Result(false, "NETWORK_ERROR", e.getMessage(), true);
        }

        try {
            boolean valid = "true".equals(jsonField(body, "valid"));
            String code = jsonString(body, "code");
            String message = jsonString(body, "message");
            String echoedNonce = jsonString(body, "nonce");
            String signature = jsonString(body, "signature");
            long timestamp = Long.parseLong(jsonField(body, "timestamp"));

            if (!nonce.equals(echoedNonce)) {
                return new Result(false, "NONCE_MISMATCH", "Response nonce does not match (replay?).", false);
            }
            if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_CLOCK_SKEW_MS) {
                return new Result(false, "STALE_RESPONSE", "Response timestamp outside the allowed window.", false);
            }
            String payload = pluginId + "|" + licenseKey + "|" + nonce + "|" + valid + "|" + timestamp;
            if (!verifySignature(payload, signature)) {
                return new Result(false, "BAD_SIGNATURE", "Response signature is invalid — server not trusted.", false);
            }
            return new Result(valid, code, message, false);
        } catch (Exception e) {
            // The server WAS reachable but answered garbage — deliberately not a
            // network error, so fail-open logic can't be tricked by a fake server.
            return new Result(false, "MALFORMED_RESPONSE", "Could not parse server response.", false);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Internals                                                          */
    /* ------------------------------------------------------------------ */

    private static boolean verifySignature(String payload, String signatureB64) {
        try {
            byte[] der = Base64.getDecoder().decode(PUBLIC_KEY_B64);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(der));
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signatureB64));
        } catch (Exception e) {
            // Any crypto failure (bad base64, wrong length, wrong key) = untrusted.
            return false;
        }
    }

    private static String randomNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String readOrCreateKeyFile(Path dataFolder, Logger log) {
        Path file = dataFolder.resolve("license.key");
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("#")) return t;
                }
            } else {
                Files.createDirectories(dataFolder);
                Files.writeString(file,
                        "# Paste your MCLicense key below (a line like MCL-XXXX-XXXX-XXXX-XXXX)\n" +
                        "# then restart the server.\n",
                        StandardCharsets.UTF_8);
            }
            log.severe("[MCLicense] No license key found. Paste your key into " + file + " and restart.");
        } catch (Exception e) {
            log.severe("[MCLicense] Could not read/create " + file + ": " + e.getMessage());
        }
        return null;
    }

    private static int detectPort(Object plugin) {
        try {
            Object server = plugin.getClass().getMethod("getServer").invoke(plugin);
            return (int) server.getClass().getMethod("getPort").invoke(server);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String mask(String key) {
        return key.length() < 8 ? "****" : "****" + key.substring(key.length() - 5);
    }

    /* Minimal JSON field extraction — the response is a flat, server-controlled
       object, and the signature check binds the fields that matter. */
    private static String jsonString(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String jsonField(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*([^,}\\s\"]+)").matcher(json);
        return m.find() ? m.group(1) : "";
    }
}

package fr.bloup.simulatedLag.version;

public class VersionHandlerFactory {
    public static VersionHandler getVersionHandler(String version) {
        return switch (version) {
            case "1.21" -> new VersionHandler_1_21();
            default -> throw new IllegalArgumentException("Unsupported version: " + version);
        };
    }
}

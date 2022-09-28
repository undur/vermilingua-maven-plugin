package ng.packaging;

import java.io.File;

/**
 * A project dependency along with some metadata.
 */

public record Dependency( String groupId, String artifactId, String version, File file ) {}
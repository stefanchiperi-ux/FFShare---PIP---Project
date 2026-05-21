package core;

import java.nio.file.Path;

public class UserFile {
	private final Path path;
	
	public UserFile(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path getPath() {
        return path;
    }

    public String getPathAsString() {
        return path.toString();
    }

    public String getName() {
        return path.getFileName().toString();
    }
}

package core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
	private final List<UserFile> files = new ArrayList<>();

    public void addFile(Path path) {
        files.add(new UserFile(path));
    }

    public List<UserFile> getFiles() {
        return Collections.unmodifiableList(files);
    }
}

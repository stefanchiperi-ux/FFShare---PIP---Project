package core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
	private final List<UserFile> files = new ArrayList<>();
	private String fullName;
	
	public User(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void addFile(Path path) {
        files.add(new UserFile(path));
    }

    public List<UserFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public List<String> getFilePaths() {
        return files.stream()
                .map(UserFile::getPathAsString)
                .toList();
    }
}

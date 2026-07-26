package engine.vfs;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public record FileEvent(Path path, WatchEvent.Kind<?> kind) {
}

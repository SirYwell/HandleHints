import java.lang.foreign.MemoryLayout;
import java.lang.invoke.MethodHandle;

class MemoryLayoutScaleHandle {
    void m(MemoryLayout memoryLayout) {
        // scaleHandle type is always the same, therefore known even for unknown layouts
        MethodHandle handle = memoryLayout.scaleHandle();
    }
}
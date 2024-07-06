import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;

class MemoryLayoutSequenceLayout {
    void m() {
        // 0 is allowed
        <info descr="[0:int4]">SequenceLayout sl0 = <info descr="[0:int4]">MemoryLayout.sequenceLayout(0, ValueLayout.JAVA_INT)</info>;</info>
        // larger counts are allowed
        <info descr="[10:int4]">SequenceLayout sl1 = <info descr="[10:int4]">MemoryLayout.sequenceLayout(10, ValueLayout.JAVA_INT)</info>;</info>
        // negative counts are not allowed
        <info descr="⊤">SequenceLayout sl2 = <info descr="⊤">MemoryLayout.sequenceLayout(-10, ValueLayout.JAVA_INT)</info>;</info>
        // overflows are not allowed
        <info descr="⊤">SequenceLayout sl3 = <info descr="⊤">MemoryLayout.sequenceLayout(Long.MAX_VALUE >> 1, ValueLayout.JAVA_LONG)</info>;</info>
        // alignment mismatches are not allowed
        <info descr="⊤">SequenceLayout sl4 = <info descr="⊤">MemoryLayout.sequenceLayout(1, <info descr="8%[long8int4]">MemoryLayout.structLayout(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)</info>)</info>;</info>
        // but matching alignment is fine
        <info descr="[1:8%[long8x4int4]]">SequenceLayout sl5 = <info descr="[1:8%[long8x4int4]]">MemoryLayout.sequenceLayout(1, <info descr="8%[long8x4int4]">MemoryLayout.structLayout(ValueLayout.JAVA_LONG, <info descr="x4">MemoryLayout.paddingLayout(4)</info>, ValueLayout.JAVA_INT)</info>)</info>;</info>
    }
}
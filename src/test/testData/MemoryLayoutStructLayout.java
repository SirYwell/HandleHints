import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

class MemoryLayoutStructLayout {
    void m() {
        <info descr="int4">ValueLayout vl0 = ValueLayout.JAVA_INT;</info>
        <info descr="1%int4">ValueLayout vl1 = ValueLayout.JAVA_INT_UNALIGNED;</info>
        <info descr="1%[]">MemoryLayout ml0 = <info descr="1%[]">MemoryLayout.structLayout()</info>;</info>
        <info descr="4%[int4int4]">MemoryLayout ml1 = <info descr="4%[int4int4]">MemoryLayout.structLayout(vl0, vl0)</info>;</info>
        <info descr="4%[4%[int4int4]1%int4]">MemoryLayout ml2 = <info descr="4%[4%[int4int4]1%int4]">MemoryLayout.structLayout(ml1, vl1)</info>;</info>
    }
}
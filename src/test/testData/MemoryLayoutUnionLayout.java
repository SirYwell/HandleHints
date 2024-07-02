import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;

class MemoryLayoutUnionLayout {
    void m() {
        <info descr="[int4|long8]">UnionLayout ul0 = <info descr="[int4|long8]">MemoryLayout.unionLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)</info>;</info>
        <info descr="8%[[int4|long8][int4|long8]]">StructLayout sl0 = <info descr="8%[[int4|long8][int4|long8]]">MemoryLayout.structLayout(ul0, ul0)</info>;</info>
        <info descr="[int4|int4|int4|int4|int4]">UnionLayout ul1 = <info descr="[int4|int4|int4|int4|int4]">MemoryLayout.unionLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)</info>;</info>
    }
}
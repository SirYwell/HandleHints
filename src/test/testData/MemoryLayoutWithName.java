import java.lang.foreign.MemoryLayout;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;

class MemoryLayoutWithName {
    void m() {
        // names for different layout types
        <info descr="1%int4(jiu)">ValueLayout vl0 = <info descr="1%int4(jiu)">ValueLayout.JAVA_INT_UNALIGNED.withName("jiu")</info>;</info>
        <info descr="boolean1(jb)">ValueLayout vl1 = <info descr="boolean1(jb)">ValueLayout.JAVA_BOOLEAN.withName("jb")</info>;</info>
        <info descr="1%[1%int4(jiu)1%int4(jiu)boolean1(jb)](struct)">StructLayout sl0 = <info descr="1%[1%int4(jiu)1%int4(jiu)boolean1(jb)](struct)"><info descr="1%[1%int4(jiu)1%int4(jiu)boolean1(jb)]">MemoryLayout.structLayout(vl0, vl0, vl1)</info>.withName("struct")</info>;</info>
        <info descr="1%[1%int4(jiu)|boolean1(jb)](union)">UnionLayout ul0 = <info descr="1%[1%int4(jiu)|boolean1(jb)](union)"><info descr="1%[1%int4(jiu)|boolean1(jb)]">MemoryLayout.unionLayout(vl0, vl1)</info>.withName("union")</info>;</info>
        <info descr="[10:1%[1%int4(jiu)|boolean1(jb)](union)](seq)">SequenceLayout sl1 = <info descr="[10:1%[1%int4(jiu)|boolean1(jb)](union)](seq)"><info descr="[10:1%[1%int4(jiu)|boolean1(jb)](union)]">MemoryLayout.sequenceLayout(10, ul0)</info>.withName("seq")</info>;</info>
        <info descr="x123(pad)">PaddingLayout pl0 = <info descr="x123(pad)"><info descr="x123">MemoryLayout.paddingLayout(123)</info>.withName("pad")</info>;</info>

        // stripping the name again...
        <info descr="1%[1%int4(jiu)|boolean1(jb)]">UnionLayout ul1 = <info descr="1%[1%int4(jiu)|boolean1(jb)]">ul0.withoutName()</info>;</info>

        // unknown names
        <info descr="long8({⊤})">ValueLayout vl2 = <info descr="long8({⊤})">ValueLayout.JAVA_LONG.withName(<error descr="Cannot resolve symbol 'unknown'">unknown</error>)</info>;</info>
        <info descr="long8">ValueLayout vl3 = <info descr="long8">vl2.withoutName()</info>;</info>
    }
}
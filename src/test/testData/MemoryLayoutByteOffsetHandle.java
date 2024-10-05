import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.MemoryLayout.PathElement.*;

class MemoryLayoutByteOffsetHandle {
    void byteOffsetHandlePaths() {
        // simple byteOffset
        <info descr="4%[int4int4]">MemoryLayout ml00 = <info descr="4%[int4int4]">MemoryLayout.structLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)</info>;</info>
        <info descr="(long)long">MethodHandle mh00 = <info descr="(long)long">ml00.byteOffsetHandle(<info descr="groupElement(1)">MemoryLayout.PathElement.groupElement(1)</info>)</info>;</info>
        <info descr="[10:4%[int4int4]]">SequenceLayout ml10 = <info descr="[10:4%[int4int4]]">MemoryLayout.sequenceLayout(10, ml00)</info>;</info>
        // byteOffset contributing a coordinate
        <info descr="(long,long)long">MethodHandle mh10 = <info descr="(long,long)long">ml10.byteOffsetHandle(<info descr="sequenceElement()">sequenceElement()</info>)</info>;</info>
        // byteOffset not contributing a coordinate
        <info descr="(long)long">MethodHandle mh11 = <info descr="(long)long">ml10.byteOffsetHandle(<info descr="sequenceElement(5)">sequenceElement(5)</info>)</info>;</info>

        // dereference elements are not supported
        <info descr="a?:4%[int4int4]">MemoryLayout ml20 = <info descr="a?:4%[int4int4]">ValueLayout.ADDRESS.withTargetLayout(ml00)</info>;</info>
        <info descr="(0=long)⊤">MethodHandle mh20 = <info descr="(0=long)⊤">ml20.byteOffsetHandle(<warning descr="Dereference path element is not allowed here."><info descr="dereferenceElement()">dereferenceElement()</info></warning>)</info>;</info>
    }
}
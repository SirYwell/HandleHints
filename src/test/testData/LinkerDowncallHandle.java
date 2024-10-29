import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.Linker;
import java.lang.invoke.MethodHandle;

class LinkerDowncallHandle {

    void linker(Linker linker, MemorySegment address) {
        <info descr="(4%[int41%long8]boolean1)int4">FunctionDescriptor fd00 = <info descr="(4%[int41%long8]boolean1)int4">FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                <info descr="4%[int41%long8]">MemoryLayout.structLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG_UNALIGNED)</info>,
                ValueLayout.JAVA_BOOLEAN
        )</info>;</info>
        <info descr="()1%[]">FunctionDescriptor fd10 = <info descr="()1%[]">FunctionDescriptor.of(<info descr="1%[]">MemoryLayout.structLayout()</info>)</info>;</info>

        <info descr="(MemorySegment,MemorySegment,boolean)int">MethodHandle mh00 = <info descr="(MemorySegment,MemorySegment,boolean)int">linker.downcallHandle(fd00)</info>;</info>
        <info descr="(MemorySegment,SegmentAllocator)MemorySegment">MethodHandle mh01 = <info descr="(MemorySegment,SegmentAllocator)MemorySegment">linker.downcallHandle(fd10)</info>;</info>

        <info descr="(MemorySegment,boolean)int">MethodHandle mh10 = <info descr="(MemorySegment,boolean)int">linker.downcallHandle(address, fd00)</info>;</info>
        <info descr="(SegmentAllocator)MemorySegment">MethodHandle mh11 = <info descr="(SegmentAllocator)MemorySegment">linker.downcallHandle(address, fd10)</info>;</info>
    }
}
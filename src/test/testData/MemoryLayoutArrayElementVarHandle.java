import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

class MemoryLayoutArrayElementVarHandle {
    void m(MemoryLayout unknown) {
        StructLayout struct = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("i"), ValueLayout.JAVA_FLOAT.withName("f"));
        SequenceLayout sequence = MemoryLayout.sequenceLayout(123, struct);
        // valid paths
        VarHandle structVH = struct.arrayElementVarHandle(groupElement("f"));
        VarHandle sequenceVH = sequence.arrayElementVarHandle(sequenceElement(), groupElement("i"));

        // we still now the leading coordinates
        VarHandle unknownVH = unknown.arrayElementVarHandle();
    }
}
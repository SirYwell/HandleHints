import java.lang.foreign.MemoryLayout;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;

class MemoryLayoutStructLayoutInspection {
    void m() {
        ValueLayout vl0 = ValueLayout.JAVA_SHORT;
        PaddingLayout pl0 = MemoryLayout.paddingLayout(6);
        ValueLayout vl1 = ValueLayout.JAVA_LONG;
        // invalid, padding needed for vl1
        StructLayout sl0 = MemoryLayout.structLayout(vl0, <warning descr="Layout requires a byte alignment of 8 but is inserted at an offset of 2.">vl1</warning>);
        StructLayout sl1 = MemoryLayout.structLayout(vl0, <warning descr="Layout requires a byte alignment of 4 but is inserted at an offset of 2.">vl1.withByteAlignment(4)</warning>);
        StructLayout sl2 = MemoryLayout.structLayout(vl0, MemoryLayout.paddingLayout(1), <warning descr="Layout requires a byte alignment of 8 but is inserted at an offset of 3.">vl1</warning>);
        // valid, proper padding
        StructLayout sl3 = MemoryLayout.structLayout(vl0, pl0, vl1);
        StructLayout sl4 = MemoryLayout.structLayout(vl0, vl1.withByteAlignment(1));
        // structs typically have an alignment < byteSize
        // withByteAlignment will fail if smaller than the largest alignment of member
        StructLayout sl5 = MemoryLayout.structLayout(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT);
        StructLayout sl6 = MemoryLayout.structLayout(sl5, <warning descr="Layout requires a byte alignment of 8 but is inserted at an offset of 12.">sl5</warning>);
        StructLayout sl7 = MemoryLayout.structLayout(sl5, sl5.withByteAlignment(<warning descr="This layout cannot be 4 byte aligned due to one of its member layouts.">4</warning>));
    }
}
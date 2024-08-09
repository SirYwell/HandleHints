import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

class MemoryLayoutVarHandle {
    void m(ValueLayout vlU, long any, String unknown) {
        <info descr="sequenceElement()">MemoryLayout.PathElement pe0 = <info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info>;</info>
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh0 = <info descr="(0=MemorySegment)(⊤)">vlU.varHandle()</info>;</info>
        <info descr="[10:int4]">SequenceLayout sl0 = <info descr="[10:int4]">MemoryLayout.sequenceLayout(10, ValueLayout.JAVA_INT)</info>;</info>
        <info descr="[?:int4]">SequenceLayout sl1 = <info descr="[?:int4]">MemoryLayout.sequenceLayout(any, ValueLayout.JAVA_INT)</info>;</info>
        <info descr="[int4]">StructLayout sl2 = <info descr="[int4]">MemoryLayout.structLayout(ValueLayout.JAVA_INT)</info>;</info>
        <info descr="[int4]">UnionLayout ul0 = <info descr="[int4]">MemoryLayout.unionLayout(ValueLayout.JAVA_INT)</info>;</info>
        <info descr="[int4(a)]">UnionLayout ul1 = <info descr="[int4(a)]">MemoryLayout.unionLayout(<info descr="int4(a)">ValueLayout.JAVA_INT.withName("a")</info>)</info>;</info>
        <info descr="[int4(a)|boolean1(b)]">UnionLayout ul2 = <info descr="[int4(a)|boolean1(b)]">MemoryLayout.unionLayout(<info descr="int4(a)">ValueLayout.JAVA_INT.withName("a")</info>, <info descr="boolean1(b)">ValueLayout.JAVA_BOOLEAN.withName("b")</info>)</info>;</info>
        <info descr="[int4(a)|boolean1(a)]">UnionLayout ul3 = <info descr="[int4(a)|boolean1(a)]">MemoryLayout.unionLayout(<info descr="int4(a)">ValueLayout.JAVA_INT.withName("a")</info>, <info descr="boolean1(a)">ValueLayout.JAVA_BOOLEAN.withName("a")</info>)</info>;</info>
        <info descr="[int4({⊤})|boolean1(a)]">UnionLayout ul4 = <info descr="[int4({⊤})|boolean1(a)]">MemoryLayout.unionLayout(<info descr="int4({⊤})">ValueLayout.JAVA_INT.withName(unknown)</info>, <info descr="boolean1(a)">ValueLayout.JAVA_BOOLEAN.withName("a")</info>)</info>;</info>
        <info descr="[int4(a)|boolean1({⊤})]">UnionLayout ul5 = <info descr="[int4(a)|boolean1({⊤})]">MemoryLayout.unionLayout(<info descr="int4(a)">ValueLayout.JAVA_INT.withName("a")</info>, <info descr="boolean1({⊤})">ValueLayout.JAVA_BOOLEAN.withName(unknown)</info>)</info>;</info>
        <info descr="4%[[int4(s00)](s0)[int4(s10)](s1)]">StructLayout sl3 = <info descr="4%[[int4(s00)](s0)[int4(s10)](s1)]">MemoryLayout.structLayout(
                <info descr="[int4(s00)](s0)"><info descr="[int4(s00)]">MemoryLayout.structLayout(<info descr="int4(s00)">ValueLayout.JAVA_INT.withName("s00")</info>)</info>.withName("s0")</info>,
                <info descr="[int4(s10)](s1)"><info descr="[int4(s10)]">MemoryLayout.structLayout(<info descr="int4(s10)">ValueLayout.JAVA_INT.withName("s10")</info>)</info>.withName("s1")</info>
        )</info>;</info>
        <info descr="a?:[int4(a)|boolean1(b)]">AddressLayout al0 = <info descr="a?:[int4(a)|boolean1(b)]">ValueLayout.ADDRESS.withTargetLayout(ul2)</info>;</info>
        // invalid - not a value layout
        <info descr="⊤">VarHandle vh1 = <info descr="⊤"><warning descr="The layout targeted by the given path is not a 'ValueLayout'.">sl0.varHandle</warning>()</info>;</info>
        // valid
        <info descr="(MemorySegment,long)(int)">VarHandle vh2 = <info descr="(MemorySegment,long)(int)">sl0.varHandle(pe0)</info>;</info>
        <info descr="(MemorySegment)(int)">VarHandle vh3 = <info descr="(MemorySegment)(int)">sl0.varHandle(<info descr="sequenceElement(0)">MemoryLayout.PathElement.sequenceElement(0)</info>)</info>;</info>
        // invalid index (negative)
        <info descr="⊤">VarHandle vh4 = <info descr="⊤">sl0.varHandle(<info descr="{⊤}">MemoryLayout.PathElement.sequenceElement(<warning descr="Argument must be >= 0 but is -1.">-1</warning>)</info>)</info>;</info>
        <info descr="⊤">VarHandle vh5 = <info descr="⊤">sl1.varHandle(<info descr="{⊤}">MemoryLayout.PathElement.sequenceElement(<warning descr="Argument must be >= 0 but is -1.">-1</warning>)</info>)</info>;</info>
        // we need to assume the index is valid
        <info descr="(MemorySegment)(int)">VarHandle vh6 = <info descr="(MemorySegment)(int)">sl1.varHandle(<info descr="sequenceElement(12345)">MemoryLayout.PathElement.sequenceElement(12345)</info>)</info>;</info>
        // a sequence element on a value layout is invalid
        <info descr="(0=MemorySegment,1=long)(⊤)">VarHandle vh7 = <info descr="(0=MemorySegment,1=long)(⊤)">sl0.varHandle(<info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info>, <warning descr="A sequence path element cannot be applied to a ValueLayout."><info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info></warning>)</info>;</info>
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh8 = <info descr="(0=MemorySegment)(⊤)">ValueLayout.JAVA_CHAR.varHandle(<warning descr="A sequence path element cannot be applied to a ValueLayout."><info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info></warning>)</info>;</info>
        // same for structs, unions, padding
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh9 = <info descr="(0=MemorySegment)(⊤)">sl2.varHandle(<warning descr="A sequence path element cannot be applied to a StructLayout."><info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info></warning>)</info>;</info>
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh10 = <info descr="(0=MemorySegment)(⊤)">ul0.varHandle(<warning descr="A sequence path element cannot be applied to a UnionLayout."><info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info></warning>)</info>;</info>
        <info descr="⊤">VarHandle vh11 = <info descr="⊤"><warning descr="The layout targeted by the given path is not a 'ValueLayout'."><info descr="x1">MemoryLayout.paddingLayout(1)</info>.varHandle</warning>()</info>;</info>
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh12 = <info descr="(0=MemorySegment)(⊤)"><info descr="x1">MemoryLayout.paddingLayout(1)</info>.varHandle(<warning descr="A sequence path element cannot be applied to a PaddingLayout."><info descr="sequenceElement()">MemoryLayout.PathElement.sequenceElement()</info></warning>)</info>;</info>
        // sequenceElement(start, step)
        // with invalid step
        <info descr="⊤">VarHandle vh13 = <info descr="⊤">sl0.varHandle(<info descr="{⊤}">MemoryLayout.PathElement.sequenceElement(0, <warning descr="Argument must be != 0 but is 0.">0</warning>)</info>)</info>;</info>
        // with invalid start
        <info descr="⊤">VarHandle vh14 = <info descr="⊤">sl0.varHandle(<info descr="{⊤}">MemoryLayout.PathElement.sequenceElement(<warning descr="Argument must be >= 0 but is -1.">-1</warning>, 1)</info>)</info>;</info>
        // valid
        <info descr="(MemorySegment,long)(int)">VarHandle vh15 = <info descr="(MemorySegment,long)(int)">sl0.varHandle(<info descr="sequenceElement(5, -1)">MemoryLayout.PathElement.sequenceElement(5, -1)</info>)</info>;</info>
        // valid group element via index
        <info descr="(MemorySegment)(int)">VarHandle vh16 = <info descr="(MemorySegment)(int)">sl2.varHandle(<info descr="groupElement(0)">MemoryLayout.PathElement.groupElement(0)</info>)</info>;</info>
        // negative group element index
        <info descr="⊤">VarHandle vh17 = <info descr="⊤">sl2.varHandle(<info descr="{⊤}">MemoryLayout.PathElement.groupElement(<warning descr="Argument must be >= 0 but is -1.">-1</warning>)</info>)</info>;</info>
        // out of bounds group element index
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh18 = <info descr="(0=MemorySegment)(⊤)">sl2.varHandle(<warning descr="Group element at index 0 exceeds the number of member layouts 1."><info descr="groupElement(1)">MemoryLayout.PathElement.groupElement(1)</info></warning>)</info>;</info>
        // unknown index
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh19 = <info descr="(0=MemorySegment)(⊤)">sl2.varHandle(<info descr="groupElement(index?)">MemoryLayout.PathElement.groupElement(any)</info>)</info>;</info>
        // valid group element via name
        <info descr="(MemorySegment)(int)">VarHandle vh20 = <info descr="(MemorySegment)(int)">ul1.varHandle(<info descr="groupElement(a)">MemoryLayout.PathElement.groupElement("a")</info>)</info>;</info>
        <info descr="(MemorySegment)(int)">VarHandle vh21 = <info descr="(MemorySegment)(int)">ul2.varHandle(<info descr="groupElement(a)">MemoryLayout.PathElement.groupElement("a")</info>)</info>;</info>
        <info descr="(MemorySegment)(boolean)">VarHandle vh22 = <info descr="(MemorySegment)(boolean)">ul2.varHandle(<info descr="groupElement(b)">MemoryLayout.PathElement.groupElement("b")</info>)</info>;</info>
        // multiple members named 'a'
        <info descr="(MemorySegment)(int)">VarHandle vh23 = <info descr="(MemorySegment)(int)">ul3.varHandle(<info descr="groupElement(a)">MemoryLayout.PathElement.groupElement("a")</info>)</info>;</info>
        // member with unknown name before 'a'
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh24 = <info descr="(0=MemorySegment)(⊤)">ul4.varHandle(<info descr="groupElement(a)">MemoryLayout.PathElement.groupElement("a")</info>)</info>;</info>
        // the other way round we find 'a' directly
        <info descr="(MemorySegment)(int)">VarHandle vh25 = <info descr="(MemorySegment)(int)">ul5.varHandle(<info descr="groupElement(a)">MemoryLayout.PathElement.groupElement("a")</info>)</info>;</info>
        // no member with that name
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh26 = <info descr="(0=MemorySegment)(⊤)">ul2.varHandle(<warning descr="Group layout does not have a member layout with name x."><info descr="groupElement(x)">MemoryLayout.PathElement.groupElement("x")</info></warning>)</info>;</info>
        // with an unknown name, it might exist though
        <info descr="(0=MemorySegment)(⊤)">VarHandle vh27 = <info descr="(0=MemorySegment)(⊤)">ul5.varHandle(<info descr="groupElement(x)">MemoryLayout.PathElement.groupElement("x")</info>)</info>;</info>
        // nested
        <info descr="(MemorySegment)(int)">VarHandle vh28 = <info descr="(MemorySegment)(int)">sl3.varHandle(<info descr="groupElement(1)">MemoryLayout.PathElement.groupElement(1)</info>, <info descr="groupElement(0)">MemoryLayout.PathElement.groupElement(0)</info>)</info>;</info>
        // dereference
        <info descr="(MemorySegment)(int)">VarHandle vh29 = <info descr="(MemorySegment)(int)">al0.varHandle(<info descr="dereferenceElement()">MemoryLayout.PathElement.dereferenceElement()</info>, <info descr="groupElement(a)">MemoryLayout.PathElement.groupElement("a")</info>)</info>;</info>
    }
}
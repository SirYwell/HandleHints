import java.lang.foreign.ValueLayout;

class MemoryLayoutWithByteAlignment {
    void m() {
        <info descr="1%double8">ValueLayout vl0 = <info descr="1%double8">ValueLayout.JAVA_DOUBLE.withByteAlignment(1)</info>;</info>
        <info descr="2%double8">ValueLayout vl1 = <info descr="2%double8">ValueLayout.JAVA_DOUBLE.withByteAlignment(2)</info>;</info>
        <info descr="128%double8">ValueLayout vl3 = <info descr="128%double8">ValueLayout.JAVA_DOUBLE.withByteAlignment(128L)</info>;</info>
        <info descr="⊤">ValueLayout vl4 = <info descr="⊤">ValueLayout.JAVA_DOUBLE.withByteAlignment(-1)</info>;</info>
        <info descr="⊤">ValueLayout vl5 = <info descr="⊤">ValueLayout.JAVA_DOUBLE.withByteAlignment(-1L)</info>;</info>
        <info descr="⊤">ValueLayout vl6 = <info descr="⊤">ValueLayout.JAVA_DOUBLE.withByteAlignment(127L)</info>;</info>
    }
}
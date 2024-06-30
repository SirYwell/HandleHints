import java.lang.foreign.MemoryLayout;
import java.lang.foreign.PaddingLayout;

class MemoryLayoutPaddingLayout {
    void m() {
        <info descr="x1">PaddingLayout pl0 = <info descr="x1">MemoryLayout.paddingLayout(1)</info>;</info>
        <info descr="x10">PaddingLayout pl1 = <info descr="x10">MemoryLayout.paddingLayout(10L)</info>;</info>
        <info descr="⊤">PaddingLayout pl2 = <info descr="⊤">MemoryLayout.paddingLayout(-10L)</info>;</info>
        <info descr="⊤">PaddingLayout pl3 = <info descr="⊤">MemoryLayout.paddingLayout(0L)</info>;</info>
    }
}
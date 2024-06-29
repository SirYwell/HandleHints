import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InitialTypes {

    void m(MethodType mt, boolean b, Inner inner) {
        MethodHandle mh0;
        if (b) {
            <info descr="⊤">mh0 = <info descr="⊤">MethodHandles.empty(mt)</info></info>;
        } else {
            <info descr="()int">mh0 = <info descr="()int">MethodHandles.zero(int.class)</info></info>;
        }
        <info descr="⊤">MethodHandle mh1 = <info descr="⊤">myMethodHandleMethod()</info>;</info>
        <info descr="⊤">MethodHandle mh2 = inner.methodHandle;</info>
        <info descr="⊤">MethodHandle mh3 = create().methodHandle;</info>
    }

    private MethodHandle myMethodHandleMethod() {
        return MethodHandles.zero(Object.class);
    }

    private Inner create() {
        return new Inner();
    }
    static class Inner {
        MethodHandle methodHandle;
    }
}

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InitialTypes {

    void m(MethodType mt, boolean b, Inner inner) {
        MethodHandle mh0;
        if (b) {
            <info descr="TopMethodHandleType">mh0 = <info descr="TopMethodHandleType">MethodHandles.empty(mt)</info></info>;
        } else {
            <info descr="()int">mh0 = <info descr="()int">MethodHandles.zero(int.class)</info></info>;
        }
        <info descr="TopMethodHandleType">MethodHandle mh1 = <info descr="TopMethodHandleType">myMethodHandleMethod()</info>;</info>
        <info descr="TopMethodHandleType">MethodHandle mh2 = inner.methodHandle;</info>
        <info descr="TopMethodHandleType">MethodHandle mh3 = create().methodHandle;</info>
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

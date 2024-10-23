import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

class MethodHandlesTryFinally {
  void tryFinally_() {
    <info descr="()int">MethodHandle mh00 = <info descr="()int">MethodHandles.zero(int.class)</info>;</info>
    <info descr="(Exception,int)double">MethodHandle mh01 = <info descr="(Exception,int)double">MethodHandles.empty(<info descr="(Exception,int)double">methodType(double.class, Exception.class, int.class)</info>)</info>;</info>
    <info descr="(Exception)int">MethodHandle mh02 = <info descr="(Exception)int">MethodHandles.empty(<info descr="(Exception)int">methodType(int.class, Exception.class)</info>)</info>;</info>
    <info descr="(Exception,double)int">MethodHandle mh03 = <info descr="(Exception,double)int">MethodHandles.empty(<info descr="(Exception,double)int">methodType(int.class, Exception.class, double.class)</info>)</info>;</info>
    <info descr="(Exception,int)int">MethodHandle mh04 = <info descr="(Exception,int)int">MethodHandles.empty(<info descr="(Exception,int)int">methodType(int.class, Exception.class, int.class)</info>)</info>;</info>

    // incompatible return types
    <info descr="()int">MethodHandle mh10 = <info descr="()int">MethodHandles.tryFinally(mh00, <warning descr="Return types do not match: int != double">mh01</warning>)</info>;</info>
    // missing parameter at index 1
    <info descr="⊤">MethodHandle mh11 = <info descr="⊤">MethodHandles.tryFinally(mh00, <warning descr="Expected at least 2 parameters, got 1">mh02</warning>)</info>;</info>
    // parameter at index 1 type mismatch
    <info descr="()int">MethodHandle mh12 = <info descr="()int">MethodHandles.tryFinally(mh00, <warning descr="Parameter at index 1 must match the return type (int) but was double">mh03</warning>)</info>;</info>
    // no type issues!
    <info descr="()int">MethodHandle mh13 = <info descr="()int">MethodHandles.tryFinally(mh00, mh04)</info>;</info>
  }
}
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesFilterReturnValue {
  void filterReturnValue_(MethodHandle mh03) {
    <info descr="()void">MethodHandle mh00 = <info descr="()void">MethodHandles.zero(void.class)</info>;</info>
    <info descr="()int">MethodHandle mh01 = <info descr="()int">MethodHandles.zero(int.class)</info>;</info>
    <info descr="()long">MethodHandle mh02 = <info descr="()long">MethodHandles.zero(long.class)</info>;</info>
    <info descr="(int)void">MethodHandle mh05 = <info descr="(int)void">MethodHandles.empty(<info descr="(int)void">MethodType.methodType(void.class, int.class)</info>)</info>;</info>
    <info descr="(int,double)void">MethodHandle mh06 = <info descr="(int,double)void">MethodHandles.empty(<info descr="(int,double)void">MethodType.methodType(void.class, int.class, double.class)</info>)</info>;</info>

    // void target => filter must not have any params
    <info descr="⊤">MethodHandle mh10 = <info descr="⊤">MethodHandles.filterReturnValue(mh00, <warning descr="The target MethodHandle returns ''void'' but the filter expects parameters">mh05</warning>)</info>;</info>
    // non-void target => filter must have matching param
    <info descr="()void">MethodHandle mh11 = <info descr="()void">MethodHandles.filterReturnValue(mh01, mh05)</info>;</info>
    <info descr="⊤">MethodHandle mh12 = <info descr="⊤">MethodHandles.filterReturnValue(mh02, <warning descr="Filter parameter 'int' is incompatible with target return type 'long'">mh05</warning>)</info>;</info>
    // in neither case, more than 1 parameter should be accepted
    <info descr="⊤">MethodHandle mh13 = <info descr="⊤">MethodHandles.filterReturnValue(mh00, <warning descr="The target MethodHandle returns ''void'' but the filter expects parameters">mh06</warning>)</info>;</info>
    <info descr="⊤">MethodHandle mh14 = <info descr="⊤">MethodHandles.filterReturnValue(mh01, <warning descr="Filter must accept at most one parameter">mh06</warning>)</info>;</info>

    // if we don't know the target return type, both 0 and 1 param filters might be fine
    <info descr="⊤">MethodHandle mh20 = <info descr="⊤">MethodHandles.filterReturnValue(mh03, mh05)</info>;</info>
    <info descr="⊤">MethodHandle mh21 = <info descr="⊤">MethodHandles.filterReturnValue(mh03, mh00)</info>;</info>
    <info descr="⊤">MethodHandle mh22 = <info descr="⊤">MethodHandles.filterReturnValue(mh03, <warning descr="Filter must accept at most one parameter">mh06</warning>)</info>;</info>
  }
}
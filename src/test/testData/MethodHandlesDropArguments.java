import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesDropArguments {
  void dropArgs(MethodHandle mh00, int i) {
    // adding an argument at any position on any method handle, we assume it succeeds
    <info descr="(0=int)⊤">MethodHandle mh01 = <info descr="(0=int)⊤">MethodHandles.dropArguments(mh00, 0, int.class)</info>;</info>
    <info descr="(1=int)⊤">MethodHandle mh02 = <info descr="(1=int)⊤">MethodHandles.dropArguments(mh00, 1, int.class)</info>;</info>
    // adding multiple arguments, same situation
    <info descr="(1=int,2=boolean)⊤">MethodHandle mh03 = <info descr="(1=int,2=boolean)⊤">MethodHandles.dropArguments(mh00, 1, int.class, boolean.class)</info>;</info>
    // negative values however are bad
    <info descr="⊤">MethodHandle mh04 = <info descr="⊤">MethodHandles.dropArguments(mh00, <warning descr="Negative index value -1 not allowed.">-1</warning>, int.class, boolean.class)</info>;</info>
    // same as void arguments
    <info descr="(0=⊤)⊤">MethodHandle mh05 = <info descr="(0=⊤)⊤">MethodHandles.dropArguments(mh00, 0, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
    // even if the index is unknown or negative
    <info descr="⊤">MethodHandle mh06 = <info descr="⊤">MethodHandles.dropArguments(mh00, i, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
    <info descr="⊤">MethodHandle mh07 = <info descr="⊤">MethodHandles.dropArguments(mh00, <warning descr="Negative index value -1 not allowed.">-1</warning>, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
    // or if they appear in between something else
    <info descr="(1=int,2=⊤,3=boolean)⊤">MethodHandle mh08 = <info descr="(1=int,2=⊤,3=boolean)⊤">MethodHandles.dropArguments(mh00, 1, int.class, <warning descr="Type must not be void.">void.class</warning>, boolean.class)</info>;</info>

    <info descr="()void">MethodHandle mh10 = <info descr="()void">MethodHandles.zero(void.class)</info>;</info>
    // negative index is also bad for known method handle types
    <info descr="⊤">MethodHandle mh11 = <info descr="⊤">MethodHandles.dropArguments(mh10, <warning descr="Negative index value -1 not allowed.">-1</warning>, int.class)</info>;</info>
    // additionally, we know the upper bound
    <info descr="⊤">MethodHandle mh12 = <info descr="⊤">MethodHandles.dropArguments(mh10, <warning descr="Position argument value 1 is out of bounds [0, 0].">1</warning>, int.class)</info>;</info>
    <info descr="(int)void">MethodHandle mh13 = <info descr="(int)void">MethodHandles.dropArguments(mh10, 0, int.class)</info>;</info>
    <info descr="(int,double)void">MethodHandle mh14 = <info descr="(int,double)void">MethodHandles.dropArguments(mh13, 1, double.class)</info>;</info>
  }
}
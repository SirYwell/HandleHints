import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesDropReturn {
  void droppingReturn(MethodHandle mh10) {
    <info descr="(int)int">MethodHandle mh00 = <info descr="(int)int">MethodHandles.identity(int.class)</info>;</info>
    // dropping makes sense
    <info descr="(int)void">MethodHandle mh01 = <info descr="(int)void">MethodHandles.dropReturn(mh00)</info>;</info>
    // dropping doesn't make sense, but type stays!
    <info descr="(int)void">MethodHandle mh02 = <warning descr="MethodHandle to drop return from already has return type 'void'."><info descr="(int)void">MethodHandles.dropReturn(mh01)</info></warning>;</info>
    // we don't know if dropping makes sense - assume it does
    <info descr="⊤">MethodHandle mh11 = <info descr="⊤">MethodHandles.dropReturn(mh10)</info>;</info>
  }
}
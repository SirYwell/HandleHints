import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

class LookupFindConstructor {
  static {
    try {
      <info descr="(byte[])String">MethodHandle ctor = <info descr="(byte[])String">MethodHandles.lookup().findConstructor(String.class, <info descr="(byte[])void">MethodType.methodType(void.class, byte[].class)</info>)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
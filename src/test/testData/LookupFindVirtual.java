import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

class LookupFindVirtual {
  static {
    try {
      <info descr="(String)int">MethodHandle virtual = <info descr="(String)int">MethodHandles.lookup().findVirtual(String.class, "length", <info descr="()int">MethodType.methodType(int.class)</info>)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
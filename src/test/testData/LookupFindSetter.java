import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

class LookupFindSetter {
  static {
    try {
      <info descr="(String,byte[])void">MethodHandle set = <info descr="(String,byte[])void">MethodHandles.lookup().findSetter(String.class, "value", byte[].class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
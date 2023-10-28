import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

class LookupFindGetter {
  static {
    try {
      <info descr="(String)byte[]">MethodHandle get = <info descr="(String)byte[]">MethodHandles.lookup().findGetter(String.class, "value", byte[].class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
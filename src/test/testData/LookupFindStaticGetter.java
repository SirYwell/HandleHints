import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

class LookupFindStaticGetter {
  static {
    try {
      <info descr="()Boolean">MethodHandle get = <info descr="()Boolean">MethodHandles.lookup().findStaticGetter(Boolean.class, "FALSE", Boolean.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
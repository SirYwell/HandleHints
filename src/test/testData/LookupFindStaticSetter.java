import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

class LookupFindStaticSetter {
  static {
    try {
      <info descr="(Boolean)void">MethodHandle set = <info descr="(Boolean)void">MethodHandles.lookup().findStaticSetter(Boolean.class, "FALSE", Boolean.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
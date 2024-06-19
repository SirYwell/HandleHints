import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

class LookupFindStaticVarHandle {
  static {
    try {
      <info descr="()(Boolean)">VarHandle vh = <info descr="()(Boolean)">MethodHandles.lookup().findStaticVarHandle(Boolean.class, "FALSE", Boolean.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
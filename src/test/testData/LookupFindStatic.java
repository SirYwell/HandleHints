import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

class LookupFindStatic {
  static {
    try {
      <info descr="(long)String">MethodHandle static_ = <info descr="(long)String">MethodHandles.lookup().findStatic(String.class, "valueOf", <info descr="(long)String">MethodType.methodType(String.class, long.class)</info>)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
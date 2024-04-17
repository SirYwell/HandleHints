import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesDropReturn {
  static {
    try {
      <info descr="(int)void">MethodHandle mh = <info descr="(int)void">MethodHandles.dropReturn(<info descr="(int)int">MethodHandles.identity(int.class)</info>)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
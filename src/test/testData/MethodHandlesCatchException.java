import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesCatchException {
  static {
    try {
      <info descr="(int)int">MethodHandle mh = <info descr="(int)int">MethodHandles.catchException(<info descr="(int)int">MethodHandles.identity(int.class)</info>, IllegalArgumentException.class, <info descr="(IllegalArgumentException,int)int">MethodHandles.empty(<info descr="(IllegalArgumentException,int)int">MethodType.methodType(int.class, IllegalArgumentException.class, int.class)</info>)</info>)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
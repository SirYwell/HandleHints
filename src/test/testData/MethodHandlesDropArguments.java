import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesDropArguments {
  static {
    try {
      <info descr="(int,String,boolean)int">MethodHandle mh = <info descr="(int,String,boolean)int">MethodHandles.dropArguments(<info descr="(int)int">MethodHandles.identity(int.class)</info>, 1, String.class, boolean.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
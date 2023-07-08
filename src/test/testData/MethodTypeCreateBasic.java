import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeWrap {
  <info descr="()void">private static final MethodType A = <info descr="()void">MethodType.methodType(void.class)</info>;</info>
  <info descr="(String)int">private static final MethodType B = <info descr="(String)int">MethodType.methodType(int.class, String.class)</info>;</info>
}
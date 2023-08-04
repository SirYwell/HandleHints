import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeWrap {
  <info descr="(int,float)void">private static final MethodType A = <info descr="(int,float)void">MethodType.methodType(void.class, <info descr="(int,float)String">MethodType.methodType(String.class, int.class, float.class)</info>)</info>;</info>
}
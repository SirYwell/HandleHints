import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeCreateWithParameters {
  <info descr="(int,float)void">private static final MethodType A = <info descr="(int,float)void">MethodType.methodType(void.class, <info descr="(int,float)String">MethodType.methodType(String.class, int.class, float.class)</info>)</info>;</info>
  void create() {
    <info descr="(⊤)void">MethodType mt00 = <info descr="(⊤)void">MethodType.methodType(void.class, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
  }
}
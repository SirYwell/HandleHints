import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeAppendParameterTypes {
  <info descr="(boolean,String,double)int">private static final MethodType A = <info descr="(boolean,String,double)int"><info descr="(boolean)int">MethodType.methodType(int.class, boolean.class)</info>.appendParameterTypes(String.class, double.class)</info>;</info>
  void append() {
    <info descr="()void">MethodType mt30 = <info descr="()void">MethodType.methodType(void.class)</info>;</info>
    <info descr="(⊤)void">MethodType mt31 = <info descr="(⊤)void">mt30.appendParameterTypes(<warning descr="Type must not be void.">void.class</warning>)</info>;</info>
    <info descr="(int,⊤,double,⊤)void">MethodType mt32 = <info descr="(int,⊤,double,⊤)void">mt30.appendParameterTypes(int.class, <warning descr="Type must not be void.">void.class</warning>, double.class, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
  }
}
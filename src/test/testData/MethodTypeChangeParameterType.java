import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeChangeParameterType {
  <info descr="(double,int)void">private static final MethodType A = <info descr="(double,int)void"><info descr="(boolean,int)void">MethodType.methodType(void.class, boolean.class, int.class)</info>.changeParameterType(0, double.class)</info>;</info>
  <info descr="(boolean,double)void">private static final MethodType B = <info descr="(boolean,double)void"><info descr="(boolean,int)void">MethodType.methodType(void.class, boolean.class, int.class)</info>.changeParameterType(1, double.class)</info>;</info>
  void change(MethodType unknown) {
    <info descr="(0=⊤)⊤">MethodType mt40 = <info descr="(0=⊤)⊤">unknown.changeParameterType(0, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
  }
}
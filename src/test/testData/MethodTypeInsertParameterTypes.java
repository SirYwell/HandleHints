import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeInsertParameterTypes {
  <info descr="(boolean,int,double,float,long)void">private static final MethodType A = <info descr="(boolean,int,double,float,long)void"><info descr="(boolean,float,long)void">MethodType.methodType(void.class, boolean.class, float.class, long.class)</info>.insertParameterTypes(1, int.class, double.class)</info>;</info>
  void insert() {
    <info descr="()void">MethodType mt20 = <info descr="()void">MethodType.methodType(void.class)</info>;</info>
    <info descr="(⊤)void">MethodType mt21 = <info descr="(⊤)void">mt20.insertParameterTypes(0, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
    <info descr="(int,⊤)void">MethodType mt22 = <info descr="(int,⊤)void">mt20.insertParameterTypes(0, int.class, <warning descr="Type must not be void.">void.class</warning>)</info>;</info>
  }
}
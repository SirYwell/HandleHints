import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeChangeReturnType {
  <info descr="(boolean,int)double">private static final MethodType A = <info descr="(boolean,int)double"><info descr="(boolean,int)void">MethodType.methodType(void.class, boolean.class, int.class)</info>.changeReturnType(double.class)</info>;</info>
  <info descr="()double">private static final MethodType B = <info descr="()double"><info descr="()int">MethodType.methodType(int.class)</info>.changeReturnType(double.class)</info>;</info>
}
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeDropParameterTypes {
  <info descr="(boolean,float,long)void">private static final MethodType A = <info descr="(boolean,float,long)void"><info descr="(boolean,int,double,float,long)void">MethodType.methodType(void.class, boolean.class, int.class, double.class, float.class, long.class)</info>.dropParameterTypes(1, 3)</info>;</info>
}
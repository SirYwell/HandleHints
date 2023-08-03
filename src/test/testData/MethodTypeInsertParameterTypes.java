import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeInsertParameterTypes {
  <info descr="(boolean,int,double,float,long)void">private static final MethodType A = <info descr="(boolean,int,double,float,long)void"><info descr="(boolean,float,long)void">MethodType.methodType(void.class, boolean.class, float.class, long.class)</info>.insertParameterTypes(1, int.class, double.class)</info>;</info>
}
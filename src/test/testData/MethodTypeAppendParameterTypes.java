import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodTypeAppendParameterTypes {
  <info descr="(boolean,String,double)int">private static final MethodType A = <info descr="(boolean,String,double)int"><info descr="(boolean)int">MethodType.methodType(int.class, boolean.class)</info>.appendParameterTypes(String.class, double.class)</info>;</info>
}
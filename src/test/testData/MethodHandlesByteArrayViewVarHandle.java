import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

class MethodHandlesByteArrayViewVarHandle {
  void byteArrayView() {
    <info descr="(byte[],int)(⊤)">VarHandle vh00 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Unexpected class type 'long', must be an array type.">long.class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(long)">VarHandle vh10 = <info descr="(byte[],int)(long)">MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    // if the arg isn't directly a C.class expression, suggest using .arrayType()
    Class<Long> lc = long.class;
    <info descr="(byte[],int)(⊤)">VarHandle vh20 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Unexpected class type 'long', must be an array type.">lc</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(⊤)">VarHandle vh21 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Unexpected class type 'long', must be an array type.">Long.TYPE</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    // component type must be one of short, char, int, long, float, double
    <info descr="(byte[],int)(⊤)">VarHandle vh30 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Expected one of 'short', 'char', 'int', 'long', 'float', 'double', got 'Integer'">Integer[].class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(⊤)">VarHandle vh31 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Expected one of 'short', 'char', 'int', 'long', 'float', 'double', got 'byte'">byte[].class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(⊤)">VarHandle vh32 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Expected one of 'short', 'char', 'int', 'long', 'float', 'double', got 'boolean'">boolean[].class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(short)">VarHandle vh33 = <info descr="(byte[],int)(short)">MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(char)">VarHandle vh34 = <info descr="(byte[],int)(char)">MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(int)">VarHandle vh35 = <info descr="(byte[],int)(int)">MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(long)">VarHandle vh36 = <info descr="(byte[],int)(long)">MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(float)">VarHandle vh37 = <info descr="(byte[],int)(float)">MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(double)">VarHandle vh38 = <info descr="(byte[],int)(double)">MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(byte[],int)(⊤)">VarHandle vh39 = <info descr="(byte[],int)(⊤)">MethodHandles.byteArrayViewVarHandle(<warning descr="Unexpected class type 'void', must be an array type.">void.class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
  }
}

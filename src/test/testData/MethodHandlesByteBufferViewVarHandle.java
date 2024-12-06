import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

class MethodHandlesByteBufferViewVarHandle {
  void byteBufferView() {
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh00 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Unexpected class type 'long', must be an array type.">long.class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(long)">VarHandle vh10 = <info descr="(ByteBuffer,int)(long)">MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    // if the arg isn't directly a C.class expression, suggest using .arrayType()
    Class<Long> lc = long.class;
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh20 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Unexpected class type 'long', must be an array type.">lc</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh21 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Unexpected class type 'long', must be an array type.">Long.TYPE</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    // component type must be one of short, char, int, long, float, double
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh30 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Expected one of 'short', 'char', 'int', 'long', 'float', 'double', got 'Integer'">Integer[].class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh31 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Expected one of 'short', 'char', 'int', 'long', 'float', 'double', got 'byte'">byte[].class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh32 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Expected one of 'short', 'char', 'int', 'long', 'float', 'double', got 'boolean'">boolean[].class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(short)">VarHandle vh33 = <info descr="(ByteBuffer,int)(short)">MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(char)">VarHandle vh34 = <info descr="(ByteBuffer,int)(char)">MethodHandles.byteBufferViewVarHandle(char[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(int)">VarHandle vh35 = <info descr="(ByteBuffer,int)(int)">MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(long)">VarHandle vh36 = <info descr="(ByteBuffer,int)(long)">MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(float)">VarHandle vh37 = <info descr="(ByteBuffer,int)(float)">MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(double)">VarHandle vh38 = <info descr="(ByteBuffer,int)(double)">MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN)</info>;</info>
    <info descr="(ByteBuffer,int)(⊤)">VarHandle vh39 = <info descr="(ByteBuffer,int)(⊤)">MethodHandles.byteBufferViewVarHandle(<warning descr="Unexpected class type 'void', must be an array type.">void.class</warning>, ByteOrder.BIG_ENDIAN)</info>;</info>
  }
}

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

class MethodHandlesArrayElementVarHandle {
  void m() {
    // valid use
    <info descr="(int[],int)(int)">VarHandle vh0 = <info descr="(int[],int)(int)">MethodHandles.arrayElementVarHandle(int[].class)</info>;</info>
    // not an array type
    <info descr="(⊤,int)(⊤)">VarHandle vh1 = <info descr="(⊤,int)(⊤)">MethodHandles.arrayElementVarHandle(int.class)</info>;</info>
    // multiple dimensions
    <info descr="(String[][][],int)(String[][])">VarHandle vh2 = <info descr="(String[][][],int)(String[][])">MethodHandles.arrayElementVarHandle(String[][][].class)</info>;</info>
  }
}
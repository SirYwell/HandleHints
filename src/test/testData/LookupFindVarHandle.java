import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

class LookupFindVarHandle {
  int myField;
  static {
    try {
      // valid
      <info descr="(LookupFindVarHandle)(int)">VarHandle vh0 = <info descr="(LookupFindVarHandle)(int)">MethodHandles.lookup().findVarHandle(LookupFindVarHandle.class, "myField", int.class)</info>;</info>
      // invalid - field cannot be of type void
      <info descr="(LookupFindVarHandle)(⊤)">VarHandle vh1 = <info descr="(LookupFindVarHandle)(⊤)">MethodHandles.lookup().findVarHandle(LookupFindVarHandle.class, "myField", void.class)</info>;</info>
      // invalid - target class cannot be void
      <info descr="(⊤)(int)">VarHandle vh2 = <info descr="(⊤)(int)">MethodHandles.lookup().findVarHandle(void.class, "myField", int.class)</info>;</info>
      // invalid - target class cannot be int
      <info descr="(⊤)(int)">VarHandle vh3 = <info descr="(⊤)(int)">MethodHandles.lookup().findVarHandle(int.class, "myField", int.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
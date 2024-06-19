import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

class LookupFindVarHandle {
  int myField;
  static {
    try {
      <info descr="(LookupFindVarHandle)(int)">VarHandle vh = <info descr="(LookupFindVarHandle)(int)">MethodHandles.lookup().findVarHandle(LookupFindVarHandle.class, "myField", int.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
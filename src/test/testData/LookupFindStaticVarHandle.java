import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

class LookupFindStaticVarHandle {
  static {
    try {
      // valid
      <info descr="()(Boolean)">VarHandle vh0 = <info descr="()(Boolean)">MethodHandles.lookup().findStaticVarHandle(Boolean.class, "FALSE", Boolean.class)</info>;</info>
      // invalid - field cannot be of type void
      <info descr="()(⊤)">VarHandle vh1 = <info descr="()(⊤)">MethodHandles.lookup().findStaticVarHandle(Boolean.class, "FALSE", void.class)</info>;</info>
      // invalid - target class cannot be void
      <info descr="()(Boolean)">VarHandle vh2 = <info descr="()(Boolean)">MethodHandles.lookup().findStaticVarHandle(void.class, "FALSE", Boolean.class)</info>;</info>
      // invalid - target class cannot be int
      <info descr="()(Boolean)">VarHandle vh3 = <info descr="()(Boolean)">MethodHandles.lookup().findStaticVarHandle(int.class, "FALSE", Boolean.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
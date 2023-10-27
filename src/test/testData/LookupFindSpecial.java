import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

class LookupFindSpecial {
  static class Listie extends ArrayList {
    public String toString() { return "[wee Listie]"; }
    static Lookup lookup() { return MethodHandles.lookup(); }
  }
  static {
    try {
      <info descr="(Listie)String">MethodHandle special = <info descr="(Listie)String">MethodHandles.lookup().findSpecial(ArrayList.class, "toString", <info descr="()String">MethodType.methodType(String.class)</info>, Listie.class)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
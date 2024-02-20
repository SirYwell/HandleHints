import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesCollectArguments {
  static {
    try {
      MethodHandle target = MethodHandles.lookup().findStatic(Object.class, "?", MethodType.methodType(void.class, String.class, float.class, Double.class));
      // one in target results in many in adapter
      MethodHandle oneToMany = MethodHandles.lookup().findStatic(Object.class, "?", MethodType.methodType(String.class, double.class, Float.class));
      // one in target results in zero in adapter
      MethodHandle oneToZero = MethodHandles.lookup().findStatic(Object.class, "?", MethodType.methodType(float.class));
      // zero in target results in many in adapter
      MethodHandle zeroToMany = MethodHandles.lookup().findStatic(Object.class, "?", MethodType.methodType(void.class, Integer.class, char.class));
      <caret>
      <info descr="(double,Float,float,Double)void">MethodHandle mh0 = <info descr="(double,Float,float,Double)void">MethodHandles.collectArguments(target, 0, oneToMany)</info>;</info>
      <info descr="(String,Double)void">MethodHandle mh1 = <info descr="(String,Double)void">MethodHandles.collectArguments(target, 1, oneToZero)</info>;</info>
      <info descr="(String,float,Integer,char,Double)void">MethodHandle mh2 = <info descr="(String,float,Integer,char,Double)void">MethodHandles.collectArguments(target, 2, zeroToMany)</info>;</info>
    } catch (Throwable ignored) {}
  }
}
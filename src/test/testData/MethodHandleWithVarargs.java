import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

class MethodHandleWithVarargs {

  void m() {
    <info descr="(int)int">MethodHandle mh00 = <info descr="(int)int">MethodHandles.identity(int.class)</info>;</info>
    // last parameter type mismatch
    <info descr="⊤">MethodHandle mh01 = <info descr="⊤"><warning descr="Last parameter type must be an array but was int.">mh00</warning>.withVarargs(true)</info>;</info>
    // doesn't matter when passing false
    <info descr="(int)int">MethodHandle mh02 = <warning descr="Redundant method invocation."><info descr="(int)int">mh00.withVarargs(false)</info></warning>;</info>

    MethodHandle mh10 = MethodHandles.empty(<error descr="Cannot resolve method 'methodType' in 'MethodHandleWithVarargs'">methodType</error>(void.class));
    // no parameter
    <info descr="⊤">MethodHandle mh11 = <info descr="⊤">mh10.withVarargs(true)</info>;</info>
    // again - doesn't matter when passing false
    <info descr="⊤">MethodHandle mh12 = <info descr="⊤">mh10.withVarargs(false)</info>;</info>

    <info descr="(int[])int[]">MethodHandle mh20 = <info descr="(int[])int[]">MethodHandles.identity(int[].class)</info>;</info>
    // works with actual array parameter!
    <info descr="(int[])int[]">MethodHandle mh21 = <info descr="(int[])int[]">mh20.withVarargs(true)</info>;</info>
    // redundant
    <info descr="(int[])int[]">MethodHandle mh22 = <warning descr="Redundant method invocation."><info descr="(int[])int[]">mh21.withVarargs(true)</info></warning>;</info>
    <info descr="(int[])int[]">MethodHandle mh23 = <warning descr="Redundant method invocation."><info descr="(int[])int[]">mh20.withVarargs(false)</info></warning>;</info>
  }
}

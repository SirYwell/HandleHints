import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

class VarHandleWithInvokeBehavior {

  void invokeBehavior() {
    <info descr="(double[],int)(double)">VarHandle vh0 = <info descr="(double[],int)(double)">MethodHandles.arrayElementVarHandle(double[].class)</info>;</info>
    // redundant - vh0 has invoke behavior
    <info descr="(double[],int)(double)">VarHandle vh1 = <warning descr="VarHandle already has 'invoke' behavior"><info descr="(double[],int)(double)">vh0.withInvokeBehavior()</info></warning>;</info>
    // not redundant
    <info descr="(double[],int)(double)">VarHandle vh2 = <info descr="(double[],int)(double)">vh0.withInvokeExactBehavior()</info>;</info>
    // redundant - vh2 has invokeExact behavior
    <info descr="(double[],int)(double)">VarHandle vh3 = <warning descr="VarHandle already has 'invokeExact' behavior"><info descr="(double[],int)(double)">vh2.withInvokeExactBehavior()</info></warning>;</info>
    // not redundant
    <info descr="(double[],int)(double)">VarHandle vh4 = <info descr="(double[],int)(double)">vh2.withInvokeBehavior()</info>;</info>
  }
}

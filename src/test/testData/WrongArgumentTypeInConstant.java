import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

class WrongArgumentTypeInConstant {
  private static final MethodHandle a = MethodHandles.constant(byte.class, <warning descr="Expected parameter of type byte but got int.">0</warning>);
  private static final MethodHandle b = MethodHandles.constant(int.class, <warning descr="Expected parameter of type int but got String.">"Hello World"</warning>); // CCE
  private static final MethodHandle c = MethodHandles.constant(byte.class, <warning descr="Expected parameter of type byte but got int.">0</warning>); // CCE
  private static final MethodHandle d = MethodHandles.constant(byte.class, (byte) 1); // fine
  private static final MethodHandle e = MethodHandles.constant(int.class, (byte) 1); // fine
  private static final MethodHandle f = MethodHandles.constant(CharSequence.class, "Hello"); // fine
  private static final MethodHandle g = MethodHandles.constant(String.class, (CharSequence) "a"); // fine
  private static final MethodHandle h = MethodHandles.constant(String.class, <warning descr="Expected parameter of type String but got Integer.">Integer.valueOf(1)</warning>); // CCE
}
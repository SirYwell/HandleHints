import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

class WrongArgumentTypeInConstant {
  private static final MethodHandle a = <warning descr="Expected parameter of type byte but got int.">MethodHandles.constant</warning>(byte.class, 0);
  private static final MethodHandle b = <warning descr="Expected parameter of type int but got String.">MethodHandles.constant</warning>(int.class, "Hello World"); // CCE
  private static final MethodHandle c = <warning descr="Expected parameter of type byte but got int.">MethodHandles.constant</warning>(byte.class, 0); // CCE
  private static final MethodHandle d = MethodHandles.constant(byte.class, (byte) 0); // fine
  private static final MethodHandle e = MethodHandles.constant(int.class, (byte) 0); // fine
  private static final MethodHandle f = MethodHandles.constant(CharSequence.class, "Hello"); // fine
  private static final MethodHandle g = MethodHandles.constant(String.class, (CharSequence) "a"); // fine
  private static final MethodHandle h = <warning descr="Expected parameter of type String but got Integer.">MethodHandles.constant</warning>(String.class, Integer.valueOf(1)); // CCE
}
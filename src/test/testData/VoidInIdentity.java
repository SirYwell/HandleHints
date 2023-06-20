import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

class VoidInIdentity {
  private static final MethodHandle A = MethodHandles.identity(<warning descr="Parameter must not be of type void.">void.class</warning>);
}
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

class VoidInIdentity {
  private static final MethodHandle A = MethodHandles.identity(<warning descr="Type must not be void.">void.class</warning>);
}
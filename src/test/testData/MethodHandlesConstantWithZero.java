import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class MethodHandlesConstantWithZero {
  void m() {
    MethodHandle mh00 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(int.class, 0)</warning>;
    MethodHandle mh01 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(int.class, '\0')</warning>;
    MethodHandle mh02 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(String.class, null)</warning>;
    MethodHandle mh03 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(Integer.class, null)</warning>;
    MethodHandle mh04 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(boolean.class, false)</warning>;
    MethodHandle mh05 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(char.class, (char) ('a' - 'a'))</warning>;
    MethodHandle mh06 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(long.class, 0)</warning>;
    MethodHandle mh07 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(long.class, 0L)</warning>;
    MethodHandle mh08 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(byte.class, (byte) 0)</warning>;
    MethodHandle mh09 = <warning descr="Usage of 'constant' with zero value can be replaced with 'zero'.">MethodHandles.constant(short.class, (short) 0)</warning>;
  }
}
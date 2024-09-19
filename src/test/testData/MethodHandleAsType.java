import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodType.methodType;

class MethodHandleAsType {

  void asType(Class<?> ca, Class<?> cb) {
    // widening conversions for primitives
    <info descr="(int)int">MethodHandle mh00 = <info descr="(int)int">MethodHandles.identity(int.class)</info>;</info>
    <info descr="(int)long">MethodHandle mh01 = <info descr="(int)long">mh00.asType(<info descr="(int)long">methodType(long.class, int.class)</info>)</info>;</info>
    <info descr="(char)long">MethodHandle mh02 = <info descr="(char)long">mh00.asType(<info descr="(char)long">methodType(long.class, char.class)</info>)</info>;</info>
    // narrowing is not allowed
    <info descr="(⊤)⊤">MethodHandle mh03 = <info descr="(⊤)⊤"><warning descr="Incompatible primitive conversion: int to long">mh00</warning>.asType(<info descr="(long)char">methodType(char.class, long.class)</info>)</info>;</info>
    // int cannot be cast to Long
    <info descr="(int)⊤">MethodHandle mh04 = <info descr="(int)⊤"><warning descr="Incompatible boxed assignment: int to Long.">mh00</warning>.asType(<info descr="(int)Long">methodType(Long.class, int.class)</info>)</info>;</info>

    // reference conversions are just like casts, and might work on null values
    <info descr="(String)String">MethodHandle mh10 = <info descr="(String)String">MethodHandles.identity(String.class)</info>;</info>
    <info descr="(Integer)Double">MethodHandle mh11 = <info descr="(Integer)Double">mh10.asType(<info descr="(Integer)Double">methodType(Double.class, Integer.class)</info>)</info>;</info>

    // no-op
    <info descr="(int)int">MethodHandle mh20 = <warning descr="Call to 'asType' is redundant as the MethodHandle already has that type"><info descr="(int)int">mh00.asType(<info descr="(int)int">mh00.type()</info>)</info></warning>;</info>
    <info descr="(int)int">MethodHandle mh21 = <warning descr="Call to 'asType' is redundant as the MethodHandle already has that type"><info descr="(int)int">mh00.asType(<info descr="(int)int">methodType(int.class, int.class)</info>)</info></warning>;</info>

    // potentially no no-op
    <info descr="(⊤)⊤">MethodHandle mh30 = <info descr="(⊤)⊤">MethodHandles.identity(ca)</info>;</info>
    <info descr="(⊤)⊤">MethodHandle mh31 = <info descr="(⊤)⊤">mh30.asType(<info descr="(⊤)⊤">methodType(cb, cb)</info>)</info>;</info>

    <info descr="(Integer)Integer">MethodHandle mh40 = <info descr="(Integer)Integer">MethodHandles.identity(Integer.class)</info>;</info>
    // basic boxing/unboxing conversions
    <info descr="(int)int">MethodHandle mh41 = <info descr="(int)int">mh40.asType(<info descr="(int)int">methodType(int.class, int.class)</info>)</info>;</info>
    // var x = (long) (Integer) 1, allowed
    <info descr="(int)long">MethodHandle mh42 = <info descr="(int)long">mh40.asType(<info descr="(int)long">methodType(long.class, int.class)</info>)</info>;</info>
    // var x = (byte) (Integer) 1, not allowed
    <info descr="(int)⊤">MethodHandle mh43 = <info descr="(int)⊤"><warning descr="Cannot cast reference type Integer to primitive type byte">mh40</warning>.asType(<info descr="(int)byte">methodType(byte.class, int.class)</info>)</info>;</info>

    // non-wrapper types may be cast to specific primitive types
    <info descr="(Number)Number">MethodHandle mh50 = <info descr="(Number)Number">MethodHandles.identity(Number.class)</info>;</info>
    // var x = (int) (Number) 1, allowed
    <info descr="(Number)int">MethodHandle mh51 = <info descr="(Number)int">mh50.asType(<info descr="(Number)int">methodType(int.class, Number.class)</info>)</info>;</info>
    // var x = (char) (Number) 1, not allowed
    <info descr="(Number)⊤">MethodHandle mh52 = <info descr="(Number)⊤"><warning descr="Cannot cast reference type Number to primitive type char">mh50</warning>.asType(<info descr="(Number)char">methodType(char.class, Number.class)</info>)</info>;</info>

    <info descr="()void">MethodHandle mh60 = <info descr="()void">MethodHandles.empty(<info descr="()void">methodType(void.class)</info>)</info>;</info>
    // "If the return type T0 is void and T1 a primitive, a zero value is introduced."
    <info descr="()int">MethodHandle mh61 = <info descr="()int">mh60.asType(<info descr="()int">methodType(int.class)</info>)</info>;</info>
    // "If the return type T0 is void and T1 a reference, a null value is introduced."
    <info descr="()String">MethodHandle mh62 = <info descr="()String">mh60.asType(<info descr="()String">methodType(String.class)</info>)</info>;</info>
    // but the inspection is still working
    <info descr="()void">MethodHandle mh63 = <warning descr="Call to 'asType' is redundant as the MethodHandle already has that type"><info descr="()void">mh60.asType(<info descr="()void">methodType(void.class)</info>)</info></warning>;</info>

    <info descr="()String">MethodHandle mh70 = <info descr="()String">MethodHandles.empty(<info descr="()String">methodType(String.class)</info>)</info>;</info>
    // "If the return type T1 is marked as void, any returned value is discarded"
    <info descr="()void">MethodHandle mh71 = <info descr="()void">mh70.asType(<info descr="()void">methodType(void.class)</info>)</info>;</info>
  }
}

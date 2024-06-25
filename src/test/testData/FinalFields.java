import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class FinalFields {

    <info descr="(double)⊤">private static final MethodType METHOD_TYPE;</info>

    static {
        if (Math.random() > 0.5) {
            <info descr="(double)int">METHOD_TYPE = <info descr="(double)int">MethodType.methodType(int.class, double.class)</info></info>;
            <info descr="(double)int">MethodHandle ms = <info descr="(double)int">MethodHandles.empty(METHOD_TYPE)</info>;</info>
        } else {
            <info descr="(double)float">METHOD_TYPE = <info descr="(double)float">MethodType.methodType(float.class, double.class)</info></info>;
            <info descr="(double)float">MethodHandle ms = <info descr="(double)float">MethodHandles.empty(FinalFields.METHOD_TYPE)</info>;</info>
        }
        <info descr="(double)⊤">MethodHandle ms = <info descr="(double)⊤">MethodHandles.empty(METHOD_TYPE)</info>;</info>
    }

    <info descr="(⊤)int">private final MethodType methodType;</info>

    FinalFields() {
        <info descr="(String)int">this.methodType = <info descr="(String)int">MethodType.methodType(int.class, String.class)</info></info>;
        <info descr="(String)int">MethodHandle mn = <info descr="(String)int">MethodHandles.empty(this.methodType)</info>;</info>
    }
    FinalFields(FinalFields other) {
        <info descr="(CharSequence)int">this.methodType = <info descr="(CharSequence)int">MethodType.methodType(int.class, CharSequence.class)</info></info>;
        <info descr="(CharSequence)int">MethodHandle mn = <info descr="(CharSequence)int">MethodHandles.empty(this.methodType)</info>;</info>
        <info descr="(CharSequence)int">MethodHandle mni = <info descr="(CharSequence)int">MethodHandles.empty(methodType)</info>;</info>
        <info descr="TopMethodHandleType">MethodHandle o = <info descr="TopMethodHandleType">MethodHandles.empty(other.methodType)</info>;</info>
        if (Math.random() < 0.5) {
            other = this;
            <info descr="TopMethodHandleType">MethodHandle oy = <info descr="TopMethodHandleType">MethodHandles.empty(other.methodType)</info>;</info>
        }
        <info descr="TopMethodHandleType">MethodHandle om = <info descr="TopMethodHandleType">MethodHandles.empty(other.methodType)</info>;</info>
    }

    void m() {
        <info descr="(double)⊤">MethodHandle ms = <info descr="(double)⊤">MethodHandles.empty(METHOD_TYPE)</info>;</info>

        <info descr="(⊤)int">MethodHandle mn = <info descr="(⊤)int">MethodHandles.empty(this.methodType)</info>;</info>
    }
}

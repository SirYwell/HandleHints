import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

class MethodHandlesTableSwitch {
    void switching() {
        // missing cases
        <info descr="(int)int">MethodHandle ts0 = <info descr="(int)int">tableSwitch(<info descr="(int)int">identity(int.class)</info>)</info>;</info>
        // that works
        <info descr="(int)int">MethodHandle ts1 = <info descr="(int)int">tableSwitch(<info descr="(int)int">identity(int.class)</info>, <info descr="(int)int">identity(int.class)</info>)</info>;</info>
        // no leading int param
        <info descr="⊤">MethodHandle ts2 = <info descr="⊤">tableSwitch(<info descr="(String)String">identity(String.class)</info>, <info descr="(String)String">identity(String.class)</info>)</info>;</info>
        // parameter list differs in length
        <info descr="⊤">MethodHandle ts3 = <info descr="⊤">tableSwitch(<info descr="(int)void">empty(<info descr="(int)void">methodType(void.class, int.class)</info>)</info>, <info descr="(int,String)void">empty(<info descr="(int,String)void">methodType(void.class, int.class, String.class)</info>)</info>)</info>;</info>
        // parameter list is empty
        <info descr="⊤">MethodHandle ts4 = <info descr="⊤">tableSwitch(<info descr="()void">empty(<info descr="()void">methodType(void.class)</info>)</info>, <info descr="()void">empty(<info descr="()void">methodType(void.class)</info>)</info>)</info>;</info>
        // parameter list differs in types
        <info descr="⊤">MethodHandle ts5 = <info descr="⊤">tableSwitch(<info descr="(int,CharSequence)void">empty(<info descr="(int,CharSequence)void">methodType(void.class, int.class, CharSequence.class)</info>)</info>, <info descr="(int,String)void">empty(<info descr="(int,String)void">methodType(void.class, int.class, String.class)</info>)</info>)</info>;</info>
        // return types differ
        <info descr="⊤">MethodHandle ts6 = <info descr="⊤">tableSwitch(<info descr="(int)String">empty(<info descr="(int)String">methodType(String.class, int.class)</info>)</info>, <info descr="(int)CharSequence">empty(<info descr="(int)CharSequence">methodType(CharSequence.class, int.class)</info>)</info>)</info>;</info>
        // that works but is more complex
        <info descr="(int,String)int">MethodHandle ts7 = <info descr="(int,String)int">tableSwitch(
                <info descr="(int,String)int">empty(<info descr="(int,String)int">methodType(int.class, int.class, String.class)</info>)</info>,
                <info descr="(int,String)int">empty(<info descr="(int,String)int">methodType(int.class, int.class, String.class)</info>)</info>,
                <info descr="(int,String)int">empty(<info descr="(int,String)int">methodType(int.class, int.class, String.class)</info>)</info>
        )</info>;</info>
    }
}

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodType;

class FunctionDescriptorMethods {

    void m() {
        <info descr="([float4])int4">FunctionDescriptor fd00 = <info descr="([float4])int4">FunctionDescriptor.of(ValueLayout.JAVA_INT, <info descr="[float4]">MemoryLayout.structLayout(ValueLayout.JAVA_FLOAT)</info>)</info>;</info>
        <info descr="([float4])">FunctionDescriptor fd01 = <info descr="([float4])">fd00.dropReturnLayout()</info>;</info>
        <info descr="([float4]double8)int4">FunctionDescriptor fd02 = <info descr="([float4]double8)int4">fd00.appendArgumentLayouts(ValueLayout.JAVA_DOUBLE)</info>;</info>
        <info descr="([float4][boolean1|byte1]double8)int4">FunctionDescriptor fd03 = <info descr="([float4][boolean1|byte1]double8)int4">fd02.insertArgumentLayouts(1, <info descr="[boolean1|byte1]">MemoryLayout.unionLayout(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BYTE)</info>)</info>;</info>
        <info descr="([float4])char2">FunctionDescriptor fd04 = <info descr="([float4])char2">fd00.changeReturnLayout(ValueLayout.JAVA_CHAR)</info>;</info>
        <info descr="(MemorySegment)int">MethodType mt00 = <info descr="(MemorySegment)int">fd00.toMethodType()</info>;</info>
        <info descr="(MemorySegment,MemorySegment,double)int">MethodType mt01 = <info descr="(MemorySegment,MemorySegment,double)int">fd03.toMethodType()</info>;</info>
    }
}
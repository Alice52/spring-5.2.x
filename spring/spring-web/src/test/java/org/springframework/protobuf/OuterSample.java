// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sample.proto

package org.springframework.protobuf;

@SuppressWarnings("deprecation")
public class OuterSample {
    static com.google.protobuf.Descriptors.Descriptor internal_static_Msg_descriptor;

    static com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_Msg_fieldAccessorTable;

    static com.google.protobuf.Descriptors.Descriptor internal_static_SecondMsg_descriptor;

    static com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_SecondMsg_fieldAccessorTable;

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    private OuterSample() {}

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {}

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    static {
        java.lang.String[] descriptorData = {
            "\n\014sample.proto\",\n\003Msg\022\013\n\003foo\030\001 \001(\t\022\030\n\004bl"
                    + "ah\030\002 \001(\0132\n.SecondMsg\"\031\n\tSecondMsg\022\014\n\004bla"
                    + "h\030\001 \001(\005B-\n\034org.springframework.protobufB"
                    + "\013OuterSampleP\001"
        };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                    public com.google.protobuf.ExtensionRegistry assignDescriptors(
                            com.google.protobuf.Descriptors.FileDescriptor root) {
                        descriptor = root;
                        internal_static_Msg_descriptor = getDescriptor().getMessageTypes().get(0);
                        internal_static_Msg_fieldAccessorTable =
                                new com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                                        internal_static_Msg_descriptor,
                                        new java.lang.String[] {
                                            "Foo", "Blah",
                                        });
                        internal_static_SecondMsg_descriptor =
                                getDescriptor().getMessageTypes().get(1);
                        internal_static_SecondMsg_fieldAccessorTable =
                                new com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                                        internal_static_SecondMsg_descriptor,
                                        new java.lang.String[] {
                                            "Blah",
                                        });
                        return null;
                    }
                };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
                descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {}, assigner);
    }

    // @@protoc_insertion_point(outer_class_scope)
}
